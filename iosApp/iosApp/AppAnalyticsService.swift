import Foundation
#if canImport(FirebaseAnalytics)
import FirebaseAnalytics
#endif

/// Structured product analytics only. Do not add coordinates, location names,
/// URLs, astronaut names, transaction identifiers, or raw error descriptions.
@MainActor
final class AppAnalyticsService {
    static let shared = AppAnalyticsService()

    private var currentScreen = "unknown"

    private init() {}

    func configure() {
        #if canImport(FirebaseAnalytics)
        Analytics.setAnalyticsCollectionEnabled(true)
        Analytics.setUserProperty("ios", forName: "platform")
        #endif
    }

    func trackScreen(_ value: String?) {
        let screenName = normalizedScreen(value)
        currentScreen = screenName
        #if canImport(FirebaseAnalytics)
        Analytics.logEvent(
            AnalyticsEventScreenView,
            parameters: [
                AnalyticsParameterScreenName: screenName,
                AnalyticsParameterScreenClass: screenName
            ]
        )
        #endif
    }

    func trackInteraction(_ actionId: String) {
        #if canImport(FirebaseAnalytics)
        Analytics.logEvent(
            "feature_interaction",
            parameters: [
                "screen_name": currentScreen,
                "action": normalizedAction(actionId)
            ]
        )
        #endif
    }

    func trackSetting(_ setting: String, value: String) {
        #if canImport(FirebaseAnalytics)
        Analytics.logEvent(
            "setting_changed",
            parameters: [
                "setting_name": sanitize(setting),
                "setting_value": sanitize(value)
            ]
        )
        #endif
    }

    func trackPurchaseFlow(_ stage: String) {
        #if canImport(FirebaseAnalytics)
        Analytics.logEvent(
            "ad_removal_flow",
            parameters: ["stage": sanitize(stage)]
        )
        #endif
    }

    func updateAdFreeState(_ isAdFree: Bool) {
        #if canImport(FirebaseAnalytics)
        Analytics.setUserProperty(isAdFree.description, forName: "ad_free")
        #endif
    }

    private func normalizedScreen(_ value: String?) -> String {
        switch value?.uppercased() {
        case "MAP": "map"
        case "PASSES": "sky_path"
        case "CREW": "on_duty"
        case "SETTINGS": "settings"
        case "ABOUT": "about"
        case "PRIVACY_POLICY": "privacy_policy"
        case "TERMS_OF_USE": "terms_of_use"
        default: "unknown"
        }
    }

    private func normalizedAction(_ actionId: String) -> String {
        switch actionId {
        case "open_url":
            currentScreen == "map" ? "open_live_stream" : "open_external_content"
        case "request_location": "request_location"
        case "meaningful_interaction": "select_tab"
        case "schedule_pass_notification": "schedule_pass_alert"
        case "add_pass_to_calendar": "add_pass_to_calendar"
        case "share_pass": "share_pass"
        case "enable_automatic_pass_alerts": "enable_automatic_pass_alerts"
        case "disable_automatic_pass_alerts": "disable_automatic_pass_alerts"
        case "open_background_settings": "open_background_settings"
        case "privacy_choices": "open_privacy_choices"
        case "refresh_automatic_pass_alerts": "change_pass_alert_preferences"
        case "purchase_ad_removal": "start_ad_removal_purchase"
        case "restore_ad_removal": "restore_ad_removal_purchase"
        case "contact_support": "contact_support"
        case "rate_app": "rate_app"
        case "share_app": "share_app"
        case "retry_crew": "retry_crew"
        default: sanitize(actionId)
        }
    }

    private func sanitize(_ value: String) -> String {
        let filtered = value
            .lowercased()
            .replacingOccurrences(of: "[^a-z0-9_]+", with: "_", options: .regularExpression)
            .trimmingCharacters(in: CharacterSet(charactersIn: "_"))
        let result = String(filtered.prefix(40))
        return result.isEmpty ? "unknown" : result
    }
}
