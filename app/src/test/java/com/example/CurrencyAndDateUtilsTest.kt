package com.example

import com.example.data.model.Category
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.util.buildMonthlyReportShareText
import com.example.util.daysInMonth
import com.example.util.epochDayFromYmd
import com.example.util.formatCompactRupiah
import com.example.util.formatMonthLabel
import com.example.util.formatRupiah
import com.example.util.monthKeyFromEpochDay
import com.example.util.shiftMonthKey
import com.example.util.ymdFromEpochDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyAndDateUtilsTest {

    @Test
    fun epochDayAndMonthKeyRoundTrip_isConsistent() {
        val epochDay = epochDayFromYmd(2026, 11, 19)
        assertEquals(Triple(2026, 11, 19), ymdFromEpochDay(epochDay))
        assertEquals("2026-11", monthKeyFromEpochDay(epochDay))
        assertEquals("2026-10", shiftMonthKey("2026-11", -1))
        assertEquals("2027-01", shiftMonthKey("2026-12", 1))
        assertEquals(30, daysInMonth("2026-11"))
        assertEquals(28, daysInMonth("2026-02"))
        assertEquals("November 2026", formatMonthLabel("2026-11"))
    }

    @Test
    fun rupiahFormatting_formatsLargeAndCompactNumbers() {
        val formatted = formatRupiah(1_250_000L)
        assertTrue(formatted.startsWith("Rp "))
        assertTrue(formatted.contains("1.250.000") || formatted.contains("1,250,000"))

        assertEquals("Rp 25rb", formatCompactRupiah(25_000L))
        assertTrue(formatCompactRupiah(2_500_000L).contains("jt"))
    }

    @Test
    fun monthlyReportShareText_containsTotalsAndCsvRows() {
        val tx = TransactionRecord(
            id = "tx1",
            userId = "u1",
            title = "Makan Siang",
            amount = 35_000L,
            type = TransactionType.EXPENSE.name,
            category = Category.FOOD.name,
            paymentMethod = PaymentMethod.QRIS.name,
            note = "Warung Padang",
            dateEpochDay = epochDayFromYmd(2026, 11, 19),
            monthKey = "2026-11",
        )
        val text = buildMonthlyReportShareText(
            monthKey = "2026-11",
            totalExpense = 35_000L,
            totalIncome = 500_000L,
            categoryTotals = listOf(Category.FOOD to 35_000L),
            transactions = listOf(tx),
        )
        assertTrue(text.contains("NOVEMBER 2026"))
        assertTrue(text.contains("Makan & Minum"))
        assertTrue(text.contains("Makan Siang,35000,Warung Padang"))
    }
}
