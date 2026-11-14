package com.finansio.app.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finansio.app.ui.theme.Background
import com.finansio.app.ui.theme.Lavender
import com.finansio.app.ui.theme.TextSecondary

/** Layar penutup saat aplikasi terkunci. Menutupi seluruh isi aplikasi dan menahan sentuhan. */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    // Langsung tampilkan dialog sidik jari saat layar kunci muncul
    LaunchedEffect(Unit) { onUnlock() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(96.dp).clip(CircleShape).background(Lavender.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Lavender, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Finansio terkunci", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Buka dengan sidik jari atau kunci layar HP-mu.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onUnlock,
                colors = ButtonDefaults.buttonColors(containerColor = Lavender, contentColor = Background),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Buka Kunci", fontWeight = FontWeight.Bold)
            }
        }
    }
}
