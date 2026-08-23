import CoreText
import Foundation
import GoogleMobileAds
import SwiftUI
import UIKit
import UserMessagingPlatform

enum AppFonts {
    private static let resourceDirectory =
        "compose-resources/composeResources/" +
        "com.restart.spacestationtracker.shared.resources/font"

    private static let registration: Void = {
        ["exo_variable", "exo_italic_variable", "orbitron_variable"].forEach { name in
            guard let url = Bundle.main.url(
                forResource: name,
                withExtension: "ttf",
                subdirectory: resourceDirectory
            ) else { return }
            CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        }
    }()

    static func exo(size: CGFloat, weight: UIFont.Weight = .regular) -> UIFont {
        _ = registration
        let preferredName: String
        if weight.rawValue >= UIFont.Weight.bold.rawValue {
            preferredName = "Exo2_700wght"
        } else if weight.rawValue >= UIFont.Weight.semibold.rawValue {
            preferredName = "Exo2_600wght"
        } else if weight.rawValue >= UIFont.Weight.medium.rawValue {
            preferredName = "Exo2_500wght"
        } else {
            preferredName = "Exo2-Regular"
        }
        return customFont(
            preferredName: preferredName,
            regularName: "Exo2-Regular",
            size: size,
            weight: weight
        )
    }

    static func orbitron(size: CGFloat, weight: UIFont.Weight = .regular) -> UIFont {
        _ = registration
        let preferredName: String
        if weight.rawValue >= UIFont.Weight.bold.rawValue {
            preferredName = "Orbitron_700wght"
        } else if weight.rawValue >= UIFont.Weight.semibold.rawValue {
            preferredName = "Orbitron_600wght"
        } else if weight.rawValue >= UIFont.Weight.medium.rawValue {
            preferredName = "Orbitron_500wght"
        } else {
            preferredName = "Orbitron-Regular"
        }
        return customFont(
            preferredName: preferredName,
            regularName: "Orbitron-Regular",
            size: size,
            weight: weight
        )
    }

    private static func customFont(
        preferredName: String,
        regularName: String,
        size: CGFloat,
        weight: UIFont.Weight
    ) -> UIFont {
        if let font = UIFont(name: preferredName, size: size) {
            return font
        }
        if let regular = UIFont(name: regularName, size: size) {
            let descriptor = regular.fontDescriptor.addingAttributes([
                .traits: [UIFontDescriptor.TraitKey.weight: weight]
            ])
            return UIFont(descriptor: descriptor, size: size)
        }
        return .systemFont(ofSize: size, weight: weight)
    }
}

private enum AdConfiguration {
    static var adsEnabled: Bool {
        infoBoolean(for: "ISSTrackerAdsEnabled", defaultValue: false)
    }

    static var bannerAdUnitId: String? {
        infoString(for: "ISSTrackerBannerAdUnitId")
    }

    static var appOpenAdUnitId: String? {
        infoString(for: "ISSTrackerAppOpenAdUnitId")
    }

    static var passesNativeAdUnitId: String? {
        infoString(for: "ISSTrackerPassesNativeAdUnitId")
    }

    static var crewNativeAdUnitId: String? {
        infoString(for: "ISSTrackerCrewNativeAdUnitId")
    }

    static var isConfigured: Bool {
        adsEnabled &&
            infoString(for: "GADApplicationIdentifier") != nil
    }

    static func nativeAdUnitId(for slotId: String) -> String? {
        slotId.hasPrefix("passes-") ? passesNativeAdUnitId : crewNativeAdUnitId
    }

