package com.finansio.app.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
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
import androidx.fragment.app.FragmentActivity
import com.finansio.app.data.backup.BackupFile
import com.finansio.app.data.backup.BackupManager
import com.finansio.app.data.export.ExportFormat
import com.finansio.app.data.export.ReportExporter
import com.finansio.app.data.reminder.DailyReminder
import com.finansio.app.data.settings.ReminderTime
import com.finansio.app.data.settings.SettingsRepository
import com.finansio.app.security.AppLock
import com.finansio.app.ui.components.FinansioCard
import com.finansio.app.ui.components.FinansioTopBar
import com.finansio.app.ui.components.MonthNavigatorBar
import com.finansio.app.ui.theme.Background
import com.finansio.app.ui.theme.Coral
import com.finansio.app.ui.theme.Lavender
import com.finansio.app.ui.theme.Lime
import com.finansio.app.ui.theme.TextSecondary
import com.finansio.app.ui.theme.Yellow
import com.finansio.app.util.DateUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/** Halaman Profil & Pengaturan: nama pengguna, Kelola Kategori, Anggaran, Ekspor Laporan, Pengingat Harian, Kunci Sidik Jari, Backup/Restore, dan Tentang Aplikasi. */
@Composable
fun ProfileScreen(
    settingsRepository: SettingsRepository,
    reportExporter: ReportExporter,
    backupManager: BackupManager,
    onNavigateToCategories: () -> Unit,
    onNavigateToBudget: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userName by settingsRepository.userName.collectAsState()
    val reminderEnabled by settingsRepository.reminderEnabled.collectAsState()
    val reminderTime by settingsRepository.reminderTime.collectAsState()
    val lockEnabled by settingsRepository.lockEnabled.collectAsState()

    var showNameDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<BackupFile?>(null) }
    var busyMessage by remember { mutableStateOf<String?>(null) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    // --- Backup: pilih lokasi simpan (memori HP atau Google Drive) ---
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busyMessage = "Membuat backup…"
            runCatching { backupManager.backupTo(uri) }
                .onSuccess { toast("Backup berhasil: ${it.transactions.size} transaksi, ${it.categories.size} kategori, ${it.budgets.size} anggaran") }
                .onFailure { toast("Backup gagal: ${it.message}") }
            busyMessage = null
        }
    }

    // --- Restore: pilih file, periksa isinya, lalu minta konfirmasi ---
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { backupManager.readBackup(uri) }
                .onSuccess { pendingRestore = it }
                .onFailure { toast(it.message ?: "File tidak bisa dibaca.") }
        }
    }

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
                SettingsRow(Icons.Rounded.Category, Lavender, "Kelola Kategori", "Tambah, ubah, dan hapus kategori", onClick = onNavigateToCategories)
                SettingsRow(Icons.Rounded.AccountBalanceWallet, Lime, "Anggaran", "Atur batas pengeluaran bulanan", onClick = onNavigateToBudget)
                SettingsRow(Icons.Rounded.FileDownload, Yellow, "Ekspor Laporan", "Laporan bulanan PDF atau Excel (CSV)") { showExport = true }
            }

            SectionTitle("Notifikasi & Keamanan")
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Rounded.Alarm,
                    tint = Coral,
                    title = "Pengingat Harian",
                    subtitle = if (reminderEnabled) "Setiap hari pukul $reminderTime • ketuk untuk ubah jam" else "Nonaktif",
                    onClick = { if (reminderEnabled) showTimePicker = true },
                    trailing = {
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = {
                                settingsRepository.setReminderEnabled(it)
                                DailyReminder.reschedule(context)
                            }
                        )
                    }
                )
                SettingsRow(
                    icon = Icons.Rounded.Fingerprint,
                    tint = Lavender,
                    title = "Kunci Sidik Jari",
                    subtitle = if (lockEnabled) "Aktif • diminta saat membuka aplikasi" else "Nonaktif",
                    onClick = null,
                    trailing = {
                        Switch(
                            checked = lockEnabled,
                            onCheckedChange = { enable ->
                                val activity = context as? FragmentActivity ?: return@Switch
                                val reason = AppLock.unavailableReason(context)
                                if (reason != null) {
                                    toast(reason)
                                    return@Switch
                                }
                                // Verifikasi dulu sebelum mengubah pengaturan kunci
                                AppLock.authenticate(
                                    activity,
                                    title = if (enable) "Aktifkan kunci Finansio" else "Matikan kunci Finansio",
                                    subtitle = "Konfirmasi dengan sidik jari atau kunci layar HP",
                                    onSuccess = { settingsRepository.setLockEnabled(enable) }
                                )
                            }
                        )
                    }
                )
            }

            SectionTitle("Data")
            SettingsGroup {
                SettingsRow(Icons.Rounded.CloudUpload, Lime, "Backup Data", "Simpan semua data ke file (bisa ke Google Drive)") {
                    val date = SimpleDateFormat("yyyy-MM-dd", DateUtils.localeID).format(Date())
                    backupLauncher.launch("Finansio-backup-$date.json")
                }
                SettingsRow(Icons.Rounded.CloudDownload, Yellow, "Restore Data", "Pulihkan data dari file backup") {
                    restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                }
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

    if (showTimePicker) {
        ReminderTimeDialog(
            initial = reminderTime,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                settingsRepository.setReminderTime(it)
                DailyReminder.reschedule(context)
                showTimePicker = false
                toast("Pengingat diatur setiap hari pukul $it")
            }
        )
    }

    if (showExport) {
        ExportDialog(
            onDismiss = { showExport = false },
            onExport = { year, month, format ->
                showExport = false
                scope.launch {
                    busyMessage = "Membuat laporan…"
                    runCatching { reportExporter.createShareIntent(year, month, format, userName) }
                        .onSuccess { context.startActivity(it) }
                        .onFailure { toast("Gagal membuat laporan: ${it.message}") }
                    busyMessage = null
                }
            }
        )
    }

    pendingRestore?.let { backup ->
        val created = SimpleDateFormat("d MMMM yyyy, HH:mm", DateUtils.localeID).format(Date(backup.createdAt))
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            icon = { Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = Yellow) },
            title = { Text("Pulihkan data?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backup tanggal $created berisi:")
                    Text(
                        "• ${backup.transactions.size} transaksi\n• ${backup.categories.size} kategori\n• ${backup.budgets.size} anggaran",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Semua data yang ada di HP sekarang akan DIGANTI dengan isi backup ini. Tindakan ini tidak bisa dibatalkan.",
                        color = Coral,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestore = null
                        scope.launch {
                            busyMessage = "Memulihkan data…"
                            runCatching { backupManager.restore(backup) }
                                .onSuccess { toast("Data berhasil dipulihkan") }
                                .onFailure { toast("Restore gagal, data lama tidak diubah: ${it.message}") }
                            busyMessage = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Coral)
                ) { Text("Ganti Data") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Batal") } }
        )
    }

    busyMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(16.dp))
                    Text(message)
                }
            }
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(initial: ReminderTime, onDismiss: () -> Unit, onConfirm: (ReminderTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Jam pengingat", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                TimePicker(state = state)
                Text(
                    "Pengingat dilewati jika hari itu kamu sudah mencatat transaksi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(ReminderTime(state.hour, state.minute)) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun ExportDialog(onDismiss: () -> Unit, onExport: (Int, Int, ExportFormat) -> Unit) {
    val now = remember { Calendar.getInstance() }
    var year by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }
    fun shift(delta: Int) {
        val cal = Calendar.getInstance().apply { set(year, month, 1); add(Calendar.MONTH, delta) }
        year = cal.get(Calendar.YEAR); month = cal.get(Calendar.MONTH)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ekspor Laporan", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pilih bulan laporan:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                MonthNavigatorBar(
                    label = DateUtils.formatMonthYear(year, month),
                    onPrev = { shift(-1) },
                    onNext = { shift(1) }
                )
                Text("Pilih format:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                ExportOption(Icons.Rounded.PictureAsPdf, Coral, "PDF", "Laporan rapi untuk dibagikan atau dicetak") {
                    onExport(year, month, ExportFormat.PDF)
                }
                ExportOption(Icons.Rounded.TableChart, Lime, "Excel (CSV)", "Data transaksi untuk Excel / Google Sheets") {
                    onExport(year, month, ExportFormat.CSV)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun ExportOption(icon: ImageVector, tint: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
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
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
        if (trailing != null) trailing()
        else Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextSecondary)
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
