package com.example.artsan_finder.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.artsan_finder.data.model.Artisan
import com.example.artsan_finder.data.model.Service
import com.example.artsan_finder.ui.theme.CyanGradient
import com.example.artsan_finder.utils.bounceClickable
import com.example.artsan_finder.utils.animateEntrance
import com.example.artsan_finder.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtisanDetailScreen(
    viewModel: ArtisanDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showInquirySheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.inquirySent) {
        if (uiState.inquirySent) {
            snackbarHostState.showSnackbar("Inquiry sent successfully!")
            viewModel.resetInquiryStatus()
        }
    }

    // A rejected inquiry must be visible — silently swallowing it strands the
    // customer in a chat thread the artisan will never receive.
    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection).animateEntrance(),
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        uiState.artisan?.name ?: "Artisan Profile",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share or more action */ }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Black,
                    scrolledContainerColor = Color.Black,
                    titleContentColor = Color.White
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            Button(
                onClick = { showInquirySheet = true },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(56.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(CyanGradient), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Mail, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BOOK APPOINTMENT", fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00B4D8))
            }
        } else {
            uiState.artisan?.let { artisan ->
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        ArtisanProfileHeaderRefined(artisan)
                    }
                    item {
                        Text(
                            text = "OFFERED SERVICES",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )
                    }
                    items(uiState.services) { service ->
                        ServiceDetailItemRefined(service)
                    }
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    if (showInquirySheet) {
        InquirySheet(
            onDismiss = { showInquirySheet = false },
            onSend = { message: String ->
                viewModel.sendInquiry(message)
                showInquirySheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InquirySheet(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var message by remember { mutableStateOf("") }

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
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Contact Artisan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter your requirements below.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("How can they help you?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00B4D8),
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedLabelColor = Color(0xFF00B4D8),
                    unfocusedLabelColor = Color.Gray,
                    cursorColor = Color(0xFF00B4D8)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onSend(message) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = message.isNotBlank(),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (message.isNotBlank()) Brush.horizontalGradient(CyanGradient)
                            else Brush.horizontalGradient(listOf(Color.Gray, Color.DarkGray)),
                            RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SEND MESSAGE", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ArtisanProfileHeaderRefined(artisan: Artisan) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF121212))
        ) {
            AsyncImage(
                model = artisan.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )
            // Badges
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFF00B4D8),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "PREMIUM",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = Color(0xFF4CAF50),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "VERIFIED",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = artisan.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Color(0xFF00B4D8),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = " ${artisan.rating} • 120+ Reviews",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "ABOUT THE ARTISAN",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = artisan.bio,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.LightGray,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        var showContactSheet by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ContactInfoItem(
                icon = Icons.Rounded.Phone,
                label = "Call",
                value = artisan.phoneNumber,
                onClick = { showContactSheet = true }
            )
            ContactInfoItem(
                icon = Icons.Rounded.Email,
                label = "Email",
                value = artisan.email,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${artisan.email}")
                        putExtra(Intent.EXTRA_SUBJECT, "Inquiry for ${artisan.name} (via Artisan Finder)")
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, "Send Email..."))
                    } catch (e: Exception) {
                        // Fallback if no email client is found
                    }
                }
            )
            ContactInfoItem(
                icon = Icons.Rounded.LocationOn,
                label = "Location",
                value = listOf(artisan.ward, artisan.region)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                    .ifEmpty { "View on Map" },
                onClick = {
                    // Using geo: intent with coordinates for exact precision
                    // 'q' parameter adds a marker with the artisan's name
                    val mapUri = Uri.parse("geo:${artisan.latitude},${artisan.longitude}?q=${artisan.latitude},${artisan.longitude}(${Uri.encode(artisan.name)})")
                    val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                    
                    try {
                        context.startActivity(mapIntent)
                    } catch (e: Exception) {
                        // Fallback to Google Maps in browser if no app is available
                        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${artisan.latitude},${artisan.longitude}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                    }
                }
            )
        }

        if (showContactSheet) {
            ContactOptionsSheet(
                phoneNumber = artisan.phoneNumber,
                onDismiss = { showContactSheet = false }
            )
        }
    }
}

@Composable
fun ContactInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = if (onClick != null) Modifier.bounceClickable(onClick = onClick) else Modifier
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF00B4D8), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ServiceDetailItemRefined(service: Service) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Estimated Time: 2-4 Hours",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Surface(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00B4D8))
            ) {
                Text(
                    text = CurrencyUtils.formatTsh(service.price),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF00B4D8),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactOptionsSheet(
    phoneNumber: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Contact Artisan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Call Option
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phoneNumber")
                    }
                    context.startActivity(intent)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00B4D8), Color(0xFF0096C7))
                            ),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Rounded.Phone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "CALL NOW",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // WhatsApp Option
            Button(
                onClick = {
                    val formattedPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
                    val whatsappUrl = "https://wa.me/$formattedPhone"
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(whatsappUrl)
                    }
                    try {
                        context.startActivity(intent)
                        onDismiss()
                    } catch (e: Exception) {
                        // WhatsApp not installed, fallback to web
                        val webIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://web.whatsapp.com/send?phone=$formattedPhone")
                        }
                        context.startActivity(webIntent)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF25D366), Color(0xFF20BA5A))
                            ),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Message,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "WHATSAPP CHAT",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


