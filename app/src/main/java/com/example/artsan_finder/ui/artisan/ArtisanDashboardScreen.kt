package com.example.artsan_finder.ui.artisan

import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.text.format.DateUtils
import com.example.artsan_finder.data.model.Artisan
import com.example.artsan_finder.data.model.Inquiry
import com.example.artsan_finder.data.model.Service
import com.example.artsan_finder.ui.theme.CyanGradient
import com.example.artsan_finder.ui.theme.CyanPrimary
import com.example.artsan_finder.ui.theme.DarkSurface
import com.example.artsan_finder.ui.theme.DarkSurfaceVariant
import com.example.artsan_finder.utils.CurrencyUtils
import com.example.artsan_finder.utils.bounceClickable
import com.example.artsan_finder.ui.theme.OnDarkSurface
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

private val StatusReplied = Color(0xFF4CAF50)
private val StatusPending = Color(0xFFFFB300)
private val SubtleBorder = Color(0xFF1E1E1E)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ArtisanDashboardScreen(
    viewModel: ArtisanViewModel,
    onLogout: () -> Unit,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddServiceSheet by remember { mutableStateOf(false) }
    var serviceToEdit by remember { mutableStateOf<Service?>(null) }
    var serviceToDelete by remember { mutableStateOf<Service?>(null) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Services", "Inquiries")
    val pendingCount = uiState.inquiries.count { it.status != "replied" }
    val snackbarHostState = remember { SnackbarHostState() }

    // GPS permission handling for "Pin My Workshop"
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    var pendingLocationRequest by remember { mutableStateOf(false) }
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (pendingLocationRequest && locationPermissions.allPermissionsGranted) {
            pendingLocationRequest = false
            viewModel.updateMyLocation()
        }
    }
    val onSetLocation: () -> Unit = {
        if (locationPermissions.allPermissionsGranted) {
            viewModel.updateMyLocation()
        } else {
            pendingLocationRequest = true
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(uiState.locationMessage) {
        uiState.locationMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearLocationMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.background(Color.Black)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.artisan?.name ?: "Artisan Hub",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text("Manage your business & clients", color = Color.Gray, fontSize = 12.sp)
                        }
                    },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Logout", tint = Color.Red)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Black,
                    contentColor = CyanPrimary,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = CyanPrimary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(title, fontSize = 14.sp)
                                    if (index == 2 && pendingCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Badge(
                                            containerColor = StatusPending,
                                            contentColor = Color.Black
                                        ) {
                                            Text("$pendingCount", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 1) { // Services tab
                ExtendedFloatingActionButton(
                    text = { Text("New Service", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    onClick = { showAddServiceSheet = true },
                    containerColor = CyanPrimary,
                    contentColor = Color.White
                )
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyanPrimary)
            }
        } else {
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> ArtisanOverviewContent(
                        uiState = uiState,
                        pendingCount = pendingCount,
                        onAddService = {
                            selectedTab = 1
                            showAddServiceSheet = true
                        },
                        onViewServices = { selectedTab = 1 },
                        onViewInquiries = { selectedTab = 2 },
                        onOpenChat = onOpenChat,
                        onSetLocation = onSetLocation
                    )
                    1 -> ArtisanServicesContent(
                        uiState = uiState,
                        onEditService = { serviceToEdit = it }
                    )
                    2 -> ArtisanInquiriesContent(
                        uiState = uiState,
                        pendingCount = pendingCount,
                        onOpenChat = onOpenChat
                    )
                }
            }
        }
    }

    if (showAddServiceSheet) {
        ServiceSheet(
            existing = null,
            onDismiss = { showAddServiceSheet = false },
            onSave = { name, price ->
                viewModel.addService(name, price)
                showAddServiceSheet = false
            }
        )
    }

    serviceToEdit?.let { service ->
        ServiceSheet(
            existing = service,
            onDismiss = { serviceToEdit = null },
            onSave = { name, price ->
                viewModel.updateService(service, name, price)
                serviceToEdit = null
            },
            onDelete = {
                serviceToEdit = null
                serviceToDelete = service
            }
        )
    }

    serviceToDelete?.let { service ->
        AlertDialog(
            onDismissRequest = { serviceToDelete = null },
            containerColor = DarkSurface,
            title = { Text("Delete service?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${service.name}\" will be removed from your profile everywhere. Customers can no longer see it.",
                    color = Color.Gray
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteService(service.id)
                    serviceToDelete = null
                }) {
                    Text("DELETE", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { serviceToDelete = null }) {
                    Text("CANCEL", color = Color.Gray)
                }
            }
        )
    }
}

// ============================================================
// TAB 1 — BUSINESS OVERVIEW
// ============================================================

