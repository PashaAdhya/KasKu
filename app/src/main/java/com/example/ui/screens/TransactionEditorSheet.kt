package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.theme.BrutalButton
import com.example.ui.theme.BrutalChip
import com.example.ui.theme.BrutalMonoNumber
import com.example.ui.theme.BrutalTextField
import com.example.ui.theme.LocalBrutalPalette
import com.example.util.epochDayFromUtcPickerMillis
import com.example.util.formatDateHeader
import com.example.util.formatRupiah
import com.example.util.todayEpochDay
import com.example.util.utcPickerMillisFromEpochDay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditorSheet(
    initialRecord: TransactionRecord?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        title: String,
        amount: Long,
        type: TransactionType,
        category: Category,
        paymentMethod: PaymentMethod,
        note: String,
        dateEpochDay: Long,
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null,
) {
    val palette = LocalBrutalPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember(initialRecord) {
        mutableStateOf(initialRecord?.parsedType ?: TransactionType.EXPENSE)
    }
    var amountDigits by remember(initialRecord) {
        mutableStateOf(initialRecord?.amount?.takeIf { it > 0 }?.toString() ?: "")
    }
    var title by remember(initialRecord) {
        mutableStateOf(initialRecord?.title ?: "")
    }
    var selectedCategory by remember(initialRecord) {
        mutableStateOf(initialRecord?.parsedCategory ?: Category.FOOD)
    }
    var selectedPayment by remember(initialRecord) {
        mutableStateOf(initialRecord?.parsedPaymentMethod ?: PaymentMethod.QRIS)
    }
    var note by remember(initialRecord) {
        mutableStateOf(initialRecord?.note ?: "")
    }
    var dateEpochDay by remember(initialRecord) {
        mutableLongStateOf(initialRecord?.dateEpochDay ?: todayEpochDay())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val availableCategories = remember(selectedType) { Category.forType(selectedType) }
    if (selectedCategory.type != selectedType) {
        selectedCategory = availableCategories.first()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.background,
        contentColor = palette.textPrimary,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (initialRecord == null) "CATAT TRANSAKSI" else "UBAH TRANSAKSI",
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.textPrimary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (initialRecord != null && onDelete != null) {
                        IconButton(
                            onClick = { onDelete(initialRecord.id) },
                            modifier = Modifier.testTag("delete_tx_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Hapus transaksi",
                                tint = palette.expense,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup",
                            tint = palette.textPrimary,
                        )
                    }
                }
            }

            // Segmented Type Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BrutalChip(
                    label = "Pengeluaran",
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { selectedType = TransactionType.EXPENSE },
                    selectedColor = palette.expense,
                    selectedTextColor = palette.onExpense,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("type_expense_chip"),
                )
                BrutalChip(
                    label = "Pemasukan",
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { selectedType = TransactionType.INCOME },
                    selectedColor = palette.income,
                    selectedTextColor = palette.onIncome,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("type_income_chip"),
                )
            }

            // Nominal input + quick nominal adders
            BrutalTextField(
                value = amountDigits,
                onValueChange = { raw ->
                    val cleaned = raw.filter { it.isDigit() }.take(12)
                    amountDigits = cleaned
                    validationError = null
                },
                label = "NOMINAL (RUPIAH)",
                placeholder = "Contoh: 25000",
                prefix = {
                    Text(
                        text = "Rp ",
                        style = BrutalMonoNumber,
                        color = palette.textSecondary,
                    )
                },
                textStyle = BrutalMonoNumber.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.testTag("tx_amount_input"),
            )

            val currentAmount = amountDigits.toLongOrNull() ?: 0L
            if (currentAmount > 0L) {
                Text(
                    text = "Terbaca: ${formatRupiah(currentAmount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(10_000L to "+10rb", 25_000L to "+25rb", 50_000L to "+50rb", 100_000L to "+100rb").forEach { (inc, label) ->
                    BrutalChip(
                        label = label,
                        selected = false,
                        onClick = {
                            val next = (amountDigits.toLongOrNull() ?: 0L) + inc
                            amountDigits = next.coerceAtMost(1_000_000_000_000L).toString()
                            validationError = null
                        },
                    )
                }
                if (amountDigits.isNotEmpty()) {
                    BrutalChip(
                        label = "Reset",
                        selected = false,
                        onClick = { amountDigits = "" },
                    )
                }
            }

            // Title input
            BrutalTextField(
                value = title,
                onValueChange = {
                    title = it.take(80)
                    validationError = null
                },
                label = "JUDUL TRANSAKSI",
                placeholder = if (selectedType == TransactionType.EXPENSE) "Misal: Nasi Goreng, Bensin" else "Misal: Gaji, Proyek",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.testTag("tx_title_input"),
            )

            // Category picker
            Text(
                text = "KATEGORI",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textPrimary,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                availableCategories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val accent = cat.accentColor(palette)
                    Surface(
                        onClick = { selectedCategory = cat },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) accent else palette.surface,
                        border = BorderStroke(2.dp, palette.border),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = null,
                                tint = if (isSelected) palette.onPrimaryAccent else palette.textPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = cat.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) palette.onPrimaryAccent else palette.textPrimary,
                            )
                        }
                    }
                }
            }

            // Payment method selector
            Text(
                text = "METODE PEMBAYARAN",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textPrimary,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PaymentMethod.entries.forEach { method ->
                    BrutalChip(
                        label = method.label,
                        selected = selectedPayment == method,
                        onClick = { selectedPayment = method },
                        selectedColor = palette.secondaryAccent,
                        selectedTextColor = palette.onSecondaryAccent,
                        leadingIcon = {
                            Icon(
                                imageVector = method.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }

            // Date selector
            Text(
                text = "TANGGAL",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textPrimary,
            )
            val today = todayEpochDay()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrutalChip(
                    label = "Hari Ini",
                    selected = dateEpochDay == today,
                    onClick = { dateEpochDay = today },
                )
                BrutalChip(
                    label = "Kemarin",
                    selected = dateEpochDay == today - 1,
                    onClick = { dateEpochDay = today - 1 },
                )
                BrutalChip(
                    label = if (dateEpochDay != today && dateEpochDay != today - 1) formatDateHeader(dateEpochDay) else "Pilih Tanggal",
                    selected = dateEpochDay != today && dateEpochDay != today - 1,
                    onClick = { showDatePicker = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }

            // Note input
            BrutalTextField(
                value = note,
                onValueChange = { note = it.take(250) },
                label = "CATATAN (OPSIONAL)",
                placeholder = "Tambahkan keterangan singkat…",
                singleLine = false,
                maxLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.testTag("tx_note_input"),
            )

            if (validationError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.expense)
                        .border(2.dp, palette.border, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                ) {
                    Text(
                        text = validationError ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.onExpense,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            BrutalButton(
                onClick = {
                    val parsedAmount = amountDigits.toLongOrNull() ?: 0L
                    val cleanTitle = title.trim()
                    when {
                        parsedAmount <= 0L -> validationError = "Masukkan nominal lebih dari Rp 0."
                        cleanTitle.isEmpty() -> validationError = "Judul transaksi tidak boleh kosong."
                        else -> onSave(
                            initialRecord?.id,
                            cleanTitle,
                            parsedAmount,
                            selectedType,
                            selectedCategory,
                            selectedPayment,
                            note.trim(),
                            dateEpochDay,
                        )
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_tx_button"),
                containerColor = palette.primaryAccent,
                contentColor = palette.onPrimaryAccent,
            ) {
                Text(
                    text = if (isSaving) "MENYIMPAN…" else if (initialRecord == null) "SIMPAN TRANSAKSI" else "PERBARUI TRANSAKSI",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = utcPickerMillisFromEpochDay(dateEpochDay),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dateEpochDay = epochDayFromUtcPickerMillis(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