    private static func infoString(for key: String) -> String? {
        let value = (Bundle.main.object(forInfoDictionaryKey: key) as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return value?.isEmpty == false ? value : nil
    }

    private static func infoBoolean(for key: String, defaultValue: Bool) -> Bool {
        if let value = Bundle.main.object(forInfoDictionaryKey: key) as? Bool {
            return value
        }
        guard let value = Bundle.main.object(forInfoDictionaryKey: key) as? String else {
            return defaultValue
        }
        switch value.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() {
        case "1", "YES", "TRUE":
            return true
        case "0", "NO", "FALSE":
            return false
        default:
            return defaultValue
        }
    }
}

@MainActor
final class AdsService: ObservableObject {
    @Published private(set) var canRequestAds = false
    @Published private(set) var isPrivacyOptionsRequired = false
    @Published private(set) var isAdFree = false
    var onConsentStateChanged: ((Bool, Bool) -> Void)?

    var bannerAdUnitId: String? {
        canRequestAds && !isAdFree ? AdConfiguration.bannerAdUnitId : nil
    }

    private var appOpenAd: AppOpenAd?
    private var appOpenLoadDate: Date?
    private var isLoadingAppOpenAd = false
    private var pendingAppOpenCount = 0
    private var shouldShowAppOpenWhenLoaded = false
    private weak var pendingRootViewController: UIViewController?
    private var isPreparingAds = false
    private var nativeAds: [String: NativeAd] = [:]
    private var nativeAdLoaders: [String: NativeAdSlotLoader] = [:]
    private var nativeAdCallbacks: [String: [(NativeAd) -> Void]] = [:]
    private let appOpenStartThreshold: Int
    private let appOpenExpiration: TimeInterval

    init(
        appOpenStartThreshold: Int,
        appOpenExpiration: TimeInterval
    ) {
        self.appOpenStartThreshold = appOpenStartThreshold
        self.appOpenExpiration = appOpenExpiration
    }

    func configure() {
        guard AdConfiguration.isConfigured, !isPreparingAds else { return }
        isPreparingAds = true

        let parameters = RequestParameters()
        ConsentInformation.shared.requestConsentInfoUpdate(with: parameters) { [weak self] _ in
            Task { @MainActor in
                guard let self else { return }
                try? await ConsentForm.loadAndPresentIfRequired(
                    from: AppModel.topViewController()
                )
                self.refreshConsentState()
                self.startAdsIfAllowed()
                self.isPreparingAds = false
            }
        }
    }

    func handleAppOpen(
        appOpenCount: Int,
        rootViewController: UIViewController?
    ) {
        pendingAppOpenCount = appOpenCount
        pendingRootViewController = rootViewController
        guard canRequestAds, !isAdFree else { return }

        if appOpenCount < appOpenStartThreshold {
            loadAppOpenAdIfNeeded()
            return
        }

        if let ad = appOpenAd, !isAppOpenAdExpired {
            appOpenAd = nil
            appOpenLoadDate = nil
            shouldShowAppOpenWhenLoaded = false
            ad.present(from: rootViewController)
            loadAppOpenAdIfNeeded()
        } else {
            shouldShowAppOpenWhenLoaded = true
            loadAppOpenAdIfNeeded()
        }
    }

    func showPrivacyOptions(rootViewController: UIViewController?) {
        ConsentForm.presentPrivacyOptionsForm(from: rootViewController) { [weak self] _ in
            Task { @MainActor in
                self?.refreshConsentState()
                self?.startAdsIfAllowed()
            }
        }
    }

    func setAdFree(_ enabled: Bool) {
        guard isAdFree != enabled else { return }
        isAdFree = enabled
        if enabled {
            clearLoadedAds()
        } else if canRequestAds {
            loadAppOpenAdIfNeeded()
        }
    }

    func makeNativeAdView(
        slotId: String,
        rootViewController: UIViewController?
    ) -> UIView {
        guard canRequestAds,
              !isAdFree,
              let adUnitId = AdConfiguration.nativeAdUnitId(for: slotId) else {
            return UIView()
        }
        return NativeIssAdContainerView(
            service: self,
            slotId: slotId,
            adUnitId: adUnitId,
            rootViewController: rootViewController
        )
    }

