package com.finansio.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Lembar pilihan cara mencatat transaksi: input manual, scan struk, atau suara. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    onDismissRequest: () -> Unit,
    onManualInputClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    onVoiceInputClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            Text(
                text = "Pilih Metode Input",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Divider()
            
            ListItem(
                headlineContent = { Text("Input Manual") },
                leadingContent = {
                    Icon(Icons.Rounded.Edit, contentDescription = "Manual")
                },
                modifier = Modifier.clickable {
                    onDismissRequest()
                    onManualInputClick()
                }
            )
            
            ListItem(
                headlineContent = { Text("Scan Struk (AI)") },
                leadingContent = {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = "Scan Struk")
                },
                modifier = Modifier.clickable {
                    onDismissRequest()
                    onScanReceiptClick()
                }
            )

            ListItem(
                headlineContent = { Text("Rekam Suara (AI)") },
                leadingContent = {
                    Icon(Icons.Rounded.Mic, contentDescription = "Rekam Suara")
                },
                modifier = Modifier.clickable {
                    onDismissRequest()
                    onVoiceInputClick()
                }
            )
        }
    }
}
