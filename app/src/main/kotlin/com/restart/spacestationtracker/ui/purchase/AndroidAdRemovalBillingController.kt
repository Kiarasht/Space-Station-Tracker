package com.restart.spacestationtracker.ui.purchase

import android.app.Activity
import android.content.Context
import android.os.Build
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.restart.spacestationtracker.analytics.AppAnalytics
import com.restart.spacestationtracker.data.settings.SettingsRepository
import com.restart.spacestationtracker.shared.ui.SharedPurchaseStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdRemovalPurchaseUiState(
    val priceText: String = AD_REMOVAL_FALLBACK_PRICE,
    val isLoading: Boolean = true,
    val isPurchaseInProgress: Boolean = false,
    val isPurchaseAvailable: Boolean = true,
    val isEntitlementCheckComplete: Boolean = false,
    val statusCode: String? = null
)

class AndroidAdRemovalBillingController(
    context: Context,
    private val settingsRepository: SettingsRepository
) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext
    private var productDetails: ProductDetails? = null
    private var isConnected = false
    private var purchaseFlowStarted = false

    private val _state = MutableStateFlow(AdRemovalPurchaseUiState())
    val state: StateFlow<AdRemovalPurchaseUiState> = _state.asStateFlow()

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun start() {
        if (!isInstalledFromGooglePlay()) {
            markPurchasesUnavailable(entitlementCheckComplete = true)
            return
        }

        updateState { it.copy(isLoading = true, statusCode = null) }
        if (billingClient.isReady) {
            isConnected = true
            refreshStoreState()
            return
        }

        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    isConnected =
                        billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    if (isConnected) {
                        refreshStoreState()
                    } else {
                        markPurchasesUnavailable()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isConnected = false
                    updateState {
                        it.copy(
                            isLoading = false,
                            isPurchaseAvailable = false,
                            statusCode = SharedPurchaseStatus.UNAVAILABLE
                        )
                    }
                }
            }
        )
    }

    fun stop() {
        if (billingClient.isReady) billingClient.endConnection()
        isConnected = false
    }

    fun purchase(activity: Activity?) {
        AppAnalytics.trackPurchaseFlow("started")
        if (settingsRepository.hasLifetimeAdRemoval()) {
            AppAnalytics.trackPurchaseFlow("already_owned")
            updateState {
                it.copy(
                    isPurchaseInProgress = false,
                    statusCode = SharedPurchaseStatus.ALREADY_REMOVED
                )
            }
            return
        }

        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            AppAnalytics.trackPurchaseFlow("failed")
            updateState { it.copy(statusCode = SharedPurchaseStatus.FAILED) }
            return
        }
        if (!isInstalledFromGooglePlay()) {
            AppAnalytics.trackPurchaseFlow("unavailable")
            markPurchasesUnavailable(entitlementCheckComplete = true)
            return
        }

        val details = productDetails
        if (!isConnected || details == null) {
            updateState {
                it.copy(isLoading = true, statusCode = SharedPurchaseStatus.CONNECTING)
            }
            start()
            return
        }

        val offerDetails = details.oneTimePurchaseOfferDetailsList?.firstOrNull()
        if (details.oneTimePurchaseOfferDetailsList != null &&
            offerDetails?.offerToken.isNullOrBlank()
        ) {
            updateState {
                it.copy(
                    isPurchaseInProgress = false,
                    isPurchaseAvailable = false,
                    statusCode = SharedPurchaseStatus.CONNECTING
                )
            }
            queryProductDetails()
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply {
                offerDetails?.offerToken
                    ?.takeIf(String::isNotBlank)
                    ?.let { setOfferToken(it) }
            }
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        updateState { it.copy(isPurchaseInProgress = true, statusCode = null) }
        val result = runCatching {
            billingClient.launchBillingFlow(activity, flowParams)
        }.getOrElse {
            AppAnalytics.trackPurchaseFlow("failed")
            updateState {
                it.copy(
                    isPurchaseInProgress = false,
                    statusCode = SharedPurchaseStatus.FAILED
                )
            }
            return
        }
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            AppAnalytics.trackPurchaseFlow("failed")
            updateState {
                it.copy(
                    isPurchaseInProgress = false,
                    statusCode = SharedPurchaseStatus.FAILED
                )
            }
        } else {
            purchaseFlowStarted = true
        }
    }

    fun restore() {
        AppAnalytics.trackPurchaseFlow("restore_started")
        if (!isConnected || !billingClient.isReady) {
            updateState {
                it.copy(
                    isLoading = true,
                    statusCode = SharedPurchaseStatus.CHECKING
                )
            }
            start()
            return
        }
        updateState {
            it.copy(
                isLoading = true,
                statusCode = SharedPurchaseStatus.CHECKING
            )
        }
        queryOwnedPurchases(showRestoreMessage = true)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty().firstOrNull(::isAdRemovalPurchase)?.let(::handlePurchase)
                    ?: updateState {
                        it.copy(
                            isPurchaseInProgress = false,
                            statusCode = SharedPurchaseStatus.CHECKING
                        )
                    }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                purchaseFlowStarted = false
                AppAnalytics.trackPurchaseFlow("cancelled")
                updateState {
                    it.copy(
                        isPurchaseInProgress = false,
                        statusCode = SharedPurchaseStatus.CANCELED
                    )
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryOwnedPurchases(showRestoreMessage = true)
            }
            else -> {
                purchaseFlowStarted = false
                AppAnalytics.trackPurchaseFlow("failed")
                updateState {
                    it.copy(
                        isPurchaseInProgress = false,
                        statusCode = SharedPurchaseStatus.FAILED
                    )
                }
            }
        }
    }

    private fun refreshStoreState() {
        queryProductDetails()
        queryOwnedPurchases(showRestoreMessage = false)
    }

    private fun queryProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(AD_REMOVAL_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                markPurchasesUnavailable(entitlementCheckComplete = false)
                return@queryProductDetailsAsync
            }

            val details = result.productDetailsList.firstOrNull {
                it.productId == AD_REMOVAL_PRODUCT_ID
            }
            productDetails = details
            updateState {
                it.copy(
                    priceText = details?.formattedOneTimePrice() ?: AD_REMOVAL_FALLBACK_PRICE,
                    isLoading = false,
                    isPurchaseAvailable = details?.hasUsableOneTimeOffer() == true,
                    statusCode = when {
                        details?.hasUsableOneTimeOffer() == true -> it.statusCode
                        details != null -> SharedPurchaseStatus.CONNECTING
                        else -> SharedPurchaseStatus.NOT_CONFIGURED
                    }
                )
            }
        }
    }

    private fun queryOwnedPurchases(showRestoreMessage: Boolean) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                updateState {
                    it.copy(
                        isLoading = false,
                        isPurchaseInProgress = false,
                        statusCode = SharedPurchaseStatus.UNAVAILABLE
                    )
                }
                return@queryPurchasesAsync
            }

            val purchase = purchases.firstOrNull(::isAdRemovalPurchase)
            if (purchase != null) {
                handlePurchase(purchase, restored = showRestoreMessage)
            } else {
                settingsRepository.setLifetimeAdRemoval(false)
                if (showRestoreMessage) {
                    AppAnalytics.trackPurchaseFlow("restore_not_found")
                }
                updateState {
                    it.copy(
                        isLoading = false,
                        isPurchaseInProgress = false,
                        isEntitlementCheckComplete = true,
                        statusCode = if (showRestoreMessage) {
                            SharedPurchaseStatus.RESTORE_NOT_FOUND
                        } else {
                            it.statusCode
                        }
                    )
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase, restored: Boolean = false) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (purchase.isAcknowledged) {
                    activateAdRemoval(restored)
                    return
                }
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(params) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        activateAdRemoval(restored)
                    } else {
                        updateState {
                            it.copy(
                                isLoading = false,
                                isPurchaseInProgress = false,
                                statusCode = SharedPurchaseStatus.PENDING
                            )
                        }
                    }
                }
            }
            Purchase.PurchaseState.PENDING -> {
                purchaseFlowStarted = false
                AppAnalytics.trackPurchaseFlow("pending")
                updateState {
                    it.copy(
                        isLoading = false,
                        isPurchaseInProgress = false,
                        isEntitlementCheckComplete = true,
                        statusCode = SharedPurchaseStatus.PENDING
                    )
                }
            }
            else -> {
                purchaseFlowStarted = false
                AppAnalytics.trackPurchaseFlow("failed")
                settingsRepository.setLifetimeAdRemoval(false)
                updateState {
                    it.copy(
                        isLoading = false,
                        isPurchaseInProgress = false,
                        isEntitlementCheckComplete = true,
                        statusCode = SharedPurchaseStatus.FAILED
                    )
                }
            }
        }
    }

    private fun activateAdRemoval(restored: Boolean) {
        when {
            restored -> AppAnalytics.trackPurchaseFlow("restored")
            purchaseFlowStarted -> AppAnalytics.trackPurchaseFlow("completed")
        }
        purchaseFlowStarted = false
        settingsRepository.setLifetimeAdRemoval(true)
        updateState {
            it.copy(
                isLoading = false,
                isPurchaseInProgress = false,
                isPurchaseAvailable = true,
                isEntitlementCheckComplete = true,
                statusCode = if (restored) {
                    SharedPurchaseStatus.RESTORED
                } else {
                    SharedPurchaseStatus.REMOVED
                }
            )
        }
    }

    private fun markPurchasesUnavailable(entitlementCheckComplete: Boolean = false) {
        productDetails = null
        updateState {
            it.copy(
                isLoading = false,
                isPurchaseInProgress = false,
                isPurchaseAvailable = false,
                isEntitlementCheckComplete =
                    it.isEntitlementCheckComplete || entitlementCheckComplete,
                statusCode = SharedPurchaseStatus.UNAVAILABLE
            )
        }
    }

    private fun isAdRemovalPurchase(purchase: Purchase): Boolean {
        return AD_REMOVAL_PRODUCT_ID in purchase.products
    }

    private fun ProductDetails.formattedOneTimePrice(): String? {
        return oneTimePurchaseOfferDetails?.formattedPrice
            ?: oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
    }

    private fun ProductDetails.hasUsableOneTimeOffer(): Boolean {
        val offers = oneTimePurchaseOfferDetailsList
        return when {
            offers == null -> oneTimePurchaseOfferDetails != null
            offers.isEmpty() -> false
            else -> offers.any { !it.offerToken.isNullOrBlank() }
        }
    }

    private fun isInstalledFromGooglePlay(): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                appContext.packageManager
                    .getInstallSourceInfo(appContext.packageName)
                    .installingPackageName
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getInstallerPackageName(appContext.packageName)
            }
        }.getOrNull() == "com.android.vending"
    }

    private fun updateState(update: (AdRemovalPurchaseUiState) -> AdRemovalPurchaseUiState) {
        _state.value = update(_state.value)
    }
}

const val AD_REMOVAL_PRODUCT_ID = "remove_ads_lifetime"
private const val AD_REMOVAL_FALLBACK_PRICE = "$9.99"
