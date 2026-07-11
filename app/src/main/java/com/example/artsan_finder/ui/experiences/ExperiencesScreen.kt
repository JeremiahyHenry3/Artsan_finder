package com.example.artsan_finder.ui.experiences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.artsan_finder.utils.bounceClickable
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperiencesScreen(
    onBack: () -> Unit,
    onArtisanClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val experiences = getSampleExperiences()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Artisan Experiences", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = 24.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(experiences) { experience ->
                ExperienceCard(experience = experience, onClick = { artisanId -> onArtisanClick(artisanId) })
            }
        }
    }
}

@Composable
fun ExperienceCard(experience: ArtisanExperience, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().bounceClickable { onClick(experience.artisanId) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = experience.artisanImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = experience.artisanName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = experience.artisanRole,
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = experience.rating.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = experience.comment,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "By ${experience.customerName}",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

data class ArtisanExperience(
    val id: String,
    val artisanId: String,
    val artisanName: String,
    val artisanRole: String,
    val artisanImageUrl: String,
    val customerName: String,
    val comment: String,
    val rating: Float
)

fun getSampleExperiences(): List<ArtisanExperience> {
    return listOf(
        ArtisanExperience(
            id = "exp_1",
            artisanId = "art_1",
            artisanName = "Juma Hassan",
            artisanRole = "Expert Plumber",
            artisanImageUrl = "https://i.pinimg.com/736x/6d/66/af/6d66af4d10a9a7d19d1df880b0ce3b23.jpg",
            customerName = "Fatma R.",
            comment = "Juma did an amazing job fixing the leaks in my bathroom. Very professional and arrived on time. Highly recommended for any plumbing issues!",
            rating = 5.0f
        ),
        ArtisanExperience(
            id = "exp_2",
            artisanId = "art_2",
            artisanName = "Mary M.",
            artisanRole = "Certified Electrician",
            artisanImageUrl = "https://images.unsplash.com/photo-1544724569-5f546fd6f2b5?q=80&w=2070&auto=format&fit=crop",
            customerName = "Said A.",
            comment = "Great service! Mary fixed our wiring problems quickly and explained everything clearly. Feel much safer now.",
            rating = 4.8f
        ),
        ArtisanExperience(
            id = "exp_3",
            artisanId = "art_3",
            artisanName = "David L.",
            artisanRole = "Modern Carpenter",
            artisanImageUrl = "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?q=80&w=2070&auto=format&fit=crop",
            customerName = "Emmanuel K.",
            comment = "David built a custom bookshelf for my study. The craftsmanship is top-notch. It's exactly what I wanted!",
            rating = 4.9f
        ),
        ArtisanExperience(
            id = "exp_4",
            artisanId = "art_4",
            artisanName = "Sarah K.",
            artisanRole = "Professional Painter",
            artisanImageUrl = "https://i.pinimg.com/1200x/01/78/a1/0178a10b5c4fba3512c66e4c950c4ffd.jpg",
            customerName = "Lillian M.",
            comment = "The painting work was flawless. Sarah was very neat and finished the job ahead of schedule. My living room looks brand new!",
            rating = 4.7f
        ),
        ArtisanExperience(
            id = "exp_5",
            artisanId = "art_5",
            artisanName = "Amos P.",
            artisanRole = "24/7 Emergency Plumber",
            artisanImageUrl = "https://i.pinimg.com/736x/42/4d/66/424d66f9d433eb170b5e14badfce7950.jpg",
            customerName = "Hassan M.",
            comment = "Had a terrible pipe burst at midnight. Amos arrived within 30 minutes and fixed everything perfectly. Lifesaver!",
            rating = 5.0f
        ),
        ArtisanExperience(
            id = "exp_6",
            artisanId = "art_6",
            artisanName = "Grace J.",
            artisanRole = "Rapid Response Electrician",
            artisanImageUrl = "https://i.pinimg.com/1200x/00/ce/ab/00ceabcf6b336b1a9e2a22eb971baa9b.jpg",
            customerName = "Amina K.",
            comment = "Power went out and Grace was there in minutes. Professional, safe, and incredibly fast. Highly trusted her expertise.",
            rating = 4.9f
        ),
        ArtisanExperience(
            id = "exp_7",
            artisanId = "art_7",
            artisanName = "Kelvin T.",
            artisanRole = "Drain Specialist",
            artisanImageUrl = "https://i.pinimg.com/1200x/b6/54/f3/b654f3336a2ccec52ed2d78346903541.jpg",
            customerName = "Peter N.",
            comment = "Severe drain clog that other services couldn't handle. Kelvin cleared it in less than an hour with advanced equipment.",
            rating = 4.8f
        ),
        ArtisanExperience(
            id = "exp_8",
            artisanId = "art_8",
            artisanName = "Peter O.",
            artisanRole = "Custom Furniture Specialist",
            artisanImageUrl = "https://i.pinimg.com/736x/dc/81/43/dc8143a0f8c5b060aa51a8b0119bdbe9.jpg",
            customerName = "James W.",
            comment = "Peter transformed my living room with custom furniture. His designs are unique and the quality is exceptional. Worth every penny!",
            rating = 4.9f
        ),
        ArtisanExperience(
            id = "exp_9",
            artisanId = "art_9",
            artisanName = "Aisha S.",
            artisanRole = "Interior Painter",
            artisanImageUrl = "https://images.unsplash.com/photo-1562259949-e8e7689d7828?q=80&w=2070&auto=format&fit=crop",
            customerName = "Maria R.",
            comment = "Aisha painted our entire home with beautiful finishes. Her attention to detail and professionalism is unmatched in the industry.",
            rating = 4.9f
        ),
        ArtisanExperience(
            id = "exp_10",
            artisanId = "art_10",
            artisanName = "Bakari",
            artisanRole = "Kitchen Remodeling Expert",
            artisanImageUrl = "https://images.unsplash.com/photo-1556911220-e15b29be8c8f?q=80&w=2070&auto=format&fit=crop",
            customerName = "Linda P.",
            comment = "Bakari completely remodeled our kitchen exactly as we wanted. On time, on budget, and the results are stunning. Highly recommend!",
            rating = 4.8f
        )
    )
}
