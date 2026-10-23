package com.restart.spacestationtracker.shared.ui

internal object SharedLegalContent {
    const val privacyPolicy = """Last Updated: October 22, 2026

ISS Tracker (also listed as Space Station Tracker) is provided by Restart. This policy applies to the Android and iOS apps. The app does not require an account. If you contact support, we receive the email address, message, and any information you choose to include so we can respond.

Location

ISS Tracker requests location permission to find visible ISS passes and configure pass alerts. Predictions are calculated on your device using downloaded orbital elements; the app does not send your coordinates to CelesTrak or Where the ISS at? to calculate passes. The platform location service and reverse-geocoding service may process your coordinates to determine your position and a nearby place name, under the platform provider's privacy terms. Saved alert coordinates, altitude, and place names are stored locally. ISS Tracker does not operate a server that stores your location history.

Local App Data

Your theme, map preferences, alert configuration, scheduled reminder information, purchase entitlement flag, review eligibility, advertising launch count, and cached orbital elements are stored using platform storage. These settings are not synced through an ISS Tracker account. Device backups or device-to-device transfers may retain or restore local data according to your operating-system settings.

Notifications and Calendar

Reminders are scheduled as local device notifications. Automatic alerts may use background refresh and, on Android, alarm scheduling. Calendar access or the system calendar editor is used when you choose to add a pass. Calendar entries and content you explicitly share are handled by the destination app or service. You can revoke permissions in system settings. Disabling automatic alerts does not cancel manually scheduled reminders or delete calendar entries.

Advertising and Consent

ISS Tracker uses Google AdMob and Google User Messaging Platform. Advertising requests begin only when Google's consent status says ads may be requested. Depending on your region and choices, Google and its partners may process device information, IP address, advertising identifiers, approximate location, and ad interactions to deliver and measure ads.

On iOS, the app requests App Tracking Transparency permission when applicable before preparing advertising consent. The app remains usable if you decline tracking. A Privacy Choices entry is shown when Google requires one. AdMob choices and Apple's tracking permission do not themselves disable the app's separately enabled Analytics or Crashlytics collection.

Google's privacy policy is available at https://policies.google.com/privacy.

Purchases

The optional lifetime ad-removal purchase is processed by Google Play or the Apple App Store. The app uses store purchase records and entitlement status to complete and restore the purchase and stores an ad-removal flag locally. ISS Tracker does not receive your payment-card details. Removing ads does not disable Analytics, Crashlytics, maps, or other network requests.

Analytics and Diagnostics

ISS Tracker uses Google Analytics for Firebase to measure app launches, sessions, active users, screen views, feature interactions, settings changes, and purchase-flow outcomes. This helps understand general usage and improve the app. ISS Tracker does not send precise coordinates, saved location names, astronaut names, live-stream URLs, calendar or notification contents, support-email contents, payment-card details, transaction identifiers, or raw diagnostic messages as custom analytics parameters. The Analytics SDK uses an app-instance identifier and may derive approximate location and device information as described by Google. Advertising personalization signals from Analytics are disabled by default.

ISS Tracker also includes Firebase Crashlytics on Android and iOS. Crashlytics automatically collects crash reports and related diagnostics, including installation identifiers, stack traces, exception information, app version, and device/operating-system information, to help investigate failures. Analytics events may also appear as crash-report breadcrumbs.

Analytics and Crashlytics are currently enabled separately from advertising consent, and the app does not currently provide an in-app switch for either service. Google's descriptions of these services and their data handling are available at https://firebase.google.com/support/privacy and https://policies.google.com/privacy.

Third-Party Data Providers

The app downloads satellite positions and orbital elements from Where the ISS at? and CelesTrak, crew information and live-stream listings from GitHub-hosted feeds, and biographies and images from Wikipedia/Wikimedia and other image hosts referenced by those feeds. Maps use Google Maps on Android and Apple MapKit on iOS. Opening a live stream or another external link sends you to YouTube or the selected destination service. Requests expose standard network information such as your IP address to the relevant provider; map requests also identify the area being displayed. Each provider applies its own privacy terms. Apple describes its practices at https://www.apple.com/legal/privacy/ and GitHub at https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement.

Retention, Deletion, and Security

Local settings and saved alert locations are retained until app data is cleared or the app is deleted, subject to platform backup and restore behavior. On iOS, offloading an app can retain its data. Pending notifications and calendar entries are managed by the operating system; delete calendar entries separately in your calendar app.

Uninstalling the app does not delete information already received by Google or other providers, store purchase records, backups, or support emails. Analytics retention depends on the project's Google Analytics settings and the type of data. Firebase currently states that Crashlytics retains crash traces and associated identifiers for 90 days before beginning removal from live and backup systems. Providers may retain other records under their own policies or legal obligations. Support correspondence is retained as needed to address the request and meet applicable obligations. Contact restartapplication@gmail.com to ask about access or deletion; we will explain what records we can identify and what requests must be handled by the relevant provider.

App data requests use HTTPS, and providers may process information in other countries. Device storage and backups are protected by platform controls. No network or storage system can be guaranteed completely secure.

Children

ISS Tracker is not directed to children under 13. If you believe a child has provided personal information through the app or support, contact us so we can investigate and address it. The app has no user accounts; this does not mean its third-party SDKs collect no data.

Your Choices

You can change location, notification, tracking, and advertising choices where the platform or consent form offers them, and clear local app data through platform controls. Access, correction, deletion, objection, or other privacy rights may apply depending on where you live. Contact us to exercise applicable rights or ask about the current retention settings. You may also contact your relevant data-protection authority.

Changes and Contact

This policy may be updated when app features or third-party services change. The date above identifies the latest revision. Questions can be sent to Restart at restartapplication@gmail.com."""

