package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.AuthScreen
import com.example.auth.attemptAutoSignIn
import com.example.brain.GeminiBrainService
import com.example.features.ImageAiService
import com.example.memory.MemoryRepository
import com.example.ui.components.HolographicBackground
import com.example.ui.screens.CoreScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.SystemsScreen
import com.example.ui.screens.VisionLabScreen
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloGlass
import com.example.ui.theme.HoloObsidian
import com.example.viewmodel.JaniViewModel
import com.example.viewmodel.VisionLabViewModel
import com.example.voice.VoiceManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_CORE = "core"
const val ROUTE_MEMORY = "memory"
const val ROUTE_VISION = "vision"
const val ROUTE_SYSTEMS = "systems"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val JaniDestinations = listOf(
    NavDestinationItem(ROUTE_CORE, R.string.nav_core, Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
    NavDestinationItem(ROUTE_MEMORY, R.string.nav_memory, Icons.Filled.Memory, Icons.Outlined.Memory),
    NavDestinationItem(ROUTE_VISION, R.string.nav_vision, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    NavDestinationItem(ROUTE_SYSTEMS, R.string.nav_systems, Icons.Filled.Tune, Icons.Outlined.Tune)
)

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = user.uid
        key(uid) {
            JaniDashboard(
                currentUserId = uid,
                onSignedOut = { currentUser = Firebase.auth.currentUser }
            )
        }
    }
}

@Composable
private fun JaniDashboard(
    currentUserId: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val janiViewModel: JaniViewModel = viewModel(
        key = currentUserId,
        factory = remember(currentUserId) {
            val databaseId = appContext.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            JaniViewModel.provideFactory(
                repository = MemoryRepository(db),
                brainService = GeminiBrainService(),
                voiceManager = VoiceManager(appContext),
                currentUserId = currentUserId
            )
        }
    )

    val visionViewModel: VisionLabViewModel = viewModel(
        key = "vision_$currentUserId",
        factory = remember(currentUserId) {
            VisionLabViewModel.provideFactory(ImageAiService())
        }
    )

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_CORE

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    HolographicBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            if (isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    NavigationRail(
                        containerColor = HoloObsidian.copy(alpha = 0.92f)
                    ) {
                        JaniDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = HoloCyan,
                                    selectedTextColor = HoloCyan,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }

                    JaniNavHost(
                        navController = navController,
                        janiViewModel = janiViewModel,
                        visionViewModel = visionViewModel,
                        onSignedOut = onSignedOut,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        NavigationBar(
                            containerColor = HoloObsidian.copy(alpha = 0.95f),
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            JaniDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(dest.labelRes),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = HoloCyan,
                                        selectedTextColor = HoloCyan,
                                        indicatorColor = HoloGlass,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    JaniNavHost(
                        navController = navController,
                        janiViewModel = janiViewModel,
                        visionViewModel = visionViewModel,
                        onSignedOut = onSignedOut,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun JaniNavHost(
    navController: androidx.navigation.NavHostController,
    janiViewModel: JaniViewModel,
    visionViewModel: VisionLabViewModel,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_CORE,
        modifier = modifier
    ) {
        composable(ROUTE_CORE) {
            CoreScreen(viewModel = janiViewModel)
        }
        composable(ROUTE_MEMORY) {
            MemoryScreen(viewModel = janiViewModel)
        }
        composable(ROUTE_VISION) {
            VisionLabScreen(
                visionViewModel = visionViewModel,
                janiViewModel = janiViewModel
            )
        }
        composable(ROUTE_SYSTEMS) {
            SystemsScreen(
                viewModel = janiViewModel,
                onSignedOut = onSignedOut
            )
        }
    }
}
