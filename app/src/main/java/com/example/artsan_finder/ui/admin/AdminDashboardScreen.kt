package com.example.artsan_finder.ui.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.artsan_finder.data.model.Artisan
import com.example.artsan_finder.data.repository.ArtisanRepository
import com.example.artsan_finder.data.model.Category
import com.example.artsan_finder.data.model.User
import com.example.artsan_finder.ui.theme.CyanGradient
import com.example.artsan_finder.utils.bounceClickable
import com.example.artsan_finder.utils.TanzaniaLocations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRegisterSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var managedArtisan by remember { mutableStateOf<Artisan?>(null) }
    var editingArtisan by remember { mutableStateOf<Artisan?>(null) }
    var artisanToDelete by remember { mutableStateOf<Artisan?>(null) }
    var managedCustomer by remember { mutableStateOf<User?>(null) }
    var editingCustomer by remember { mutableStateOf<User?>(null) }
    var customerToDelete by remember { mutableStateOf<User?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Artisans", "Customers")

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar("Operation successful!")
            viewModel.resetSuccessState()
        }
    }

    // Toast (not snackbar): must stay visible above open modal bottom sheets
    val toastContext = LocalContext.current
    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            Toast.makeText(toastContext, msg, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.Black)) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Admin Console", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Managing Artisan Finder Ecosystem", color = Color.Gray, fontSize = 12.sp)
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
                    contentColor = Color(0xFF00B4D8),
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF00B4D8)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 14.sp) }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 1) { // Artisans tab
                FloatingActionButton(
                    onClick = { showRegisterSheet = true },
                    containerColor = Color(0xFF00B4D8),
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = "Register Artisan")
                }
            }
        },
        containerColor = Color.Black,
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00B4D8))
            }
        } else {
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> AdminOverviewContent(uiState, viewModel, onProfileClick = { showProfileSheet = true })
                    1 -> AdminArtisansContent(uiState, onManageArtisan = { managedArtisan = it })
                    2 -> AdminCustomersContent(uiState, onManageCustomer = { managedCustomer = it })
                }
            }
        }
    }

    if (showProfileSheet) {
        AdminProfileSheet(
            user = uiState.adminUser,
            onDismiss = { showProfileSheet = false },
            onSave = { name, email, currentPassword, newPassword ->
                viewModel.updateAdminProfile(name, email, currentPassword, newPassword)
                showProfileSheet = false
            }
        )
    }

    if (showRegisterSheet) {
        ArtisanRegistrationSheet(
            categories = uiState.categories,
            uiState = uiState,
            onVerifyNida = { viewModel.verifyNida(it) },
            onPreviewLocation = { region, ward, street ->
                viewModel.previewArtisanLocation(region, ward, street)
            },
            onDismiss = {
                viewModel.clearNidaData()
                viewModel.clearLocationPreview()
                showRegisterSheet = false
            },
            onRegister = { name, catId, region, ward, street, phone, email, password, bio, imageUrl, isVerified ->
                viewModel.registerArtisan(name, catId, region, ward, street, phone, email, password, bio, imageUrl, isVerified)
                viewModel.clearLocationPreview()
                showRegisterSheet = false
            }
        )
    }

    managedArtisan?.let { artisan ->
        ArtisanManageSheet(
            artisan = artisan,
            onDismiss = { managedArtisan = null },
            onEdit = {
                editingArtisan = artisan
                managedArtisan = null
            },
            onToggleVerification = {
                if (artisan.verified) viewModel.rejectArtisan(artisan.id) else viewModel.approveArtisan(artisan.id)
                managedArtisan = null
            },
            onDelete = {
                artisanToDelete = artisan
                managedArtisan = null
            }
        )
    }

    editingArtisan?.let { artisan ->
        ArtisanEditSheet(
            artisan = artisan,
            categories = uiState.categories,
            isGeocoding = uiState.isGeocoding,
            previewCoordinates = uiState.previewCoordinates,
            previewFailed = uiState.previewFailed,
            onPreviewLocation = { region, ward, street ->
                viewModel.previewArtisanLocation(region, ward, street)
            },
            onDismiss = {
                viewModel.clearLocationPreview()
                editingArtisan = null
            },
            onSave = { updated, region, ward, street ->
                viewModel.updateArtisan(updated, region, ward, street)
                viewModel.clearLocationPreview()
                editingArtisan = null
            },
            onSendPasswordReset = { viewModel.sendPasswordReset(artisan.id) },
            onChangeLogin = { currentPassword, newEmail, newPassword ->
                viewModel.changeArtisanLogin(artisan.id, currentPassword, newEmail, newPassword)
                editingArtisan = null
            }
        )
    }

    artisanToDelete?.let { artisan ->
        ConfirmDeleteDialog(
            title = "Delete ${artisan.name}?",
            message = "This removes the artisan profile, all their services and their login account. This action cannot be undone.",
            onConfirm = {
                viewModel.deleteArtisan(artisan.id)
                artisanToDelete = null
            },
            onDismiss = { artisanToDelete = null }
        )
    }

    managedCustomer?.let { customer ->
        CustomerManageSheet(
            customer = customer,
            onDismiss = { managedCustomer = null },
            onEdit = {
                editingCustomer = customer
                managedCustomer = null
            },
            onDelete = {
                customerToDelete = customer
                managedCustomer = null
            }
        )
    }

    editingCustomer?.let { customer ->
        CustomerEditSheet(
            customer = customer,
            onDismiss = { editingCustomer = null },
            onSave = { updated ->
                viewModel.updateCustomer(updated)
                editingCustomer = null
            },
            onSendPasswordReset = { viewModel.sendPasswordReset(customer.id) }
        )
    }

    customerToDelete?.let { customer ->
        ConfirmDeleteDialog(
            title = "Delete ${customer.name}?",
            message = "This removes the customer's account. They will no longer be able to log in. This action cannot be undone.",
            onConfirm = {
                viewModel.deleteCustomer(customer.id)
                customerToDelete = null
            },
            onDismiss = { customerToDelete = null }
        )
    }
}

