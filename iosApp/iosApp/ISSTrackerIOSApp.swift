import BackgroundTasks
#if canImport(FirebaseCore)
import FirebaseCore
#endif
import ISSTrackerShared
import SwiftUI
import UserNotifications

enum PassAlertAltitudeStorage {
    static let altitudeKey = "auto_pass_alert_altitude"
    static let unitKey = "auto_pass_alert_altitude_unit"
    static let metersUnit = "meters"

    static func migrateToMetersIfNeeded(in defaults: UserDefaults) {
        guard defaults.string(forKey: unitKey) != metersUnit else { return }
        if defaults.object(forKey: altitudeKey) != nil {
            let legacyKilometers = max(0, defaults.double(forKey: altitudeKey))
            defaults.set(legacyKilometers * 1_000, forKey: altitudeKey)
        }
        defaults.set(metersUnit, forKey: unitKey)
    }

    static func storeMeters(_ altitude: Double, in defaults: UserDefaults) {
        defaults.set(max(0, altitude), forKey: altitudeKey)
        defaults.set(metersUnit, forKey: unitKey)
    }
}

@main
struct ISSTrackerIOSApp: App {
    @UIApplicationDelegateAdaptor(ISSAppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}

final class ISSAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [
            UIApplication.LaunchOptionsKey: Any
        ]? = nil
    ) -> Bool {
        // Must be set before launch finishes so a tap that cold-launches the app is delivered.
        UNUserNotificationCenter.current().delegate = PassNotificationDelegate.shared
        configureFirebase()
        AppAnalyticsService.shared.configure()
        PassAlertAltitudeStorage.migrateToMetersIfNeeded(
            in: UserDefaults(suiteName: "settings") ?? .standard
        )
        PassBackgroundRefreshCoordinator.shared.register()
        PassBackgroundRefreshCoordinator.shared.schedule()
        return true
    }

    private func configureFirebase() {
        #if canImport(FirebaseCore)
        guard FirebaseApp.app() == nil else { return }
        let resourceName = Bundle.main.bundleIdentifier?.hasSuffix(".dev") == true
            ? "GoogleService-Info-Debug"
            : "GoogleService-Info"
        guard let path = Bundle.main.path(forResource: resourceName, ofType: "plist"),
              let options = FirebaseOptions(contentsOfFile: path) else {
            return
        }
        FirebaseApp.configure(options: options)
        #endif
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        PassBackgroundRefreshCoordinator.shared.schedule()
    }
}

final class PassBackgroundRefreshCoordinator {
    static let shared = PassBackgroundRefreshCoordinator()
    static let taskIdentifier = "com.restart.spacestationtracker.pass-refresh"

    private let bridge = IosPassAlertBridge()
    private let liveEventBridge = IosLiveEventAlertBridge()
    private let defaults = UserDefaults(suiteName: "settings") ?? .standard

    func register() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: Self.taskIdentifier,
            using: nil
        ) { [weak self] task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            self?.handle(refreshTask)
        }
    }

    func schedule() {
        let passAlerts = defaults.bool(forKey: "auto_pass_alerts_enabled")
        let liveEventAlerts = defaults.bool(forKey: "live_event_alerts_enabled")
        guard passAlerts || liveEventAlerts else { return }
        let request = BGAppRefreshTaskRequest(identifier: Self.taskIdentifier)
        // NASA's broadcast schedule changes more often than pass predictions, so ask sooner when it's on.
        // iOS still decides when the refresh actually runs.
        request.earliestBeginDate = Date(timeIntervalSinceNow: (liveEventAlerts ? 6 : 12) * 60 * 60)
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: Self.taskIdentifier)
        try? BGTaskScheduler.shared.submit(request)
    }

    /// Stops the refresh once neither pass alerts nor live-event alerts need it.
    func cancel() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: Self.taskIdentifier)
        schedule()
    }

    private func handle(_ task: BGAppRefreshTask) {
        schedule()
        refreshLiveEventAlerts()
        PassAlertAltitudeStorage.migrateToMetersIfNeeded(in: defaults)
        guard defaults.bool(forKey: "auto_pass_alerts_enabled"),
              defaults.object(forKey: "auto_pass_alert_latitude") != nil,
              defaults.object(forKey: "auto_pass_alert_longitude") != nil else {
            task.setTaskCompleted(success: true)
            return
        }

        var didFinish = false
        task.expirationHandler = {
            guard !didFinish else { return }
            didFinish = true
            task.setTaskCompleted(success: false)
        }

        bridge.loadAutomaticSchedules(
            latitude: defaults.double(forKey: "auto_pass_alert_latitude"),
            longitude: defaults.double(forKey: "auto_pass_alert_longitude"),
            altitude: defaults.double(forKey: "auto_pass_alert_altitude"),
            nowMillis: Int64(Date().timeIntervalSince1970 * 1_000),
            onSuccess: { passes in
                Task {
                    await Self.replaceAutomaticNotifications(with: passes)
                    guard !didFinish else { return }
                    didFinish = true
                    task.setTaskCompleted(success: true)
                }
            },
            onFailure: { _ in
                guard !didFinish else { return }
                didFinish = true
                task.setTaskCompleted(success: false)
            }
        )
    }

    /// Fire and forget: the schedule is one small download, finished well within the task's time.
    private func refreshLiveEventAlerts() {
        guard defaults.bool(forKey: "live_event_alerts_enabled") else { return }
        let defaults = defaults
        liveEventBridge.loadAlerts(
            nowMillis: Int64(Date().timeIntervalSince1970 * 1_000),
            onSuccess: { alerts in
                Task {
                    let settings = await UNUserNotificationCenter.current().notificationSettings()
                    guard settings.authorizationStatus == .authorized ||
                            settings.authorizationStatus == .provisional,
                          defaults.bool(forKey: "live_event_alerts_enabled") else { return }
                    await LiveEventNotificationScheduler.replace(with: alerts)
                }
            },
            onFailure: { _ in }
        )
    }

    private static func replaceAutomaticNotifications(
        with passes: [IosPassAlertPayload]
    ) async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        guard settings.authorizationStatus == .authorized ||
                settings.authorizationStatus == .provisional else {
            return
        }

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
}