    func makeBannerAdView(rootViewController: UIViewController?) -> UIView {
        guard let adUnitId = bannerAdUnitId else { return UIView() }
        return ResponsiveBannerContainerView(
            adUnitId: adUnitId,
            rootViewController: rootViewController
        )
    }

    private func refreshConsentState() {
        canRequestAds = ConsentInformation.shared.canRequestAds
        isPrivacyOptionsRequired =
            ConsentInformation.shared.privacyOptionsRequirementStatus == .required
        onConsentStateChanged?(canRequestAds, isPrivacyOptionsRequired)
        if !canRequestAds {
            clearLoadedAds()
        }
    }

    private func startAdsIfAllowed() {
        guard canRequestAds else { return }
        MobileAds.shared.start { _ in }
        guard !isAdFree else { return }
        loadAppOpenAdIfNeeded()
        handleAppOpen(
            appOpenCount: pendingAppOpenCount,
            rootViewController: pendingRootViewController ?? AppModel.topViewController()
        )
    }

    private var isAppOpenAdExpired: Bool {
        guard let appOpenLoadDate else { return true }
        return Date().timeIntervalSince(appOpenLoadDate) > appOpenExpiration
    }

    private func loadAppOpenAdIfNeeded() {
        guard canRequestAds,
              !isAdFree,
              !isLoadingAppOpenAd,
              appOpenAd == nil || isAppOpenAdExpired,
              let adUnitId = AdConfiguration.appOpenAdUnitId else {
            return
        }

        isLoadingAppOpenAd = true
        AppOpenAd.load(with: adUnitId, request: Request()) { [weak self] ad, _ in
            Task { @MainActor in
                guard let self else { return }
                self.isLoadingAppOpenAd = false
                guard !self.isAdFree, self.canRequestAds else {
                    self.appOpenAd = nil
                    self.appOpenLoadDate = nil
                    return
                }
                self.appOpenAd = ad
                self.appOpenLoadDate = ad == nil ? nil : Date()
                if let ad, self.shouldShowAppOpenWhenLoaded {
                    self.shouldShowAppOpenWhenLoaded = false
                    self.appOpenAd = nil
                    self.appOpenLoadDate = nil
                    ad.present(
                        from: self.pendingRootViewController ?? AppModel.topViewController()
                    )
                    self.loadAppOpenAdIfNeeded()
                }
            }
        }
    }

    fileprivate func loadNativeAd(
        slotId: String,
        adUnitId: String,
        rootViewController: UIViewController?,
        onLoaded: @escaping (NativeAd) -> Void
    ) {
        guard canRequestAds, !isAdFree else { return }
        if let cachedAd = nativeAds[slotId] {
            cachedAd.rootViewController = rootViewController
            onLoaded(cachedAd)
            return
        }

        nativeAdCallbacks[slotId, default: []].append(onLoaded)
        guard nativeAdLoaders[slotId] == nil else { return }

        let loader = NativeAdSlotLoader(
            adUnitId: adUnitId,
            rootViewController: rootViewController,
            onLoaded: { [weak self] ad in
                guard let self, !self.isAdFree else { return }
                ad.rootViewController = rootViewController
                self.nativeAds[slotId] = ad
                let callbacks = self.nativeAdCallbacks.removeValue(forKey: slotId) ?? []
                callbacks.forEach { $0(ad) }
                self.nativeAdLoaders[slotId] = nil
            },
            onFailed: { [weak self] in
                self?.nativeAdCallbacks[slotId] = nil
                self?.nativeAdLoaders[slotId] = nil
            }
        )
        nativeAdLoaders[slotId] = loader
        loader.load()
    }

    private func clearLoadedAds() {
        appOpenAd = nil
        appOpenLoadDate = nil
        isLoadingAppOpenAd = false
        shouldShowAppOpenWhenLoaded = false
        nativeAds.removeAll()
        nativeAdLoaders.removeAll()
        nativeAdCallbacks.removeAll()
    }
}

final class ResponsiveBannerContainerView: UIView, BannerViewDelegate {
    private let bannerView = BannerView(adSize: AdSizeBanner)
    private let adUnitId: String
    private var loadedWidth: CGFloat = 0