@Composable
fun AdminOverviewContent(uiState: AdminUiState, viewModel: AdminViewModel, onProfileClick: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("PLATFORM PERFORMANCE", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // High-level highlight card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF00B4D8), Color(0xFF0077B6))))
                            .padding(24.dp)
                    ) {
                        Column {
                            Text("Global Engagement", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = (uiState.stats["total_inquiries"] ?: 0).toString(),
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Total Interactions", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                            }
                        }
                        Icon(
                            Icons.Rounded.Analytics,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(80.dp).align(Alignment.CenterEnd)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        label = "Total Users",
                        value = uiState.stats["total_users"]?.toString() ?: "0",
                        icon = Icons.Rounded.People,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Active Services",
                        value = uiState.stats["total_services"]?.toString() ?: "0",
                        icon = Icons.Rounded.ShoppingBag,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Text("SYSTEM CONTROL", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionItem(
                        title = "Announce",
                        icon = Icons.Rounded.NotificationsActive,
                        color = Color(0xFF00B4D8),
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        title = "Profile",
                        icon = Icons.Rounded.ManageAccounts,
                        color = Color(0xFFFF9800),
                        onClick = onProfileClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionItem(
                        title = "Backup",
                        icon = Icons.Rounded.CloudUpload,
                        color = Color(0xFF4CAF50),
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        title = "Settings",
                        icon = Icons.Rounded.Settings,
                        color = Color.Gray,
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (uiState.pendingArtisans.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("VERIFICATION QUEUE", style = MaterialTheme.typography.labelSmall, color = Color.Red, letterSpacing = 1.sp)
                    Surface(
                        color = Color.Red.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${uiState.pendingArtisans.size} PENDING",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red
                        )
                    }
                }
            }
            items(uiState.pendingArtisans) { artisan ->
                VerificationCard(
                    artisan = artisan,
                    onApprove = { viewModel.approveArtisan(artisan.id) },
                    onReject = { viewModel.rejectArtisan(artisan.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun QuickActionItem(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.heightIn(min = 100.dp).bounceClickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AdminArtisansContent(uiState: AdminUiState, onManageArtisan: (Artisan) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("ARTISAN DIRECTORY", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
        }

        items(uiState.allArtisans, key = { it.id }) { artisan ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClickable(onClick = { onManageArtisan(artisan) }),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E1E1E))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).background(Color(0xFF1E1E1E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(artisan.name.take(1), color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(artisan.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(artisan.email, color = Color.Gray, fontSize = 12.sp)
                    }
                    if (artisan.verified) {
                        Surface(
                            color = Color(0xFF00B4D8).copy(alpha = 0.1f),
                            shape = CircleShape
                        ) {
                            Icon(
                                Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = Color(0xFF00B4D8),
                                modifier = Modifier.padding(4.dp).size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "Manage artisan",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun AdminCustomersContent(uiState: AdminUiState, onManageCustomer: (User) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("CUSTOMER DATABASE", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
        }

        items(uiState.allCustomers, key = { it.id }) { customer ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClickable(onClick = { onManageCustomer(customer) }),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E1E1E))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).background(Color(0xFF1E1E1E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(customer.name.take(1), color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(customer.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(customer.email, color = Color.Gray, fontSize = 12.sp)
                    }
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "Manage customer",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtisanRegistrationSheet(
    categories: List<Category>,
    uiState: AdminUiState,
    onVerifyNida: (String) -> Unit,
    onPreviewLocation: (String, String, String?) -> Unit,
    onDismiss: () -> Unit,
    onRegister: (String, String, String, String, String, String, String, String, String, String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var nidaNumber by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }
    
    // Structured Location State
    var selectedRegion by remember { mutableStateOf("") }
    var selectedWard by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    var isManualEntry by remember { mutableStateOf(false) }

    // Sync state with NIDA profile when it's fetched
    LaunchedEffect(uiState.nidaProfile) {
        uiState.nidaProfile?.let { profile ->
            name = profile.fullName
            imageUrl = profile.photoUrl
            isManualEntry = false
        }
    }

    val isFormUnlocked = isManualEntry || uiState.nidaProfile != null

    var categoryExpanded by remember { mutableStateOf(false) }
    var regionExpanded by remember { mutableStateOf(false) }
    var wardExpanded by remember { mutableStateOf(false) }

    val regions = remember { TanzaniaLocations.getRegions() }
    val wards = remember(selectedRegion) { TanzaniaLocations.getWards(selectedRegion) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        contentColor = Color.White
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Register New Artisan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Identity verification via NIDA",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- NIDA Verification Section ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        "NIDA VERIFICATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00B4D8),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = nidaNumber,
                            onValueChange = { if (it.length <= 20) nidaNumber = it },
                            label = { Text("NIDA Number (20 digits)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = adminTextFieldColors(),
                            singleLine = true,
                            enabled = uiState.nidaProfile == null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onVerifyNida(nidaNumber) },
                            enabled = nidaNumber.length == 20 && !uiState.isVerifyingNida && uiState.nidaProfile == null,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8))
                        ) {
                            if (uiState.isVerifyingNida) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("FETCH", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (uiState.nidaProfile == null && !uiState.isVerifyingNida) {
                        TextButton(
                            onClick = { isManualEntry = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Skip verification (Unverified Profile)", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    uiState.nidaError?.let {
                        Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }

                    // NIDA Profile Preview Card
                    uiState.nidaProfile?.let { profile ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color.DarkGray)
                            ) {
                                coil.compose.AsyncImage(
                                    model = profile.photoUrl,
                                    contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(profile.fullName, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("DOB: ${profile.dateOfBirth} • ${profile.gender}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                Text("✓ IDENTITY VERIFIED", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Professional Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors(),
                    enabled = isFormUnlocked // Only allow registration if unlocked
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategoryId }?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        enabled = isFormUnlocked,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = adminTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E1E1E))
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name, color = Color.White) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // --- Region Dropdown ---
            item {
                ExposedDropdownMenuBox(
                    expanded = regionExpanded,
                    onExpandedChange = { regionExpanded = !regionExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedRegion.ifEmpty { "Select Region" },
                        onValueChange = {},
                        readOnly = true,
                        enabled = isFormUnlocked,
                        label = { Text("Region") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = adminTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = regionExpanded,
                        onDismissRequest = { regionExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E1E1E))
                    ) {
                        regions.forEach { region ->
                            DropdownMenuItem(
                                text = { Text(region, color = Color.White) },
                                onClick = {
                                    selectedRegion = region
                                    selectedWard = "" // Reset ward when region changes
                                    regionExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // --- Ward Dropdown ---
            item {
                ExposedDropdownMenuBox(
                    expanded = wardExpanded,
                    onExpandedChange = { if (selectedRegion.isNotEmpty()) wardExpanded = !wardExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedWard.ifEmpty { "Select Ward" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ward") },
                        enabled = selectedRegion.isNotEmpty(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wardExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = adminTextFieldColors()
                    )
                    if (selectedRegion.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = wardExpanded,
                            onDismissRequest = { wardExpanded = false },
                            modifier = Modifier.background(Color(0xFF1E1E1E))
                        ) {
                            wards.forEach { ward ->
                                DropdownMenuItem(
                                    text = { Text(ward, color = Color.White) },
                                    onClick = {
                                        selectedWard = ward
                                        wardExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- Street Input ---
            item {
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("Street Name / Building") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormUnlocked,
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            // --- Location Verification (check the exact pin before registering) ---
            item {
                Column {
                    OutlinedButton(
                        onClick = {
                            onPreviewLocation(selectedRegion, selectedWard, street.trim().ifEmpty { null })
                        },
                        enabled = selectedRegion.isNotEmpty() && selectedWard.isNotEmpty() && !uiState.isGeocoding,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00B4D8))
                    ) {
                        if (uiState.isGeocoding) {
                            CircularProgressIndicator(
                                color = Color(0xFF00B4D8),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                Icons.Rounded.TravelExplore,
                                contentDescription = null,
                                tint = Color(0xFF00B4D8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("VERIFY LOCATION ON MAP", color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold)
                        }
                    }

                    uiState.previewCoordinates?.let { (lat, lng) ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF4CAF50).copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Address found",
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Pin: %.5f, %.5f".format(lat, lng),
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                            TextButton(onClick = {
                                val mapUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(New+Artisan)")
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, mapUri))
                                } catch (e: Exception) {
                                    val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                }
                            }) {
                                Text("OPEN MAP", color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    if (uiState.previewFailed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Could not find this exact address. Registering will pin the artisan near the region center — for exact accuracy, the artisan can use \"Pin My Workshop\" from their dashboard after logging in.",
                            color = Color(0xFFFF9800),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormUnlocked,
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormUnlocked,
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Login Password") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormUnlocked,
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            item {
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Profile Image URL") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormUnlocked,
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            item {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio / Experience") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormUnlocked,
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onRegister(name, selectedCategoryId, selectedRegion, selectedWard, street, phone, email, password, bio, imageUrl, uiState.nidaProfile != null)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = isFormUnlocked && name.isNotBlank() && selectedCategoryId.isNotBlank() && selectedRegion.isNotBlank() && selectedWard.isNotBlank() && street.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(CyanGradient), RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("REGISTER ARTISAN", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun adminTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF00B4D8),
    unfocusedBorderColor = Color(0xFF333333),
    focusedLabelColor = Color(0xFF00B4D8),
    unfocusedLabelColor = Color.Gray,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = Color(0xFF00B4D8)
)

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFF00B4D8).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF00B4D8), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun VerificationCard(artisan: Artisan, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Color.Red.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(artisan.name.take(1), color = Color.Red, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = artisan.name, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = artisan.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onReject,
                    modifier = Modifier.size(40.dp).background(Color.Red.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Reject", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onApprove,
                    modifier = Modifier.size(40.dp).background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Approve", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileSheet(
    user: com.example.artsan_finder.data.model.User?,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, currentPassword: String, newPassword: String) -> Unit
) {
    if (user == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    // Changing the login email or password requires re-authentication
    val credentialsChanging = newPassword.isNotBlank() || !email.trim().equals(user.email, ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Admin Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Update your administrative credentials",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Admin Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = adminTextFieldColors()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = adminTextFieldColors()
            )

            Text(
                "LOGIN CREDENTIALS",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF00B4D8),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "To change your login email or password, enter your current password. A new login email must be confirmed via the verification link Firebase sends to it.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password (leave blank to keep current)") },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = adminTextFieldColors()
            )

            if (credentialsChanging) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password (required)") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onSave(name, email, currentPassword, newPassword) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                enabled = name.isNotBlank() && email.isNotBlank() &&
                    (!credentialsChanging || currentPassword.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(CyanGradient), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("UPDATE PROFILE", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ============================================================
// ADMIN MANAGEMENT — EDIT / VERIFY / DELETE
// ============================================================

@Composable
fun AdminActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtisanManageSheet(
    artisan: Artisan,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleVerification: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).background(Color(0xFF1E1E1E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(artisan.name.take(1), color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(artisan.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(artisan.email, color = Color.Gray, fontSize = 12.sp)
                }
                Surface(
                    color = if (artisan.verified) Color(0xFF00B4D8).copy(alpha = 0.12f) else Color(0xFFFF9800).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (artisan.verified) "VERIFIED" else "PENDING",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (artisan.verified) Color(0xFF00B4D8) else Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AdminActionRow(
                icon = Icons.Rounded.Edit,
                title = "Edit Profile",
                subtitle = "Update name, category, contact & bio",
                color = Color(0xFF00B4D8),
                onClick = onEdit
            )
            Spacer(modifier = Modifier.height(12.dp))
            AdminActionRow(
                icon = if (artisan.verified) Icons.Rounded.Cancel else Icons.Rounded.Verified,
                title = if (artisan.verified) "Revoke Verification" else "Approve Verification",
                subtitle = if (artisan.verified) "Move artisan back to the pending queue" else "Mark this artisan as verified",
                color = if (artisan.verified) Color(0xFFFF9800) else Color(0xFF4CAF50),
                onClick = onToggleVerification
            )
            Spacer(modifier = Modifier.height(12.dp))
            AdminActionRow(
                icon = Icons.Rounded.Delete,
                title = "Delete Artisan",
                subtitle = "Remove profile, services & login account",
                color = Color.Red,
                onClick = onDelete
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerManageSheet(
    customer: User,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).background(Color(0xFF1E1E1E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(customer.name.take(1), color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(customer.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(customer.email, color = Color.Gray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AdminActionRow(
                icon = Icons.Rounded.Edit,
                title = "Edit Customer",
                subtitle = "Update name & email",
                color = Color(0xFF00B4D8),
                onClick = onEdit
            )
            Spacer(modifier = Modifier.height(12.dp))
            AdminActionRow(
                icon = Icons.Rounded.Delete,
                title = "Delete Customer",
                subtitle = "Remove this customer's account",
                color = Color.Red,
                onClick = onDelete
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtisanEditSheet(
    artisan: Artisan,
    categories: List<Category>,
    isGeocoding: Boolean,
    previewCoordinates: Pair<Double, Double>?,
    previewFailed: Boolean,
    onPreviewLocation: (String, String, String?) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Artisan, String?, String?, String?) -> Unit,
    onSendPasswordReset: () -> Unit,
    onChangeLogin: (currentPassword: String, newEmail: String, newPassword: String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(artisan.name) }
    var phone by remember { mutableStateOf(artisan.phoneNumber) }
    var email by remember { mutableStateOf(artisan.email) }
    var bio by remember { mutableStateOf(artisan.bio) }
    var imageUrl by remember { mutableStateOf(artisan.imageUrl) }
    var selectedCategoryId by remember { mutableStateOf(artisan.categoryId) }
    var categoryExpanded by remember { mutableStateOf(false) }

    // Login credential change (the profile email field above does not touch the login)
    var newLoginEmail by remember { mutableStateOf(artisan.email) }
    var newLoginPassword by remember { mutableStateOf("") }
    var currentLoginPassword by remember { mutableStateOf(ArtisanRepository.DEMO_ARTISAN_PASSWORD) }

    // Location change (optional — blank keeps the current coordinates)
    var selectedRegion by remember { mutableStateOf("") }
    var selectedWard by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var regionExpanded by remember { mutableStateOf(false) }
    var wardExpanded by remember { mutableStateOf(false) }
    val regions = remember { TanzaniaLocations.getRegions() }
    val wards = remember(selectedRegion) { TanzaniaLocations.getWards(selectedRegion) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        contentColor = Color.White
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Edit Artisan Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Profile changes save to the cloud instantly on all devices",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Professional Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategoryId }?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = adminTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E1E1E))
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name, color = Color.White) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            item {
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Profile Image URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            item {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio / Experience") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            // --- Location Update Section ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "UPDATE LOCATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00B4D8),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    "Leave blank to keep the current location. Selecting a new region & ward will update the artisan's map position automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = regionExpanded,
                    onExpandedChange = { regionExpanded = !regionExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedRegion.ifEmpty { "Keep current region" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("New Region") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = adminTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = regionExpanded,
                        onDismissRequest = { regionExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E1E1E))
                    ) {
                        regions.forEach { region ->
                            DropdownMenuItem(
                                text = { Text(region, color = Color.White) },
                                onClick = {
                                    selectedRegion = region
                                    selectedWard = "" // Reset ward when region changes
                                    regionExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = wardExpanded,
                    onExpandedChange = { if (selectedRegion.isNotEmpty()) wardExpanded = !wardExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedWard.ifEmpty { "Select Ward" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("New Ward") },
                        enabled = selectedRegion.isNotEmpty(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wardExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = adminTextFieldColors()
                    )
                    if (selectedRegion.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = wardExpanded,
                            onDismissRequest = { wardExpanded = false },
                            modifier = Modifier.background(Color(0xFF1E1E1E))
                        ) {
                            wards.forEach { ward ->
                                DropdownMenuItem(
                                    text = { Text(ward, color = Color.White) },
                                    onClick = {
                                        selectedWard = ward
                                        wardExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("Street Name / Building") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedRegion.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            // --- Location Verification (check the exact pin before saving) ---
            item {
                Column {
                    OutlinedButton(
                        onClick = {
                            onPreviewLocation(selectedRegion, selectedWard, street.trim().ifEmpty { null })
                        },
                        enabled = selectedRegion.isNotEmpty() && selectedWard.isNotEmpty() && !isGeocoding,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00B4D8))
                    ) {
                        if (isGeocoding) {
                            CircularProgressIndicator(
                                color = Color(0xFF00B4D8),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                Icons.Rounded.TravelExplore,
                                contentDescription = null,
                                tint = Color(0xFF00B4D8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("VERIFY NEW LOCATION", color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold)
                        }
                    }

                    previewCoordinates?.let { (lat, lng) ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF4CAF50).copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Address found",
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Pin: %.5f, %.5f".format(lat, lng),
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                            TextButton(onClick = {
                                val mapUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(New+Location)")
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, mapUri))
                                } catch (e: Exception) {
                                    val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                }
                            }) {
                                Text("OPEN MAP", color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    if (previewFailed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Could not find this exact address. Saving will pin the artisan near the region center — for exact accuracy, ask the artisan to use \"Pin My Workshop\" from their dashboard.",
                            color = Color(0xFFFF9800),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // --- Login Credentials Section ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "LOGIN CREDENTIALS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00B4D8),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    "Passwords live in Firebase Authentication and are never visible to anyone — not even the admin. Send the artisan a secure reset link, or set a new login for them below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            item {
                OutlinedButton(
                    onClick = onSendPasswordReset,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Rounded.LockReset, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Password Reset Email")
                }
            }

            // --- Set New Login (turns demo accounts into real ones) ---
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Set a new login email and password for this artisan. Demo artisans' current password is ${ArtisanRepository.DEMO_ARTISAN_PASSWORD}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            item {
                OutlinedTextField(
                    value = newLoginEmail,
                    onValueChange = { newLoginEmail = it },
                    label = { Text("New Login Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            item {
                OutlinedTextField(
                    value = newLoginPassword,
                    onValueChange = { newLoginPassword = it },
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            item {
                OutlinedTextField(
                    value = currentLoginPassword,
                    onValueChange = { currentLoginPassword = it },
                    label = { Text("Current Password") },
                    supportingText = {
                        Text("Needed to retire the old login", color = Color.Gray)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = adminTextFieldColors()
                )
            }

            item {
                OutlinedButton(
                    onClick = { onChangeLogin(currentLoginPassword, newLoginEmail.trim(), newLoginPassword) },
                    enabled = newLoginPassword.isNotBlank() || !newLoginEmail.trim().equals(artisan.email, ignoreCase = true),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00B4D8))
                ) {
                    Icon(Icons.Rounded.LockReset, contentDescription = null, tint = Color(0xFF00B4D8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("UPDATE LOGIN CREDENTIALS", color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold)
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onSave(
                            artisan.copy(
                                name = name.trim(),
                                categoryId = selectedCategoryId,
                                phoneNumber = phone.trim(),
                                email = email.trim(),
                                bio = bio.trim(),
                                imageUrl = imageUrl.trim()
                            ),
                            selectedRegion.ifEmpty { null },
                            selectedWard.ifEmpty { null },
                            street.trim().ifEmpty { null }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = name.isNotBlank() && email.isNotBlank() && selectedCategoryId.isNotBlank() &&
                        (selectedRegion.isEmpty() || selectedWard.isNotEmpty()),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(CyanGradient), RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SAVE CHANGES", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerEditSheet(
    customer: User,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit,
    onSendPasswordReset: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(customer.name) }
    var email by remember { mutableStateOf(customer.email) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Edit Customer",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Update this customer's account details",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Customer Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = adminTextFieldColors()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = adminTextFieldColors()
            )

            Text(
                text = "Passwords live in Firebase Authentication and are never visible to anyone. To change this customer's password, send them a secure reset link by email.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            OutlinedButton(
                onClick = onSendPasswordReset,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Rounded.LockReset, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Password Reset Email")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onSave(
                        customer.copy(
                            name = name.trim(),
                            email = email.trim()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                enabled = name.isNotBlank() && email.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(CyanGradient), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SAVE CHANGES", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color.Gray,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("DELETE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
