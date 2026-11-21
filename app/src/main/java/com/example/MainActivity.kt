package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.auth.authStateFlow
import com.example.data.auth.signOutGoogle
import com.example.data.firestore.FinanceRepository
import com.example.data.model.TransactionRecord
import com.example.data.preferences.ThemePreferencesRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BudgetAndRecurringScreen
import com.example.ui.screens.DailyLedgerScreen
import com.example.ui.screens.MonthlyReportScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.TransactionEditorSheet
import com.example.ui.theme.BrutalButton
import com.example.ui.theme.BrutalIconButton
import com.example.ui.theme.BrutalTagBadge
import com.example.ui.theme.KasKuTheme
import com.example.ui.theme.LocalBrutalPalette
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.KasKuViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private const val ROUTE_DAILY = "daily"
private const val ROUTE_REPORT = "report"
private const val ROUTE_BUDGET = "budget"
private const val ROUTE_PROFILE = "profile"

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val KasKuTabs = listOf(
    NavTab(ROUTE_DAILY, R.string.nav_daily, Icons.Rounded.ReceiptLong, Icons.Outlined.ReceiptLong, "tab_daily"),
    NavTab(ROUTE_REPORT, R.string.nav_report, Icons.Rounded.BarChart, Icons.Outlined.BarChart, "tab_report"),
    NavTab(ROUTE_BUDGET, R.string.nav_budget, Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "tab_budget"),
    NavTab(ROUTE_PROFILE, R.string.nav_settings, Icons.Rounded.Person, Icons.Outlined.Person, "tab_profile"),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = applicationContext
            val themeRepo = remember(context) { ThemePreferencesRepository(context) }
            val themeMode by themeRepo.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val scope = rememberCoroutineScope()

            KasKuTheme(themeMode = themeMode) {
                AppNavigation(
                    themeMode = themeMode,
                    onThemeModeChange = { next ->
                        scope.launch { themeRepo.setThemeMode(next) }
                    },
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    auth: FirebaseAuth = Firebase.auth,
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(
            themeMode = themeMode,
            onToggleTheme = {
                val next = when (themeMode) {
                    ThemeMode.DARK -> ThemeMode.LIGHT
                    else -> ThemeMode.DARK
                }
                onThemeModeChange(next)
            },
            onAuthSuccess = {},
        )
    } else {
        key(user.uid) {
            AuthenticatedKasKuApp(
                currentUser = user,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
            )
        }
    }
}

@Composable
private fun AuthenticatedKasKuApp(
    currentUser: FirebaseUser,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val currentUserId = currentUser.uid
    val palette = LocalBrutalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: KasKuViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                KasKuViewModel(FinanceRepository(db), currentUserId)
            }
        },
    )

    val selectedMonthKey by viewModel.selectedMonthKey.collectAsStateWithLifecycle()
    val todayExpenseTotal by viewModel.todayExpenseTotal.collectAsStateWithLifecycle()
    val dailyState by viewModel.dailyGroupedState.collectAsStateWithLifecycle()
    val reportState by viewModel.monthlyReportState.collectAsStateWithLifecycle()
    val budgetStatusState by viewModel.budgetStatusState.collectAsStateWithLifecycle()
    val recurringState by viewModel.recurringState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val paymentFilter by viewModel.paymentFilter.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var showEditorSheet by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<TransactionRecord?>(null) }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DAILY

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        val isWide = maxWidth >= 600.dp

        Scaffold(
            containerColor = palette.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        modifier = Modifier
                            .padding(12.dp)
                            .border(2.dp, palette.border, RoundedCornerShape(10.dp)),
                        containerColor = palette.surface,
                        contentColor = palette.textPrimary,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = data.visuals.message,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            },
            topBar = {
                // Neo-Brutalist Top App Bar
                Surface(
                    color = palette.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                BrutalTagBadge(
                                    text = "KASKU",
                                    containerColor = palette.primaryAccent,
                                    contentColor = palette.onPrimaryAccent,
                                )
                                Text(
                                    text = when (currentRoute) {
                                        ROUTE_DAILY -> "Buku Kas Harian"
                                        ROUTE_REPORT -> "Laporan Bulanan"
                                        ROUTE_BUDGET -> "Anggaran & Rutin"
                                        else -> "Profil & Tema"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.textPrimary,
                                )
                            }

                            BrutalIconButton(
                                onClick = {
                                    val next = if (palette.isDark) ThemeMode.LIGHT else ThemeMode.DARK
                                    onThemeModeChange(next)
                                },
                                modifier = Modifier.testTag("topbar_theme_toggle"),
                            ) {
                                Icon(
                                    imageVector = if (palette.isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = "Ubah mode terang atau gelap",
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(palette.border),
                        )
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute != ROUTE_PROFILE) {
                    BrutalButton(
                        onClick = {
                            editingRecord = null
                            showEditorSheet = true
                        },
                        containerColor = palette.primaryAccent,
                        contentColor = palette.onPrimaryAccent,
                        shadowOffset = 4.dp,
                        modifier = Modifier.testTag("fab_add_transaction"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Catat transaksi baru",
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.add_transaction).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            },
            bottomBar = {
                if (!isWide) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.surface)
                            .navigationBarsPadding(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(palette.border),
                        )
                        NavigationBar(
                            containerColor = palette.surface,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets(0, 0, 0, 0),
                        ) {
                            KasKuTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    modifier = Modifier.testTag(tab.testTag),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes),
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            ),
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = palette.onPrimaryAccent,
                                        selectedTextColor = palette.textPrimary,
                                        indicatorColor = palette.primaryAccent,
                                        unselectedIconColor = palette.textSecondary,
                                        unselectedTextColor = palette.textSecondary,
                                    ),
                                )
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (isWide) {
                    Row(modifier = Modifier.fillMaxHeight()) {
                        NavigationRail(
                            containerColor = palette.surface,
                            modifier = Modifier
                                .fillMaxHeight()
                                .navigationBarsPadding(),
                        ) {
                            Spacer(Modifier.height(8.dp))
                            KasKuTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    modifier = Modifier.testTag(tab.testTag),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes),
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = palette.onPrimaryAccent,
                                        selectedTextColor = palette.textPrimary,
                                        indicatorColor = palette.primaryAccent,
                                        unselectedIconColor = palette.textSecondary,
                                        unselectedTextColor = palette.textSecondary,
                                    ),
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(palette.border),
                        )
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_DAILY,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    composable(ROUTE_DAILY) {
                        DailyLedgerScreen(
                            selectedMonthKey = selectedMonthKey,
                            todayExpenseTotal = todayExpenseTotal,
                            dailyState = dailyState,
                            reportState = reportState,
                            searchQuery = searchQuery,
                            typeFilter = typeFilter,
                            paymentFilter = paymentFilter,
                            categoryFilter = categoryFilter,
                            onShiftMonth = viewModel::shiftMonth,
                            onJumpCurrentMonth = viewModel::jumpToCurrentMonth,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onToggleTypeFilter = viewModel::setTypeFilter,
                            onTogglePaymentFilter = viewModel::setPaymentFilter,
                            onClearFilters = viewModel::clearAllFilters,
                            onQuickLog = viewModel::quickLogTemplate,
                            onOpenNewTransaction = {
                                editingRecord = null
                                showEditorSheet = true
                            },
                            onEditTransaction = { tx ->
                                editingRecord = tx
                                showEditorSheet = true
                            },
                        )
                    }

                    composable(ROUTE_REPORT) {
                        MonthlyReportScreen(
                            selectedMonthKey = selectedMonthKey,
                            reportState = reportState,
                            onShiftMonth = viewModel::shiftMonth,
                            onJumpCurrentMonth = viewModel::jumpToCurrentMonth,
                        )
                    }

                    composable(ROUTE_BUDGET) {
                        BudgetAndRecurringScreen(
                            selectedMonthKey = selectedMonthKey,
                            budgetStatusState = budgetStatusState,
                            recurringState = recurringState,
                            isSaving = isSaving,
                            onShiftMonth = viewModel::shiftMonth,
                            onJumpCurrentMonth = viewModel::jumpToCurrentMonth,
                            onSaveBudget = { catKey, limit, onDone ->
                                viewModel.saveBudget(catKey, limit) { ok -> if (ok) onDone() }
                            },
                            onDeleteBudget = viewModel::deleteBudget,
                            onAddRecurring = { title, amount, type, cat, method, dueDay, onDone ->
                                viewModel.addRecurringTemplate(title, amount, type, cat, method, dueDay) { ok ->
                                    if (ok) onDone()
                                }
                            },
                            onLogRecurring = viewModel::logRecurringForCurrentMonth,
                            onDeleteRecurring = viewModel::deleteRecurringTemplate,
                        )
                    }

                    composable(ROUTE_PROFILE) {
                        ProfileSettingsScreen(
                            currentUser = currentUser,
                            themeMode = themeMode,
                            reportState = reportState,
                            onThemeModeChange = onThemeModeChange,
                            onSignOut = {
                                signOutGoogle(credentialManager, scope)
                            },
                        )
                    }
                }
            }
        }
    }

    if (showEditorSheet) {
        TransactionEditorSheet(
            initialRecord = editingRecord,
            isSaving = isSaving,
            onDismiss = {
                showEditorSheet = false
                editingRecord = null
            },
            onSave = { existingId, title, amount, type, category, method, note, epochDay ->
                viewModel.saveTransaction(
                    existingId = existingId,
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    paymentMethod = method,
                    note = note,
                    dateEpochDay = epochDay,
                ) { success ->
                    if (success) {
                        showEditorSheet = false
                        editingRecord = null
                    }
                }
            },
            onDelete = { txId ->
                viewModel.deleteTransaction(txId)
                showEditorSheet = false
                editingRecord = null
            },
        )
    }
}
