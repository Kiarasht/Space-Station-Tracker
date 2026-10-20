import CoreLocation
import EventKit
import EventKitUI
import Foundation
import ISSTrackerShared
import MapKit
import StoreKit
import SwiftUI
import UIKit
@preconcurrency import UserNotifications

@MainActor
final class AppModel: NSObject, ObservableObject, @preconcurrency CLLocationManagerDelegate {
    @Published private(set) var appOpenCount = 0
    @Published private(set) var isAdFree = false
    let ads: AdsService

    private let policyBridge: IosAppPolicyBridge
    private let composeBridge: IosComposeAppBridge
    private let passAlertBridge = IosPassAlertBridge()
    private let locationManager = CLLocationManager()
    private let geocoder = CLGeocoder()
    private let eventStore = EKEventStore()
    private let eventEditorDelegate = PassEventEditorDelegate()
    private let adRemovalPurchase = AdRemovalPurchaseService()
    private let analytics = AppAnalyticsService.shared
    private let settingsDefaults = UserDefaults(suiteName: "settings") ?? .standard
    private var hasStarted = false
    private var hasRequestedAdsConfiguration = false
    private var isLocationLookupInProgress = false
    private var isPrivacyOptionsRequired = false
    private var canRequestAds = false
    private var locationNameTask: Task<Void, Never>?

    override init() {
        PassAlertAltitudeStorage.migrateToMetersIfNeeded(
            in: UserDefaults(suiteName: "settings") ?? .standard
        )
        let policyBridge = IosAppPolicyBridge(
            appOpenSuiteName: "app_open_ads",
            reviewSuiteName: "app_rating"
        )
        self.policyBridge = policyBridge
        self.composeBridge = IosComposeAppBridge(
            versionText: "Version \(appVersionName) (\(appVersionCode))"
        )
        ads = AdsService(
            appOpenStartThreshold: Int(policyBridge.appOpenStartThreshold()),
            appOpenExpiration: TimeInterval(policyBridge.appOpenExpirationMillis()) / 1_000
        )
        super.init()
        settingsDefaults.removeObject(forKey: "ad_free_expiry")

        ads.onConsentStateChanged = { [weak self] canRequestAds, privacyOptionsRequired in
            Task { @MainActor in
                self?.canRequestAds = canRequestAds
                self?.isPrivacyOptionsRequired = privacyOptionsRequired
                self?.refreshSettingsPlatformState()
            }
        }
        adRemovalPurchase.onStateChanged = { [weak self] state in
            self?.refreshSettingsPlatformState()
            if state.isEntitlementCheckComplete && self?.hasRequestedAdsConfiguration == false {
                self?.hasRequestedAdsConfiguration = true
                self?.ads.configure()
            }
        }
        adRemovalPurchase.onEntitlementChanged = { [weak self] _ in
            self?.refreshAdFreeState()
            self?.refreshSettingsPlatformState()
        }
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyKilometer
        configureComposeBridge()
        PassNotificationDelegate.shared.setOpenPassesHandler { [weak self] in
            self?.composeBridge.openPassesTab()
        }
    }

    func makeComposeViewController() -> UIViewController {
        let controller = composeBridge.createViewController()
        controller.view.backgroundColor = .systemBackground
        return controller
    }

    func start() {
        guard !hasStarted else { return }
        hasStarted = true
        refreshAdFreeState()
        refreshSettingsPlatformState()
        adRemovalPurchase.configure()
        recordForegroundOpen()
    }

    func appDidReturnFromBackground() {
        guard hasStarted else { return }
        refreshAdFreeState()
        refreshSettingsPlatformState()
        adRemovalPurchase.refreshEntitlements()
        recordForegroundOpen()
    }

    func setAppActive(_ active: Bool) {
        composeBridge.setAppActive(active: active)
    }

    func applicationDidBecomeActive() {
        // Also covers returning from system permission dialogs (not just background).
        guard hasRequestedAdsConfiguration else { return }
        ads.configure()
    }

    func recordMeaningfulInteraction() -> Bool {
        let nowMillis = Self.nowMillis
        policyBridge.recordMeaningfulInteraction(nowMillis: nowMillis)
        return policyBridge.isReviewPromptEligible(nowMillis: nowMillis)
    }

