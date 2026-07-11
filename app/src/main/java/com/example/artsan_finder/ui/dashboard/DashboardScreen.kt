package com.example.artsan_finder.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.artsan_finder.data.model.Artisan
import com.example.artsan_finder.ui.theme.CyanGradient
import com.example.artsan_finder.utils.bounceClickable
import com.example.artsan_finder.utils.animateEntrance
import java.util.Locale

@Composable
fun DashboardScreen(
    userName: String,
    artisans: List<Artisan>,
    onBrowseClick: () -> Unit,
    onArtisanClick: (String) -> Unit,
    onExperiencesClick: () -> Unit,
    onServicesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedTopTab by remember { mutableStateOf(0) }
    val topTabs = listOf("Homes", "Experiences", "Services")

    // LIVE artisans only. Every card must open a profile that actually exists
    // in Firestore — a card with a stale hardcoded id produced inquiries no
    // artisan account could ever receive or reply to.
    val allArtisans = remember(artisans) {
        artisans.filter { it.verified }.ifEmpty { artisans }.sortedByDescending { it.rating }
    }
    val emergencyArtisans = remember(allArtisans) {
        allArtisans.filter { artisan ->
            listOf("emergency", "24/7", "urgent", "rapid")
                .any { artisan.bio.contains(it, ignoreCase = true) }
        }
    }
    val recommendedArtisans = remember(allArtisans) {
        // Carpenters & painters — matches the section's renovation pitch
        allArtisans.filter { it.categoryId == "cat_3" || it.categoryId == "cat_5" }
    }

    val filteredArtisans = remember(searchQuery, allArtisans) {
        if (searchQuery.isBlank()) {
            allArtisans
        } else {
            allArtisans.filter { artisan ->
                artisan.name.contains(searchQuery, ignoreCase = true) ||
                    artisan.region.contains(searchQuery, ignoreCase = true) ||
                    artisan.ward.contains(searchQuery, ignoreCase = true) ||
                    artisan.bio.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().animateEntrance(),
        containerColor = Color.Black,
        topBar = {
            Surface(
                color = Color.Black,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(28.dp)),
                        placeholder = {
                            Text(
                                "Start your search",
                                color = Color.Gray,
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(28.dp),
                        singleLine = true
                    )

                    // Tabs (Homes, Experiences, Services)
                    TabRow(
                        selectedTabIndex = selectedTopTab,
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        divider = {},
                        indicator = { positions ->
                            if (selectedTopTab < positions.size) {
                                Box(
                                    Modifier
                                        .tabIndicatorOffset(positions[selectedTopTab])
                                        .height(2.dp)
                                        .background(Color.White)
                                )
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        topTabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTopTab == index,
                                onClick = { 
                                    selectedTopTab = index 
                                    if (title == "Experiences") {
                                        onExperiencesClick()
                                    } else if (title == "Services") {
                                        onServicesClick()
                                    }
                                },
                                text = {
                                    Text(
                                        title,
                                        color = if (selectedTopTab == index) Color.White else Color.Gray,
                                        fontWeight = if (selectedTopTab == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (allArtisans.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00B4D8))
                    }
                }
            } else {
                item {
                    ArtisanSection(
                        title = if (searchQuery.isEmpty()) "Top-rated Artisans" else "Search Results",
                        subtitle = if (searchQuery.isEmpty()) "Quality work from verified local professionals." else "Found ${filteredArtisans.size} artisans matching your search",
                        artisans = filteredArtisans,
                        onArtisanClick = onArtisanClick
                    )
                }

                if (searchQuery.isEmpty() && emergencyArtisans.isNotEmpty()) {
                    item {
                        ArtisanSection(
                            title = "Emergency Repairs",
                            subtitle = "Available now for urgent plumbing and electrical needs.",
                            artisans = emergencyArtisans,
                            onArtisanClick = onArtisanClick
                        )
                    }
                }

                if (searchQuery.isEmpty() && recommendedArtisans.isNotEmpty()) {
                    item {
                        ArtisanSection(
                            title = "Recommended for your next project",
                            subtitle = "Highly skilled carpenters and painters for your home renovation.",
                            artisans = recommendedArtisans,
                            onArtisanClick = onArtisanClick
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            onDismiss = { showFilterSheet = false },
            onApply = { /* Apply filters logic */ }
        )
    }
}

@Composable
fun ArtisanSection(
    title: String,
    artisans: List<Artisan>,
    onArtisanClick: (String) -> Unit,
    subtitle: String? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            items(artisans, key = { it.id }) { artisan ->
                DashboardArtisanCard(
                    id = artisan.id,
                    name = artisan.name,
                    location = listOf(artisan.ward, artisan.region)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                        .ifBlank { "Tanzania" },
                    price = artisan.bio.substringBefore('.').take(70)
                        .ifBlank { "Tap to view services & prices" },
                    rating = artisan.rating,
                    imageUrl = artisan.imageUrl,
                    onArtisanClick = onArtisanClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardArtisanCard(
    id: String,
    name: String,
    location: String,
    price: String,
    rating: Float,
    imageUrl: String,
    onArtisanClick: (String) -> Unit
) {
    var isFavorited by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(280.dp)
            .bounceClickable { onArtisanClick(id) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = null, // You can add a painterResource here if you have one
                error = null // You can add a painterResource here if you have one
            )

            // Guest favorite badge
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "Top Artisan",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Wishlist icon
            IconButton(
                onClick = { isFavorited = !isFavorited },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Wishlist",
                    tint = if (isFavorited) Color.Red else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 16.sp
        )

        // Price/Subtitle
        Text(
            text = price,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            fontSize = 14.sp
        )

        // Rating
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = String.format(Locale.getDefault(), "%.2f", rating),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

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
                text = "Advanced Filters",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text("Minimum Rating", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                listOf("3.0+", "4.0+", "4.5+").forEach { rating ->
                    FilterChip(
                        selected = rating == "4.0+",
                        onClick = { },
                        label = { Text(rating) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00B4D8),
                            containerColor = Color(0xFF1E1E1E)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Availability", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                listOf("Now", "Today", "This Week").forEach { availability ->
                    FilterChip(
                        selected = availability == "Today",
                        onClick = { },
                        label = { Text(availability) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00B4D8),
                            containerColor = Color(0xFF1E1E1E)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onApply(); onDismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
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
                    Text("APPLY FILTERS", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