    weak var rootViewController: UIViewController? {
        didSet {
            bannerView.rootViewController = rootViewController
        }
    }

    init(adUnitId: String, rootViewController: UIViewController?) {
        self.adUnitId = adUnitId
        self.rootViewController = rootViewController
        super.init(frame: .zero)

        backgroundColor = .clear
        isOpaque = false
        bannerView.adUnitID = adUnitId
        bannerView.rootViewController = rootViewController
        bannerView.delegate = self
        bannerView.isHidden = true
        bannerView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(bannerView)

        NSLayoutConstraint.activate([
            bannerView.centerXAnchor.constraint(equalTo: centerXAnchor),
            bannerView.centerYAnchor.constraint(equalTo: centerYAnchor)
        ])
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let availableWidth = bounds.width
        guard availableWidth > 0,
              abs(availableWidth - loadedWidth) > 1 else {
            return
        }
        loadedWidth = availableWidth
        bannerView.adSize = inlineAdaptiveBanner(
            width: availableWidth,
            maxHeight: 50
        )
        bannerView.load(Request())
    }

    func bannerViewDidReceiveAd(_ bannerView: BannerView) {
        bannerView.isHidden = false
    }

    func bannerView(
        _ bannerView: BannerView,
        didFailToReceiveAdWithError error: Error
    ) {
        bannerView.isHidden = true
    }
}

private final class NativeAdSlotLoader: NSObject, NativeAdLoaderDelegate {
    private let adUnitId: String
    private weak var rootViewController: UIViewController?
    private var adLoader: AdLoader?
    private let onLoaded: (NativeAd) -> Void
    private let onFailed: () -> Void

    init(
        adUnitId: String,
        rootViewController: UIViewController?,
        onLoaded: @escaping (NativeAd) -> Void,
        onFailed: @escaping () -> Void
    ) {
        self.adUnitId = adUnitId
        self.rootViewController = rootViewController
        self.onLoaded = onLoaded
        self.onFailed = onFailed
    }

    func load() {
        let loader = AdLoader(
            adUnitID: adUnitId,
            rootViewController: rootViewController,
            adTypes: [.native],
            options: nil
        )
        adLoader = loader
        loader.delegate = self
        loader.load(Request())
    }

    func adLoader(_ adLoader: AdLoader, didReceive nativeAd: NativeAd) {
        onLoaded(nativeAd)
    }

    func adLoader(_ adLoader: AdLoader, didFailToReceiveAdWithError error: Error) {
        onFailed()
    }
}

private final class NativeIssAdContainerView: UIView {
    private weak var service: AdsService?
    private let slotId: String
    private let adUnitId: String
    private weak var rootViewController: UIViewController?
    private let adView = NativeIssAdView()

    init(
        service: AdsService,
        slotId: String,
        adUnitId: String,
        rootViewController: UIViewController?
    ) {
        self.service = service
        self.slotId = slotId
        self.adUnitId = adUnitId
        self.rootViewController = rootViewController
        super.init(frame: .zero)

        backgroundColor = .clear
        isOpaque = false
        adView.isHidden = true
        adView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(adView)
        NSLayoutConstraint.activate([
            adView.leadingAnchor.constraint(equalTo: leadingAnchor),
            adView.trailingAnchor.constraint(equalTo: trailingAnchor),
            adView.topAnchor.constraint(equalTo: topAnchor),
            adView.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])
        loadAd()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func loadAd() {
        service?.loadNativeAd(
            slotId: slotId,
            adUnitId: adUnitId,
            rootViewController: rootViewController
        ) { [weak self] nativeAd in
            guard let self else { return }
            nativeAd.rootViewController = self.rootViewController
            self.adView.bind(nativeAd)
            self.adView.isHidden = false
        }
    }
}

private final class NativeIssAdView: NativeAdView {
    private let media = MediaView()
    private let iconImageView = UIImageView()
    private let badgeLabel = UILabel()
    private let advertiserLabel = UILabel()
    private let headlineLabel = UILabel()
    private let bodyLabel = UILabel()
    private let starRatingLabel = UILabel()
    private let priceLabel = UILabel()
    private let storeLabel = UILabel()
    private let metadataStack = UIStackView()
    private let callToActionButton = UIButton(type: .system)
    private let adChoices = AdChoicesView()

