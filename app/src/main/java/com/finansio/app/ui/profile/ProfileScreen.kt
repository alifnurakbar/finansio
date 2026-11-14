package com.finansio.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finansio.app.data.settings.SettingsRepository
import com.finansio.app.ui.components.FinansioCard
import com.finansio.app.ui.components.FinansioTopBar
import com.finansio.app.ui.theme.Background
import com.finansio.app.ui.theme.Lavender
import com.finansio.app.ui.theme.Lime
import com.finansio.app.ui.theme.TextSecondary
import com.finansio.app.ui.theme.Yellow

@Composable
fun ProfileScreen(
    settingsRepository: SettingsRepository,
    onNavigateToCategories: () -> Unit,
    onNavigateToBudget: () -> Unit
) {
    val userName by settingsRepository.userName.collectAsState()
    var showNameDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            FinansioTopBar(title = "Profil")

            // Kartu profil
            FinansioCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(Lime),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Background
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName.ifBlank { "Belum ada nama" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Pengguna Finansio", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    IconButton(onClick = { showNameDialog = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Ubah nama", tint = TextSecondary)
                    }
                }
            }

            SectionTitle("Keuangan")
            SettingsGroup {
                SettingsRow(Icons.Rounded.Category, Lavender, "Kelola Kategori", "Tambah, ubah, dan hapus kategori", onNavigateToCategories)
                SettingsRow(Icons.Rounded.AccountBalanceWallet, Lime, "Anggaran", "Atur batas pengeluaran bulanan", onNavigateToBudget)
            }

            SectionTitle("Lainnya")
            SettingsGroup {
                SettingsRow(Icons.Rounded.Info, Yellow, "Tentang Aplikasi", "Versi dan informasi pengembang") { showAbout = true }
            }
        }
    }

    if (showNameDialog) {
        var name by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Nama Anda", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    singleLine = true,
                    label = { Text("Nama panggilan") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    settingsRepository.setUserName(name)
                    showNameDialog = false
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("Batal") } }
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp), content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) { null } ?: "-"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = Lime) },
        title = { Text("Finansio", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Versi $version", color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Aplikasi pencatat keuangan pribadi berbasis Android dengan integrasi AI.",
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text("Dikembangkan oleh", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text("Alif Nur Akbar", fontWeight = FontWeight.Bold)
                Text("sebagai proyek skripsi", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Dikembangkan dari starter code oleh Satria (Kawan Kode).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}
