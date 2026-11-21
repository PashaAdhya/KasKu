package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.CategoryBudget
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringTemplate
import com.example.data.model.TransactionType
import com.example.data.model.UiState
import com.example.ui.theme.BrutalButton
import com.example.ui.theme.BrutalCard
import com.example.ui.theme.BrutalChip
import com.example.ui.theme.BrutalIconButton
import com.example.ui.theme.BrutalMonoNumber
import com.example.ui.theme.BrutalProgressBar
import com.example.ui.theme.BrutalTagBadge
import com.example.ui.theme.BrutalTextField
import com.example.ui.theme.LocalBrutalPalette
import com.example.ui.viewmodel.BudgetStatusItem
import com.example.util.formatMonthLabel
import com.example.util.formatRupiah

@Composable
fun BudgetAndRecurringScreen(
    selectedMonthKey: String,
    budgetStatusState: UiState<List<BudgetStatusItem>>,
    recurringState: UiState<List<RecurringTemplate>>,
    isSaving: Boolean,
    onShiftMonth: (Int) -> Unit,
    onJumpCurrentMonth: () -> Unit,
    onSaveBudget: (categoryKey: String, limitAmount: Long, onDone: () -> Unit) -> Unit,
    onDeleteBudget: (budgetId: String) -> Unit,
    onAddRecurring: (
        title: String,
        amount: Long,
        type: TransactionType,
        category: Category,
        paymentMethod: PaymentMethod,
        dueDay: Int,
        onDone: () -> Unit,
    ) -> Unit,
    onLogRecurring: (RecurringTemplate) -> Unit,
    onDeleteRecurring: (String) -> Unit,
) {
    val palette = LocalBrutalPalette.current
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetDialogInitialCategory by remember { mutableStateOf(CategoryBudget.OVERALL_CATEGORY_KEY) }
    var budgetDialogInitialLimit by remember { mutableStateOf("") }
    var showRecurringDialog by remember { mutableStateOf(false) }

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
            // Month switcher + Add Budget button
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

                    BrutalButton(
                        onClick = {
                            budgetDialogInitialCategory = CategoryBudget.OVERALL_CATEGORY_KEY
                            budgetDialogInitialLimit = ""
                            showBudgetDialog = true
                        },
                        containerColor = palette.primaryAccent,
                        contentColor = palette.onPrimaryAccent,
                        shadowOffset = 3.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("add_budget_button"),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Anggaran", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Section 1: Monthly Budgets
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.Savings, contentDescription = null, tint = palette.textPrimary)
                    Text(
                        text = "ANGGARAN BULANAN",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary,
                    )
                }
            }

            when (budgetStatusState) {
                is UiState.Loading -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = palette.primaryAccent)
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        BrutalCard(containerColor = palette.expense) {
                            Text(budgetStatusState.message, color = palette.onExpense)
                        }
                    }
                }

                is UiState.Success -> {
                    val budgets = budgetStatusState.data
                    if (budgets.isEmpty()) {
                        item {
                            BrutalCard(containerColor = palette.surface, shadowOffset = 3.dp) {
                                Text(
                                    text = "Belum ada batas anggaran untuk ${formatMonthLabel(selectedMonthKey)}.",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.textPrimary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Tetapkan batas pengeluaran bulanan atau per kategori agar belanja tetap terkontrol.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary,
                                )
                                Spacer(Modifier.height(10.dp))
                                BrutalButton(
                                    onClick = {
                                        budgetDialogInitialCategory = CategoryBudget.OVERALL_CATEGORY_KEY
                                        budgetDialogInitialLimit = ""
                                        showBudgetDialog = true
                                    },
                                    containerColor = palette.secondaryAccent,
                                    contentColor = palette.onSecondaryAccent,
                                ) {
                                    Text("ATUR BATAS ANGGARAN", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    } else {
                        items(budgets, key = { it.budget.id }) { status ->
                            val isOverall = status.category == null
                            val barColor = when {
                                status.ratio >= 0.95f -> palette.expense
                                status.ratio >= 0.70f -> palette.warning
                                else -> palette.income
                            }
                            val statusText = when {
                                status.remainingAmount < 0L -> "LEBIH ${formatRupiah(-status.remainingAmount)}"
                                status.ratio >= 0.75f -> "WASPADA (${(status.ratio * 100).toInt()}%)"
                                else -> "AMAN (${(status.ratio * 100).toInt()}%)"
                            }

                            BrutalCard(
                                onClick = {
                                    budgetDialogInitialCategory = status.budget.category
                                    budgetDialogInitialLimit = status.budget.limitAmount.toString()
                                    showBudgetDialog = true
                                },
                                containerColor = if (isOverall) palette.surfaceVariant else palette.surface,
                                shadowOffset = 4.dp,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        if (status.category != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(status.category.accentColor(palette))
                                                    .border(1.5.dp, palette.border, RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = status.category.icon,
                                                    contentDescription = null,
                                                    tint = palette.onPrimaryAccent,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = status.category?.displayName ?: "Total Bulanan",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = palette.textPrimary,
                                            )
                                            Text(
                                                text = "${formatRupiah(status.spentAmount)} / ${formatRupiah(status.budget.limitAmount)}",
                                                style = BrutalMonoNumber.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                                                color = palette.textSecondary,
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BrutalTagBadge(
                                            text = statusText,
                                            containerColor = barColor,
                                            contentColor = palette.onPrimaryAccent,
                                        )
                                        IconButton(onClick = { onDeleteBudget(status.budget.id) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Hapus anggaran",
                                                tint = palette.textSecondary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                BrutalProgressBar(
                                    progress = status.ratio,
                                    fillColor = barColor,
                                    height = 16.dp,
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Recurring Bills & Salary Templates
            item {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.Repeat, contentDescription = null, tint = palette.textPrimary)
                        Text(
                            text = "TAGIHAN & GAJI RUTIN",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary,
                        )
                    }
                    BrutalButton(
                        onClick = { showRecurringDialog = true },
                        containerColor = palette.secondaryAccent,
                        contentColor = palette.onSecondaryAccent,
                        shadowOffset = 3.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("add_recurring_button"),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Rutin", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            when (recurringState) {
                is UiState.Loading -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = palette.secondaryAccent)
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        BrutalCard(containerColor = palette.expense) {
                            Text(recurringState.message, color = palette.onExpense)
                        }
                    }
                }

                is UiState.Success -> {
                    val itemsList = recurringState.data
                    if (itemsList.isEmpty()) {
                        item {
                            BrutalCard(containerColor = palette.surface, shadowOffset = 3.dp) {
                                Text(
                                    text = "Simpan tagihan rutin (Kos, Internet, Listrik) atau Gaji bulanan agar tinggal 1 ketuk untuk mencatat setiap bulan.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary,
                                )
                            }
                        }
                    } else {
                        items(itemsList, key = { it.id }) { tpl ->
                            val loggedThisMonth = tpl.lastLoggedMonth == selectedMonthKey
                            val cat = tpl.parsedCategory
                            val isExpense = tpl.parsedType == TransactionType.EXPENSE

                            BrutalCard(
                                containerColor = palette.surface,
                                shadowOffset = 3.dp,
                            ) {
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
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(cat.accentColor(palette))
                                                .border(2.dp, palette.border, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = cat.icon,
                                                contentDescription = null,
                                                tint = palette.onPrimaryAccent,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = tpl.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = palette.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "Setiap Tgl ${tpl.dueDay} • ${formatRupiah(tpl.amount)}",
                                                style = BrutalMonoNumber.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                                                color = if (isExpense) palette.expense else palette.income,
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onDeleteRecurring(tpl.id) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Hapus template rutin",
                                            tint = palette.textSecondary,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                if (loggedThisMonth) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(palette.income.copy(alpha = 0.2f))
                                            .border(1.5.dp, palette.border, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = palette.income,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Sudah dicatat di ${formatMonthLabel(selectedMonthKey)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.textPrimary,
                                        )
                                    }
                                } else {
                                    BrutalButton(
                                        onClick = { onLogRecurring(tpl) },
                                        containerColor = palette.primaryAccent,
                                        contentColor = palette.onPrimaryAccent,
                                        shadowOffset = 3.dp,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("CATAT UNTUK BULAN INI", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBudgetDialog) {
        BudgetEditorDialog(
            initialCategoryKey = budgetDialogInitialCategory,
            initialLimitDigits = budgetDialogInitialLimit,
            isSaving = isSaving,
            onDismiss = { showBudgetDialog = false },
            onSave = { catKey, limit ->
                onSaveBudget(catKey, limit) { showBudgetDialog = false }
            },
        )
    }

    if (showRecurringDialog) {
        RecurringEditorDialog(
            isSaving = isSaving,
            onDismiss = { showRecurringDialog = false },
            onSave = { title, amount, type, category, method, dueDay ->
                onAddRecurring(title, amount, type, category, method, dueDay) {
                    showRecurringDialog = false
                }
            },
        )
    }
}

@Composable
private fun BudgetEditorDialog(
    initialCategoryKey: String,
    initialLimitDigits: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (categoryKey: String, limitAmount: Long) -> Unit,
) {
    val palette = LocalBrutalPalette.current
    var selectedKey by remember { mutableStateOf(initialCategoryKey) }
    var limitDigits by remember { mutableStateOf(initialLimitDigits) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val options = remember {
        listOf(CategoryBudget.OVERALL_CATEGORY_KEY to "Total Bulanan") +
            Category.forType(TransactionType.EXPENSE).map { it.name to it.displayName }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.background,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textPrimary,
        title = {
            Text("ATUR ANGGARAN", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pilih Kategori", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEach { (key, label) ->
                        BrutalChip(
                            label = label,
                            selected = selectedKey == key,
                            onClick = { selectedKey = key },
                        )
                    }
                }
                BrutalTextField(
                    value = limitDigits,
                    onValueChange = {
                        limitDigits = it.filter { ch -> ch.isDigit() }.take(12)
                        errorMsg = null
                    },
                    label = "BATAS MAKSIMAL (RP)",
                    placeholder = "Contoh: 1500000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = BrutalMonoNumber,
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.expense,
                    )
                }
            }
        },
        confirmButton = {
            BrutalButton(
                onClick = {
                    val amount = limitDigits.toLongOrNull() ?: 0L
                    if (amount <= 0L) {
                        errorMsg = "Masukkan nominal batas lebih dari Rp 0."
                    } else {
                        onSave(selectedKey, amount)
                    }
                },
                enabled = !isSaving,
                shadowOffset = 2.dp,
            ) {
                Text("SIMPAN", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = palette.textSecondary)
            }
        },
    )
}

@Composable
private fun RecurringEditorDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Long,
        type: TransactionType,
        category: Category,
        paymentMethod: PaymentMethod,
        dueDay: Int,
    ) -> Unit,
) {
    val palette = LocalBrutalPalette.current
    var title by remember { mutableStateOf("") }
    var amountDigits by remember { mutableStateOf("") }
    var dueDayDigits by remember { mutableStateOf("1") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf(Category.BILLS) }
    var selectedPayment by remember { mutableStateOf(PaymentMethod.TRANSFER) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val cats = Category.forType(selectedType)
    if (selectedCategory.type != selectedType) selectedCategory = cats.first()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.background,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textPrimary,
        title = {
            Text("TAMBAH TAGIHAN / GAJI RUTIN", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrutalChip(
                        label = "Pengeluaran",
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { selectedType = TransactionType.EXPENSE },
                        selectedColor = palette.expense,
                        selectedTextColor = palette.onExpense,
                        modifier = Modifier.weight(1f),
                    )
                    BrutalChip(
                        label = "Pemasukan",
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { selectedType = TransactionType.INCOME },
                        selectedColor = palette.income,
                        selectedTextColor = palette.onIncome,
                        modifier = Modifier.weight(1f),
                    )
                }

                BrutalTextField(
                    value = title,
                    onValueChange = { title = it.take(80); errorMsg = null },
                    label = "NAMA TAGIHAN / RUTIN",
                    placeholder = "Misal: Sewa Kos, WiFi, Gaji",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrutalTextField(
                        value = amountDigits,
                        onValueChange = { amountDigits = it.filter { ch -> ch.isDigit() }.take(12); errorMsg = null },
                        label = "NOMINAL (RP)",
                        placeholder = "500000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(2f),
                    )
                    BrutalTextField(
                        value = dueDayDigits,
                        onValueChange = { dueDayDigits = it.filter { ch -> ch.isDigit() }.take(2); errorMsg = null },
                        label = "TANGGAL",
                        placeholder = "1-31",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    cats.forEach { cat ->
                        BrutalChip(
                            label = cat.displayName,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PaymentMethod.entries.forEach { method ->
                        BrutalChip(
                            label = method.label,
                            selected = selectedPayment == method,
                            onClick = { selectedPayment = method },
                            selectedColor = palette.secondaryAccent,
                            selectedTextColor = palette.onSecondaryAccent,
                        )
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.expense,
                    )
                }
            }
        },
        confirmButton = {
            BrutalButton(
                onClick = {
                    val amt = amountDigits.toLongOrNull() ?: 0L
                    val day = dueDayDigits.toIntOrNull() ?: 0
                    when {
                        title.trim().isEmpty() -> errorMsg = "Nama tidak boleh kosong."
                        amt <= 0L -> errorMsg = "Nominal harus lebih dari Rp 0."
                        day !in 1..31 -> errorMsg = "Tanggal harus antara 1 sampai 31."
                        else -> onSave(title.trim(), amt, selectedType, selectedCategory, selectedPayment, day)
                    }
                },
                enabled = !isSaving,
                shadowOffset = 2.dp,
            ) {
                Text("SIMPAN", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = palette.textSecondary)
            }
        },
    )
}
