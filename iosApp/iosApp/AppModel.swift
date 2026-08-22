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
        UNUserNotificationCenter.current().delegate = PassNotificationDelegate.shared
        configureComposeBridge()
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
                message: "Location access is unavailable. Check Location Services and try again."
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
        guard let placemark else { return "Current Location" }

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
        return nearbyPlace ?? region ?? "Current Location"
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        isLocationLookupInProgress = false
        composeBridge.setPassesLocationError(
            message: "Location could not be determined. Check Location Services and try again."
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
                    await scheduleNotification(
                        for: pass,
                        notificationTime: schedule.notificationTime,
                        triggerTimeMillis: schedule.triggerTimeMillis,
                        identifierSuffix: schedule.identifierSuffix,
                        identifierPrefix: "manual"
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

    private func scheduleNotification(
        for pass: PassPayload,
        notificationTime: String,
        triggerTimeMillis: Int64,
        identifierSuffix: String,
        identifierPrefix: String
    ) async {
        guard await requestNotificationAuthorization() else {
            openNotificationSettings()
            return
        }
        let triggerDate = Date(timeIntervalSince1970: TimeInterval(triggerTimeMillis) / 1_000)
        guard triggerDate > Date() else { return }

        let content = UNMutableNotificationContent()
        content.title = notificationTitle(for: notificationTime)
        content.body = "Look \(pass.startAzimuthCompass) for a pass peaking at \(Int(pass.maxElevation.rounded()))°."
        content.sound = .default
        content.userInfo = ["pass_start_time": pass.startTimeMillis]

        let components = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute, .second],
            from: triggerDate
        )
        let request = UNNotificationRequest(
            identifier: "\(identifierPrefix):\(pass.startTimeMillis):\(identifierSuffix)",
            content: content,
            trigger: UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        )
        try? await UNUserNotificationCenter.current().add(request)
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
                    self.cancelAutomaticPassNotifications()
                    for pass in passes {
                        let payload = PassPayload(
                            startTimeMillis: pass.startTimeMillis,
                            durationInSeconds: Int(pass.durationInSeconds),
                            magnitude: pass.magnitude,
                            maxElevation: pass.maxElevation,
                            startAzimuthCompass: pass.startAzimuthCompass,
                            endAzimuthCompass: pass.endAzimuthCompass,
                            notificationTimes: nil
                        )
                        await self.scheduleNotification(
                            for: payload,
                            notificationTime: pass.notificationTime,
                            triggerTimeMillis: pass.triggerTimeMillis,
                            identifierSuffix: pass.identifierSuffix,
                            identifierPrefix: "automatic"
                        )
                    }
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
        let center = UNUserNotificationCenter.current()
        center.getPendingNotificationRequests { requests in
            let identifiers = requests
                .map(\.identifier)
                .filter { $0.hasPrefix("automatic:") }
            UNUserNotificationCenter.current()
                .removePendingNotificationRequests(withIdentifiers: identifiers)
        }
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

    private func notificationTitle(for notificationTime: String) -> String {
        switch notificationTime {
        case "At time of event": "ISS pass is starting"
        case "10 minutes before": "ISS pass in 10 minutes"
        case "1 hour before": "ISS pass in 1 hour"
        case "12 hours before": "ISS pass in 12 hours"
        case "1 day before": "ISS pass tomorrow"
        case "1 week before": "ISS pass in 1 week"
        default: "Upcoming ISS pass"
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
        event.title = "International Space Station pass"
        event.startDate = startDate
        event.endDate = startDate.addingTimeInterval(TimeInterval(pass.durationInSeconds))
        event.notes = "Visible ISS pass from \(pass.startAzimuthCompass) to \(pass.endAzimuthCompass), peaking at \(Int(pass.maxElevation.rounded()))°."
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
        let text = "International Space Station pass on \(formatter.string(from: startDate)). Duration: \(pass.durationInSeconds / 60)m \(pass.durationInSeconds % 60)s. Peak elevation: \(Int(pass.maxElevation.rounded()))°."
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
        let text = "Track the International Space Station with ISS Tracker: \(url)"
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
                message: "Location access is unavailable. Check Location Services and try again."
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

private final class PassNotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    static let shared = PassNotificationDelegate()

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }
}

private final class SolarMapAnnotation: MKPointAnnotation {}

private final class IssMapAnnotation: MKPointAnnotation {}

private final class OrbitDirectionAnnotation: MKPointAnnotation {
    var rotationRadians: CGFloat = 0
}

private final class OrbitTimeAnnotation: MKPointAnnotation {
    var minuteOffset = 0
}

private final class NativeIssMapView: UIView, MKMapViewDelegate {
    private let mapView = MKMapView()
    private var renderedKey = ""
    private var hasSetInitialRegion = false
    private var footprintOverlay: MKCircle?
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

    func update(current: IssMapPoint?, orbit: [IssMapPoint], mapType: String) {
        let currentKey = current.map {
            "\($0.latitude)|\($0.longitude)|\($0.timestamp)|\($0.footprint)|" +
                "\($0.solarLatitude)|\($0.solarLongitude)"
        } ?? "none"
        let key = mapType + "|" + currentKey + orbit
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

    private func addOrbitCues(currentTimestamp: Int64, orbit: [IssMapPoint]) {
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
            view.image = Self.directionMarkerImage
            view.transform = CGAffineTransform(rotationAngle: direction.rotationRadians)
            view.displayPriority = .required
            view.collisionMode = .circle
            return view
        }

        if let time = annotation as? OrbitTimeAnnotation {
            let identifier = "orbit-time"
            let view = mapView.dequeueReusableAnnotationView(withIdentifier: identifier)
                ?? MKAnnotationView(annotation: annotation, reuseIdentifier: identifier)
            view.annotation = annotation
            view.image = timeMarkerImages[time.minuteOffset]
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

    private static func makeTimeMarkerImage(minuteOffset: Int) -> UIImage {
        let size = CGSize(width: 72, height: 28)
        return UIGraphicsImageRenderer(size: size).image { _ in
            let bounds = CGRect(origin: .zero, size: size).insetBy(dx: 1, dy: 1)
            let path = UIBezierPath(roundedRect: bounds, cornerRadius: 9)
            UIColor(red: 12.0 / 255.0, green: 18.0 / 255.0, blue: 68.0 / 255.0, alpha: 0.94)
                .setFill()
            path.fill()
            UIColor.systemYellow.setStroke()
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

    private static let directionMarkerImage: UIImage = {
        let size = CGSize(width: 22, height: 22)
        return UIGraphicsImageRenderer(size: size).image { _ in
            let path = UIBezierPath()
            path.move(to: CGPoint(x: 11, y: 1))
            path.addLine(to: CGPoint(x: 20, y: 20))
            path.addLine(to: CGPoint(x: 11, y: 15))
            path.addLine(to: CGPoint(x: 2, y: 20))
            path.close()
            UIColor.systemRed.setFill()
            path.fill()
        }.withRenderingMode(.alwaysOriginal)
    }()

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
        addSubview(mapView)
        NSLayoutConstraint.activate([
            mapView.leadingAnchor.constraint(equalTo: leadingAnchor),
            mapView.trailingAnchor.constraint(equalTo: trailingAnchor),
            mapView.topAnchor.constraint(equalTo: topAnchor),
            mapView.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])
    }

    private static let initialMapSpanMeters = 8_000_000.0
}

private var appVersionName: String {
    Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "7.09"
}

private var appVersionCode: String {
    Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "53"
}
