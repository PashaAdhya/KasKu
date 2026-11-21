package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.FinanceRepository
import com.example.data.model.Category
import com.example.data.model.CategoryBudget
import com.example.data.model.PaymentMethod
import com.example.data.model.QuickTemplate
import com.example.data.model.RecurringTemplate
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.model.UiState
import com.example.util.currentMonthKey
import com.example.util.daysInMonth
import com.example.util.monthKeyFromEpochDay
import com.example.util.shiftMonthKey
import com.example.util.todayEpochDay
import com.example.util.ymdFromEpochDay
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DailyDayGroup(
    val epochDay: Long,
    val totalExpense: Long,
    val totalIncome: Long,
    val transactions: List<TransactionRecord>,
)

data class CategoryBreakdownItem(
    val category: Category,
    val amount: Long,
    val percentage: Float,
    val count: Int,
)

data class PaymentBreakdownItem(
    val method: PaymentMethod,
    val amount: Long,
    val count: Int,
)

data class DailyBarPoint(
    val dayOfMonth: Int,
    val expense: Long,
)

data class MonthlyReportSummary(
    val monthKey: String,
    val totalExpense: Long,
    val totalIncome: Long,
    val netBalance: Long,
    val previousMonthExpense: Long,
    val expenseDeltaPercent: Int?,
    val dailyAverageExpense: Long,
    val peakDayOfMonth: Int?,
    val peakDayExpense: Long,
    val dailyBars: List<DailyBarPoint>,
    val categoryBreakdown: List<CategoryBreakdownItem>,
    val paymentBreakdown: List<PaymentBreakdownItem>,
    val topExpenses: List<TransactionRecord>,
    val monthTransactions: List<TransactionRecord>,
)

data class BudgetStatusItem(
    val budget: CategoryBudget,
    val category: Category?, // null means OVERALL monthly cap
    val spentAmount: Long,
    val remainingAmount: Long,
    val ratio: Float,
)

