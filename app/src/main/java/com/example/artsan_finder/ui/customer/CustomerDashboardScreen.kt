package com.example.artsan_finder.ui.customer

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.artsan_finder.data.model.Inquiry
import com.example.artsan_finder.ui.discovery.DiscoveryScreen
import com.example.artsan_finder.ui.discovery.DiscoveryViewModel
import com.example.artsan_finder.utils.bounceClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboardScreen(
    discoveryViewModel: DiscoveryViewModel,
    inquiries: List<Inquiry>,
    artisanNames: Map<String, String>,
    onArtisanClick: (String) -> Unit,
    onInquiryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Black,
                contentColor = Color(0xFF00B4D8),
                indicator = { 
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTab),
                        color = Color(0xFF00B4D8)
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Discover", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Rounded.Search, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("My Inquiries", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Rounded.Info, contentDescription = null) }
                )
            }
        },
        containerColor = Color.Black,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> DiscoveryScreen(
                    viewModel = discoveryViewModel,
                    onArtisanClick = onArtisanClick
                )
                1 -> MyInquiriesList(inquiries, artisanNames, onInquiryClick)
            }
        }
    }
}

@Composable
fun MyInquiriesList(
    inquiries: List<Inquiry>,
    artisanNames: Map<String, String>,
    onInquiryClick: (String) -> Unit
) {
    if (inquiries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No inquiries sent yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(inquiries, key = { it.id }) { inquiry ->
                InquiryCustomerCard(
                    inquiry = inquiry,
                    artisanName = artisanNames[inquiry.artisanId],
                    onClick = { onInquiryClick(inquiry.id) }
                )
            }
        }
    }
}

@Composable
fun InquiryCustomerCard(inquiry: Inquiry, artisanName: String?, onClick: () -> Unit) {
    val isReplied = inquiry.status == "replied"
    val statusColor = if (isReplied) Color(0xFF4CAF50) else Color(0xFFFFB300)
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
            .bounceClickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artisanName ?: "Inquiry to #${inquiry.artisanId.take(4)}",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isReplied) "Artisan replied" else "Waiting for reply",
                            color = statusColor,
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
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = inquiry.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = inquiry.lastMessage.ifBlank { inquiry.message },
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.ChatBubble,
                    contentDescription = null,
                    tint = Color(0xFF00B4D8),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tap to open chat",
                    color = Color(0xFF00B4D8),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
