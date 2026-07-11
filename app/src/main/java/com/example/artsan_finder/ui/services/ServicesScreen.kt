package com.example.artsan_finder.ui.services

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.artsan_finder.utils.bounceClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    onBack: () -> Unit,
    onServiceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val services = getArtisanServices()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Our Services", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Professional solutions for your home and office",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
            )

            LazyVerticalGrid(
                // Adaptive: ~2 columns on phones, more on wide screens/tablets,
                // and never cramped on narrow devices
                columns = GridCells.Adaptive(minSize = 150.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(services) { service ->
                    ServiceCard(service, onServiceClick)
                }
            }
        }
    }
}

@Composable
fun ServiceCard(service: ArtisanService, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .bounceClickable { onClick(service.name) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(service.color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = service.icon,
                    contentDescription = null,
                    tint = service.color,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = service.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "${service.providerCount} Experts",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class ArtisanService(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val providerCount: Int
)

fun getArtisanServices(): List<ArtisanService> {
    return listOf(
        ArtisanService("Plumbing", Icons.Default.Build, Color(0xFF33B5E5), 24),
        ArtisanService("Electrical", Icons.Default.FlashOn, Color(0xFFFFBB33), 18),
        ArtisanService("Carpentry", Icons.Default.Carpenter, Color(0xFF99CC00), 12),
        ArtisanService("Painting", Icons.Default.Brush, Color(0xFFEC407A), 15),
        ArtisanService("Cleaning", Icons.Default.CleaningServices, Color(0xFF7E57C2), 30),
        ArtisanService("Masonry", Icons.Default.Landscape, Color(0xFFFF8800), 8),
        ArtisanService("Appliance", Icons.Default.Settings, Color(0xFF4285F4), 10),
        ArtisanService("Gardening", Icons.Default.Park, Color(0xFF00C851), 6)
    )
}