    func markReviewFlowCompleted() {
        policyBridge.markReviewFlowCompleted(nowMillis: Self.nowMillis)
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            manager.requestLocation()
        case .denied, .restricted:
            isLocationLookupInProgress = false
            composeBridge.setAutomaticPassAlertsEnabled(enabled: false)
            cancelAutomaticPassNotifications()
            PassBackgroundRefreshCoordinator.shared.cancel()
            composeBridge.setPassesLocationError(
                message: String(localized: "location.error.unavailable")
            )
            refreshSettingsPlatformState()
        default:
            break
        }
    }

    func locationManager(
        _ manager: CLLocationManager,
        didUpdateLocations locations: [CLLocation]
    ) {
        guard let location = locations.last else { return }
        locationNameTask?.cancel()
        geocoder.cancelGeocode()
        locationNameTask = Task { [weak self] in
            guard let self else { return }
            let placemark = try? await geocoder.reverseGeocodeLocation(location).first
            guard !Task.isCancelled else { return }
            applyResolvedLocation(
                location,
                locationName: Self.locationName(from: placemark)
            )
        }
    }

    private func applyResolvedLocation(
        _ location: CLLocation,
        locationName: String
    ) {
        isLocationLookupInProgress = false
        refreshSettingsPlatformState()
        composeBridge.updateLocation(
            latitude: location.coordinate.latitude,
            longitude: location.coordinate.longitude,
            altitude: max(0, location.altitude),
            locationName: locationName
        )
        settingsDefaults.set(location.coordinate.latitude, forKey: "auto_pass_alert_latitude")
        settingsDefaults.set(location.coordinate.longitude, forKey: "auto_pass_alert_longitude")
        PassAlertAltitudeStorage.storeMeters(location.altitude, in: settingsDefaults)
        settingsDefaults.set(locationName, forKey: "auto_pass_alert_location_name")
        if settingsDefaults.bool(forKey: "auto_pass_alerts_enabled") {
            refreshAutomaticPassAlerts(location: location)
            PassBackgroundRefreshCoordinator.shared.schedule()
        }
    }

    private static func locationName(from placemark: CLPlacemark?) -> String {
        guard let placemark else { return String(localized: "location.current") }

        let nearbyPlace = [
            placemark.locality,
            placemark.subLocality,
            placemark.subAdministrativeArea
        ].compactMap { value in
            value?.trimmingCharacters(in: .whitespacesAndNewlines)
        }.first { !$0.isEmpty }
        let region = [placemark.administrativeArea, placemark.country]
            .compactMap { value in
                value?.trimmingCharacters(in: .whitespacesAndNewlines)
            }
            .first { !$0.isEmpty }

        if let nearbyPlace, let region,
           nearbyPlace.caseInsensitiveCompare(region) != .orderedSame {
            return "\(nearbyPlace), \(region)"
        }
        return nearbyPlace ?? region ?? String(localized: "location.current")
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        isLocationLookupInProgress = false
        composeBridge.setPassesLocationError(
            message: String(localized: "location.error.failed")
        )
        refreshSettingsPlatformState()
    }

    private func configureComposeBridge() {
        composeBridge.registerMapViewFactory(
            factory: {
                NativeIssMapView()
            },
            updater: { [weak self] view, current, orbit, mapType in
                guard let mapView = view as? NativeIssMapView else { return }
                let orbitPoints: [IssMapPoint] = self?.cast(orbit) ?? []
                mapView.update(current: current, orbit: orbitPoints, mapType: mapType)
            }
        )
        composeBridge.registerMapInsetUpdater { view, bottomInset in
            (view as? NativeIssMapView)?.setBottomInset(CGFloat(bottomInset))
        }
        composeBridge.registerMapSatelliteUpdater { [weak self] view, satellites, track, trackColor, onSelect in
            guard let mapView = view as? NativeIssMapView else { return }
            let markers: [SatelliteMapMarker] = self?.cast(satellites) ?? []
            let trackPoints: [IssMapPoint] = self?.cast(track) ?? []
            mapView.updateSatellites(
                markers: markers,
                track: trackPoints,
                trackColor: trackColor.int64Value,
                onSelect: { id in _ = onSelect(id) }
            )
        }
        composeBridge.registerNativeAdViewFactory { [weak self] slotId in
            self?.ads.makeNativeAdView(
                slotId: slotId,
                rootViewController: Self.topViewController()
            ) ?? UIView()
        }
        composeBridge.registerBannerAdViewFactory { [weak self] in
            self?.ads.makeBannerAdView(
                rootViewController: Self.topViewController()
            ) ?? UIView()
        }
        composeBridge.setActionHandler { [weak self] actionId, parameter in
            Task { @MainActor in
                self?.handleComposeAction(actionId: actionId, parameter: parameter)
            }
        }
    }

    private func handleComposeAction(actionId: String, parameter: String?) {
        if actionId == "screen_view" {
            analytics.trackScreen(parameter)
            return
        }
        if actionId == "map_type_changed" ||
            actionId == "show_orbit_changed" ||
            actionId == "theme_changed" {
            analytics.trackSetting(actionId, value: parameter ?? "unknown")
        } else {
            analytics.trackInteraction(actionId)
        }
        switch actionId {
        case "open_url":
            guard let parameter, let url = URL(string: parameter) else { return }
            UIApplication.shared.open(url)
        case "request_location":
            requestLocation()
        case "meaningful_interaction":
            if recordMeaningfulInteraction() {
                requestReview()
                markReviewFlowCompleted()
            }
        case "schedule_pass_notification":
            guard let pass = decodePass(parameter) else { return }
            Task {
                guard await requestNotificationAuthorization() else {
                    openNotificationSettings()
                    return
                }
                let notificationTimes = pass.notificationTimes?.isEmpty == false
                    ? pass.notificationTimes!
                    : ["10 minutes before"]
                let value = passAlertBridge.createNotificationSchedules(
                    startTimeMillis: pass.startTimeMillis,
                    notificationTimes: notificationTimes,
                    nowMillis: Self.nowMillis
                )
                let schedules: [IosNotificationSchedule] = cast(value)
                for schedule in schedules {
                    await PassNotificationScheduler.addManual(
                        PassNotificationRequest(
                            startTimeMillis: pass.startTimeMillis,
                            triggerTimeMillis: schedule.triggerTimeMillis,
                            notificationTime: schedule.notificationTime,
                            identifierSuffix: schedule.identifierSuffix,
                            startAzimuthCompass: pass.startAzimuthCompass,
                            maxElevation: pass.maxElevation
                        )
                    )
                }
            }
        case "add_pass_to_calendar":
            guard let pass = decodePass(parameter) else { return }
            addToCalendar(pass)
        case "share_pass":
            guard let pass = decodePass(parameter) else { return }
            share(pass)
        case "enable_automatic_pass_alerts":
            Task {
                let granted = await requestNotificationAuthorization()
                if granted {
                    requestLocation()
                } else {
                    composeBridge.setAutomaticPassAlertsEnabled(enabled: false)
                    refreshSettingsPlatformState()
                    openNotificationSettings()
                }
            }
        case "disable_automatic_pass_alerts":
            cancelAutomaticPassNotifications()
            PassBackgroundRefreshCoordinator.shared.cancel()
        case "open_background_settings":
            openAppSettings()
        case "privacy_choices":
            ads.showPrivacyOptions(rootViewController: Self.topViewController())
        case "refresh_automatic_pass_alerts":
            refreshAutomaticPassAlertsFromSavedLocation()
        case "purchase_ad_removal":
            adRemovalPurchase.purchase()
        case "restore_ad_removal":
            adRemovalPurchase.restore()
        case "contact_support":
            contactSupport(versionText: parameter)
        case "rate_app":
            rateApp()
        case "share_app":
            shareApp()
        default:
            break
        }
    }

    private func decodePass(_ parameter: String?) -> PassPayload? {
        guard let parameter, let data = parameter.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(PassPayload.self, from: data)
    }

    private func refreshAutomaticPassAlerts(location: CLLocation) {
        passAlertBridge.loadAutomaticSchedules(
            latitude: location.coordinate.latitude,
            longitude: location.coordinate.longitude,
            altitude: max(0, location.altitude),
            nowMillis: Self.nowMillis,
            onSuccess: { [weak self] value in
                Task { @MainActor in
                    guard let self else { return }
                    let passes: [IosPassAlertPayload] = self.cast(value)
                    guard await self.requestNotificationAuthorization() else { return }
                    await PassNotificationScheduler.replaceAutomatic(
                        with: passes.map { pass in
                            PassNotificationRequest(
                                startTimeMillis: pass.startTimeMillis,
                                triggerTimeMillis: pass.triggerTimeMillis,
                                notificationTime: pass.notificationTime,
                                identifierSuffix: pass.identifierSuffix,
                                startAzimuthCompass: pass.startAzimuthCompass,
                                maxElevation: pass.maxElevation
                            )
                        }
                    )
                }
            },
            onFailure: { _ in }
        )
    }

    private func refreshAutomaticPassAlertsFromSavedLocation() {
        guard settingsDefaults.bool(forKey: "auto_pass_alerts_enabled"),
              settingsDefaults.object(forKey: "auto_pass_alert_latitude") != nil,
              settingsDefaults.object(forKey: "auto_pass_alert_longitude") != nil else {
            return
        }
        let location = CLLocation(
            coordinate: CLLocationCoordinate2D(
                latitude: settingsDefaults.double(forKey: "auto_pass_alert_latitude"),
                longitude: settingsDefaults.double(forKey: "auto_pass_alert_longitude")
            ),
            altitude: settingsDefaults.double(forKey: "auto_pass_alert_altitude"),
            horizontalAccuracy: -1,
            verticalAccuracy: -1,
            timestamp: Date()
        )
        refreshAutomaticPassAlerts(location: location)
        PassBackgroundRefreshCoordinator.shared.schedule()
    }

    private func requestNotificationAuthorization() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            return true
        case .notDetermined:
            return (try? await center.requestAuthorization(options: [.alert, .badge, .sound])) == true
        case .denied:
            return false
        @unknown default:
            return false
        }
    }

    private func cancelAutomaticPassNotifications() {
        Task { await PassNotificationScheduler.cancelAutomatic() }
    }

    private func openNotificationSettings() {
        let value: String
        if #available(iOS 16.0, *) {
            value = UIApplication.openNotificationSettingsURLString
        } else {
            value = UIApplication.openSettingsURLString
        }
        if let url = URL(string: value) {
            UIApplication.shared.open(url)
        }
    }

    private func openAppSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }

    private func addToCalendar(_ pass: PassPayload) {
        Task {
            let accessGranted: Bool
            do {
                if #available(iOS 17.0, *) {
                    accessGranted = try await eventStore.requestWriteOnlyAccessToEvents()
                } else {
                    accessGranted = try await eventStore.requestAccess(to: .event)
                }
            } catch {
                return
            }
            guard accessGranted else { return }
            presentCalendarEditor(for: pass)
        }
    }

    private func presentCalendarEditor(for pass: PassPayload) {
        let editor = EKEventEditViewController()
        let startDate = Date(timeIntervalSince1970: TimeInterval(pass.startTimeMillis) / 1_000)
        let event = EKEvent(eventStore: eventStore)
        event.title = String(localized: "calendar.event.title")
        event.startDate = startDate
        event.endDate = startDate.addingTimeInterval(TimeInterval(pass.durationInSeconds))
        event.notes = String(
            format: String(localized: "calendar.event.notes.format"),
            CompassLabels.shared.localize(code: pass.startAzimuthCompass),
            CompassLabels.shared.localize(code: pass.endAzimuthCompass),
            Int(pass.maxElevation.rounded())
        )
        editor.eventStore = eventStore
        editor.event = event
        editor.editViewDelegate = eventEditorDelegate
        Self.topViewController()?.present(editor, animated: true)
    }

    private func share(_ pass: PassPayload) {
        let startDate = Date(timeIntervalSince1970: TimeInterval(pass.startTimeMillis) / 1_000)
        let formatter = DateFormatter()
        formatter.dateStyle = .full
        formatter.timeStyle = .short
        let text = String(
            format: String(localized: "share.pass.format"),
            formatter.string(from: startDate),
            pass.durationInSeconds / 60,
            pass.durationInSeconds % 60,
            Int(pass.maxElevation.rounded())
        )
        let controller = UIActivityViewController(
            activityItems: [text],
            applicationActivities: nil
        )
        if let popover = controller.popoverPresentationController,
           let source = Self.topViewController()?.view {
            popover.sourceView = source
            popover.sourceRect = CGRect(
                x: source.bounds.midX,
                y: source.bounds.midY,
                width: 1,
                height: 1
            )
        }
        Self.topViewController()?.present(controller, animated: true)
    }

    private func contactSupport(versionText: String?) {
        var components = URLComponents()
        components.scheme = "mailto"
        components.path = "restartapplication@gmail.com"
        components.queryItems = [
            URLQueryItem(name: "subject", value: "ISS Tracker support"),
            URLQueryItem(
                name: "body",
                value: "Hi,\n\nI need help with ISS Tracker.\n\nApp version: \(versionText ?? appVersionName)\n\n"
            )
        ]
        if let url = components.url {
            UIApplication.shared.open(url)
        }
    }

    private func shareApp() {
        let url = appStoreURL?.absoluteString ?? "https://restartapps.com/"
        let text = String(format: String(localized: "share.app.format"), url)
        let controller = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        if let popover = controller.popoverPresentationController,
           let source = Self.topViewController()?.view {
            popover.sourceView = source
            popover.sourceRect = CGRect(x: source.bounds.midX, y: source.bounds.midY, width: 1, height: 1)
        }
        Self.topViewController()?.present(controller, animated: true)
    }

    private func openWebPage(_ value: String) {
        guard let url = URL(string: value) else { return }
        UIApplication.shared.open(url)
    }

    private func requestLocation() {
        isLocationLookupInProgress = true
        composeBridge.clearPassesError()
        refreshSettingsPlatformState()
        switch locationManager.authorizationStatus {
        case .notDetermined:
            locationManager.requestWhenInUseAuthorization()
        case .authorizedAlways, .authorizedWhenInUse:
            locationManager.requestLocation()
        case .denied, .restricted:
            isLocationLookupInProgress = false
            composeBridge.setAutomaticPassAlertsEnabled(enabled: false)
            cancelAutomaticPassNotifications()
            PassBackgroundRefreshCoordinator.shared.cancel()
            composeBridge.setPassesLocationError(
                message: String(localized: "location.error.unavailable")
            )
            refreshSettingsPlatformState()
            if let settingsUrl = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(settingsUrl)
            }
        @unknown default:
            break
        }
    }

    private func requestReview() {
        guard let windowScene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive }) else {
            return
        }
        SKStoreReviewController.requestReview(in: windowScene)
    }

    private func rateApp() {
        guard let appStoreURL else {
            requestReview()
            return
        }
        var components = URLComponents(url: appStoreURL, resolvingAgainstBaseURL: false)
        var queryItems = components?.queryItems ?? []
        if !queryItems.contains(where: { $0.name == "action" }) {
            queryItems.append(URLQueryItem(name: "action", value: "write-review"))
        }
        components?.queryItems = queryItems
        UIApplication.shared.open(components?.url ?? appStoreURL)
    }

    private func refreshAdFreeState() {
        let active = adRemovalPurchase.isEntitled
        isAdFree = active
        ads.setAdFree(active)
        analytics.updateAdFreeState(active)
    }

    private func recordForegroundOpen() {
        let nowMillis = Self.nowMillis
        policyBridge.recordReviewAppOpen(nowMillis: nowMillis)
        appOpenCount = Int(policyBridge.recordAppOpen())
        ads.handleAppOpen(
            appOpenCount: appOpenCount,
            rootViewController: Self.topViewController()
        )
    }

    private func refreshSettingsPlatformState() {
        let backgroundRefreshAvailable =
            UIApplication.shared.backgroundRefreshStatus == .available
        let locationLookupInProgress = isLocationLookupInProgress
        let privacyOptionsRequired = isPrivacyOptionsRequired
        let adsAvailable = canRequestAds && !isAdFree
        let adFree = isAdFree
        let purchaseState = adRemovalPurchase.state
        Task { @MainActor in
            let settings = await UNUserNotificationCenter.current().notificationSettings()
            let notificationsAllowed: Bool
            switch settings.authorizationStatus {
            case .authorized, .provisional, .ephemeral:
                notificationsAllowed = true
            default:
                notificationsAllowed = false
            }
            let locationAllowed: Bool
            switch self.locationManager.authorizationStatus {
            case .authorizedAlways, .authorizedWhenInUse:
                locationAllowed = true
            default:
                locationAllowed = false
            }
            if self.settingsDefaults.bool(forKey: "auto_pass_alerts_enabled") &&
                (!notificationsAllowed || !locationAllowed) {
                self.composeBridge.setAutomaticPassAlertsEnabled(enabled: false)
                self.cancelAutomaticPassNotifications()
                PassBackgroundRefreshCoordinator.shared.cancel()
            }
            composeBridge.updateSettingsPlatformState(
                hasNotificationPermission: notificationsAllowed,
                hasLocationPermission: locationAllowed,
                isBackgroundUnrestricted: backgroundRefreshAvailable,
                isLocationLookupInProgress: locationLookupInProgress,
                showPrivacyChoices: privacyOptionsRequired,
                adsAvailable: adsAvailable,
                isAdFree: adFree,
                purchasePriceText: purchaseState.priceText,
                isPurchaseInProgress: purchaseState.isPurchaseInProgress,
                isPurchaseAvailable: purchaseState.isPurchaseAvailable,
                purchaseStatusCode: purchaseState.statusCode
            )
        }
    }

    private func cast<T>(_ value: Any) -> [T] {
        if let typed = value as? [T] {
            return typed
        }
        if let array = value as? NSArray {
            return array.compactMap { $0 as? T }
        }
        return []
    }

    private static var nowMillis: Int64 {
        Int64(Date().timeIntervalSince1970 * 1_000)
    }

    private var appStoreURL: URL? {
        guard let value = Bundle.main.object(
            forInfoDictionaryKey: "ISSTrackerAppStoreUrl"
        ) as? String else {
            return nil
        }
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : URL(string: trimmed)
    }

    static func topViewController() -> UIViewController? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
    }
}

