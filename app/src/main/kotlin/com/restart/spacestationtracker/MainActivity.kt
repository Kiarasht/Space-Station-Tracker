package com.restart.spacestationtracker

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.restart.spacestationtracker.util.AppReviewRequester
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var adsConsentManager: AdsConsentManager

    @Inject
    lateinit var appOpenAdManager: AppOpenAdManager

    private lateinit var adRemovalBillingController: AndroidAdRemovalBillingController
    private var isMobileAdsInitialized = false
    private var consentAllowsAdRequests = false
    private var purchaseEntitlementChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    override fun onDestroy() {
        adRemovalBillingController.stop()
        super.onDestroy()
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

    val bottomBarState = rememberSaveable { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0) {
                    bottomBarState.value = true
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (consumed.y < 0) {
                    bottomBarState.value = false
                }
                return super.onPostScroll(consumed, available, source)
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

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

    val isBottomBarVisible = when {
        currentDestination?.route == Screen.Map.route -> true
        currentDestination?.route?.startsWith("legal") == true -> false
        else -> bottomBarState.value
    }

    var navBarHeight by remember { mutableFloatStateOf(0f) }

    val bottomBarTranslationY by animateFloatAsState(
        targetValue = if (isBottomBarVisible) 0f else navBarHeight,
        label = "Bottom bar translation"
    )

    val bottomPadding = WindowInsets.navigationBars.getBottom(LocalDensity.current)
    val animatedBottomPadding by animateDpAsState(
        targetValue = if (isBottomBarVisible) 0.dp else (bottomPadding / LocalDensity.current.density).dp,
        label = "animated bottom padding"
    )

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        bottomBar = {
            Column(
                modifier = Modifier
                    .graphicsLayer { translationY = bottomBarTranslationY }
            ) {
                if (!isAdFree && canRequestAds) {
                    AdmobBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = animatedBottomPadding)
                    )
                }
                NavigationBar(
                    modifier = Modifier.onSizeChanged { navBarHeight = it.height.toFloat() },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.background
                            ),
                            icon = {
                                val painter = when (screen.route) {
                                    Screen.IssPasses.route -> painterResource(id = R.drawable.ic_passes)
                                    Screen.PeopleInSpace.route -> painterResource(id = R.drawable.ic_astronaut)
                                    else -> rememberVectorPainter(image = screen.icon!!)
                                }
                                Icon(
                                    painter = painter,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(stringResource(id = screen.labelResId)) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                AppAnalytics.trackInteraction("navigate_${screen.route}")
                                navController.navigate(screen.route) {
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
    ) { innerPadding ->
        Column {
            NavHost(
                navController,
                startDestination = Screen.Map.route,
            ) {
                composable(Screen.Map.route) {
                    SharedMapRoute(
                        contentPadding = innerPadding,
                        canRequestAds = canRequestAds,
                        purchaseState = purchaseState,
                        onPurchaseAdRemoval = onPurchaseAdRemoval,
                        onRestoreAdRemoval = onRestoreAdRemoval
                    )
                }
                composable(Screen.IssPasses.route) { IssPassesScreen(contentPadding = innerPadding) }
                composable(Screen.PeopleInSpace.route) {
                    PeopleInSpaceScreen(
                        contentPadding = innerPadding
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        contentPadding = innerPadding,
                        isPrivacyOptionsRequired = isPrivacyOptionsRequired,
                        onPrivacyOptionsClick = onPrivacyOptionsClick
                    )
                }
                composable(Screen.About.route) {
                    SharedAboutRoute(
                        contentPadding = innerPadding,
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
