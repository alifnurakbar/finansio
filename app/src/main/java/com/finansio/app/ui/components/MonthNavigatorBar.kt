package com.finansio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finansio.app.ui.theme.Lime
import com.finansio.app.ui.theme.SurfaceRaised
import com.finansio.app.ui.theme.TextPrimary
import com.finansio.app.ui.theme.TextSecondary

/** Navigasi bulan sebelumnya/berikutnya dengan label bulan. */
@Composable
fun MonthNavigatorBar(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous month button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceRaised)
                .clickable(onClick = onPrev),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = "Bulan Sebelumnya",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Month + year label
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        // Next month button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceRaised)
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Bulan Berikutnya",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