private struct PassPayload: Codable {
    let startTimeMillis: Int64
    let durationInSeconds: Int
    let magnitude: Double
    let maxElevation: Double
    let startAzimuthCompass: String
    let endAzimuthCompass: String
    let notificationTimes: [String]?
}

private final class PassEventEditorDelegate: NSObject, EKEventEditViewDelegate {
    func eventEditViewController(
        _ controller: EKEventEditViewController,
        didCompleteWith action: EKEventEditViewAction
    ) {
        controller.dismiss(animated: true)
    }
}

private final class SolarMapAnnotation: MKPointAnnotation {}

private final class IssMapAnnotation: MKPointAnnotation {}

private final class OrbitDirectionAnnotation: MKPointAnnotation {
    var rotationRadians: CGFloat = 0
    /** Set for a selected satellite's path; nil keeps the ISS's red. */
    var colorArgb: Int64?
}

private final class OrbitTimeAnnotation: MKPointAnnotation {
    var minuteOffset = 0
    /** Set for a selected satellite's path; nil keeps the ISS's yellow border. */
    var colorArgb: Int64?
}

/** Tiangong, Hubble and other non-ISS spacecraft. */
private final class SatelliteAnnotation: MKPointAnnotation {
    var id = ""
    var label = ""
    var colorArgb: Int64 = 0
    var isStation = true
    var isSelected = false
}

