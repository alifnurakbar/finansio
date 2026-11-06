package com.finansio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.finansio.app.ui.FinansioViewModelFactory
import com.finansio.app.ui.navigation.AppNavGraph
import com.finansio.app.ui.navigation.Screen
import com.finansio.app.ui.theme.FinansioTheme
import com.finansio.app.ui.components.AddTransactionBottomSheet
import com.finansio.app.ui.scanner.ReceiptScannerHelper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finansio.app.ui.MonthFilterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as FinansioApplication
        // MonthFilterViewModel is created outside the factory so it's shared across screens
        val monthFilterViewModel = MonthFilterViewModel()
        val factory = FinansioViewModelFactory(app.transactionRepository, app.categoryRepository, app.aiRepository, monthFilterViewModel)
        
        enableEdgeToEdge()
        setContent {
            FinansioTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                val isBottomBarVisible = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Statistics.route,
                    Screen.Category.route
                )

                val categoryViewModel: com.finansio.app.ui.category.CategoryViewModel = viewModel(factory = factory)
                val homeViewModel: com.finansio.app.ui.transaction.list.HomeViewModel = viewModel(factory = factory)
                val statisticsViewModel: com.finansio.app.ui.statistics.StatisticsViewModel = viewModel(factory = factory)
                val transactionFormViewModel: com.finansio.app.ui.transaction.form.TransactionFormViewModel = viewModel(factory = factory)
                val transactionHistoryViewModel: com.finansio.app.ui.transaction.history.TransactionHistoryViewModel = viewModel(factory = factory)
                val scanResultViewModel: com.finansio.app.ui.scanner.ScanResultViewModel = viewModel(factory = factory)

                var showBottomSheet by remember { mutableStateOf(false) }
                val context = LocalContext.current

                val scannerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                        val imageUri = scanningResult?.pages?.firstOrNull()?.imageUri
                        if (imageUri != null) {
                            scanResultViewModel.processScannedImage(context, imageUri)
                            navController.navigate(Screen.ScanResult.route)
                        }
                    }
                }

                val scanner = remember { ReceiptScannerHelper.getScanner() }

                val speechLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        val data = result.data
                        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        val voiceText = results?.firstOrNull()
                        if (!voiceText.isNullOrBlank()) {
                            scanResultViewModel.processVoiceText(voiceText)
                            navController.navigate(Screen.ScanResult.route)
                        }
                    }
                }

                fun launchSpeechToText() {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Silakan bicara dalam Bahasa Indonesia...")
                    }
                    try {
                        speechLauncher.launch(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "Perangkat tidak mendukung Speech-to-Text", Toast.LENGTH_SHORT).show()
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        launchSpeechToText()
                    } else {
                        Toast.makeText(context, "Izin mikrofon ditolak", Toast.LENGTH_SHORT).show()
                    }
                }

                if (showBottomSheet) {
                    AddTransactionBottomSheet(
                        onDismissRequest = { showBottomSheet = false },
                        onManualInputClick = {
                            navController.navigate(Screen.TransactionForm.createRoute())
                        },
                        onScanReceiptClick = {
                            scanner.getStartScanIntent(this@MainActivity)
                                .addOnSuccessListener { intentSender ->
                                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                }
                        },
                        onVoiceInputClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                launchSpeechToText()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (isBottomBarVisible) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Home.route,
                                    onClick = { navController.navigate(Screen.Home.route) { popUpTo(0) } },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Statistics.route,
                                    onClick = { navController.navigate(Screen.Statistics.route) { popUpTo(0) } },
                                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Statistik") },
                                    label = { Text("Statistik") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Category.route,
                                    onClick = { navController.navigate(Screen.Category.route) { popUpTo(0) } },
                                    icon = { Icon(Icons.Default.List, contentDescription = "Kategori") },
                                    label = { Text("Kategori") }
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        if (isBottomBarVisible) {
                            FloatingActionButton(
                                onClick = { 
                                    if (currentRoute == Screen.Category.route) {
                                        categoryViewModel.triggerAddCategory()
                                    } else {
                                        showBottomSheet = true
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Default.Add, contentDescription = if (currentRoute == Screen.Category.route) "Tambah Kategori" else "Catat", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                ) { _ ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavGraph(
                            navController = navController,
                            homeViewModel = homeViewModel,
                            statisticsViewModel = statisticsViewModel,
                            categoryViewModel = categoryViewModel,
                            transactionFormViewModel = transactionFormViewModel,
                            transactionHistoryViewModel = transactionHistoryViewModel,
                            scanResultViewModel = scanResultViewModel,
                            monthFilterViewModel = monthFilterViewModel
                        )
                    }
                }
            }
        }
    }
}