class KasKuViewModel(
    private val repository: FinanceRepository,
    private val currentUserId: String,
) : ViewModel() {

    private val _selectedMonthKey = MutableStateFlow(currentMonthKey())
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow<TransactionType?>(null)
    val typeFilter: StateFlow<TransactionType?> = _typeFilter.asStateFlow()

    private val _paymentFilter = MutableStateFlow<PaymentMethod?>(null)
    val paymentFilter: StateFlow<PaymentMethod?> = _paymentFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow<Category?>(null)
    val categoryFilter: StateFlow<Category?> = _categoryFilter.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private fun <T> Flow<List<T>>.asUiStateFlow(label: String): StateFlow<UiState<List<T>>> =
        this.map<List<T>, UiState<List<T>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Gagal memuat $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val transactionsState: StateFlow<UiState<List<TransactionRecord>>> =
        repository.observeTransactions(currentUserId).asUiStateFlow("transaksi")

    val budgetsState: StateFlow<UiState<List<CategoryBudget>>> =
        repository.observeBudgets(currentUserId).asUiStateFlow("anggaran")

    val recurringState: StateFlow<UiState<List<RecurringTemplate>>> =
        repository.observeRecurring(currentUserId).asUiStateFlow("tagihan rutin")

    val dailyGroupedState: StateFlow<UiState<List<DailyDayGroup>>> = combine(
        transactionsState,
        _selectedMonthKey,
        _searchQuery,
        _typeFilter,
        _paymentFilter,
    ) { txState, monthKey, query, typeF, payF ->
        Triple(txState, monthKey, Triple(query, typeF, payF))
    }.combine(_categoryFilter) { (txState, monthKey, filterTriple), catF ->
        val (query, typeF, payF) = filterTriple
        when (txState) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> txState
            is UiState.Success -> {
                val q = query.trim().lowercase()
                val filtered = txState.data
                    .filter { tx ->
                        tx.monthKey == monthKey &&
                            (typeF == null || tx.parsedType == typeF) &&
                            (payF == null || tx.parsedPaymentMethod == payF) &&
                            (catF == null || tx.parsedCategory == catF) &&
                            (q.isEmpty() ||
                                tx.title.lowercase().contains(q) ||
                                tx.note.lowercase().contains(q) ||
                                tx.parsedCategory.displayName.lowercase().contains(q))
                    }
                    .sortedWith(
                        compareByDescending<TransactionRecord> { it.dateEpochDay }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )

                val grouped = filtered
                    .groupBy { it.dateEpochDay }
                    .entries
                    .sortedByDescending { it.key }
                    .map { (day, list) ->
                        DailyDayGroup(
                            epochDay = day,
                            totalExpense = list.filter { it.parsedType == TransactionType.EXPENSE }.sumOf { it.amount },
                            totalIncome = list.filter { it.parsedType == TransactionType.INCOME }.sumOf { it.amount },
                            transactions = list,
                        )
                    }
                UiState.Success(grouped)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
    )

    val monthlyReportState: StateFlow<UiState<MonthlyReportSummary>> = combine(
        transactionsState,
        _selectedMonthKey,
    ) { txState, monthKey ->
        when (txState) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> txState
            is UiState.Success -> {
                val allTx = txState.data
                val monthTx = allTx.filter { it.monthKey == monthKey }
                val prevMonthKey = shiftMonthKey(monthKey, -1)
                val prevMonthExpense = allTx
                    .filter { it.monthKey == prevMonthKey && it.parsedType == TransactionType.EXPENSE }
                    .sumOf { it.amount }

                val expenses = monthTx.filter { it.parsedType == TransactionType.EXPENSE }
                val incomes = monthTx.filter { it.parsedType == TransactionType.INCOME }
                val totalExpense = expenses.sumOf { it.amount }
                val totalIncome = incomes.sumOf { it.amount }

                val deltaPct = if (prevMonthExpense > 0L) {
                    (((totalExpense - prevMonthExpense) * 100.0) / prevMonthExpense).toInt()
                } else null

                val totalDays = daysInMonth(monthKey)
                val todayMonth = currentMonthKey()
                val elapsedDays = if (monthKey == todayMonth) {
                    ymdFromEpochDay(todayEpochDay()).third.coerceAtLeast(1)
                } else {
                    totalDays.coerceAtLeast(1)
                }

                val expenseByDom = mutableMapOf<Int, Long>()
                expenses.forEach { tx ->
                    val dom = ymdFromEpochDay(tx.dateEpochDay).third
                    expenseByDom[dom] = (expenseByDom[dom] ?: 0L) + tx.amount
                }

                val dailyBars = (1..totalDays).map { dom ->
                    DailyBarPoint(dayOfMonth = dom, expense = expenseByDom[dom] ?: 0L)
                }
                val peakEntry = expenseByDom.maxByOrNull { it.value }

                val categoryBreakdown = expenses
                    .groupBy { it.parsedCategory }
                    .map { (cat, list) ->
                        val sum = list.sumOf { it.amount }
                        CategoryBreakdownItem(
                            category = cat,
                            amount = sum,
                            percentage = if (totalExpense > 0L) sum.toFloat() / totalExpense.toFloat() else 0f,
                            count = list.size,
                        )
                    }
                    .sortedByDescending { it.amount }

                val paymentBreakdown = expenses
                    .groupBy { it.parsedPaymentMethod }
                    .map { (method, list) ->
                        PaymentBreakdownItem(
                            method = method,
                            amount = list.sumOf { it.amount },
                            count = list.size,
                        )
                    }
                    .sortedByDescending { it.amount }

                val topExpenses = expenses.sortedByDescending { it.amount }.take(5)

                UiState.Success(
                    MonthlyReportSummary(
                        monthKey = monthKey,
                        totalExpense = totalExpense,
                        totalIncome = totalIncome,
                        netBalance = totalIncome - totalExpense,
                        previousMonthExpense = prevMonthExpense,
                        expenseDeltaPercent = deltaPct,
                        dailyAverageExpense = totalExpense / elapsedDays,
                        peakDayOfMonth = peakEntry?.key,
                        peakDayExpense = peakEntry?.value ?: 0L,
                        dailyBars = dailyBars,
                        categoryBreakdown = categoryBreakdown,
                        paymentBreakdown = paymentBreakdown,
                        topExpenses = topExpenses,
                        monthTransactions = monthTx,
                    )
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
    )

    val budgetStatusState: StateFlow<UiState<List<BudgetStatusItem>>> = combine(
        budgetsState,
        transactionsState,
        _selectedMonthKey,
    ) { bState, txState, monthKey ->
        when {
            bState is UiState.Error -> bState
            txState is UiState.Error -> txState
            bState is UiState.Loading || txState is UiState.Loading -> UiState.Loading
            bState is UiState.Success && txState is UiState.Success -> {
                val monthExpenses = txState.data.filter {
                    it.monthKey == monthKey && it.parsedType == TransactionType.EXPENSE
                }
                val totalMonthExpense = monthExpenses.sumOf { it.amount }

                val statuses = bState.data
                    .filter { it.monthKey == monthKey }
                    .map { budget ->
                        val isOverall = budget.category == CategoryBudget.OVERALL_CATEGORY_KEY
                        val cat = if (isOverall) null else Category.fromRaw(budget.category)
                        val spent = if (isOverall) {
                            totalMonthExpense
                        } else {
                            monthExpenses.filter { it.category == budget.category }.sumOf { it.amount }
                        }
                        val ratio = if (budget.limitAmount > 0L) spent.toFloat() / budget.limitAmount.toFloat() else 0f
                        BudgetStatusItem(
                            budget = budget,
                            category = cat,
                            spentAmount = spent,
                            remainingAmount = budget.limitAmount - spent,
                            ratio = ratio,
                        )
                    }
                    .sortedWith(
                        compareBy<BudgetStatusItem> { it.category != null }
                            .thenByDescending { it.ratio }
                    )
                UiState.Success(statuses)
            }
            else -> UiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
    )

    val todayExpenseTotal: StateFlow<Long> = transactionsState.map { state ->
        if (state is UiState.Success) {
            val today = todayEpochDay()
            state.data
                .filter { it.dateEpochDay == today && it.parsedType == TransactionType.EXPENSE }
                .sumOf { it.amount }
        } else 0L
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = 0L,
    )

    fun shiftMonth(delta: Int) {
        _selectedMonthKey.value = shiftMonthKey(_selectedMonthKey.value, delta)
    }

    fun jumpToCurrentMonth() {
        _selectedMonthKey.value = currentMonthKey()
    }

    fun updateSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun setTypeFilter(type: TransactionType?) {
        _typeFilter.value = if (_typeFilter.value == type) null else type
    }

    fun setPaymentFilter(method: PaymentMethod?) {
        _paymentFilter.value = if (_paymentFilter.value == method) null else method
    }

    fun setCategoryFilter(category: Category?) {
        _categoryFilter.value = if (_categoryFilter.value == category) null else category
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _typeFilter.value = null
        _paymentFilter.value = null
        _categoryFilter.value = null
    }

    fun saveTransaction(
        existingId: String?,
        title: String,
        amount: Long,
        type: TransactionType,
        category: Category,
        paymentMethod: PaymentMethod,
        note: String,
        dateEpochDay: Long,
        onComplete: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val monthKey = monthKeyFromEpochDay(dateEpochDay)
            val result = if (existingId.isNullOrBlank()) {
                repository.addTransaction(
                    userId = currentUserId,
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    paymentMethod = paymentMethod,
                    note = note,
                    dateEpochDay = dateEpochDay,
                    monthKey = monthKey,
                )
            } else {
                repository.updateTransaction(
                    userId = currentUserId,
                    txId = existingId,
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    paymentMethod = paymentMethod,
                    note = note,
                    dateEpochDay = dateEpochDay,
                    monthKey = monthKey,
                )
            }
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _selectedMonthKey.value = monthKey
                    _messages.tryEmit(if (existingId.isNullOrBlank()) "Transaksi dicatat!" else "Transaksi diperbarui!")
                    onComplete(true)
                },
                onFailure = { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Gagal menyimpan transaksi.")
                    onComplete(false)
                },
            )
        }
    }

    fun quickLogTemplate(template: QuickTemplate) {
        val today = todayEpochDay()
        saveTransaction(
            existingId = null,
            title = template.title,
            amount = template.amount,
            type = TransactionType.EXPENSE,
            category = template.category,
            paymentMethod = template.paymentMethod,
            note = "Catat cepat",
            dateEpochDay = today,
        )
    }

    fun deleteTransaction(txId: String) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(currentUserId, txId)
            result.fold(
                onSuccess = { _messages.tryEmit("Transaksi dihapus.") },
                onFailure = { err -> _messages.tryEmit(err.localizedMessage ?: "Gagal menghapus transaksi.") },
            )
        }
    }

    fun saveBudget(categoryKey: String, limitAmount: Long, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.upsertBudget(
                userId = currentUserId,
                monthKey = _selectedMonthKey.value,
                categoryKey = categoryKey,
                limitAmount = limitAmount,
            )
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _messages.tryEmit("Anggaran tersimpan!")
                    onComplete(true)
                },
                onFailure = { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Gagal menyimpan anggaran.")
                    onComplete(false)
                },
            )
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            val result = repository.deleteBudget(currentUserId, budgetId)
            result.fold(
                onSuccess = { _messages.tryEmit("Anggaran dihapus.") },
                onFailure = { err -> _messages.tryEmit(err.localizedMessage ?: "Gagal menghapus anggaran.") },
            )
        }
    }

    fun addRecurringTemplate(
        title: String,
        amount: Long,
        type: TransactionType,
        category: Category,
        paymentMethod: PaymentMethod,
        dueDay: Int,
        onComplete: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.addRecurringTemplate(
                userId = currentUserId,
                title = title,
                amount = amount,
                type = type,
                category = category,
                paymentMethod = paymentMethod,
                dueDay = dueDay,
            )
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _messages.tryEmit("Template rutin ditambahkan!")
                    onComplete(true)
                },
                onFailure = { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Gagal menambahkan template rutin.")
                    onComplete(false)
                },
            )
        }
    }

    fun logRecurringForCurrentMonth(template: RecurringTemplate) {
        viewModelScope.launch {
            val today = todayEpochDay()
            val monthKey = _selectedMonthKey.value
            val result = repository.markRecurringLogged(currentUserId, template, monthKey, today)
            result.fold(
                onSuccess = { _messages.tryEmit("${template.title} dicatat ke ledger!") },
                onFailure = { err -> _messages.tryEmit(err.localizedMessage ?: "Gagal mencatat tagihan rutin.") },
            )
        }
    }

    fun deleteRecurringTemplate(itemId: String) {
        viewModelScope.launch {
            val result = repository.deleteRecurringTemplate(currentUserId, itemId)
            result.fold(
                onSuccess = { _messages.tryEmit("Template rutin dihapus.") },
                onFailure = { err -> _messages.tryEmit(err.localizedMessage ?: "Gagal menghapus template rutin.") },
            )
        }
    }

    private companion object {
        const val TAG = "KasKuVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
