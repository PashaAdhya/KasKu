package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Category
import com.example.data.model.DefaultQuickTemplates
import com.example.data.model.PaymentMethod
import com.example.data.model.QuickTemplate
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.model.UiState
import com.example.ui.theme.BrutalButton
import com.example.ui.theme.BrutalCard
import com.example.ui.theme.BrutalChip
import com.example.ui.theme.BrutalIconButton
import com.example.ui.theme.BrutalMonoNumber
import com.example.ui.theme.BrutalTagBadge
import com.example.ui.theme.LocalBrutalPalette
import com.example.ui.viewmodel.DailyDayGroup
import com.example.ui.viewmodel.MonthlyReportSummary
import com.example.util.formatCompactRupiah
import com.example.util.formatDateHeader
import com.example.util.formatMonthLabel
import com.example.util.formatRupiah

@Composable
fun DailyLedgerScreen(
    selectedMonthKey: String,
    todayExpenseTotal: Long,
    dailyState: UiState<List<DailyDayGroup>>,
    reportState: UiState<MonthlyReportSummary>,
    searchQuery: String,
    typeFilter: TransactionType?,
    paymentFilter: PaymentMethod?,
    categoryFilter: Category?,
    onShiftMonth: (Int) -> Unit,
    onJumpCurrentMonth: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleTypeFilter: (TransactionType) -> Unit,
    onTogglePaymentFilter: (PaymentMethod) -> Unit,
    onClearFilters: () -> Unit,
    onQuickLog: (QuickTemplate) -> Unit,
    onOpenNewTransaction: () -> Unit,
    onEditTransaction: (TransactionRecord) -> Unit,
) {
    val palette = LocalBrutalPalette.current
    val summary = (reportState as? UiState.Success)?.data
    val hasActiveFilters = searchQuery.isNotBlank() || typeFilter != null || paymentFilter != null || categoryFilter != null

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
            // 1. Header bar: Month switcher + Cloud Sync pill
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
                        BrutalIconButton(
                            onClick = { onShiftMonth(-1) },
                            modifier = Modifier.testTag("prev_month_btn"),
                        ) {
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
                        BrutalIconButton(
                            onClick = { onShiftMonth(1) },
                            modifier = Modifier.testTag("next_month_btn"),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Bulan berikutnya")
                        }
                    }

                    BrutalTagBadge(
                        text = "CLOUD SYNC",
                        containerColor = palette.income,
                        contentColor = palette.onIncome,
                    )
                }
            }

            // 2. Hero Summary Card
            item {
                BrutalCard(
                    containerColor = palette.primaryAccent,
                    shadowOffset = 5.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "SALDO BERSIH BULAN INI",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.onPrimaryAccent,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Today,
                                contentDescription = null,
                                tint = palette.onPrimaryAccent,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "Hari ini: ${formatCompactRupiah(todayExpenseTotal)}",
                                style = BrutalMonoNumber.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                                color = palette.onPrimaryAccent,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = formatRupiah(summary?.netBalance ?: 0L),
                        style = BrutalMonoNumber.copy(
                            fontSize = MaterialTheme.typography.displaySmall.fontSize,
                            lineHeight = MaterialTheme.typography.displaySmall.lineHeight,
                        ),
                        color = palette.onPrimaryAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.surface)
                                .border(2.dp, palette.border, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                        ) {
                            Column {
                                Text(
                                    text = "PEMASUKAN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = formatRupiah(summary?.totalIncome ?: 0L),
                                    style = BrutalMonoNumber.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                                    color = palette.income,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.surface)
                                .border(2.dp, palette.border, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                        ) {
                            Column {
                                Text(
                                    text = "PENGELUARAN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = formatRupiah(summary?.totalExpense ?: 0L),
                                    style = BrutalMonoNumber.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                                    color = palette.expense,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            // 3. Catat Cepat (1-tap quick templates)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = palette.textPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "CATAT CEPAT (1 KETUK)",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textPrimary,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DefaultQuickTemplates.forEach { tpl ->
                            BrutalCard(
                                onClick = { onQuickLog(tpl) },
                                containerColor = palette.surface,
                                shadowOffset = 3.dp,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("quick_tpl_${tpl.id}"),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(tpl.category.accentColor(palette))
                                            .border(1.5.dp, palette.border, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = tpl.category.icon,
                                            contentDescription = null,
                                            tint = palette.onPrimaryAccent,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = tpl.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.textPrimary,
                                        )
                                        Text(
                                            text = formatRupiah(tpl.amount),
                                            style = BrutalMonoNumber.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                                            color = palette.expense,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Search & Filter bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Cari transaksi, kategori, atau catatan…", color = palette.textSecondary) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.textPrimary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Bersihkan pencarian", tint = palette.textPrimary)
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.surface,
                            unfocusedContainerColor = palette.surface,
                            focusedBorderColor = palette.border,
                            unfocusedBorderColor = palette.border,
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_tx_input"),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BrutalChip(
                            label = "Pengeluaran",
                            selected = typeFilter == TransactionType.EXPENSE,
                            onClick = { onToggleTypeFilter(TransactionType.EXPENSE) },
                            selectedColor = palette.expense,
                            selectedTextColor = palette.onExpense,
                        )
                        BrutalChip(
                            label = "Pemasukan",
                            selected = typeFilter == TransactionType.INCOME,
                            onClick = { onToggleTypeFilter(TransactionType.INCOME) },
                            selectedColor = palette.income,
                            selectedTextColor = palette.onIncome,
                        )
                        PaymentMethod.entries.forEach { method ->
                            BrutalChip(
                                label = method.label,
                                selected = paymentFilter == method,
                                onClick = { onTogglePaymentFilter(method) },
                                selectedColor = palette.secondaryAccent,
                                selectedTextColor = palette.onSecondaryAccent,
                            )
                        }
                        if (hasActiveFilters) {
                            BrutalChip(
                                label = "Reset Filter",
                                selected = true,
                                onClick = onClearFilters,
                                selectedColor = palette.warning,
                                selectedTextColor = palette.onWarning,
                            )
                        }
                    }
                }
            }

            // 5. Daily Transactions List
            when (dailyState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
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
                                text = "Gagal memuat transaksi",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.onExpense,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = dailyState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.onExpense,
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    val groups = dailyState.data
                    if (groups.isEmpty()) {
                        item {
                            BrutalCard(
                                containerColor = palette.surface,
                                shadowOffset = 4.dp,
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_empty_ledger_1795098378957),
                                        contentDescription = "Belum ada catatan keuangan",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(2.dp, palette.border, RoundedCornerShape(8.dp)),
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = if (hasActiveFilters) "Tidak ada transaksi yang cocok" else "Buku Kas Masih Kosong",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = palette.textPrimary,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (hasActiveFilters) {
                                            "Coba ubah kata kunci atau reset filter di atas."
                                        } else {
                                            "Mulai catat pengeluaran atau pemasukan pertamamu di bulan ${formatMonthLabel(selectedMonthKey)}!"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary,
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    BrutalButton(
                                        onClick = if (hasActiveFilters) onClearFilters else onOpenNewTransaction,
                                        containerColor = palette.secondaryAccent,
                                        contentColor = palette.onSecondaryAccent,
                                    ) {
                                        if (!hasActiveFilters) {
                                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = if (hasActiveFilters) "RESET FILTER" else "CATAT SEKARANG",
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        groups.forEach { group ->
                            item(key = "header_${group.epochDay}") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = formatDateHeader(group.epochDay).uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.textPrimary,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (group.totalIncome > 0L) {
                                            Text(
                                                text = "+${formatCompactRupiah(group.totalIncome)}",
                                                style = BrutalMonoNumber.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                                                color = palette.income,
                                            )
                                        }
                                        if (group.totalExpense > 0L) {
                                            Text(
                                                text = "-${formatCompactRupiah(group.totalExpense)}",
                                                style = BrutalMonoNumber.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                                                color = palette.expense,
                                            )
                                        }
                                    }
                                }
                            }

                            items(
                                items = group.transactions,
                                key = { it.id },
                            ) { tx ->
                                TransactionRowCard(
                                    tx = tx,
                                    onClick = { onEditTransaction(tx) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRowCard(
    tx: TransactionRecord,
    onClick: () -> Unit,
) {
    val palette = LocalBrutalPalette.current
    val cat = tx.parsedCategory
    val method = tx.parsedPaymentMethod
    val isExpense = tx.parsedType == TransactionType.EXPENSE
    val amountColor = if (isExpense) palette.expense else palette.income
    val sign = if (isExpense) "-" else "+"

    BrutalCard(
        onClick = onClick,
        containerColor = palette.surface,
        shadowOffset = 3.dp,
        contentPadding = PaddingValues(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_card_${tx.id}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(cat.accentColor(palette))
                    .border(2.dp, palette.border, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = cat.icon,
                    contentDescription = cat.displayName,
                    tint = palette.onPrimaryAccent,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BrutalTagBadge(
                        text = cat.displayName,
                        containerColor = palette.surfaceVariant,
                        contentColor = palette.textPrimary,
                    )
                    BrutalTagBadge(
                        text = method.label,
                        containerColor = palette.secondaryAccent,
                        contentColor = palette.onSecondaryAccent,
                    )
                }
                if (tx.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tx.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign${formatRupiah(tx.amount)}",
                    style = BrutalMonoNumber.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                    color = amountColor,
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.Rounded.CloudDone,
                    contentDescription = "Tersinkron",
                    tint = palette.textSecondary,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(14.dp),
                )
            }
        }
    }
}
