package com.finansio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finansio.app.data.local.entity.TransactionEntity
import com.finansio.app.data.local.entity.TransactionType
import com.finansio.app.util.CurrencyFormatter

/** Satu baris transaksi: ikon, kategori, catatan, dan nominal berwarna. */
@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    categoryColorHex: String?,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val sign = if (isIncome) "+" else "-"
    
    val iconColor = try {
        Color(android.graphics.Color.parseColor(categoryColorHex ?: "#8E8F94"))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Receipt,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.categoryName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!transaction.note.isNullOrBlank()) {
                Text(
                    text = transaction.note,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Text(
            text = "$sign${CurrencyFormatter.formatRupiah(transaction.amount)}",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = amountColor
        )
    }
}
