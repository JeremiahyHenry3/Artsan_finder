package com.example.artsan_finder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.navigation3.ui.NavDisplay
import com.example.artsan_finder.data.model.UserRole
import com.example.artsan_finder.ui.auth.AuthViewModel
import com.example.artsan_finder.ui.auth.LoginScreen
import com.example.artsan_finder.ui.auth.RegisterScreen
import com.example.artsan_finder.ui.customer.CustomerDashboardScreen
import com.example.artsan_finder.ui.dashboard.DashboardScreen
import com.example.artsan_finder.ui.detail.ArtisanDetailScreen
import com.example.artsan_finder.ui.detail.ArtisanDetailViewModel
import com.example.artsan_finder.ui.discovery.DiscoveryViewModel
import com.example.artsan_finder.ui.experiences.ExperiencesScreen
import com.example.artsan_finder.ui.services.ServicesScreen
import com.example.artsan_finder.ui.profile.ProfileScreen
import com.example.artsan_finder.ui.profile.ProfileViewModel
import com.example.artsan_finder.ui.navigation.Route
import com.example.artsan_finder.ui.theme.Artsan_FinderTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.artsan_finder.ui.admin.AdminDashboardScreen
import com.example.artsan_finder.ui.admin.AdminViewModel
import com.example.artsan_finder.ui.artisan.ArtisanDashboardScreen
import com.example.artsan_finder.ui.artisan.ArtisanViewModel
import com.example.artsan_finder.ui.chat.ChatScreen
import com.example.artsan_finder.ui.chat.ChatViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val appContainer = (application as ArtisanApplication).container

        setContent {
            Artsan_FinderTheme {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModel.provideFactory(appContainer.authRepository)
                )
                val currentUser by authViewModel.currentUser.collectAsState()

                // Persistent back stack across configuration changes
                val backStack = rememberNavBackStack(Route.Login)

                // Handle manual login navigation (Modern Explicit Navigation)
                LaunchedEffect(Unit) {
                    authViewModel.navigationEvent.collect { event ->
                        when (event) {
                            is com.example.artsan_finder.ui.auth.AuthNavigationEvent.NavigateToDashboard -> {
                                val targetRoute = when (event.role) {
                                    UserRole.ADMIN -> Route.AdminDashboard
                                    UserRole.ARTISAN -> Route.ArtisanDashboard
                                    UserRole.CUSTOMER -> Route.Dashboard
                                }
                                backStack.clear()
                                backStack += targetRoute
                            }
                        }
                    }
                }

                // Role-based routing effect for Logout and Security blocking
                LaunchedEffect(currentUser) {
                    val currentTop = backStack.lastOrNull()
                    if (currentUser == null) {
                        // If user is not authenticated, always force to Login screen
                        if (currentTop != Route.Login && currentTop != Route.Register) {
                            backStack.clear()
                            backStack += Route.Login
                        }
                    }
                    // Auto-login logic removed to ensure users must manually authenticate on app open
                }

                Scaffold(
                    bottomBar = {
                        val currentRoute = backStack.lastOrNull()
                        if (currentRoute is Route.Dashboard || currentRoute is Route.Discovery || currentRoute is Route.Profile) {
                            NavigationBar(
                                containerColor = Color.Black,
                                contentColor = Color.White
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute is Route.Dashboard,
                                    onClick = {
                                        if (currentRoute !is Route.Dashboard) {
                                            backStack.clear()
                                            backStack += Route.Dashboard
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00B4D8),
                                        selectedTextColor = Color(0xFF00B4D8),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentRoute is Route.Discovery,
                                    onClick = {
                                        if (currentRoute !is Route.Discovery) {
                                            backStack += Route.Discovery
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Search, contentDescription = "Discovery") },
                                    label = { Text("Discovery") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00B4D8),
                                        selectedTextColor = Color(0xFF00B4D8),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentRoute is Route.Profile,
                                    onClick = {
                                        if (currentRoute !is Route.Profile) {
                                            backStack += Route.Profile
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                    label = { Text("Profile") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00B4D8),
                                        selectedTextColor = Color(0xFF00B4D8),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
                        val directive = remember(windowAdaptiveInfo) {
                            calculatePaneScaffoldDirective(windowAdaptiveInfo)
                                .copy(horizontalPartitionSpacerSize = 0.dp)
                        }
                        val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

                        val provider: (NavKey) -> NavEntry<NavKey> = entryProvider {
                            entry<Route.Login> {
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onRegisterClick = { backStack += Route.Register }
                                )
                            }
                            entry<Route.Register> {
                                RegisterScreen(
                                    viewModel = authViewModel,
                                    onBack = { backStack.removeLastOrNull() }
                                )
                            }
                            entry<Route.AdminDashboard> {
                                // Never boot the dashboard with a placeholder id: its listeners
                                // would attach to a nonexistent account, then tear down and
                                // reload when the real user arrives. Wait the frame instead.
                                val adminId = currentUser?.id
                                if (adminId == null) {
                                    FullScreenLoading()
                                } else {
                                    val adminViewModel: AdminViewModel = viewModel(
                                        key = adminId,
                                        factory = AdminViewModel.provideFactory(
                                            adminId,
                                            appContainer.artisanRepository,
                                            appContainer.authRepository,
                                            appContainer.nidaService
                                        )
                                    )
                                    AdminDashboardScreen(
                                        viewModel = adminViewModel,
                                        onLogout = { authViewModel.logout() }
                                    )
                                }
                            }
                            entry<Route.ArtisanDashboard> {
                                val artisanId = currentUser?.id
                                if (artisanId == null) {
                                    FullScreenLoading()
                                } else {
                                    val artisanViewModel: ArtisanViewModel = viewModel(
                                        key = artisanId,
                                        factory = ArtisanViewModel.provideFactory(artisanId, appContainer.artisanRepository)
                                    )
                                    ArtisanDashboardScreen(
                                        viewModel = artisanViewModel,
                                        onLogout = { authViewModel.logout() },
                                        onOpenChat = { inquiryId -> backStack += Route.Chat(inquiryId) }
                                    )
                                }
                            }
                            entry<Route.Dashboard> {
                                // Live artisan directory: home cards must always point at
                                // artisans that exist in Firestore, so inquiries started
                                // here always reach a real, logged-in artisan account.
                                val homeArtisans by appContainer.artisanRepository.artisans
                                    .collectAsState(initial = emptyList())
                                DashboardScreen(
                                    userName = currentUser?.name ?: "Guest",
                                    artisans = homeArtisans,
                                    onBrowseClick = { backStack += Route.Discovery },
                                    onArtisanClick = { artisanId -> backStack += Route.ArtisanDetail(artisanId) },
                                    onExperiencesClick = { backStack += Route.Experiences },
                                    onServicesClick = { backStack += Route.Services }
                                )
                            }
                            entry<Route.Experiences> {
                                ExperiencesScreen(
                                    onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                                    onArtisanClick = { artisanId -> backStack += Route.ArtisanDetail(artisanId) }
                                )
                            }
                            entry<Route.Services> {
                                ServicesScreen(
                                    onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                                    onServiceClick = { serviceName ->
                                        // You could navigate to a filtered discovery page here
                                        backStack += Route.Discovery
                                    }
                                )
                            }
                            entry<Route.Profile> {
                                val profileUserId = currentUser?.id
                                if (profileUserId == null) {
                                    FullScreenLoading()
                                } else {
                                    val profileViewModel: ProfileViewModel = viewModel(
                                        key = profileUserId,
                                        factory = ProfileViewModel.provideFactory(
                                            profileUserId,
                                            appContainer.authRepository,
                                            appContainer.artisanRepository
                                        )
                                    )
                                    ProfileScreen(
                                        viewModel = profileViewModel,
                                        onLogoutClick = { authViewModel.logout() }
                                    )
                                }
                            }
                            entry<Route.Discovery>(
                                metadata = ListDetailSceneStrategy.listPane(
                                    detailPlaceholder = {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("Select an artisan to see details", style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                )
                            ) {
                                val discoveryViewModel: DiscoveryViewModel = viewModel(
                                    factory = DiscoveryViewModel.provideFactory(
                                        appContainer.artisanRepository,
                                        appContainer.locationProvider
                                    )
                                )
                                // Observe user inquiries for the customer dashboard.
                                // remember() keyed on the user id: without it every
                                // recomposition builds a fresh Flow, tearing down and
                                // re-attaching the Firestore listener over and over.
                                val inquiriesUserId = currentUser?.id ?: ""
                                val userInquiries by remember(inquiriesUserId) {
                                    appContainer.artisanRepository.getInquiriesForUser(inquiriesUserId)
                                }.collectAsState(initial = emptyList())
                                // Live artisan directory so inquiry cards show real names
                                val allArtisans by appContainer.artisanRepository.artisans
                                    .collectAsState(initial = emptyList())
                                val artisanNames = remember(allArtisans) {
                                    allArtisans.associate { it.id to it.name }
                                }

                                CustomerDashboardScreen(
                                    discoveryViewModel = discoveryViewModel,
                                    inquiries = userInquiries,
                                    artisanNames = artisanNames,
                                    onArtisanClick = { artisanId ->
                                        backStack += Route.ArtisanDetail(artisanId)
                                    },
                                    onInquiryClick = { inquiryId ->
                                        backStack += Route.Chat(inquiryId)
                                    }
                                )
                            }
                            entry<Route.ArtisanDetail>(
                                metadata = ListDetailSceneStrategy.detailPane()
                            ) { route ->
                                val artisanDetailViewModel: ArtisanDetailViewModel = viewModel(
                                    key = route.artisanId,
                                    factory = ArtisanDetailViewModel.provideFactory(
                                        route.artisanId,
                                        currentUser?.id ?: "guest",
                                        appContainer.artisanRepository
                                    )
                                )
                                ArtisanDetailScreen(
                                    viewModel = artisanDetailViewModel,
                                    onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
                                )
                            }
                            entry<Route.Chat> { route ->
                                // The ViewModel is keyed by inquiryId only, so if it were
                                // created while currentUser is still null it would keep an
                                // empty user id forever and misresolve the chat partner.
                                val chatUserId = currentUser?.id
                                if (chatUserId == null) {
                                    FullScreenLoading()
                                } else {
                                    val chatViewModel: ChatViewModel = viewModel(
                                        key = route.inquiryId,
                                        factory = ChatViewModel.provideFactory(
                                            route.inquiryId,
                                            chatUserId,
                                            appContainer.artisanRepository
                                        )
                                    )
                                    ChatScreen(
                                        viewModel = chatViewModel,
                                        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
                                    )
                                }
                            }
                        }

                        NavDisplay(
                            backStack = backStack,
                            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                            sceneStrategy = listDetailStrategy,
                            transitionSpec = {
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) + fadeIn(animationSpec = tween(400)) togetherWith 
                                slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut(animationSpec = tween(400))
                            },
                            popTransitionSpec = {
                                slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn(animationSpec = tween(400)) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) + fadeOut(animationSpec = tween(400))
                            },
                            entryProvider = provider
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sub-frame placeholder shown while the authenticated user's identity is
 * resolving — role dashboards and chat must never start with a fake user id.
 */
@Composable
private fun FullScreenLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFF00B4D8))
    }
}
