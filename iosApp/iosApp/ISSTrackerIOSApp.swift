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
        guard defaults.bool(forKey: "auto_pass_alerts_enabled") else { return }
        let request = BGAppRefreshTaskRequest(identifier: Self.taskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 12 * 60 * 60)
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: Self.taskIdentifier)
        try? BGTaskScheduler.shared.submit(request)
    }

    func cancel() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: Self.taskIdentifier)
    }

    private func handle(_ task: BGAppRefreshTask) {
        schedule()
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

    private static func replaceAutomaticNotifications(
        with passes: [IosPassAlertPayload]
    ) async {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        guard settings.authorizationStatus == .authorized ||
                settings.authorizationStatus == .provisional else {
            return
        }

        let pending = await center.pendingNotificationRequests()
        center.removePendingNotificationRequests(
            withIdentifiers: pending
                .map(\.identifier)
                .filter { $0.hasPrefix("automatic:") }
        )

        for pass in passes {
            let triggerDate = Date(
                timeIntervalSince1970: TimeInterval(pass.triggerTimeMillis) / 1_000
            )
            guard triggerDate > Date() else { continue }

            let content = UNMutableNotificationContent()
            content.title = notificationTitle(for: pass.notificationTime)
            content.body = "Look \(pass.startAzimuthCompass) for a pass peaking at \(Int(pass.maxElevation.rounded()))°."
            content.sound = .default
            content.userInfo = ["pass_start_time": pass.startTimeMillis]

            let components = Calendar.current.dateComponents(
                [.year, .month, .day, .hour, .minute, .second],
                from: triggerDate
            )
            let request = UNNotificationRequest(
                identifier: "automatic:\(pass.startTimeMillis):\(pass.identifierSuffix)",
                content: content,
                trigger: UNCalendarNotificationTrigger(
                    dateMatching: components,
                    repeats: false
                )
            )
            try? await center.add(request)
        }
    }

    private static func notificationTitle(for notificationTime: String) -> String {
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
}

private extension UNUserNotificationCenter {
    func pendingNotificationRequests() async -> [UNNotificationRequest] {
        await withCheckedContinuation { continuation in
            getPendingNotificationRequests { continuation.resume(returning: $0) }
        }
    }
}