    override init(frame: CGRect) {
        super.init(frame: frame)
        configureLayout()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func bind(_ ad: NativeAd) {
        media.mediaContent = ad.mediaContent
        iconImageView.image = ad.icon?.image
        iconImageView.isHidden = ad.icon?.image == nil
        advertiserLabel.text = ad.advertiser
        advertiserLabel.isHidden = ad.advertiser?.isEmpty ?? true
        headlineLabel.text = ad.headline
        bodyLabel.text = ad.body
        bodyLabel.isHidden = ad.body?.isEmpty ?? true
        if let rating = ad.starRating?.doubleValue, rating > 0 {
            starRatingLabel.text = String(format: "★ %.1f", rating)
            starRatingLabel.isHidden = false
        } else {
            starRatingLabel.text = nil
            starRatingLabel.isHidden = true
        }
        priceLabel.text = ad.price
        priceLabel.isHidden = ad.price?.isEmpty ?? true
        storeLabel.text = ad.store
        storeLabel.isHidden = ad.store?.isEmpty ?? true
        metadataStack.isHidden = starRatingLabel.isHidden &&
            priceLabel.isHidden && storeLabel.isHidden
        callToActionButton.setTitle(ad.callToAction, for: .normal)
        callToActionButton.isHidden = ad.callToAction?.isEmpty ?? true
        nativeAd = ad
    }

    private func configureLayout() {
        backgroundColor = UIColor { traits in
            traits.userInterfaceStyle == .dark
                ? UIColor(
                    red: 12.0 / 255.0,
                    green: 18.0 / 255.0,
                    blue: 68.0 / 255.0,
                    alpha: 1
                )
                : .white
        }
        layer.cornerRadius = 12
        layer.cornerCurve = .continuous
        layer.borderWidth = 1
        layer.borderColor = UIColor.separator.withAlphaComponent(0.35).cgColor
        clipsToBounds = true

        media.backgroundColor = .tertiarySystemBackground
        media.layer.cornerRadius = 8
        media.clipsToBounds = true
        media.translatesAutoresizingMaskIntoConstraints = false

        iconImageView.contentMode = .scaleAspectFill
        iconImageView.layer.cornerRadius = 8
        iconImageView.clipsToBounds = true
        iconImageView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            iconImageView.widthAnchor.constraint(equalToConstant: 58),
            iconImageView.heightAnchor.constraint(equalToConstant: 58)
        ])

        badgeLabel.text = "Ad"
        badgeLabel.font = AppFonts.exo(size: 11, weight: .semibold)
        badgeLabel.textAlignment = .center
        badgeLabel.textColor = UIColor(
            red: 0,
            green: 0,
            blue: 32.0 / 255.0,
            alpha: 1
        )
        badgeLabel.backgroundColor = UIColor(
            red: 1,
            green: 235.0 / 255.0,
            blue: 59.0 / 255.0,
            alpha: 1
        )
        badgeLabel.layer.cornerRadius = 4
        badgeLabel.clipsToBounds = true
        NSLayoutConstraint.activate([
            badgeLabel.widthAnchor.constraint(greaterThanOrEqualToConstant: 24),
            badgeLabel.heightAnchor.constraint(equalToConstant: 20)
        ])
        advertiserLabel.font = AppFonts.exo(size: 12, weight: .semibold)
        advertiserLabel.textColor = .secondaryLabel
        advertiserLabel.numberOfLines = 1
        headlineLabel.font = AppFonts.orbitron(size: 18, weight: .bold)
        headlineLabel.textColor = .label
        headlineLabel.numberOfLines = 2
        bodyLabel.font = AppFonts.exo(size: 13)
        bodyLabel.textColor = .secondaryLabel
        bodyLabel.numberOfLines = 3

