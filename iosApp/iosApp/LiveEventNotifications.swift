import Foundation
import ISSTrackerShared
import UserNotifications

/// Local alerts shortly before NASA's scheduled broadcasts (spacewalks, launches, dockings).
enum LiveEventNotificationScheduler {
    static let identifierPrefix = "live-event-"
    static let urlKey = "live_event_url"

    /// Replaces every pending live-event alert with `alerts`.
    static func replace(with alerts: [IosLiveEventAlertPayload]) async {
        await cancelAll()
        for alert in alerts {
            await add(alert)
        }
    }

    static func cancelAll() async {
        let center = UNUserNotificationCenter.current()
        let pending = await center.pendingNotificationRequests()
        center.removePendingNotificationRequests(
            withIdentifiers: pending.map(\.identifier).filter { $0.hasPrefix(identifierPrefix) }
        )
    }

    private static func add(_ alert: IosLiveEventAlertPayload) async {
        let triggerDate = Date(timeIntervalSince1970: TimeInterval(alert.triggerTimeMillis) / 1_000)
        guard triggerDate > Date() else { return }
        let startDate = Date(timeIntervalSince1970: TimeInterval(alert.scheduledStartMillis) / 1_000)

        let content = UNMutableNotificationContent()
        content.title = String(localized: "notification.live_event.title")
        content.body = String(
            format: String(localized: "notification.live_event.body.format"),
            alert.title,
            startDate.formatted(date: .omitted, time: .shortened)
        )
        content.sound = .default
        content.userInfo = [urlKey: alert.watchUrl]

        // Pinning the time zone keeps the alert at the same instant if the device changes zones.
        let components = Calendar.current.dateComponents(
            [.timeZone, .year, .month, .day, .hour, .minute, .second],
            from: triggerDate
        )
        let request = UNNotificationRequest(
            identifier: alert.identifier,
            content: content,
            trigger: UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        )
        try? await UNUserNotificationCenter.current().add(request)
    }
}
