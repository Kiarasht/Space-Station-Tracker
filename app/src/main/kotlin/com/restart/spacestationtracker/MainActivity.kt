package com.restart.spacestationtracker

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.restart.spacestationtracker.data.settings.SettingsRepository
import com.restart.spacestationtracker.data.settings.defaultAppSettings
import com.restart.spacestationtracker.ui.about.AboutScreen
import com.restart.spacestationtracker.ui.about.LegalScreen
import com.restart.spacestationtracker.ui.iss_live.MapScreen
import com.restart.spacestationtracker.ui.iss_passes.IssPassesScreen
import com.restart.spacestationtracker.ui.people_in_space.PeopleInSpaceScreen
import com.restart.spacestationtracker.ui.settings.SettingsScreen
import com.restart.spacestationtracker.ui.theme.SpaceStationTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.appSettingsFlow.collectAsState(initial = defaultAppSettings)
            val useDarkTheme = when (settings.theme) {
                "Follow System" -> isSystemInDarkTheme()
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            SpaceStationTrackerTheme(darkTheme = useDarkTheme) {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
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

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        bottomBar = {
            Column(
                modifier = Modifier
                    .graphicsLayer { translationY = bottomBarTranslationY }
            ) {
                AdmobBanner(modifier = Modifier.fillMaxWidth())
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
                            label = { Text(screen.route.split("/").first()) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
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
                composable(Screen.Map.route) { MapScreen(contentPadding = innerPadding) }
                composable(Screen.IssPasses.route) { IssPassesScreen(contentPadding = innerPadding) }
                composable(Screen.PeopleInSpace.route) {
                    PeopleInSpaceScreen(
                        contentPadding = innerPadding
                    )
                }
                composable(Screen.Settings.route) { SettingsScreen(contentPadding = innerPadding) }
                composable(Screen.About.route) {
                    AboutScreen(
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

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun AdmobBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = {
            AdView(it).apply {
                adUnitId = context.getString(R.string.banner_ad_unit_id)
            }
        },
        update = {
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidth)
            it.setAdSize(adSize)
            it.loadAd(AdRequest.Builder().build())
        }
    )
}

sealed class Screen(val route: String, val icon: ImageVector? = null) {
    object Map : Screen("Map", Icons.Filled.Map)
    object IssPasses : Screen("Flybys", Icons.Filled.Public)
    object PeopleInSpace : Screen("On Duty", Icons.Filled.People)
    object Settings : Screen("Settings", Icons.Filled.Settings)
    object About : Screen("About", Icons.Filled.Info)
    object Legal : Screen("legal/{titleResId}/{contentResId}")
}
