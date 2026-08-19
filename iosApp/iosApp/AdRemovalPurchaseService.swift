import Foundation
import StoreKit

struct AdRemovalPurchaseState {
    var priceText = "$9.99"
    var isPurchaseInProgress = false
    var isPurchaseAvailable = true
    var isEntitlementCheckComplete = false
    var statusCode: String?
}

private enum PurchaseStatusCode {
    static let unavailable = "unavailable"
    static let alreadyRemoved = "already_removed"
    static let canceled = "canceled"
    static let failed = "failed"
    static let notConfigured = "not_configured"
    static let checking = "checking"
    static let restoreNotFound = "restore_not_found"
    static let pending = "pending"
    static let restored = "restored"
    static let removed = "removed"
}

@MainActor
final class AdRemovalPurchaseService {
    private enum Constants {
        static let productId = "remove_ads_lifetime"
        static let entitlementKey = "ad_removal_lifetime_enabled"
        static let settingsSuite = "settings"
    }

    private let defaults = UserDefaults(suiteName: Constants.settingsSuite) ?? .standard
    private var product: Product?
    private var transactionUpdatesTask: Task<Void, Never>?

    private(set) var state = AdRemovalPurchaseState() {
        didSet { onStateChanged?(state) }
    }
    private(set) var isEntitled: Bool

    var onStateChanged: ((AdRemovalPurchaseState) -> Void)?
    var onEntitlementChanged: ((Bool) -> Void)?

    init() {
        isEntitled = defaults.bool(forKey: Constants.entitlementKey)
    }

    deinit {
        transactionUpdatesTask?.cancel()
    }

    func configure() {
        transactionUpdatesTask?.cancel()
        transactionUpdatesTask = Task { [weak self] in
            for await update in Transaction.updates {
                await self?.handle(transactionUpdate: update)
            }
        }

        Task {
            await loadProduct()
            await refreshCurrentEntitlements(showRestoreMessage: false)
        }
    }

    func refreshEntitlements() {
        Task {
            await refreshCurrentEntitlements(showRestoreMessage: false)
        }
    }

    func purchase() {
        AppAnalyticsService.shared.trackPurchaseFlow("started")
        guard !isEntitled else {
            AppAnalyticsService.shared.trackPurchaseFlow("already_owned")
            state.statusCode = PurchaseStatusCode.alreadyRemoved
            return
        }

        Task {
            if product == nil {
                await loadProduct()
            }
            guard let product else {
                AppAnalyticsService.shared.trackPurchaseFlow("unavailable")
                state.isPurchaseAvailable = false
                state.statusCode = PurchaseStatusCode.notConfigured
                return
            }

            state.isPurchaseInProgress = true
            state.statusCode = nil
            do {
                switch try await product.purchase() {
                case .success(let verification):
                    let transaction = try verifiedTransaction(from: verification)
                    AppAnalyticsService.shared.trackPurchaseFlow("completed")
                    activateEntitlement(statusCode: PurchaseStatusCode.removed)
                    await transaction.finish()
                case .pending:
                    AppAnalyticsService.shared.trackPurchaseFlow("pending")
                    state.statusCode = PurchaseStatusCode.pending
                case .userCancelled:
                    AppAnalyticsService.shared.trackPurchaseFlow("cancelled")
                    state.statusCode = PurchaseStatusCode.canceled
                @unknown default:
                    AppAnalyticsService.shared.trackPurchaseFlow("failed")
                    state.statusCode = PurchaseStatusCode.failed
                }
            } catch {
                AppAnalyticsService.shared.trackPurchaseFlow("failed")
                state.statusCode = PurchaseStatusCode.failed
            }
            state.isPurchaseInProgress = false
        }
    }

    func restore() {
        AppAnalyticsService.shared.trackPurchaseFlow("restore_started")
        Task {
            state.isPurchaseInProgress = true
            state.statusCode = PurchaseStatusCode.checking
            do {
                try await AppStore.sync()
                await refreshCurrentEntitlements(showRestoreMessage: true)
            } catch {
                AppAnalyticsService.shared.trackPurchaseFlow("restore_failed")
                state.statusCode = PurchaseStatusCode.failed
            }
            state.isPurchaseInProgress = false
        }
    }

    private func loadProduct() async {
        do {
            let products = try await Product.products(for: [Constants.productId])
            product = products.first
            state.priceText = product?.displayPrice ?? state.priceText
            state.isPurchaseAvailable = product != nil
            if product == nil {
                state.statusCode = PurchaseStatusCode.notConfigured
            }
        } catch {
            product = nil
            state.isPurchaseAvailable = false
            state.statusCode = PurchaseStatusCode.unavailable
        }
    }

    private func refreshCurrentEntitlements(showRestoreMessage: Bool) async {
        var found = false
        for await entitlement in Transaction.currentEntitlements {
            do {
                let transaction = try verifiedTransaction(from: entitlement)
                guard transaction.productID == Constants.productId else { continue }
                found = true
                activateEntitlement(
                    statusCode: showRestoreMessage ? PurchaseStatusCode.restored : nil
                )
            } catch {
                state.statusCode = PurchaseStatusCode.failed
            }
        }

        if !found {
            setEntitlement(false)
        }

        if showRestoreMessage && !found {
            AppAnalyticsService.shared.trackPurchaseFlow("restore_not_found")
            state.statusCode = PurchaseStatusCode.restoreNotFound
        } else if showRestoreMessage {
            AppAnalyticsService.shared.trackPurchaseFlow("restored")
        }
        state.isEntitlementCheckComplete = true
    }

    private func handle(
        transactionUpdate: VerificationResult<Transaction>
    ) async {
        do {
            let transaction = try verifiedTransaction(from: transactionUpdate)
            guard transaction.productID == Constants.productId else { return }
            if transaction.revocationDate != nil {
                setEntitlement(false)
            } else {
                activateEntitlement(statusCode: PurchaseStatusCode.removed)
            }
            await transaction.finish()
        } catch {
            state.statusCode = PurchaseStatusCode.failed
        }
    }

    private func activateEntitlement(statusCode: String?) {
        setEntitlement(true)
        state.isPurchaseAvailable = true
        state.statusCode = statusCode
    }

    private func setEntitlement(_ enabled: Bool) {
        defaults.set(enabled, forKey: Constants.entitlementKey)
        guard isEntitled != enabled else { return }
        isEntitled = enabled
        onEntitlementChanged?(enabled)
    }

    private func verifiedTransaction(
        from verification: VerificationResult<Transaction>
    ) throws -> Transaction {
        switch verification {
        case .verified(let transaction):
            return transaction
        case .unverified(_, let error):
            throw error
        }
    }
}