private final class NativeIssMapView: UIView, MKMapViewDelegate, UIGestureRecognizerDelegate {
    private let mapView = MKMapView()
    private var renderedKey = ""
    private var hasSetInitialRegion = false
    private var current: IssMapPoint?
    private var orbit: [IssMapPoint] = []
    private var mapTypeName = "Normal"
    private var satellites: [SatelliteMapMarker] = []
    private var selectedTrack: [IssMapPoint] = []
    private var selectedTrackColor: Int64 = 0
    private var onSelect: ((String?) -> Void)?
    private var isRenderScheduled = false
    private var satelliteMarkerImages: [String: UIImage] = [:]
    private var coloredCueImages: [String: UIImage] = [:]
    private var footprintOverlay: MKCircle?
    /** The selected satellite's next orbit. MapKit's polyline factory can't build Swift subclasses, so it's matched by identity. */
    private var selectedTrackOverlay: MKPolyline?
    private lazy var timeMarkerImages: [Int: UIImage] = Dictionary(
        uniqueKeysWithValues: stride(from: 30, through: 120, by: 30).map {
            ($0, Self.makeTimeMarkerImage(minuteOffset: $0))
        }
    )

    override init(frame: CGRect) {
        super.init(frame: frame)
        configure()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        configure()
    }

