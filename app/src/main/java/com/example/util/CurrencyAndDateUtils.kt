package com.example.util

import com.example.data.model.Category
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

private const val MILLIS_PER_DAY = 86_400_000L
private val INDONESIAN_LOCALE = Locale.forLanguageTag("id-ID")
private val UTC_TZ = TimeZone.getTimeZone("UTC")

private val MONTH_NAMES_FULL = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
)

private val MONTH_NAMES_SHORT = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
)

private val DAY_NAMES_SHORT = listOf(
    "Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"
)

fun formatRupiah(amount: Long, prefixSign: String = ""): String {
    val formatter = NumberFormat.getIntegerInstance(INDONESIAN_LOCALE)
    val absText = formatter.format(abs(amount))
    val sign = when {
        amount < 0L -> "-"
        prefixSign.isNotEmpty() -> prefixSign
        else -> ""
    }
    return "${sign}Rp $absText"
}

fun formatCompactRupiah(amount: Long): String {
    val absVal = abs(amount)
    val sign = if (amount < 0) "-" else ""
    return when {
        absVal >= 1_000_000_000L -> "${sign}Rp ${String.format(INDONESIAN_LOCALE, "%.1f", absVal / 1_000_000_000.0)}M"
        absVal >= 1_000_000L -> "${sign}Rp ${String.format(INDONESIAN_LOCALE, "%.1f", absVal / 1_000_000.0)}jt"
        absVal >= 1_000L -> "${sign}Rp ${absVal / 1_000L}rb"
        else -> "${sign}Rp $absVal"
    }
}

fun epochDayFromYmd(year: Int, month1Based: Int, day: Int): Long {
    val cal = Calendar.getInstance(UTC_TZ).apply {
        clear()
        set(year, month1Based - 1, day, 0, 0, 0)
    }
    return cal.timeInMillis / MILLIS_PER_DAY
}

fun ymdFromEpochDay(epochDay: Long): Triple<Int, Int, Int> {
    val cal = Calendar.getInstance(UTC_TZ).apply {
        clear()
        timeInMillis = epochDay * MILLIS_PER_DAY
    }
    return Triple(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
    )
}

fun todayEpochDay(): Long {
    val local = Calendar.getInstance()
    return epochDayFromYmd(
        local.get(Calendar.YEAR),
        local.get(Calendar.MONTH) + 1,
        local.get(Calendar.DAY_OF_MONTH),
    )
}

fun epochDayFromUtcPickerMillis(utcMillis: Long): Long {
    val cal = Calendar.getInstance(UTC_TZ).apply {
        timeInMillis = utcMillis
    }
    return epochDayFromYmd(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
    )
}

fun utcPickerMillisFromEpochDay(epochDay: Long): Long = epochDay * MILLIS_PER_DAY

fun monthKeyFromEpochDay(epochDay: Long): String {
    val (y, m, _) = ymdFromEpochDay(epochDay)
    return String.format(Locale.US, "%04d-%02d", y, m)
}

fun currentMonthKey(): String = monthKeyFromEpochDay(todayEpochDay())

fun parseMonthKey(monthKey: String): Pair<Int, Int> {
    val parts = monthKey.split("-")
    val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 1
    return y to m
}

fun shiftMonthKey(monthKey: String, deltaMonths: Int): String {
    val (y, m) = parseMonthKey(monthKey)
    val cal = Calendar.getInstance(UTC_TZ).apply {
        clear()
        set(y, m - 1, 1, 0, 0, 0)
        add(Calendar.MONTH, deltaMonths)
    }
    return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}

fun daysInMonth(monthKey: String): Int {
    val (y, m) = parseMonthKey(monthKey)
    val cal = Calendar.getInstance(UTC_TZ).apply {
        clear()
        set(y, m - 1, 1, 0, 0, 0)
    }
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
}

fun formatMonthLabel(monthKey: String): String {
    val (y, m) = parseMonthKey(monthKey)
    return "${MONTH_NAMES_FULL[(m - 1).coerceIn(0, 11)]} $y"
}

fun formatShortMonthLabel(monthKey: String): String {
    val (y, m) = parseMonthKey(monthKey)
    return "${MONTH_NAMES_SHORT[(m - 1).coerceIn(0, 11)]} $y"
}

fun formatDateHeader(epochDay: Long): String {
    val today = todayEpochDay()
    return when (epochDay) {
        today -> "Hari Ini"
        today - 1 -> "Kemarin"
        else -> {
            val cal = Calendar.getInstance(UTC_TZ).apply {
                clear()
                timeInMillis = epochDay * MILLIS_PER_DAY
            }
            val dow = DAY_NAMES_SHORT[(cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val m = MONTH_NAMES_SHORT[cal.get(Calendar.MONTH).coerceIn(0, 11)]
            val y = cal.get(Calendar.YEAR)
            "$dow, $d $m $y"
        }
    }
}

fun formatShortDate(epochDay: Long): String {
    val (y, m, d) = ymdFromEpochDay(epochDay)
    return "$d ${MONTH_NAMES_SHORT[(m - 1).coerceIn(0, 11)]} $y"
}

fun buildMonthlyReportShareText(
    monthKey: String,
    totalExpense: Long,
    totalIncome: Long,
    categoryTotals: List<Pair<Category, Long>>,
    transactions: List<TransactionRecord>,
): String {
    val sb = StringBuilder()
    sb.appendLine("=== LAPORAN KEUANGAN KASKU (${formatMonthLabel(monthKey).uppercase()}) ===")
    sb.appendLine("Total Pemasukan : ${formatRupiah(totalIncome)}")
    sb.appendLine("Total Pengeluaran : ${formatRupiah(totalExpense)}")
    sb.appendLine("Selisih Bersih : ${formatRupiah(totalIncome - totalExpense)}")
    sb.appendLine()
    if (categoryTotals.isNotEmpty()) {
        sb.appendLine("Rincian Kategori Pengeluaran:")
        categoryTotals.forEach { (cat, sum) ->
            val pct = if (totalExpense > 0) (sum * 100 / totalExpense) else 0
            sb.appendLine("- ${cat.displayName}: ${formatRupiah(sum)} ($pct%)")
        }
        sb.appendLine()
    }
    if (transactions.isNotEmpty()) {
        sb.appendLine("Daftar Transaksi (CSV):")
        sb.appendLine("Tanggal,Tipe,Kategori,Metode,Judul,Nominal,Catatan")
        transactions.sortedByDescending { it.dateEpochDay }.forEach { tx ->
            val dateStr = formatShortDate(tx.dateEpochDay)
            val typeStr = TransactionType.fromRaw(tx.type).label
            val catStr = Category.fromRaw(tx.category).displayName
            val payStr = PaymentMethod.fromRaw(tx.paymentMethod).label
            val cleanTitle = tx.title.replace(",", " ")
            val cleanNote = tx.note.replace(",", " ")
            sb.appendLine("$dateStr,$typeStr,$catStr,$payStr,$cleanTitle,${tx.amount},$cleanNote")
        }
    }
    return sb.toString()
}