@Composable
private fun ArtisanOverviewContent(
    uiState: ArtisanDashboardUiState,
    pendingCount: Int,
    onAddService: () -> Unit,
    onViewServices: () -> Unit,
    onViewInquiries: () -> Unit,
    onOpenChat: (String) -> Unit,
    onSetLocation: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "BUSINESS PERFORMANCE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                letterSpacing = 1.sp
            )
        }

        item {
            DashboardHeader(
                artisan = uiState.artisan,
                serviceCount = uiState.services.size,
                inquiryCount = uiState.inquiries.size,
                pendingCount = pendingCount,
                onViewInquiries = onViewInquiries
            )
        }

        item {
            Text(
                "QUICK ACTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                letterSpacing = 1.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ArtisanQuickAction(
                    title = "Add Service",
                    icon = Icons.Rounded.Add,
                    color = CyanPrimary,
                    onClick = onAddService,
                    modifier = Modifier.weight(1f)
                )
                ArtisanQuickAction(
                    title = "My Services",
                    icon = Icons.Rounded.Handyman,
                    color = Color(0xFF4CAF50),
                    onClick = onViewServices,
                    modifier = Modifier.weight(1f)
                )
                ArtisanQuickAction(
                    title = "Inquiries",
                    icon = Icons.Rounded.Inbox,
                    color = StatusPending,
                    badgeCount = pendingCount,
                    onClick = onViewInquiries,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                "WORKSHOP LOCATION",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClickable(onClick = onSetLocation),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(CyanPrimary.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pin My Workshop",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Stand at your workshop and tap — your exact GPS position is saved so customers find you precisely.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (uiState.isUpdatingLocation) {
                        CircularProgressIndicator(
                            color = CyanPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "RECENT INQUIRIES",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = onViewInquiries) {
                    Text("See all", color = CyanPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (uiState.inquiries.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Inbox,
                    title = "No inquiries yet",
                    subtitle = "New customer messages will appear here as soon as they arrive."
                )
            }
        } else {
            items(uiState.inquiries.take(2), key = { it.id }) { inquiry ->
                InquiryArtisanCard(
                    inquiry = inquiry,
                    customerName = uiState.customerNames[inquiry.userId],
                    onOpenChat = { onOpenChat(inquiry.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun DashboardHeader(
    artisan: Artisan?,
    serviceCount: Int,
    inquiryCount: Int,
    pendingCount: Int,
    onViewInquiries: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(CyanGradient),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome back 👋",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = artisan?.name?.takeIf { it.isNotBlank() } ?: "Your Business",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (artisan != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFE082),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "%.1f".format(artisan.rating),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (artisan.region.isNotBlank()) {
                                Text(
                                    text = "  •  ${artisan.region}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (artisan.verified) "VERIFIED" else "PENDING REVIEW",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = if (artisan.verified) Color.White else Color(0xFFFFE082),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storefront,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroStat(
                    value = serviceCount,
                    label = "Services",
                    modifier = Modifier.weight(1f)
                )
                HeroStatDivider()
                HeroStat(
                    value = inquiryCount,
                    label = "Inquiries",
                    modifier = Modifier.weight(1f)
                )
                HeroStatDivider()
                HeroStat(
                    value = pendingCount,
                    label = "Pending",
                    modifier = Modifier.weight(1f),
                    valueColor = if (pendingCount > 0) Color(0xFFFFE082) else Color.White
                )
            }

            if (pendingCount > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    onClick = onViewInquiries,
                    color = Color.White,
                    contentColor = Color(0xFF0077B6),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MarkChatUnread,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$pendingCount ${if (pendingCount == 1) "customer is" else "customers are"} waiting for your reply",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Open inquiries",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStat(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$value",
            color = valueColor,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun HeroStatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(Color.White.copy(alpha = 0.25f))
    )
}

@Composable
private fun ArtisanQuickAction(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            // min not fixed: the tile grows instead of clipping its label when
            // the user has a large system font size
            .heightIn(min = 100.dp)
            .bounceClickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                if (badgeCount > 0) {
                    Badge(
                        containerColor = StatusPending,
                        contentColor = Color.Black,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text("$badgeCount", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ============================================================
// TAB 2 — SERVICES
// ============================================================

@Composable
private fun ArtisanServicesContent(
    uiState: ArtisanDashboardUiState,
    onEditService: (Service) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "My Services",
                count = uiState.services.size,
                icon = Icons.Rounded.Handyman
            )
        }

        if (uiState.services.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Handyman,
                    title = "No services yet",
                    subtitle = "Tap \"New Service\" to publish your first offering and start receiving inquiries."
                )
            }
        } else {
            items(uiState.services, key = { it.id }) { service ->
                ServiceManagementCard(service, onEdit = { onEditService(service) })
            }
        }
    }
}

// ============================================================
// TAB 3 — CUSTOMER INQUIRIES (MESSAGES)
// ============================================================

@Composable
private fun ArtisanInquiriesContent(
    uiState: ArtisanDashboardUiState,
    pendingCount: Int,
    onOpenChat: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Inbox,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "CUSTOMER INQUIRIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (pendingCount > 0) {
                    Surface(
                        color = StatusPending.copy(alpha = 0.12f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "$pendingCount PENDING",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusPending,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (uiState.inquiries.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Inbox,
                    title = "No inquiries received",
                    subtitle = "When customers message you about your services, their inquiries will appear here."
                )
            }
        } else {
            items(uiState.inquiries, key = { it.id }) { inquiry ->
                InquiryArtisanCard(
                    inquiry = inquiry,
                    customerName = uiState.customerNames[inquiry.userId],
                    onOpenChat = { onOpenChat(inquiry.id) }
                )
            }
        }
    }
}

// ============================================================
// SHARED COMPONENTS
// ============================================================

@Composable
private fun SectionHeader(title: String, count: Int, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Surface(
            color = DarkSurfaceVariant,
            shape = CircleShape
        ) {
            Text(
                text = "$count",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun EmptyStateCard(icon: ImageVector, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF4A4A4A),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ServiceManagementCard(service: Service, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(CyanPrimary.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Handyman,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = CurrencyUtils.formatTsh(service.price),
                    color = CyanPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = CyanPrimary)
            }
        }
    }
}

@Composable
fun InquiryArtisanCard(inquiry: Inquiry, customerName: String? = null, onOpenChat: () -> Unit) {
    val isReplied = inquiry.status == "replied"
    val lastActivity = maxOf(inquiry.lastActivityAt, inquiry.createdAt)
    val timeText = if (lastActivity > 0L) {
        DateUtils.getRelativeTimeSpanString(
            lastActivity,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    } else ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClickable(onClick = onOpenChat),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (customerName != null) CyanPrimary.copy(alpha = 0.15f) else DarkSurfaceVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (customerName != null) {
                        Text(
                            text = customerName.take(1).uppercase(),
                            color = CyanPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customerName ?: "Inquiry #${inquiry.id.take(6).uppercase()}",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isReplied) "Responded" else "Awaiting your reply",
                            color = if (isReplied) StatusReplied else StatusPending,
                            style = MaterialTheme.typography.labelSmall
                        )
                        if (timeText.isNotEmpty()) {
                            Text(
                                text = "  •  $timeText",
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                StatusChip(isReplied = isReplied, status = inquiry.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = inquiry.lastMessage.ifBlank { inquiry.message },
                    modifier = Modifier.padding(12.dp),
                    color = OnDarkSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            GradientButton(
                text = if (isReplied) "OPEN CHAT" else "REPLY IN CHAT",
                leadingIcon = Icons.Rounded.ChatBubble,
                height = 44.dp,
                onClick = onOpenChat
            )
        }
    }
}

@Composable
private fun StatusChip(isReplied: Boolean, status: String) {
    val accent = if (isReplied) StatusReplied else StatusPending
    Surface(
        color = accent.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = accent,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 52.dp,
    leadingIcon: ImageVector? = null
) {
    val background: Brush = if (enabled) {
        Brush.horizontalGradient(CyanGradient)
    } else {
        SolidColor(DarkSurfaceVariant)
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(height / 2),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (enabled) Color.White else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = if (enabled) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Add/edit sheet for a service. With [existing] null it creates; otherwise the
 * fields come prefilled and [onDelete] offers removal. Saves propagate live to
 * every screen observing the services collection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceSheet(
    existing: Service?,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var price by remember(existing) {
        mutableStateOf(
            existing?.price?.let { p ->
                if (p % 1.0 == 0.0) p.toLong().toString() else p.toString()
            } ?: ""
        )
    }
    val sheetState = rememberModalBottomSheetState()

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CyanPrimary,
        unfocusedBorderColor = Color(0xFF333333),
        focusedLabelColor = CyanPrimary,
        unfocusedLabelColor = Color.Gray,
        cursorColor = CyanPrimary,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (existing == null) "Add New Service" else "Edit Service",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (existing == null) {
                    "Describe what you offer and set your price."
                } else {
                    "Changes appear instantly everywhere customers see this service."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Service Name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = price,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                        price = input
                    }
                },
                label = { Text("Price (Tsh.)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            GradientButton(
                text = if (existing == null) "ADD SERVICE" else "SAVE CHANGES",
                enabled = name.isNotBlank() && price.toDoubleOrNull() != null,
                height = 56.dp,
                onClick = {
                    price.toDoubleOrNull()?.let { onSave(name.trim(), it) }
                }
            )
            if (onDelete != null) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete this service", color = Color.Red, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}