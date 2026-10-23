package com.restart.spacestationtracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.restart.spacestationtracker.shared.ui.SpaceNavItem
import com.restart.spacestationtracker.shared.ui.SpaceFloatingNavBar
import com.restart.spacestationtracker.shared.ui.SpaceBottomChrome
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.restart.spacestationtracker.analytics.AppAnalytics
import com.restart.spacestationtracker.data.settings.SettingsRepository
import com.restart.spacestationtracker.data.settings.defaultAppSettings
import com.restart.spacestationtracker.ui.about.LegalScreen
import com.restart.spacestationtracker.ui.about.SharedAboutRoute
import com.restart.spacestationtracker.ui.ads.AdMobIds
import com.restart.spacestationtracker.ui.ads.AdsConsentManager
import com.restart.spacestationtracker.ui.ads.AppOpenAdManager
import com.restart.spacestationtracker.ui.iss_live.SharedMapRoute
import com.restart.spacestationtracker.ui.iss_passes.IssPassesScreen
import com.restart.spacestationtracker.ui.people_in_space.PeopleInSpaceScreen
import com.restart.spacestationtracker.ui.purchase.AdRemovalPurchaseUiState
import com.restart.spacestationtracker.ui.purchase.AndroidAdRemovalBillingController
import com.restart.spacestationtracker.ui.settings.SettingsScreen
import com.restart.spacestationtracker.ui.theme.SpaceStationTrackerTheme
import com.restart.spacestationtracker.util.AppRatingManager
import com.restart.spacestationtracker.util.LiveEventAlertWorkScheduler
import com.restart.spacestationtracker.util.AppReviewRequester
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var adsConsentManager: AdsConsentManager

    @Inject
    lateinit var liveEventAlertWorkScheduler: LiveEventAlertWorkScheduler

    @Inject
    lateinit var appOpenAdManager: AppOpenAdManager

    private lateinit var adRemovalBillingController: AndroidAdRemovalBillingController
    private var isMobileAdsInitialized = false
    private var consentAllowsAdRequests = false
    private var purchaseEntitlementChecked = false
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            pendingRoute = intent.consumeOpenRoute()
        }
        adRemovalBillingController = AndroidAdRemovalBillingController(
            context = this,
            settingsRepository = settingsRepository
        )
        lifecycleScope.launch {
            adRemovalBillingController.state.collect { purchaseState ->
                purchaseEntitlementChecked = purchaseState.isEntitlementCheckComplete
                maybeInitializeMobileAds()
            }
        }
        lifecycleScope.launch {
            settingsRepository.awaitInitialLoad()
            adRemovalBillingController.start()
        }
        // Opening the app is a cheap moment to pick up newly scheduled NASA broadcasts.
        lifecycleScope.launch {
            if (settingsRepository.appSettingsFlow.first().liveEventAlertsEnabled) {
                liveEventAlertWorkScheduler.runImmediateSync()
            }
        }
        AppRatingManager(applicationContext).recordAppLaunch()
        appOpenAdManager.register(application)
        adsConsentManager.gatherConsent(this) { canRequestAds ->
            consentAllowsAdRequests = canRequestAds
            maybeInitializeMobileAds()
        }
        setContent {
            val settings by settingsRepository.appSettingsFlow.collectAsState(initial = defaultAppSettings)
            val purchaseState by adRemovalBillingController.state.collectAsState()
            val canRequestAds by adsConsentManager.canRequestAds.collectAsState()
            val isPrivacyOptionsRequired by adsConsentManager.isPrivacyOptionsRequired.collectAsState()
            val useDarkTheme = when (settings.theme) {
                "Follow System" -> isSystemInDarkTheme()
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            val isAdFree = settings.hasLifetimeAdRemoval
            
            SpaceStationTrackerTheme(darkTheme = useDarkTheme) {
                MainScreen(
                    pendingRoute = pendingRoute,
                    onPendingRouteHandled = { pendingRoute = null },
                    isAdFree = isAdFree,
                    canRequestAds =
                        canRequestAds && purchaseState.isEntitlementCheckComplete,
                    purchaseState = purchaseState,
                    isPrivacyOptionsRequired = isPrivacyOptionsRequired,
                    onPurchaseAdRemoval = {
                        AppAnalytics.trackInteraction("start_ad_removal_purchase", "map")
                        adRemovalBillingController.purchase(this)
                    },
                    onRestoreAdRemoval = {
                        AppAnalytics.trackInteraction("restore_ad_removal_purchase", "map")
                        adRemovalBillingController.restore()
                    },
                    onPrivacyOptionsClick = {
                        AppAnalytics.trackInteraction("open_privacy_choices", "settings")
                        adsConsentManager.showPrivacyOptionsForm(this)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adRemovalBillingController.refreshPurchases()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.consumeOpenRoute()?.let { pendingRoute = it }
    }

    override fun onDestroy() {
        adRemovalBillingController.stop()
        super.onDestroy()
    }

    private fun Intent.consumeOpenRoute(): String? {
        val route = getStringExtra(EXTRA_OPEN_ROUTE)
        removeExtra(EXTRA_OPEN_ROUTE)
        return route
    }

    companion object {
        const val EXTRA_OPEN_ROUTE = "extra_open_route"
    }

    private fun maybeInitializeMobileAds() {
        if (consentAllowsAdRequests && purchaseEntitlementChecked) {
            initializeMobileAds()
        }
    }

    private fun initializeMobileAds() {
        if (isMobileAdsInitialized) {
            return
        }
        isMobileAdsInitialized = true
        MobileAds.initialize(this) {
            appOpenAdManager.register(application)
            appOpenAdManager.onAdsReady(this)
        }
    }
}

@Composable
fun MainScreen(
    pendingRoute: String?,
    onPendingRouteHandled: () -> Unit,
    isAdFree: Boolean,
    canRequestAds: Boolean,
    purchaseState: AdRemovalPurchaseUiState,
    isPrivacyOptionsRequired: Boolean,
    onPurchaseAdRemoval: () -> Unit,
    onRestoreAdRemoval: () -> Unit,
    onPrivacyOptionsClick: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val appRatingManager = remember { AppRatingManager(context.applicationContext) }
    val bottomNavItems = listOf(
        Screen.Map,
        Screen.IssPasses,
        Screen.PeopleInSpace,
        Screen.Settings,
        Screen.About
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    var chromeHeightPx by remember { mutableIntStateOf(0) }
    val chromeHeight = with(LocalDensity.current) { chromeHeightPx.toDp() }

    LaunchedEffect(currentRoute) {
        AppAnalytics.trackScreen(currentRoute)
        appRatingManager.recordScreenVisit(currentRoute)
        (context as? Activity)?.let { activity ->
            AppReviewRequester.maybeRequestReview(activity, appRatingManager)
        }
    }

    LaunchedEffect(isAdFree) {
        AppAnalytics.updateAdFreeState(isAdFree)
    }

    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        if (bottomNavItems.any { it.route == route }) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
        onPendingRouteHandled()
    }

    // Legal pages are full screen.
    val isBottomBarVisible = currentRoute?.startsWith("legal") != true
    val selectedRoute = bottomNavItems.firstOrNull { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }?.route ?: Screen.Map.route
    val navItems = bottomNavItems.map { screen ->
        SpaceNavItem(
            key = screen.route,
            label = stringResource(id = screen.labelResId),
            icon = when (screen.route) {
                Screen.IssPasses.route -> painterResource(id = R.drawable.ic_passes)
                Screen.PeopleInSpace.route -> painterResource(id = R.drawable.ic_astronaut)
                else -> rememberVectorPainter(image = screen.icon!!)
            }
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        // Screens draw under the floating bar; its measured height becomes their bottom padding.
        val contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            bottom = if (isBottomBarVisible) chromeHeight else innerPadding.calculateBottomPadding()
        )
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController,
                startDestination = Screen.Map.route,
            ) {
                composable(Screen.Map.route) {
                    SharedMapRoute(
                        contentPadding = contentPadding,
                        canRequestAds = canRequestAds,
                        purchaseState = purchaseState,
                        onPurchaseAdRemoval = onPurchaseAdRemoval,
                        onRestoreAdRemoval = onRestoreAdRemoval
                    )
                }
                composable(Screen.IssPasses.route) { IssPassesScreen(contentPadding = contentPadding) }
                composable(Screen.PeopleInSpace.route) {
                    PeopleInSpaceScreen(
                        contentPadding = contentPadding
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        contentPadding = contentPadding,
                        isPrivacyOptionsRequired = isPrivacyOptionsRequired,
                        onPrivacyOptionsClick = onPrivacyOptionsClick
                    )
                }
                composable(Screen.About.route) {
                    SharedAboutRoute(
                        contentPadding = contentPadding,
                        onNavigateToLegal = { titleResId, contentResId ->
                            navController.navigate("legal/$titleResId/$contentResId")
                        }
                    )
                }
                composable(
                    route = Screen.Legal.route,
                    arguments = listOf(
                        navArgument("titleResId") { type = NavType.IntType },
                        navArgument("contentResId") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val title = backStackEntry.arguments?.getInt("titleResId") ?: 0
                    val content = backStackEntry.arguments?.getInt("contentResId") ?: 0
                    LegalScreen(
                        titleResId = title,
                        contentResId = content,
                        onNavigateUp = { navController.popBackStack() }
                    )
                }
            }

            if (isBottomBarVisible) {
                SpaceBottomChrome(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { chromeHeightPx = it.height },
                    banner = if (!isAdFree && canRequestAds) {
                        { AdmobBanner(modifier = Modifier.fillMaxWidth()) }
                    } else {
                        null
                    }
                ) {
                    SpaceFloatingNavBar(
                        items = navItems,
                        selectedKey = selectedRoute,
                        onSelect = { route ->
                            AppAnalytics.trackInteraction("navigate_$route")
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    }

}

@Composable
fun AdmobBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(MAX_BANNER_HEIGHT_DP.dp),
        contentAlignment = Alignment.Center
    ) {
        val availableWidthDp = maxWidth.value.roundToInt().coerceAtLeast(1)
        val adSize = remember(context, availableWidthDp) {
            AdSize.getInlineAdaptiveBannerAdSize(
                availableWidthDp,
                MAX_BANNER_HEIGHT_DP
            )
        }
        val adView = remember(context) {
            AdView(context).apply {
                adUnitId = AdMobIds.banner(context)
            }
        }

        LaunchedEffect(adView, adSize) {
            adView.setAdSize(adSize)
            adView.loadAd(AdRequest.Builder().build())
        }
        DisposableEffect(adView) {
            onDispose(adView::destroy)
        }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(MAX_BANNER_HEIGHT_DP.dp),
            factory = { adView }
        )
    }
}

private const val MAX_BANNER_HEIGHT_DP = 50

sealed class Screen(val route: String, val labelResId: Int, val icon: ImageVector? = null) {
    object Map : Screen("Map", R.string.nav_map, Icons.Filled.Map)
    object IssPasses : Screen("Sky Path", R.string.nav_sky_path, Icons.Filled.Public)
    object PeopleInSpace : Screen("On Duty", R.string.nav_on_duty, Icons.Filled.People)
    object Settings : Screen("Settings", R.string.nav_settings, Icons.Filled.Settings)
    object About : Screen("About", R.string.nav_about, Icons.Filled.Info)
    object Legal : Screen("legal/{titleResId}/{contentResId}", R.string.nav_about)
}
