package com.finansio.app.ui.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finansio.app.ui.theme.SurfaceRaised
import com.finansio.app.ui.theme.TextPrimary
import com.finansio.app.ui.theme.TextSecondary
import com.finansio.app.util.CurrencyFormatter
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow

/** Format ringkas untuk sumbu grafik: 500rb, 1,2jt, 3M. */
fun compactRupiah(value: Long): String {
    val v = value.toDouble()
    fun fmt(x: Double) = if (x % 1.0 == 0.0) "%.0f".format(x) else "%.1f".format(x).replace('.', ',')
    return when {
        v >= 1_000_000_000 -> fmt(v / 1_000_000_000) + "M"
        v >= 1_000_000 -> fmt(v / 1_000_000) + "jt"
        v >= 1_000 -> fmt(v / 1_000) + "rb"
        else -> "%.0f".format(v)
    }
}

/** Batas atas sumbu yang "bulat" (1, 2, 2,5, 5 x 10^n) supaya label gridline rapi. */
private fun niceMax(value: Long): Long {
    if (value <= 0) return 1
    val exp = 10.0.pow(kotlin.math.floor(log10(value.toDouble())))
    val f = value / exp
    val nice = when {
        f <= 1 -> 1.0
        f <= 2 -> 2.0
        f <= 2.5 -> 2.5
        f <= 5 -> 5.0
        else -> 10.0
    }
    return ceil(nice * exp).toLong()
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

/**
 * Grafik kolom berpasangan (Income vs Outcome) per periode.
 * Kolom tumbuh dengan animasi, ujung atas membulat 4dp, dasar rata. Ketuk sebuah periode
 * untuk menyorotnya dan melihat angka pastinya di kartu keterangan.
 */
@Composable
fun GroupedColumnChart(
    data: List<PeriodTotal>,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier,
    initialSelected: Int? = null,
    chartHeight: androidx.compose.ui.unit.Dp = 180.dp
) {
    if (data.isEmpty()) return
    var selected by remember(data) { mutableStateOf(initialSelected) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = TextStyle(color = TextSecondary, fontSize = 10.sp)
    val labelStyle = TextStyle(color = TextSecondary, fontSize = 11.sp)
    val labelSelectedStyle = TextStyle(color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    val gridColor = SurfaceRaised
    val maxValue = niceMax(data.maxOf { maxOf(it.income, it.expense) })

    Column(modifier) {
        // Kartu keterangan periode yang dipilih (pengganti tooltip)
        val info = selected?.let { data.getOrNull(it) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceRaised)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (info == null) {
                Text("Ketuk grafik untuk melihat detail", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            } else {
                Text(info.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(incomeColor))
                        Spacer(Modifier.width(6.dp))
                        Text(CurrencyFormatter.formatRupiah(info.income), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(expenseColor))
                        Spacer(Modifier.width(6.dp))
                        Text(CurrencyFormatter.formatRupiah(info.expense), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val axisWidth = 40.dp.toPx()
                        val slot = (size.width - axisWidth) / data.size
                        val index = ((offset.x - axisWidth) / slot).toInt()
                        if (index in data.indices) selected = if (selected == index) null else index
                    }
                }
        ) {
            val axisWidth = 40.dp.toPx()
            val labelArea = 22.dp.toPx()
            val plotTop = 6.dp.toPx()
            val plotBottom = size.height - labelArea
            val plotHeight = plotBottom - plotTop
            val slot = (size.width - axisWidth) / data.size
            val barWidth = minOf(12.dp.toPx(), slot * 0.28f)
            val pairGap = 3.dp.toPx()
            val radius = 4.dp.toPx()

            // Gridline tipis + label sumbu (0, 1/2, maksimum)
            listOf(0f, 0.5f, 1f).forEach { fraction ->
                val y = plotBottom - plotHeight * fraction
                drawLine(gridColor, Offset(axisWidth, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                val text = compactRupiah((maxValue * fraction).toLong())
                val layout = textMeasurer.measure(text, axisStyle)
                drawText(layout, topLeft = Offset(axisWidth - layout.size.width - 8.dp.toPx(), y - layout.size.height / 2f))
            }

            data.forEachIndexed { i, period ->
                val centerX = axisWidth + slot * i + slot / 2f
                val dimmed = selected != null && selected != i
                val alpha = if (dimmed) 0.3f else 1f

                // Sorotan latar untuk periode yang dipilih
                if (selected == i) {
                    drawRoundRect(
                        color = SurfaceRaised,
                        topLeft = Offset(centerX - slot / 2f + 2.dp.toPx(), plotTop - 4.dp.toPx()),
                        size = Size(slot - 4.dp.toPx(), plotHeight + 4.dp.toPx()),
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )
                }

                listOf(
                    period.income to incomeColor to (centerX - pairGap / 2f - barWidth),
                    period.expense to expenseColor to (centerX + pairGap / 2f)
                ).forEach { (valueColor, left) ->
                    val (value, color) = valueColor
                    val h = (value.toFloat() / maxValue) * plotHeight * progress.value
                    if (h > 0f) {
                        // Ujung atas membulat, dasar rata
                        val r = minOf(radius, h)
                        val path = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = left, top = plotBottom - h, right = left + barWidth, bottom = plotBottom,
                                    topLeftCornerRadius = CornerRadius(r), topRightCornerRadius = CornerRadius(r),
                                    bottomLeftCornerRadius = CornerRadius.Zero, bottomRightCornerRadius = CornerRadius.Zero
                                )
                            )
                        }
                        drawPath(path, color.copy(alpha = alpha))
                    }
                }

                val layout = textMeasurer.measure(period.label, if (selected == i) labelSelectedStyle else labelStyle)
                drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, plotBottom + 6.dp.toPx()))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(incomeColor, "Income")
            LegendDot(expenseColor, "Outcome")
        }
    }
}

data class DonutSlice(val label: String, val value: Long, val color: Color)

/**
 * Grafik donat pengeluaran per kategori dengan celah antar-segmen dan animasi putar.
 * Bagian tengah menampilkan total, atau kategori yang sedang diketuk.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 200.dp
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1L)
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        sweep.snapTo(0f)
        sweep.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(slices) {
                    detectTapGestures { offset ->
                        val center = Offset(this.size.width / 2f, this.size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val outer = minOf(this.size.width, this.size.height) / 2f
                        val distance = hypot(dx, dy)
                        if (distance > outer || distance < outer * 0.45f) { onSelect(null); return@detectTapGestures }
                        // Sudut dihitung dari arah jam 12, searah jarum jam
                        var angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 90f
                        if (angle < 0) angle += 360f
                        var acc = 0f
                        slices.forEachIndexed { i, s ->
                            val a = s.value.toFloat() / total * 360f
                            if (angle in acc..(acc + a)) {
                                onSelect(if (selectedIndex == i) null else i)
                                return@detectTapGestures
                            }
                            acc += a
                        }
                    }
                }
        ) {
            val stroke = 26.dp.toPx()
            val selectedGrow = 6.dp.toPx()
            val diameter = minOf(this.size.width, this.size.height) - stroke - selectedGrow
            val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
            // Celah 2dp antar-segmen, dihitung sebagai sudut pada keliling lingkaran
            val gapDegrees = if (slices.size > 1) Math.toDegrees((2.dp.toPx() / (diameter / 2f)).toDouble()).toFloat() else 0f

            var start = -90f
            slices.forEachIndexed { i, s ->
                val full = s.value.toFloat() / total * 360f
                val angle = (full * sweep.value - gapDegrees).coerceAtLeast(0.5f)
                val isSelected = selectedIndex == i
                val dimmed = selectedIndex != null && !isSelected
                drawArc(
                    color = s.color.copy(alpha = if (dimmed) 0.3f else 1f),
                    startAngle = start + gapDegrees / 2f,
                    sweepAngle = angle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = if (isSelected) stroke + selectedGrow else stroke)
                )
                start += full * sweep.value
            }
        }

        // Teks di tengah donat
        val focus = selectedIndex?.let { slices.getOrNull(it) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = focus?.label ?: "Total",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Text(
                text = CurrencyFormatter.formatRupiah(focus?.value ?: slices.sumOf { it.value }),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            if (focus != null) {
                Text(
                    text = "%.0f%%".format(focus.value * 100f / total),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
