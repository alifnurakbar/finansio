package com.finansio.app.ui.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.finansio.app.ui.theme.Background
import com.finansio.app.ui.theme.Coral
import com.finansio.app.ui.theme.Lavender
import com.finansio.app.ui.theme.Lime
import com.finansio.app.ui.theme.SurfaceRaised
import com.finansio.app.ui.theme.TextSecondary

@Composable
fun ChatScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Gulir ke pesan terbaru setiap ada pesan baru atau indikator mengetik
    LaunchedEffect(messages.size, isLoading) {
        val count = messages.size + if (isLoading) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Scaffold(
        topBar = { ChatTopBar(onBack) },
        bottomBar = {
            ChatInputBar(
                value = input,
                onValueChange = { input = it },
                enabled = !isLoading,
                onSend = {
                    viewModel.send(input)
                    input = ""
                }
            )
        }
    ) { padding ->
        if (messages.isEmpty() && !isLoading) {
            ChatWelcome(
                suggestions = viewModel.suggestions,
                onSuggestion = viewModel::send,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    if (message.fromUser) UserBubble(message.text)
                    else AiBubble(message, onRetry = viewModel::retry)
                }
                if (isLoading) item(key = "typing") { TypingBubble() }
            }
        }
    }
}

@Composable
private fun ChatTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
        }
        AiAvatar(size = 40)
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Finansio AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Asisten keuangan pribadimu", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun AiAvatar(size: Int) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(Lavender.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = Lavender, modifier = Modifier.size((size * 0.55).dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatWelcome(suggestions: List<String>, onSuggestion: (String) -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(Lime.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Lime, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Tanya apa saja soal keuanganmu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Finansio AI menjawab berdasarkan transaksi dan anggaran yang kamu catat.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestion(suggestion) },
                    label = { Text(suggestion, modifier = Modifier.padding(vertical = 6.dp)) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Hanya ringkasan angka yang dikirim ke AI, tanpa nama dan catatan transaksi.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text = text,
            color = Background,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                .background(Lime)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun AiBubble(message: ChatMessage, onRetry: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        AiAvatar(size = 32)
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp))
                .background(if (message.isError) Coral.copy(alpha = 0.15f) else SurfaceRaised)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (message.isError) AnnotatedString(message.text) else simpleMarkdown(message.text),
                color = if (message.isError) Coral else MaterialTheme.colorScheme.onSurface
            )
            if (message.isError) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onRetry).padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Coral, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Coba lagi", color = Coral, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.CenterVertically) {
        AiAvatar(size = 32)
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp))
                .background(SurfaceRaised)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val alpha by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(500, delayMillis = index * 150), RepeatMode.Reverse),
                    label = "dot$index"
                )
                Box(Modifier.size(8.dp).alpha(alpha).clip(CircleShape).background(Lavender))
            }
            Spacer(Modifier.width(4.dp))
            Text("Sedang menganalisis…", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun ChatInputBar(value: String, onValueChange: (String) -> Unit, enabled: Boolean, onSend: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Tanya soal keuanganmu…") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (enabled && value.isNotBlank()) onSend() })
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
                modifier = Modifier.size(52.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Lime, contentColor = Background)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Kirim")
            }
        }
    }
}

/** Mengubah **teks** menjadi tebal dan "- " / "* " di awal baris menjadi bullet. */
private fun simpleMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    text.lines().forEachIndexed { lineIndex, rawLine ->
        if (lineIndex > 0) append("\n")
        val line = rawLine.replace(Regex("^\\s*[-*]\\s+"), "• ").replace(Regex("^#+\\s*"), "")
        val parts = line.split("**")
        parts.forEachIndexed { i, part ->
            if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) }
            else append(part)
        }
    }
}
