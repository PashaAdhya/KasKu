package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.UiState
import com.example.ui.theme.BrutalButton
import com.example.ui.theme.BrutalCard
import com.example.ui.theme.BrutalIconButton
import com.example.ui.theme.BrutalMonoNumber
import com.example.ui.theme.BrutalProgressBar
import com.example.ui.theme.BrutalTagBadge
import com.example.ui.theme.LocalBrutalPalette
import com.example.ui.viewmodel.DailyBarPoint
import com.example.ui.viewmodel.MonthlyReportSummary
import com.example.util.buildMonthlyReportShareText
import com.example.util.formatCompactRupiah
import com.example.util.formatMonthLabel
import com.example.util.formatRupiah
import com.example.util.formatShortDate
import kotlin.math.abs

@Composable
fun MonthlyReportScreen(
    selectedMonthKey: String,
    reportState: UiState<MonthlyReportSummary>,
    onShiftMonth: (Int) -> Unit,
    onJumpCurrentMonth: () -> Unit,
) {
    val palette = LocalBrutalPalette.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Month Header + Share Report
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BrutalIconButton(onClick = { onShiftMonth(-1) }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya")
                        }
                        Surface(
                            onClick = onJumpCurrentMonth,
                            shape = RoundedCornerShape(8.dp),
                            color = palette.surface,
                            border = BorderStroke(2.dp, palette.border),
                        ) {
                            Text(
                                text = formatMonthLabel(selectedMonthKey),
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                        BrutalIconButton(onClick = { onShiftMonth(1) }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Bulan berikutnya")
                        }
                    }

                    val summaryData = (reportState as? UiState.Success)?.data
                    if (summaryData != null) {
                        BrutalButton(
                            onClick = {
                                val text = buildMonthlyReportShareText(
                                    monthKey = summaryData.monthKey,
                                    totalExpense = summaryData.totalExpense,
                                    totalIncome = summaryData.totalIncome,
                                    categoryTotals = summaryData.categoryBreakdown.map { it.category to it.amount },
                                    transactions = summaryData.monthTransactions,
                                )
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan KasKu ${formatMonthLabel(summaryData.monthKey)}")
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Bagikan Laporan Bulanan"))
                            },
                            containerColor = palette.secondaryAccent,
                            contentColor = palette.onSecondaryAccent,
                            shadowOffset = 3.dp,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("share_report_button"),
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ekspor", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            when (reportState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = palette.primaryAccent)
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        BrutalCard(containerColor = palette.expense) {
                            Text(
                                text = "Gagal menyusun laporan: ${reportState.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.onExpense,
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    val report = reportState.data

                    // 2. Month-over-Month Comparison Banner
                    item {
                        val delta = report.expenseDeltaPercent
                        val isLowerOrSame = (delta ?: 0) <= 0
                        val bannerBg = when {
                            delta == null -> palette.surfaceVariant
                            isLowerOrSame -> palette.income
                            else -> palette.warning
                        }
                        val bannerFg = if (delta == null) palette.textPrimary else palette.onPrimaryAccent

                        BrutalCard(
                            containerColor = bannerBg,
                            shadowOffset = 4.dp,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "RINGKASAN PENGELUARAN BULANAN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = bannerFg.copy(alpha = 0.85f),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = formatRupiah(report.totalExpense),
                                        style = BrutalMonoNumber.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize),
                                        color = bannerFg,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    val compText = when {
                                        delta == null -> "Belum ada pembanding dari bulan sebelumnya."
                                        delta < 0 -> "Hemat ${abs(delta)}% dibanding bulan lalu (${formatCompactRupiah(report.previousMonthExpense)})"
                                        delta > 0 -> "Naik $delta% dibanding bulan lalu (${formatCompactRupiah(report.previousMonthExpense)})"
                                        else -> "Sama persis dengan bulan sebelumnya."
                                    }
                                    Text(
                                        text = compText,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = bannerFg,
                                    )
                                }
                                if (delta != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(palette.surface)
                                            .border(2.dp, palette.border, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (isLowerOrSame) Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp,
                                            contentDescription = null,
                                            tint = if (isLowerOrSame) palette.income else palette.expense,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. 2x2 Bento KPI Grid
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                KpiBox(
                                    modifier = Modifier.weight(1f),
                                    label = "TOTAL MASUK",
                                    value = formatCompactRupiah(report.totalIncome),
                                    sub = formatRupiah(report.totalIncome),
                                    accentColor = palette.income,
                                )
                                KpiBox(
                                    modifier = Modifier.weight(1f),
                                    label = "SELISIH BERSIH",
                                    value = formatCompactRupiah(report.netBalance),
                                    sub = if (report.netBalance >= 0) "Surplus" else "Defisit",
                                    accentColor = if (report.netBalance >= 0) palette.primaryAccent else palette.expense,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                KpiBox(
                                    modifier = Modifier.weight(1f),
                                    label = "RATA-RATA / HARI",
                                    value = formatCompactRupiah(report.dailyAverageExpense),
                                    sub = "Belanja harian",
                                    accentColor = palette.secondaryAccent,
                                )
                                KpiBox(
                                    modifier = Modifier.weight(1f),
                                    label = "HARI TERBOROS",
                                    value = if (report.peakDayOfMonth != null) "Tgl ${report.peakDayOfMonth}" else "-",
                                    sub = if (report.peakDayExpense > 0L) formatCompactRupiah(report.peakDayExpense) else "Belum ada",
                                    accentColor = palette.warning,
                                )
                            }
                        }
                    }

                    // 4. Custom Canvas Daily Spending Bar Chart
                    item {
                        BrutalCard(
                            containerColor = palette.surface,
                            shadowOffset = 4.dp,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "GRAFIK HARIAN",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.textPrimary,
                                )
                                BrutalTagBadge(
                                    text = "${report.dailyBars.size} HARI",
                                    containerColor = palette.primaryAccent,
                                    contentColor = palette.onPrimaryAccent,
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            if (report.totalExpense == 0L) {
                                Text(
                                    text = "Belum ada pengeluaran tercatat di bulan ini untuk digambar ke grafik.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary,
                                )
                            } else {
                                DailyExpenseBrutalBarChart(
                                    bars = report.dailyBars,
                                    peakDay = report.peakDayOfMonth,
                                    dailyAverage = report.dailyAverageExpense,
                                    barColor = palette.secondaryAccent,
                                    peakColor = palette.expense,
                                    borderColor = palette.border,
                                    avgLineColor = palette.warning,
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("Tgl 1", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                    Text(
                                        "Garis putus = rata-rata (${formatCompactRupiah(report.dailyAverageExpense)})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary,
                                    )
                                    Text("Tgl ${report.dailyBars.size}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                }
                            }
                        }
                    }

                    // 5. Category Breakdown
                    item {
                        BrutalCard(
                            containerColor = palette.surface,
                            shadowOffset = 4.dp,
                        ) {
                            Text(
                                text = "RINCIAN PER KATEGORI",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.textPrimary,
                            )
                            Spacer(Modifier.height(10.dp))
                            if (report.categoryBreakdown.isEmpty()) {
                                Text(
                                    text = "Belum ada data kategori pengeluaran bulan ini.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary,
                                )
                            } else {
                                // Proportional stacked neo-brutalist strip
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(2.dp, palette.border, RoundedCornerShape(6.dp)),
                                ) {
                                    report.categoryBreakdown.forEach { item ->
                                        val w = item.percentage.coerceAtLeast(0.03f)
                                        Box(
                                            modifier = Modifier
                                                .weight(w)
                                                .fillMaxHeight()
                                                .background(item.category.accentColor(palette)),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(14.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    report.categoryBreakdown.forEach { item ->
                                        val pct = (item.percentage * 100).toInt()
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(item.category.accentColor(palette))
                                                            .border(1.5.dp, palette.border, RoundedCornerShape(6.dp)),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Icon(
                                                            imageVector = item.category.icon,
                                                            contentDescription = null,
                                                            tint = palette.onPrimaryAccent,
                                                            modifier = Modifier.size(16.dp),
                                                        )
                                                    }
                                                    Column {
                                                        Text(
                                                            text = item.category.displayName,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = palette.textPrimary,
                                                        )
                                                        Text(
                                                            text = "${item.count} transaksi",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = palette.textSecondary,
                                                        )
                                                    }
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = formatRupiah(item.amount),
                                                        style = BrutalMonoNumber.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                                                        color = palette.textPrimary,
                                                    )
                                                    Text(
                                                        text = "$pct%",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.textSecondary,
                                                    )
                                                }
                                            }
                                            BrutalProgressBar(
                                                progress = item.percentage,
                                                fillColor = item.category.accentColor(palette),
                                                height = 10.dp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Payment Method Split & Top 5 Expenses
                    if (report.paymentBreakdown.isNotEmpty()) {
                        item {
                            BrutalCard(
                                containerColor = palette.surface,
                                shadowOffset = 4.dp,
                            ) {
                                Text(
                                    text = "METODE PEMBAYARAN",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.textPrimary,
                                )
                                Spacer(Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    report.paymentBreakdown.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(palette.surfaceVariant)
                                                .border(1.5.dp, palette.border, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Icon(
                                                    imageVector = item.method.icon,
                                                    contentDescription = null,
                                                    tint = palette.textPrimary,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                                Text(
                                                    text = "${item.method.label} (${item.count}x)",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = palette.textPrimary,
                                                )
                                            }
                                            Text(
                                                text = formatRupiah(item.amount),
                                                style = BrutalMonoNumber.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                                                color = palette.textPrimary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (report.topExpenses.isNotEmpty()) {
                        item {
                            BrutalCard(
                                containerColor = palette.surface,
                                shadowOffset = 4.dp,
                            ) {
                                Text(
                                    text = "PENGELUARAN TERBESAR",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.textPrimary,
                                )
                                Spacer(Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    report.topExpenses.forEachIndexed { idx, tx ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                BrutalTagBadge(
                                                    text = "#${idx + 1}",
                                                    containerColor = if (idx == 0) palette.expense else palette.surfaceVariant,
                                                    contentColor = if (idx == 0) palette.onExpense else palette.textPrimary,
                                                )
                                                Column {
                                                    Text(
                                                        text = tx.title,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = palette.textPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        text = "${tx.parsedCategory.displayName} • ${formatShortDate(tx.dateEpochDay)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = palette.textSecondary,
                                                    )
                                                }
                                            }
                                            Text(
                                                text = formatRupiah(tx.amount),
                                                style = BrutalMonoNumber.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                                                color = palette.expense,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiBox(
    modifier: Modifier,
    label: String,
    value: String,
    sub: String,
    accentColor: Color,
) {
    val palette = LocalBrutalPalette.current
    BrutalCard(
        modifier = modifier,
        containerColor = palette.surface,
        shadowOffset = 3.dp,
        contentPadding = PaddingValues(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accentColor)
                .border(1.dp, palette.border, RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = BrutalMonoNumber.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DailyExpenseBrutalBarChart(
    bars: List<DailyBarPoint>,
    peakDay: Int?,
    dailyAverage: Long,
    barColor: Color,
    peakColor: Color,
    borderColor: Color,
    avgLineColor: Color,
) {
    val maxVal = (bars.maxOfOrNull { it.expense } ?: 0L).coerceAtLeast(1L)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
    ) {
        val count = bars.size.coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val totalGap = gap * (count - 1)
        val barWidth = ((size.width - totalGap) / count).coerceAtLeast(2f)
        val chartHeight = size.height - 6.dp.toPx()

        // Baseline
        drawLine(
            color = borderColor,
            start = Offset(0f, chartHeight),
            end = Offset(size.width, chartHeight),
            strokeWidth = 2.dp.toPx(),
        )

        // Bars
        bars.forEachIndexed { index, point ->
            val ratio = point.expense.toFloat() / maxVal.toFloat()
            val barH = if (point.expense > 0L) (chartHeight * ratio).coerceAtLeast(6.dp.toPx()) else 0f
            if (barH > 0f) {
                val left = index * (barWidth + gap)
                val top = chartHeight - barH
                val fill = if (point.dayOfMonth == peakDay) peakColor else barColor
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                )
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx()),
                )
            }
        }

        // Daily average dashed guideline
        if (dailyAverage > 0L) {
            val avgY = chartHeight - (chartHeight * (dailyAverage.toFloat() / maxVal.toFloat()))
            drawLine(
                color = avgLineColor,
                start = Offset(0f, avgY),
                end = Offset(size.width, avgY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
            )
        }
    }
}