    const val termsOfUse = """Last Updated: October 22, 2026

Please read these Terms of Use ("Terms") before using ISS Tracker, also listed as Space Station Tracker (the "Service"), provided by Restart. These Terms apply to the Android and iOS apps.

Your access to and use of the Service is conditioned on your acceptance of and compliance with these Terms. These Terms apply to all visitors, users, and others who access or use the Service.

By accessing or using the Service, you agree to be bound by these Terms. If you disagree with any part of the terms, then you may not access the Service.

1. Use License

You may install and use the app on devices permitted by your app store's usage rules. The app and third-party materials are licensed, not sold. Do not misuse the Service, interfere with its operation, or remove legally required notices.

Open-source components remain governed by their own licenses. The project's source is available at https://github.com/Kiarasht/Space-Station-Tracker with the Mozilla Public License 2.0 in LICENSE.txt. These Terms do not limit rights granted by that license, other applicable open-source licenses, or mandatory law. Third-party images, text, trademarks, maps, and data remain subject to their owners' rights and applicable licenses.

2. Disclaimer

To the extent permitted by applicable law, the Service and its materials are provided on an "as is" and "as available" basis without warranties, including implied warranties of merchantability, fitness for a particular purpose, or non-infringement. This does not exclude rights or guarantees that cannot legally be excluded.

Satellite positions, orbital elements, crew details, and other information rely on third-party data. Visible ISS passes are estimated on your device. Predictions can change with orbital maneuvers, data age, location accuracy, weather, and viewing conditions. Notifications and background refresh depend on device permissions, connectivity, and operating-system scheduling and may be delayed or missed. The Service is for educational and informational use and is not a navigation, emergency, or safety-critical system. Observe from a safe place and never look at the Sun through unprotected optical equipment.

3. Limitation of Liability

To the extent permitted by applicable law, Restart and its suppliers are not liable for indirect or consequential loss arising from use of, or inability to use, the Service. Nothing in these Terms excludes liability or consumer remedies that cannot legally be excluded or restricted.

4. Third-Party Services and Advertisements

The Service uses third-party APIs to provide its core functionality and displays third-party advertisements through Google AdMob. We have no control over and assume no responsibility for the content, privacy policies, or practices of third-party websites or services. You acknowledge and agree that we are not responsible or liable, directly or indirectly, for damage or loss caused or alleged to be caused by use of or reliance on content, goods, or services available through those websites or services.

Third-party availability, content, and terms can change. The app is an independent product and is not an official NASA, ESA, Roscosmos, CNSA, Google, or Apple app. Our Privacy Policy describes advertising, Analytics, Crashlytics, location, and other data handling. Accepting these Terms does not replace any consent required by law or the platform.

5. Optional Purchases

The current paid feature is lifetime removal of in-app advertising through the one-time product remove_ads_lifetime. It is not a subscription. The store shows the applicable localized price and handles payment, refunds, and purchase records. Applicable store rules and statutory consumer rights continue to apply.

Use Restore Purchases with the same store account to restore an eligible purchase. An Apple purchase does not automatically transfer to Google Play or vice versa. A refunded or revoked purchase can remove the entitlement. Ad removal does not disable Analytics, Crashlytics, third-party maps, or external content, and does not remove advertising shown by an external service such as YouTube. It does not promise indefinite operation of the app or its data providers, or automatic access to future separately priced products, subject to mandatory law.

6. Changes to These Terms

We reserve the right, at our sole discretion, to modify or replace these Terms at any time. We will try to provide at least 30 days' notice before new terms take effect. What constitutes a material change will be determined at our sole discretion.

By continuing to access or use the Service after revisions become effective, you agree to be bound by the revised terms. If you do not agree to the new terms, stop using the Service.

7. Governing Law

These Terms are governed by the laws of Washington, United States, except where applicable law requires otherwise. Mandatory consumer protections and any non-waivable rights to bring a claim in your local courts continue to apply.

Contact Us

If you have questions about these Terms, contact us at restartapplication@gmail.com."""
}
