import Foundation
import ISSTrackerShared
@preconcurrency import UserNotifications

/// One pass reminder ready to be handed to `UNUserNotificationCenter`.
struct PassNotificationRequest {
    let startTimeMillis: Int64
    let triggerTimeMillis: Int64
    let notificationTime: String
    let identifierSuffix: String
    let startAzimuthCompass: String
    let maxElevation: Double
}

enum PassNotificationScheduler {
    static let automaticPrefix = "automatic:"
    static let manualPrefix = "manual:"

    /// iOS keeps at most 64 pending local notifications per app and silently drops the rest.
    private static let systemPendingLimit = 64
    /// Headroom so a few manual reminders can still be added after an automatic refresh.
    private static let reservedManualSlots = 8

    /// Replaces every pending automatic reminder with `requests`, soonest first, within the
    /// system limit. Removal is awaited before anything is added, so new reminders are never
    /// deleted by a late cancellation.
    static func replaceAutomatic(with requests: [PassNotificationRequest]) async {
        let center = UNUserNotificationCenter.current()
        let pending = await center.pendingNotificationRequests()
        center.removePendingNotificationRequests(
            withIdentifiers: pending.map(\.identifier).filter { $0.hasPrefix(automaticPrefix) }
        )

        let manualCount = pending.filter { $0.identifier.hasPrefix(manualPrefix) }.count
        let budget = max(0, systemPendingLimit - reservedManualSlots - manualCount)
        let upcoming = requests
            .filter { $0.triggerTimeMillis > nowMillis }
            .sorted { $0.triggerTimeMillis < $1.triggerTimeMillis }
            .prefix(budget)
        for request in upcoming {
            await add(request, prefix: automaticPrefix)
        }
    }

    static func cancelAutomatic() async {
        let center = UNUserNotificationCenter.current()
        let pending = await center.pendingNotificationRequests()
        center.removePendingNotificationRequests(
            withIdentifiers: pending.map(\.identifier).filter { $0.hasPrefix(automaticPrefix) }
        )
    }

    /// Adds a manual reminder. Returns false when iOS's pending limit is already reached.
    @discardableResult
    static func addManual(_ request: PassNotificationRequest) async -> Bool {
        let pending = await UNUserNotificationCenter.current().pendingNotificationRequests()
        let identifier = identifier(for: request, prefix: manualPrefix)
        let isReplacing = pending.contains { $0.identifier == identifier }
        guard isReplacing || pending.count < systemPendingLimit else { return false }
        await add(request, prefix: manualPrefix)
        return true
    }

    private static func add(_ request: PassNotificationRequest, prefix: String) async {
        let triggerDate = Date(
            timeIntervalSince1970: TimeInterval(request.triggerTimeMillis) / 1_000
        )
        guard triggerDate > Date() else { return }

        let content = UNMutableNotificationContent()
        content.title = PassNotificationText.title(for: request.notificationTime)
        content.body = PassNotificationText.body(
            compass: CompassLabels.shared.localize(code: request.startAzimuthCompass),
            maxElevation: request.maxElevation
        )
        content.sound = .default
        content.userInfo = [PassNotificationText.passStartTimeKey: request.startTimeMillis]

        let components = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute, .second],
            from: triggerDate
        )
        let notificationRequest = UNNotificationRequest(
            identifier: identifier(for: request, prefix: prefix),
            content: content,
            trigger: UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        )
        try? await UNUserNotificationCenter.current().add(notificationRequest)
    }

    private static func identifier(for request: PassNotificationRequest, prefix: String) -> String {
        "\(prefix)\(request.startTimeMillis):\(request.identifierSuffix)"
    }

    private static var nowMillis: Int64 {
        Int64(Date().timeIntervalSince1970 * 1_000)
    }
}

enum PassNotificationText {
    static let passStartTimeKey = "pass_start_time"

    static func title(for notificationTime: String) -> String {
        switch notificationTime {
        case "At time of event": String(localized: "notification.title.at_event")
        case "10 minutes before": String(localized: "notification.title.10_minutes")
        case "1 hour before": String(localized: "notification.title.1_hour")
        case "12 hours before": String(localized: "notification.title.12_hours")
        case "1 day before": String(localized: "notification.title.1_day")
        case "1 week before": String(localized: "notification.title.1_week")
        default: String(localized: "notification.title.upcoming")
        }
    }

    static func body(compass: String, maxElevation: Double) -> String {
        String(
            format: String(localized: "notification.body.format"),
            compass,
            Int(maxElevation.rounded())
        )
    }
}

/// Routes taps on pass reminders to the Sky Path tab, including when the tap cold-launches the app.
final class PassNotificationDelegate: NSObject, UNUserNotificationCenterDelegate, @unchecked Sendable {
    static let shared = PassNotificationDelegate()

    private let lock = NSLock()
    private var pendingOpen = false
    private var openHandler: (@MainActor () -> Void)?

    @MainActor
    func setOpenPassesHandler(_ handler: @escaping @MainActor () -> Void) {
        let shouldOpen = lock.withLock {
            openHandler = handler
            defer { pendingOpen = false }
            return pendingOpen
        }
        if shouldOpen { handler() }
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        let identifier = response.notification.request.identifier
        guard identifier.hasPrefix(PassNotificationScheduler.automaticPrefix) ||
                identifier.hasPrefix(PassNotificationScheduler.manualPrefix) else { return }
        // Scoped locking: a plain lock()/unlock() pair isn't allowed in async code under Swift 6.
        let handler = lock.withLock {
            if openHandler == nil { pendingOpen = true }
            return openHandler
        }
        if let handler {
            await MainActor.run { handler() }
        }
    }
}
