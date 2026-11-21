package com.example.data.model

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.BrutalPalette
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class TransactionType(val label: String) {
    EXPENSE("Pengeluaran"),
    INCOME("Pemasukan");

    companion object {
        fun fromRaw(raw: String?): TransactionType =
            entries.firstOrNull { it.name == raw } ?: EXPENSE
    }
}

enum class PaymentMethod(val label: String, val icon: ImageVector) {
    CASH("Tunai", Icons.Rounded.AccountBalanceWallet),
    QRIS("QRIS", Icons.Rounded.QrCode2),
    TRANSFER("Transfer", Icons.Rounded.AccountBalance),
    CARD("Kartu", Icons.Rounded.CreditCard);

    companion object {
        fun fromRaw(raw: String?): PaymentMethod =
            entries.firstOrNull { it.name == raw } ?: QRIS
    }
}

enum class Category(
    val displayName: String,
    val type: TransactionType,
    val icon: ImageVector,
) {
    // Expense categories
    FOOD("Makan & Minum", TransactionType.EXPENSE, Icons.Rounded.Fastfood),
    TRANSPORT("Transportasi", TransactionType.EXPENSE, Icons.Rounded.DirectionsCar),
    SHOPPING("Belanja", TransactionType.EXPENSE, Icons.Rounded.ShoppingBag),
    BILLS("Tagihan & Kos", TransactionType.EXPENSE, Icons.Rounded.ReceiptLong),
    ENTERTAINMENT("Hiburan", TransactionType.EXPENSE, Icons.Rounded.Movie),
    HEALTH("Kesehatan", TransactionType.EXPENSE, Icons.Rounded.LocalHospital),
    EDUCATION("Pendidikan", TransactionType.EXPENSE, Icons.Rounded.School),
    OTHER_EXPENSE("Lainnya", TransactionType.EXPENSE, Icons.Rounded.MoreHoriz),

    // Income categories
    SALARY("Gaji", TransactionType.INCOME, Icons.Rounded.AttachMoney),
    BONUS("Bonus", TransactionType.INCOME, Icons.Rounded.AutoAwesome),
    BUSINESS("Usaha", TransactionType.INCOME, Icons.Rounded.Storefront),
    INVESTMENT("Investasi", TransactionType.INCOME, Icons.Rounded.TrendingUp),
    OTHER_INCOME("Lainnya", TransactionType.INCOME, Icons.Rounded.Bolt);

    fun accentColor(palette: BrutalPalette): Color = when (this) {
        FOOD -> palette.tangerine
        TRANSPORT -> palette.secondaryAccent
        SHOPPING -> palette.violet
        BILLS -> palette.expense
        ENTERTAINMENT -> palette.warning
        HEALTH -> palette.income
        EDUCATION -> palette.sky
        OTHER_EXPENSE -> palette.surfaceVariant
        SALARY -> palette.income
        BONUS -> palette.primaryAccent
        BUSINESS -> palette.secondaryAccent
        INVESTMENT -> palette.violet
        OTHER_INCOME -> palette.warning
    }

    companion object {
        fun forType(type: TransactionType): List<Category> =
            entries.filter { it.type == type }

        fun fromRaw(raw: String?): Category =
            entries.firstOrNull { it.name == raw } ?: OTHER_EXPENSE
    }
}

@Keep
@IgnoreExtraProperties
data class TransactionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Long = 0L,
    val type: String = TransactionType.EXPENSE.name,
    val category: String = Category.FOOD.name,
    val paymentMethod: String = PaymentMethod.QRIS.name,
    val note: String = "",
    val dateEpochDay: Long = 0L,
    val monthKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val parsedType: TransactionType get() = TransactionType.fromRaw(type)
    val parsedCategory: Category get() = Category.fromRaw(category)
    val parsedPaymentMethod: PaymentMethod get() = PaymentMethod.fromRaw(paymentMethod)
}

@Keep
@IgnoreExtraProperties
data class CategoryBudget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val monthKey: String = "",
    val category: String = OVERALL_CATEGORY_KEY,
    val limitAmount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    companion object {
        const val OVERALL_CATEGORY_KEY = "OVERALL"
    }
}

@Keep
@IgnoreExtraProperties
data class RecurringTemplate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Long = 0L,
    val type: String = TransactionType.EXPENSE.name,
    val category: String = Category.BILLS.name,
    val paymentMethod: String = PaymentMethod.TRANSFER.name,
    val dueDay: Long = 1L,
    val lastLoggedMonth: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val parsedType: TransactionType get() = TransactionType.fromRaw(type)
    val parsedCategory: Category get() = Category.fromRaw(category)
    val parsedPaymentMethod: PaymentMethod get() = PaymentMethod.fromRaw(paymentMethod)
}

data class QuickTemplate(
    val id: String,
    val title: String,
    val amount: Long,
    val category: Category,
    val paymentMethod: PaymentMethod,
)

val DefaultQuickTemplates = listOf(
    QuickTemplate("qt_coffee", "Kopi / Jajan", 18_000L, Category.FOOD, PaymentMethod.QRIS),
    QuickTemplate("qt_lunch", "Makan Siang", 28_000L, Category.FOOD, PaymentMethod.QRIS),
    QuickTemplate("qt_transport", "Transport / Parkir", 15_000L, Category.TRANSPORT, PaymentMethod.CASH),
    QuickTemplate("qt_groceries", "Belanja Harian", 50_000L, Category.SHOPPING, PaymentMethod.QRIS),
)