        [starRatingLabel, priceLabel, storeLabel].forEach { label in
            label.font = AppFonts.exo(size: 12)
            label.textColor = .secondaryLabel
            label.numberOfLines = 1
        }
        starRatingLabel.textColor = UIColor(
            red: 1,
            green: 235.0 / 255.0,
            blue: 59.0 / 255.0,
            alpha: 1
        )

        var buttonConfiguration = UIButton.Configuration.filled()
        buttonConfiguration.baseBackgroundColor = UIColor { traits in
            traits.userInterfaceStyle == .dark
                ? UIColor(red: 1, green: 235.0 / 255.0, blue: 59.0 / 255.0, alpha: 1)
                : UIColor(red: 0, green: 87.0 / 255.0, blue: 146.0 / 255.0, alpha: 1)
        }
        buttonConfiguration.baseForegroundColor = UIColor { traits in
            traits.userInterfaceStyle == .dark
                ? UIColor(red: 0, green: 0, blue: 32.0 / 255.0, alpha: 1)
                : .white
        }
        buttonConfiguration.cornerStyle = .medium
        buttonConfiguration.titleTextAttributesTransformer =
            UIConfigurationTextAttributesTransformer { incoming in
                var outgoing = incoming
                outgoing.font = AppFonts.exo(size: 15, weight: .semibold)
                return outgoing
            }
        callToActionButton.configuration = buttonConfiguration
        callToActionButton.isUserInteractionEnabled = false

        badgeLabel.setContentHuggingPriority(.required, for: .horizontal)
        badgeLabel.setContentCompressionResistancePriority(.required, for: .horizontal)
        let headerSpacer = UIView()
        let header = UIStackView(
            arrangedSubviews: [badgeLabel, advertiserLabel, headerSpacer]
        )
        header.axis = .horizontal
        header.alignment = .center
        header.spacing = 8

        let textStack = UIStackView(arrangedSubviews: [headlineLabel, header])
        textStack.axis = .vertical
        textStack.alignment = .fill
        textStack.spacing = 4

        let topRow = UIStackView(arrangedSubviews: [iconImageView, textStack])
        topRow.axis = .horizontal
        topRow.alignment = .top
        topRow.spacing = 12

        metadataStack.addArrangedSubview(starRatingLabel)
        metadataStack.addArrangedSubview(priceLabel)
        metadataStack.addArrangedSubview(storeLabel)
        metadataStack.axis = .horizontal
        metadataStack.alignment = .center
        metadataStack.spacing = 8

        let content = UIStackView(
            arrangedSubviews: [topRow, media, bodyLabel, metadataStack, callToActionButton]
        )
        content.axis = .vertical
        content.alignment = .fill
        content.spacing = 9
        content.translatesAutoresizingMaskIntoConstraints = false
        adChoices.translatesAutoresizingMaskIntoConstraints = false
        addSubview(content)
        addSubview(adChoices)

        mediaView = media
        iconView = iconImageView
        advertiserView = advertiserLabel
        headlineView = headlineLabel
        bodyView = bodyLabel
        starRatingView = starRatingLabel
        priceView = priceLabel
        storeView = storeLabel
        callToActionView = callToActionButton
        adChoicesView = adChoices

        NSLayoutConstraint.activate([
            content.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 14),
            content.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -14),
            content.topAnchor.constraint(equalTo: topAnchor, constant: 12),
            content.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -12),
            media.heightAnchor.constraint(greaterThanOrEqualToConstant: 120),
            callToActionButton.heightAnchor.constraint(equalToConstant: 38),
            adChoices.topAnchor.constraint(equalTo: topAnchor, constant: 4),
            adChoices.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -4)
        ])
    }
}
