package com.example.artsan_finder.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.artsan_finder.data.model.UserRole
import com.example.artsan_finder.ui.theme.CyanPrimary
import com.example.artsan_finder.ui.theme.DarkSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SubtleBorder = Color(0xFF1E1E1E)

/**
 * The signed-in user's account page: live registration details from the
 * users collection + Firebase Auth (name, login email, role, member since),
 * with self-service editing of the name, password and login email.
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showEditName by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showChangeEmail by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    val user = uiState.user
    val displayEmail = uiState.loginEmail.ifBlank { user?.email.orEmpty() }
    val memberSinceText = uiState.memberSince?.let {
        SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(it))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp)
        ) {
            item {
                ProfileHeader(
                    name = user?.name.orEmpty(),
                    email = displayEmail,
                    role = user?.role,
                    memberSince = memberSinceText
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                SectionTitle("ACCOUNT DETAILS")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Person,
                        title = "Full name",
                        value = user?.name?.ifBlank { "Not set" } ?: "Loading…",
                        onClick = { showEditName = true }
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Email,
                        title = "Login email",
                        value = displayEmail.ifBlank { "Loading…" },
                        onClick = { showChangeEmail = true }
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Badge,
                        title = "Account type",
                        value = user?.role?.displayName() ?: "…",
                        onClick = null
                    )
                    if (memberSinceText != null) {
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.CalendarMonth,
                            title = "Member since",
                            value = memberSinceText,
                            onClick = null
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            item {
                SectionTitle("SECURITY")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Lock,
                        title = "Change password",
                        value = "••••••••",
                        onClick = { showChangePassword = true }
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            item {
                SectionTitle("ACCOUNT")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.PersonRemove,
                        title = "Close account",
                        value = "Permanently delete your account and data",
                        tint = Color(0xFFE57373),
                        onClick = { showDeleteConfirm = true }
                    )
                }
                Spacer(modifier = Modifier.height(36.dp))
            }

            item {
                OutlinedButton(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("SIGN OUT", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (uiState.isWorking) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanPrimary)
            }
        }
    }

    if (showEditName) {
        SingleFieldDialog(
            title = "Edit name",
            label = "Full name",
            initialValue = user?.name.orEmpty(),
            confirmText = "SAVE",
            onDismiss = { showEditName = false },
            onConfirm = { newName ->
                viewModel.updateName(newName)
                showEditName = false
            }
        )
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            onDismiss = { showChangePassword = false },
            onConfirm = { current, new ->
                viewModel.changePassword(current, new)
                showChangePassword = false
            }
        )
    }

    if (showChangeEmail) {
        ChangeEmailDialog(
            currentEmail = displayEmail,
            onDismiss = { showChangeEmail = false },
            onConfirm = { password, newEmail ->
                viewModel.changeLoginEmail(password, newEmail)
                showChangeEmail = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = DarkSurface,
            title = { Text("Close your account?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This permanently deletes your profile and login. Your inquiries and chats become inaccessible. This cannot be undone.",
                    color = Color.Gray
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteAccount(onDeleted = onLogoutClick)
                }) {
                    Text("DELETE MY ACCOUNT", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL", color = Color.Gray)
                }
            }
        )
    }
}

private fun UserRole.displayName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

// ============================================================
// HEADER
// ============================================================

@Composable
private fun ProfileHeader(
    name: String,
    email: String,
    role: UserRole?,
    memberSince: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(CyanPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.trim().split(Regex("\\s+"))
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                    .ifBlank { "?" },
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = name.ifBlank { "Loading…" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        if (role != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = CyanPrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = role.displayName().uppercase(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = CyanPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
        if (memberSince != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Member since $memberSince",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

// ============================================================
// SETTINGS LIST
// ============================================================

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = Color.Gray,
        letterSpacing = 1.2.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(content = content)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = SubtleBorder, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String,
    tint: Color = CyanPrimary,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(tint.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================================
// DIALOGS
// ============================================================

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanPrimary,
    unfocusedBorderColor = Color(0xFF333333),
    focusedLabelColor = CyanPrimary,
    unfocusedLabelColor = Color.Gray,
    cursorColor = CyanPrimary,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)

@Composable
private fun SingleFieldDialog(
    title: String,
    label: String,
    initialValue: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = dialogFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.trim().isNotBlank(),
                onClick = { onConfirm(value) }
            ) {
                Text(confirmText, color = CyanPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.Gray) }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String, newPassword: String) -> Unit
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    val newTooShort = new.isNotEmpty() && new.length < 6
    val mismatch = confirm.isNotEmpty() && new != confirm
    val valid = current.isNotBlank() && new.length >= 6 && new == confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Change password", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = { Text("Current password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(14.dp),
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = new,
                    onValueChange = { new = it },
                    label = { Text("New password (min 6 characters)") },
                    singleLine = true,
                    isError = newTooShort,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(14.dp),
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("Confirm new password") },
                    singleLine = true,
                    isError = mismatch,
                    supportingText = if (mismatch) {
                        { Text("Passwords do not match", color = Color(0xFFE57373)) }
                    } else null,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(14.dp),
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(current, new) }) {
                Text("CHANGE", color = if (valid) CyanPrimary else Color.Gray, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.Gray) }
        }
    )
}

@Composable
private fun ChangeEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (password: String, newEmail: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    val valid = password.isNotBlank() &&
        newEmail.contains('@') &&
        !newEmail.trim().equals(currentEmail, ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Change login email", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "A verification link is sent to the new address. Your login email changes only after you confirm it there.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Current password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(14.dp),
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("New email address") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(password, newEmail.trim()) }) {
                Text("SEND LINK", color = if (valid) CyanPrimary else Color.Gray, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.Gray) }
        }
    )
}