    /** Keeps MapKit's logo and legal link above the floating navigation bar. */
    func setBottomInset(_ inset: CGFloat) {
        guard mapView.layoutMargins.bottom != inset + 4 else { return }
        mapView.layoutMargins = UIEdgeInsets(top: 8, left: 8, bottom: inset + 4, right: 8)
    }

    func update(current: IssMapPoint?, orbit: [IssMapPoint], mapType: String) {
        self.current = current
        self.orbit = orbit
        mapTypeName = mapType
        scheduleRender()
    }

    func updateSatellites(
        markers: [SatelliteMapMarker],
        track: [IssMapPoint],
        trackColor: Int64,
        onSelect: @escaping (String?) -> Void
    ) {
        satellites = markers
        selectedTrack = track
        selectedTrackColor = trackColor
        self.onSelect = onSelect
        scheduleRender()
    }

    /** Compose sends the ISS and the other satellites separately; draw once for both. */
    private func scheduleRender() {
        guard !isRenderScheduled else { return }
        isRenderScheduled = true
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            self.isRenderScheduled = false
            self.render()
        }
    }

    private func render() {
        let current = self.current
        let orbit = self.orbit
        let mapType = mapTypeName
        let currentKey = current.map {
            "\($0.latitude)|\($0.longitude)|\($0.timestamp)|\($0.footprint)|" +
                "\($0.solarLatitude)|\($0.solarLongitude)"
        } ?? "none"
        let satelliteKey = satellites
            .map { "|\($0.id)|\($0.latitude)|\($0.longitude)|\($0.isSelected)|\($0.label)" }
            .joined() + "|track\(selectedTrackColor)" + selectedTrack
            .map { "|\($0.latitude)|\($0.longitude)" }
            .joined()
        let key = mapType + "|" + currentKey + satelliteKey + orbit
            .map { "|\($0.latitude)|\($0.longitude)|\($0.timestamp)" }
            .joined()
        guard key != renderedKey else { return }
        renderedKey = key
        mapView.mapType = switch mapType {
        case "Satellite": .satellite
        case "Hybrid": .hybrid
        default: .standard
        }

        mapView.removeAnnotations(mapView.annotations)
        mapView.removeOverlays(mapView.overlays)
        footprintOverlay = nil
        selectedTrackOverlay = nil

        if let current {
            let footprint = MKCircle(
                center: CLLocationCoordinate2D(
                    latitude: current.latitude,
                    longitude: current.longitude
                ),
                radius: current.footprint * 500
            )
            footprintOverlay = footprint
            mapView.addOverlay(footprint, level: .aboveRoads)

            let solarAnnotation = SolarMapAnnotation()
            solarAnnotation.coordinate = CLLocationCoordinate2D(
                latitude: current.solarLatitude,
                longitude: current.solarLongitude
            )
            mapView.addAnnotation(solarAnnotation)

            let annotation = IssMapAnnotation()
            annotation.coordinate = CLLocationCoordinate2D(
                latitude: current.latitude,
                longitude: current.longitude
            )
            mapView.addAnnotation(annotation)
            if !hasSetInitialRegion {
                hasSetInitialRegion = true
                mapView.setRegion(
                    MKCoordinateRegion(
                        center: annotation.coordinate,
                        latitudinalMeters: Self.initialMapSpanMeters,
                        longitudinalMeters: Self.initialMapSpanMeters
                    ),
                    animated: true
                )
            }

            let orderedOrbit = Self.orderedOrbit(orbit)
            addOrbitPath(orderedOrbit)
            addOrbitCues(
                currentTimestamp: current.timestamp,
                orbit: orderedOrbit
            )
        }
        addSatellites()
    }

    private func addSatellites() {
        if selectedTrack.count > 1 {
            let coordinates = selectedTrack.map {
                CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
            }
            let polyline = coordinates.withUnsafeBufferPointer { buffer in
                MKGeodesicPolyline(coordinates: buffer.baseAddress!, count: buffer.count)
            }
            selectedTrackOverlay = polyline
            mapView.addOverlay(polyline, level: .aboveRoads)
            // The same arrows and +30m to +120m labels as the ISS line, in the satellite's color.
            addOrbitCues(
                currentTimestamp: selectedTrack[0].timestamp,
                orbit: selectedTrack,
                colorArgb: selectedTrackColor
            )
        }
        for marker in satellites {
            let annotation = SatelliteAnnotation()
            annotation.id = marker.id
            annotation.label = marker.label
            annotation.colorArgb = marker.colorArgb
            annotation.isStation = marker.isStation
            annotation.isSelected = marker.isSelected
            annotation.title = marker.label
            annotation.coordinate = CLLocationCoordinate2D(latitude: marker.latitude, longitude: marker.longitude)
            mapView.addAnnotation(annotation)
        }
    }

    func mapView(_ mapView: MKMapView, didSelect view: MKAnnotationView) {
        let id: String?
        switch view.annotation {
        case let satellite as SatelliteAnnotation: id = satellite.id
        case is IssMapAnnotation: id = "iss"
        default: id = nil
        }
        // Selection lives in the shared screen; clear MapKit's so the same marker can be tapped again.
        if let annotation = view.annotation {
            DispatchQueue.main.async { mapView.deselectAnnotation(annotation, animated: false) }
        }
        if let id { onSelect?(id) }
    }

    /** A tap on empty map closes the details; taps on markers are handled by didSelect. */
    @objc private func handleMapTap(_ recognizer: UITapGestureRecognizer) {
        guard recognizer.state == .ended else { return }
        var hit = mapView.hitTest(recognizer.location(in: mapView), with: nil)
        while let view = hit {
            if view is MKAnnotationView { return }
            hit = view.superview
        }
        onSelect?(nil)
    }

    func gestureRecognizer(
        _ gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
    ) -> Bool {
        true
    }

    private func addOrbitPath(_ orbit: [IssMapPoint]) {
        guard orbit.count > 1 else { return }
        let coordinates = orbit.map {
            CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
        }
        let polyline = coordinates.withUnsafeBufferPointer { buffer in
            MKGeodesicPolyline(coordinates: buffer.baseAddress!, count: buffer.count)
        }
        mapView.addOverlay(polyline, level: .aboveRoads)
    }

    private func addOrbitCues(currentTimestamp: Int64, orbit: [IssMapPoint], colorArgb: Int64? = nil) {
        guard currentTimestamp > 0 else { return }
        let future = orbit.filter { $0.timestamp > currentTimestamp }
        guard future.count > 1 else { return }

        for minuteOffset in stride(from: 15, through: 120, by: 15) {
            let targetTimestamp = currentTimestamp + Int64(minuteOffset * 60)
            guard let index = future.indices.min(by: {
                abs(future[$0].timestamp - targetTimestamp) <
                    abs(future[$1].timestamp - targetTimestamp)
            }) else { continue }
            let point = future[index]
            guard abs(point.timestamp - targetTimestamp) <= 90 else { continue }

            if minuteOffset.isMultiple(of: 30) {
                let annotation = OrbitTimeAnnotation()
                annotation.minuteOffset = minuteOffset
                annotation.colorArgb = colorArgb
                annotation.coordinate = CLLocationCoordinate2D(
                    latitude: point.latitude,
                    longitude: point.longitude
                )
                mapView.addAnnotation(annotation)
            } else {
                let next = future.indices.contains(index + 1) ? future[index + 1] : nil
                let previous = index > 0 ? future[index - 1] : nil
                let from: IssMapPoint
                let to: IssMapPoint
                if let next = next {
                    from = point
                    to = next
                } else if let previous = previous {
                    from = previous
                    to = point
                } else {
                    continue
                }
                let annotation = OrbitDirectionAnnotation()
                annotation.rotationRadians = Self.bearingRadians(from: from, to: to)
                annotation.colorArgb = colorArgb
                annotation.coordinate = CLLocationCoordinate2D(
                    latitude: point.latitude,
                    longitude: point.longitude
                )
                mapView.addAnnotation(annotation)
            }
        }
    }

    func mapView(
        _ mapView: MKMapView,
        viewFor annotation: MKAnnotation
    ) -> MKAnnotationView? {
        if annotation is MKUserLocation { return nil }

        if annotation is SolarMapAnnotation {
            let identifier = "solar"
            let view = mapView.dequeueReusableAnnotationView(withIdentifier: identifier)
                ?? MKAnnotationView(annotation: annotation, reuseIdentifier: identifier)
            view.annotation = annotation
            view.image = UIImage(systemName: "sun.max.fill")?
                .applyingSymbolConfiguration(UIImage.SymbolConfiguration(pointSize: 25, weight: .bold))?
                .withTintColor(.systemYellow, renderingMode: .alwaysOriginal)
            view.displayPriority = .required
            view.collisionMode = .circle
            return view
        }

        if annotation is IssMapAnnotation {
            let identifier = "iss"
            let view = mapView.dequeueReusableAnnotationView(withIdentifier: identifier)
                ?? MKAnnotationView(annotation: annotation, reuseIdentifier: identifier)
            view.annotation = annotation
            view.image = Self.issMarkerImage
            view.centerOffset = .zero
            view.transform = .identity
            view.displayPriority = .required
            view.collisionMode = .rectangle
            view.canShowCallout = false
            return view
        }

        if let direction = annotation as? OrbitDirectionAnnotation {
            let identifier = "orbit-direction"
            let view = mapView.dequeueReusableAnnotationView(withIdentifier: identifier)
                ?? MKAnnotationView(annotation: annotation, reuseIdentifier: identifier)
            view.annotation = annotation
            view.image = direction.colorArgb.map { coloredDirectionImage(argb: $0) } ?? Self.directionMarkerImage
            view.transform = CGAffineTransform(rotationAngle: direction.rotationRadians)
            view.displayPriority = .required
            view.collisionMode = .circle
            return view
        }

        if let satellite = annotation as? SatelliteAnnotation {
            let identifier = "satellite"
            let view = mapView.dequeueReusableAnnotationView(withIdentifier: identifier)
                ?? MKAnnotationView(annotation: annotation, reuseIdentifier: identifier)
            view.annotation = annotation
            let marker = satelliteMarkerImage(for: satellite)
            view.image = marker.image
            // The badge sits on the satellite's position; its name hangs below.
            view.centerOffset = CGPoint(x: 0, y: marker.image.size.height / 2 - marker.badgeCenterY)
            view.transform = .identity
            view.displayPriority = .required
            view.collisionMode = .circle
            view.canShowCallout = false
            view.accessibilityLabel = satellite.label
            view.zPriority = satellite.isSelected ? .max : .defaultUnselected
            return view
        }

        if let time = annotation as? OrbitTimeAnnotation {
            let identifier = "orbit-time"
            let view = mapView.dequeueReusableAnnotationView(withIdentifier: identifier)
                ?? MKAnnotationView(annotation: annotation, reuseIdentifier: identifier)
            view.annotation = annotation
            view.image = time.colorArgb.map { coloredTimeImage(minuteOffset: time.minuteOffset, argb: $0) }
                ?? timeMarkerImages[time.minuteOffset]
            view.transform = .identity
            view.displayPriority = .required
            view.collisionMode = .rectangle
            return view
        }

        return nil
    }

    func mapView(
        _ mapView: MKMapView,
        rendererFor overlay: MKOverlay
    ) -> MKOverlayRenderer {
        if let circle = overlay as? MKCircle {
            let renderer = MKCircleRenderer(circle: circle)
            if let footprint = footprintOverlay, circle === footprint {
                renderer.fillColor = UIColor(
                    red: 63.0 / 255.0,
                    green: 140.0 / 255.0,
                    blue: 1,
                    alpha: 0.15
                )
                renderer.strokeColor = UIColor(
                    red: 100.0 / 255.0,
                    green: 181.0 / 255.0,
                    blue: 246.0 / 255.0,
                    alpha: 0.80
                )
                renderer.lineWidth = 1
            }
            return renderer
        }
        if let track = overlay as? MKPolyline, let selected = selectedTrackOverlay, track === selected {
            let renderer = MKPolylineRenderer(polyline: track)
            renderer.strokeColor = Self.color(argb: selectedTrackColor)
            renderer.lineWidth = 3
            return renderer
        }
        guard let polyline = overlay as? MKPolyline else {
            return MKOverlayRenderer(overlay: overlay)
        }
        let renderer = MKPolylineRenderer(polyline: polyline)
        renderer.strokeColor = .systemRed
        renderer.lineWidth = 3
        return renderer
    }

    private static func orderedOrbit(_ orbit: [IssMapPoint]) -> [IssMapPoint] {
        var seen = Set<Int64>()
        return orbit
            .filter { $0.timestamp > 0 && seen.insert($0.timestamp).inserted }
            .sorted { $0.timestamp < $1.timestamp }
    }

    private static func bearingRadians(from: IssMapPoint, to: IssMapPoint) -> CGFloat {
        let fromLatitude = from.latitude * .pi / 180
        let toLatitude = to.latitude * .pi / 180
        let longitudeDelta = (to.longitude - from.longitude) * .pi / 180
        let y = sin(longitudeDelta) * cos(toLatitude)
        let x = cos(fromLatitude) * sin(toLatitude) -
            sin(fromLatitude) * cos(toLatitude) * cos(longitudeDelta)
        return CGFloat(atan2(y, x))
    }

    private static func normalizedLongitude(_ longitude: Double) -> Double {
        ((longitude + 540).truncatingRemainder(dividingBy: 360)) - 180
    }

    private func coloredDirectionImage(argb: Int64) -> UIImage {
        let key = "direction|\(argb)"
        if let cached = coloredCueImages[key] { return cached }
        let image = Self.makeDirectionMarkerImage(color: Self.color(argb: argb))
        coloredCueImages[key] = image
        return image
    }

    private func coloredTimeImage(minuteOffset: Int, argb: Int64) -> UIImage {
        let key = "time|\(minuteOffset)|\(argb)"
        if let cached = coloredCueImages[key] { return cached }
        let image = Self.makeTimeMarkerImage(minuteOffset: minuteOffset, border: Self.color(argb: argb))
        coloredCueImages[key] = image
        return image
    }

    private static func makeTimeMarkerImage(minuteOffset: Int, border: UIColor = .systemYellow) -> UIImage {
        let size = CGSize(width: 72, height: 28)
        return UIGraphicsImageRenderer(size: size).image { _ in
            let bounds = CGRect(origin: .zero, size: size).insetBy(dx: 1, dy: 1)
            let path = UIBezierPath(roundedRect: bounds, cornerRadius: 9)
            UIColor(red: 12.0 / 255.0, green: 18.0 / 255.0, blue: 68.0 / 255.0, alpha: 0.94)
                .setFill()
            path.fill()
            border.setStroke()
            path.lineWidth = 1.5
            path.stroke()

            let text = "+\(minuteOffset)m" as NSString
            let attributes: [NSAttributedString.Key: Any] = [
                .font: AppFonts.exo(size: 11, weight: .semibold),
                .foregroundColor: UIColor.white
            ]
            let textSize = text.size(withAttributes: attributes)
            text.draw(
                at: CGPoint(
                    x: (size.width - textSize.width) / 2,
                    y: (size.height - textSize.height) / 2
                ),
                withAttributes: attributes
            )
        }
    }

    private static let directionMarkerImage: UIImage = makeDirectionMarkerImage(color: .systemRed)

    private static func makeDirectionMarkerImage(color: UIColor) -> UIImage {
        let size = CGSize(width: 22, height: 22)
        return UIGraphicsImageRenderer(size: size).image { _ in
            let path = UIBezierPath()
            path.move(to: CGPoint(x: 11, y: 1))
            path.addLine(to: CGPoint(x: 20, y: 20))
            path.addLine(to: CGPoint(x: 11, y: 15))
            path.addLine(to: CGPoint(x: 2, y: 20))
            path.close()
            color.setFill()
            path.fill()
        }.withRenderingMode(.alwaysOriginal)
    }

    private static let issMarkerImage: UIImage = {
        let sourceWidth: CGFloat = 104
        let sourceHeight: CGFloat = 48
        let scale: CGFloat = 1.0 / 3.0
        let size = CGSize(width: sourceWidth * scale, height: sourceHeight * scale)

        return UIGraphicsImageRenderer(size: size).image { context in
            let canvas = context.cgContext
            canvas.scaleBy(x: scale, y: scale)

            let bodyColor = UIColor(
                red: 232.0 / 255.0,
                green: 157.0 / 255.0,
                blue: 54.0 / 255.0,
                alpha: 1
            )
            let panelColor = UIColor(
                red: 54.0 / 255.0,
                green: 96.0 / 255.0,
                blue: 156.0 / 255.0,
                alpha: 1
            )

            canvas.setFillColor(bodyColor.cgColor)
            canvas.fill(CGRect(x: 39, y: 14, width: 26, height: 20))
            canvas.setFillColor(panelColor.cgColor)
            canvas.fill(CGRect(x: 4, y: 12, width: 31, height: 24))
            canvas.fill(CGRect(x: 69, y: 12, width: 31, height: 24))

            canvas.setStrokeColor(bodyColor.cgColor)
            canvas.setLineWidth(1)
            canvas.move(to: CGPoint(x: 35, y: 24))
            canvas.addLine(to: CGPoint(x: 39, y: 24))
            canvas.move(to: CGPoint(x: 65, y: 24))
            canvas.addLine(to: CGPoint(x: 69, y: 24))
            canvas.move(to: CGPoint(x: 52, y: 5))
            canvas.addLine(to: CGPoint(x: 52, y: 14))
            canvas.strokePath()
            canvas.setFillColor(bodyColor.cgColor)
            canvas.fillEllipse(in: CGRect(x: 49, y: 2, width: 6, height: 6))

            canvas.setStrokeColor(UIColor.white.cgColor)
            canvas.setLineWidth(2)
            canvas.stroke(CGRect(x: 10, y: 17, width: 19, height: 14))
            canvas.stroke(CGRect(x: 75, y: 17, width: 19, height: 14))
        }.withRenderingMode(.alwaysOriginal)
    }()

    private func configure() {
        backgroundColor = .systemBackground
        mapView.translatesAutoresizingMaskIntoConstraints = false
        mapView.delegate = self
        mapView.isRotateEnabled = false
        mapView.pointOfInterestFilter = .excludingAll
        let tap = UITapGestureRecognizer(target: self, action: #selector(handleMapTap(_:)))
        tap.delegate = self
        tap.cancelsTouchesInView = false
        mapView.addGestureRecognizer(tap)
        addSubview(mapView)
        NSLayoutConstraint.activate([
            mapView.leadingAnchor.constraint(equalTo: leadingAnchor),
            mapView.trailingAnchor.constraint(equalTo: trailingAnchor),
            mapView.topAnchor.constraint(equalTo: topAnchor),
            mapView.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])
    }

    private static let initialMapSpanMeters = 8_000_000.0

    private static func color(argb: Int64) -> UIColor {
        let value = UInt32(truncatingIfNeeded: argb)
        return UIColor(
            red: CGFloat((value >> 16) & 0xFF) / 255,
            green: CGFloat((value >> 8) & 0xFF) / 255,
            blue: CGFloat(value & 0xFF) / 255,
            alpha: CGFloat((value >> 24) & 0xFF) / 255
        )
    }

    private func satelliteMarkerImage(for satellite: SatelliteAnnotation) -> (image: UIImage, badgeCenterY: CGFloat) {
        let radius: CGFloat = satellite.isSelected ? 17 : 14
        let ring: CGFloat = 2
        let key = "\(satellite.label)|\(satellite.colorArgb)|\(satellite.isStation)|\(satellite.isSelected)"
        let badgeCenterY = radius + ring
        if let cached = satelliteMarkerImages[key] {
            return (cached, badgeCenterY)
        }
        let accent = Self.color(argb: satellite.colorArgb)
        let font = AppFonts.exo(size: 11, weight: .bold)
        let text = satellite.label as NSString
        let textSize = text.size(withAttributes: [.font: font])
        let labelSize = CGSize(width: textSize.width + 14, height: 18)
        let gap: CGFloat = 3
        let size = CGSize(
            width: max(labelSize.width, (radius + ring) * 2) + 2,
            height: (radius + ring) * 2 + gap + labelSize.height + 2
        )
        let image = UIGraphicsImageRenderer(size: size).image { context in
            let center = CGPoint(x: size.width / 2, y: badgeCenterY)
            UIColor.white.setFill()
            UIBezierPath(arcCenter: center, radius: radius + ring, startAngle: 0, endAngle: .pi * 2, clockwise: true).fill()
            accent.setFill()
            UIBezierPath(arcCenter: center, radius: radius, startAngle: 0, endAngle: .pi * 2, clockwise: true).fill()

            UIColor.white.setFill()
            let unit = radius / 14
            let cg = context.cgContext
            cg.saveGState()
            cg.translateBy(x: center.x, y: center.y)
            if satellite.isStation {
                UIBezierPath(rect: CGRect(x: -3 * unit, y: -4 * unit, width: 6 * unit, height: 8 * unit)).fill()
                UIBezierPath(rect: CGRect(x: -11 * unit, y: -3 * unit, width: 6 * unit, height: 6 * unit)).fill()
                UIBezierPath(rect: CGRect(x: 5 * unit, y: -3 * unit, width: 6 * unit, height: 6 * unit)).fill()
                UIBezierPath(rect: CGRect(x: -5 * unit, y: -0.6 * unit, width: 10 * unit, height: 1.2 * unit)).fill()
            } else {
                cg.rotate(by: -35 * .pi / 180)
                UIBezierPath(
                    roundedRect: CGRect(x: -9 * unit, y: -3.5 * unit, width: 16 * unit, height: 7 * unit),
                    cornerRadius: 2 * unit
                ).fill()
                UIBezierPath(rect: CGRect(x: 7 * unit, y: -2.5 * unit, width: 3 * unit, height: 5 * unit)).fill()
                UIBezierPath(rect: CGRect(x: -3 * unit, y: -9 * unit, width: 2 * unit, height: 5.5 * unit)).fill()
                UIBezierPath(rect: CGRect(x: -3 * unit, y: 3.5 * unit, width: 2 * unit, height: 5.5 * unit)).fill()
            }
            cg.restoreGState()

            let labelRect = CGRect(
                x: (size.width - labelSize.width) / 2,
                y: badgeCenterY + radius + ring + gap,
                width: labelSize.width,
                height: labelSize.height
            )
            let pill = UIBezierPath(roundedRect: labelRect, cornerRadius: labelSize.height / 2)
            UIColor(red: 12.0 / 255.0, green: 18.0 / 255.0, blue: 68.0 / 255.0, alpha: 0.92).setFill()
            pill.fill()
            if satellite.isSelected {
                accent.setStroke()
                pill.lineWidth = 1.5
                pill.stroke()
            }
            text.draw(
                at: CGPoint(x: labelRect.midX - textSize.width / 2, y: labelRect.midY - textSize.height / 2),
                withAttributes: [.font: font, .foregroundColor: UIColor.white]
            )
        }
        satelliteMarkerImages[key] = image
        return (image, badgeCenterY)
    }
}

private var appVersionName: String {
    Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "7.10"
}

private var appVersionCode: String {
    Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "54"
}
