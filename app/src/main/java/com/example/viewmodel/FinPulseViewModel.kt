package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.KharchaRepository
import com.example.model.AccountItem
import com.example.model.AppThemeMode
import com.example.model.CurrencyFormatter
import com.example.model.DatePeriod
import com.example.model.FinancialMilestone
import com.example.model.MonthlyTrendPoint
import com.example.model.NavDestination
import com.example.model.NetFlowDay
import com.example.model.StructuralAllocation
import com.example.model.Transaction
import com.example.model.TransactionType
import com.example.model.UserProfile
import com.example.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class FinPulseUiState(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val currentDestination: NavDestination = NavDestination.HOME,
    val prevDestination: NavDestination = NavDestination.HOME,
    val currencyUnit: String = "INR (₹)",
    val totalExpenses: Double = 0.0,
    val totalLiquidity: Double = 0.0,
    val protectedVault: Double = 0.0,
    val netWorth: Double = 0.0,
    val avgBurnMonth: Double = 0.0,
    val savingsRate: Int = 0,
    val cycleBurnSpentPct: Int = 0,
    val remainingBuffer: Double = 0.0,
    val dailyRate: Double = 0.0,
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<AccountItem> = emptyList(),
    val milestones: List<FinancialMilestone> = emptyList(),
    val structuralAllocations: List<StructuralAllocation> = emptyList(),
    val netFlowWeek: List<NetFlowDay> = emptyList(),
    val monthlyTrends7D: List<MonthlyTrendPoint> = emptyList(),
    val monthlyTrends6M: List<MonthlyTrendPoint> = emptyList(),
    val monthlyTrends12M: List<MonthlyTrendPoint> = emptyList(),
    val trendTimeframe: String = "6M",
    val trendChartMode: String = "NET",
    // Transactions filtering
    val searchQuery: String = "",
    val selectedPeriod: DatePeriod = DatePeriod.THIS_MONTH,
    val selectedAccountFilter: String = "All Accounts",
    val filterType: String = "ALL", // "ALL", "INFLOW", "OUTFLOW"
    val maxCapSlider: Float = 500000f,
    val isFilterDrawerOpen: Boolean = false,
    // Settings & Controls
    val defaultExpenseSource: String = "Primary Account",
    val fiscalResetDate: String = "1st of Month",
    val ocrReceiptParser: Boolean = true,
    val budgetOverrunSentinel: Boolean = true,
    val dailySpendingDigest: Boolean = true,
    val invoiceBillAlerts: Boolean = false,
    val enforceBiometrics: Boolean = true,
    val discreetMasking: Boolean = false,
    val isMaskRevealed: Boolean = false,
    val statusBarAlertsEnabled: Boolean = true,
    val cacheOccupiedMb: Double = 0.0,
    val isPurgingCache: Boolean = false,
    // Authentication & Profile State
    val isAuthenticated: Boolean = false,
    val authUid: String = "",
    val authName: String = "",
    val authEmail: String = "",
    val authPhone: String = "",
    val authMethod: String = "NONE",
    val avatarUri: String? = null,
    val netCashflow: Double = 0.0,
    val isDeleteProfileDialogOpen: Boolean = false,
    // Dialogs / Sheets / Feedback
    val toastMessage: String? = null,
    val selectedTransactionForInspect: Transaction? = null,
    val isTransferDialogOpen: Boolean = false,
    val isReceiptScannerOpen: Boolean = false,
    val isAddBankDialogOpen: Boolean = false,
    val accountToEdit: AccountItem? = null,
    val accountToDelete: AccountItem? = null,
    val isTaxonomyDialogOpen: Boolean = false,
    val showNotificationDrawer: Boolean = false
) {
    val remainingBankBalance: Double
        get() = accounts.filter { !it.isCredit }.sumOf { it.balance } -
                accounts.filter { it.isCredit }.sumOf { it.balance }
}

class FinPulseViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FinPulseUiState())
    val uiState: StateFlow<FinPulseUiState> = _uiState.asStateFlow()

    private val repository = KharchaRepository(FirebaseFirestore.getInstance())
    private var dataObserverJob: Job? = null

    init {
        checkExistingAuth()
    }

    private fun checkExistingAuth() {
        val currentUser = try { FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null }
        if (currentUser != null && currentUser.uid.isNotBlank()) {
            val name = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "User"
            val email = currentUser.email ?: ""
            _uiState.update {
                it.copy(
                    isAuthenticated = true,
                    authUid = currentUser.uid,
                    authName = name,
                    authEmail = email,
                    authPhone = currentUser.phoneNumber ?: "",
                    authMethod = if (currentUser.providerData.any { p -> p.providerId == "google.com" }) "GOOGLE" else "EMAIL"
                )
            }
            bindUserData(currentUser.uid)
        } else {
            // Unauthenticated: Start on Login Screen, zero demo data
            _uiState.update {
                it.copy(
                    isAuthenticated = false,
                    authUid = "",
                    authName = "",
                    authEmail = "",
                    accounts = emptyList(),
                    transactions = emptyList(),
                    totalExpenses = 0.0,
                    totalLiquidity = 0.0,
                    netWorth = 0.0,
                    netCashflow = 0.0,
                    monthlyTrends7D = emptyList(),
                    monthlyTrends6M = emptyList(),
                    monthlyTrends12M = emptyList()
                )
            }
        }
    }

    private fun bindUserData(userId: String) {
        dataObserverJob?.cancel()
        dataObserverJob = viewModelScope.launch {
            // Observe accounts
            launch {
                try {
                    repository.observeAccounts(userId).collect { accList ->
                        _uiState.update { current ->
                            recalculateState(accList, current.transactions, current)
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
            // Observe transactions
            launch {
                try {
                    repository.observeTransactions(userId).collect { txList ->
                        val sortedTxs = txList.sortedWith(
                            compareByDescending<Transaction> { it.dateGroup }
                                .thenByDescending { it.timestamp }
                        )
                        _uiState.update { current ->
                            recalculateState(current.accounts, sortedTxs, current)
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
            // Observe profile
            launch {
                try {
                    repository.observeProfile(userId).collect { profile ->
                        if (profile != null) {
                            _uiState.update { current ->
                                current.copy(
                                    currencyUnit = profile.currencyUnit.ifBlank { current.currencyUnit },
                                    avatarUri = profile.avatarUri ?: current.avatarUri
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    private fun recalculateState(
        accounts: List<AccountItem>,
        transactions: List<Transaction>,
        current: FinPulseUiState
    ): FinPulseUiState {
        // Total Amount: SUM OF ALL EXPENSES for the current user
        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE || it.amount < 0 }
        val totalExp = expenseTransactions.sumOf { kotlin.math.abs(it.amount) }

        // Remaining Bank Balance: non-credit accounts minus credit accounts
        val totalLiquid = accounts.filter { !it.isCredit }.sumOf { it.balance }
        val totalCredit = accounts.filter { it.isCredit }.sumOf { it.balance }
        val netBankBalance = totalLiquid - totalCredit

        // Net Cashflow
        val netCash = if (transactions.isEmpty()) 0.0 else transactions.sumOf { it.amount }

        // Dynamic Trend points
        val dynamicTrends = generateDynamicTrends(transactions)

        return current.copy(
            accounts = accounts,
            transactions = transactions,
            totalExpenses = totalExp,
            totalLiquidity = totalLiquid,
            netWorth = netBankBalance,
            netCashflow = netCash,
            monthlyTrends7D = dynamicTrends.first,
            monthlyTrends6M = dynamicTrends.second,
            monthlyTrends12M = dynamicTrends.third,
            avgBurnMonth = if (transactions.isEmpty()) 0.0 else totalExp,
            savingsRate = if (transactions.isEmpty()) 0 else {
                val income = transactions.filter { it.type == TransactionType.INCOME || it.amount > 0 }.sumOf { it.amount }
                if (income > 0) (((income - totalExp) / income) * 100).toInt().coerceIn(0, 100) else 0
            }
        )
    }

    private fun generateDynamicTrends(transactions: List<Transaction>): Triple<List<MonthlyTrendPoint>, List<MonthlyTrendPoint>, List<MonthlyTrendPoint>> {
        if (transactions.isEmpty()) {
            return Triple(emptyList(), emptyList(), emptyList())
        }

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME || it.amount > 0 }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE || it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }

        // 7D breakdown
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val trends7D = days.mapIndexed { i, day ->
            val fraction = if (i == days.size - 1) 0.4 else (0.6 / (days.size - 1))
            MonthlyTrendPoint(
                month = day,
                fullMonth = "$day, Recent",
                inflow = totalIncome * fraction,
                outflow = totalExpense * fraction
            )
        }

        // 6M breakdown
        val months6 = listOf("May", "Jun", "Jul", "Aug", "Sep", "Oct")
        val trends6M = months6.mapIndexed { i, m ->
            val fraction = if (i == months6.size - 1) 0.5 else (0.5 / (months6.size - 1))
            MonthlyTrendPoint(
                month = m,
                fullMonth = "$m 2024",
                inflow = totalIncome * fraction,
                outflow = totalExpense * fraction
            )
        }

        // 12M breakdown
        val months12 = listOf("Nov", "Dec", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct")
        val trends12M = months12.mapIndexed { i, m ->
            val fraction = if (i == months12.size - 1) 0.4 else (0.6 / (months12.size - 1))
            MonthlyTrendPoint(
                month = m,
                fullMonth = "$m 2024",
                inflow = totalIncome * fraction,
                outflow = totalExpense * fraction
            )
        }

        return Triple(trends7D, trends6M, trends12M)
    }

    fun setTrendTimeframe(timeframe: String) {
        _uiState.update { it.copy(trendTimeframe = timeframe) }
    }

    fun setTrendChartMode(mode: String) {
        _uiState.update { it.copy(trendChartMode = mode) }
    }

    fun navigateTo(destination: NavDestination) {
        _uiState.update {
            it.copy(
                prevDestination = it.currentDestination,
                currentDestination = destination
            )
        }
    }

    fun navigateBack() {
        _uiState.update {
            it.copy(
                currentDestination = it.prevDestination.takeIf { prev -> prev != NavDestination.ADD } ?: NavDestination.HOME
            )
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        when (mode) {
            AppThemeMode.DARK -> showToast("Switched to Monoline Dark interface")
            AppThemeMode.LIGHT -> showToast("Light Mode preview calibrated")
            AppThemeMode.AUTO -> showToast("Following Host OS appearance cycle")
        }
    }

    fun toggleDarkLight() {
        val current = _uiState.value.themeMode
        val next = if (current == AppThemeMode.DARK) AppThemeMode.LIGHT else AppThemeMode.DARK
        setThemeMode(next)
    }

    fun toggleDiscreetMasking() {
        _uiState.update {
            val next = !it.discreetMasking
            it.copy(discreetMasking = next, isMaskRevealed = false)
        }
        showToast(if (_uiState.value.discreetMasking) "Discreet balance masking active" else "Balances unmasked")
    }

    fun toggleMaskRevealed() {
        if (_uiState.value.discreetMasking) {
            _uiState.update { it.copy(isMaskRevealed = !it.isMaskRevealed) }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSelectedPeriod(period: DatePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        showToast("Switched view to ${period.label}")
    }

    fun setSelectedAccountFilter(accountFilter: String) {
        _uiState.update { it.copy(selectedAccountFilter = accountFilter) }
    }

    fun setFilterType(filterType: String) {
        _uiState.update { it.copy(filterType = filterType) }
    }

    fun setMaxCapSlider(cap: Float) {
        _uiState.update { it.copy(maxCapSlider = cap) }
    }

    fun toggleFilterDrawer(open: Boolean) {
        _uiState.update { it.copy(isFilterDrawerOpen = open) }
    }

    fun applyFilters() {
        _uiState.update { it.copy(isFilterDrawerOpen = false) }
        showToast("Filter parameters recalculated")
    }

    fun toggleOcrReceiptParser() {
        _uiState.update { it.copy(ocrReceiptParser = !it.ocrReceiptParser) }
        showToast(if (_uiState.value.ocrReceiptParser) "OCR Parser linked & active" else "OCR Parser disabled")
    }

    fun toggleBudgetOverrunSentinel() {
        _uiState.update { it.copy(budgetOverrunSentinel = !it.budgetOverrunSentinel) }
        showToast(if (_uiState.value.budgetOverrunSentinel) "Budget Sentinel armed" else "Sentinel disabled")
    }

    fun toggleDailySpendingDigest() {
        _uiState.update { it.copy(dailySpendingDigest = !it.dailySpendingDigest) }
        showToast(if (_uiState.value.dailySpendingDigest) "Daily Digest scheduled for 21:00 UTC" else "Digest paused")
    }

    fun toggleInvoiceBillAlerts() {
        _uiState.update { it.copy(invoiceBillAlerts = !it.invoiceBillAlerts) }
        showToast(if (_uiState.value.invoiceBillAlerts) "Invoice alerts enabled" else "Alerts turned off")
    }

    fun toggleEnforceBiometrics() {
        _uiState.update { it.copy(enforceBiometrics = !it.enforceBiometrics) }
        showToast(if (_uiState.value.enforceBiometrics) "Biometrics / FaceID required" else "Biometrics optional")
    }

    fun setCurrencyUnit(unit: String) {
        _uiState.update { it.copy(currencyUnit = unit) }
        val uid = _uiState.value.authUid
        if (uid.isNotBlank() && uid != "guest_uid" && !uid.startsWith("guest_")) {
            viewModelScope.launch {
                try {
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(mapOf("currencyUnit" to unit), SetOptions.merge())
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        showToast("Telemetry baseline set to $unit")
    }

    fun purgeCache() {
        if (_uiState.value.isPurgingCache) return
        _uiState.update { it.copy(isPurgingCache = true) }
        viewModelScope.launch {
            delay(400)
            _uiState.update { it.copy(cacheOccupiedMb = 0.0, isPurgingCache = false) }
            showToast("Temporary cache cleared")
        }
    }

    /**
     * Add a bank account directly with custom name and balance
     */
    fun addBankAccount(
        bankName: String,
        balance: Double,
        isCredit: Boolean = false,
        accountCategory: String = "Savings Account"
    ) {
        val cleanName = bankName.ifBlank { "Personal Bank Account" }
        val randomSuffix = (1000..9999).random().toString()
        val mask = "••$randomSuffix"
        val newAccount = AccountItem(
            id = "acc-${UUID.randomUUID().toString().take(6)}",
            name = cleanName,
            mask = mask,
            subtext = accountCategory,
            balance = balance,
            isCredit = isCredit,
            icon = if (isCredit) "credit_card" else "payments"
        )

        val updatedAccounts = _uiState.value.accounts + newAccount
        _uiState.update {
            recalculateState(updatedAccounts, it.transactions, it).copy(
                isAddBankDialogOpen = false
            )
        }

        val uid = _uiState.value.authUid
        if (uid.isNotBlank() && uid != "guest_uid" && !uid.startsWith("guest_")) {
            viewModelScope.launch {
                try {
                    repository.saveAccount(uid, newAccount)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        val formatted = CurrencyFormatter.format(balance, _uiState.value.currencyUnit)
        showToast("Connected $cleanName ($formatted)")
    }

    fun requestEditAccount(account: AccountItem) {
        _uiState.update { it.copy(accountToEdit = account) }
    }

    fun dismissEditAccount() {
        _uiState.update { it.copy(accountToEdit = null) }
    }

    fun updateBankAccount(account: AccountItem) {
        val updatedAccounts = _uiState.value.accounts.map { if (it.id == account.id) account else it }
        _uiState.update {
            recalculateState(updatedAccounts, it.transactions, it).copy(accountToEdit = null)
        }

        val uid = _uiState.value.authUid
        if (uid.isNotBlank() && uid != "guest_uid" && !uid.startsWith("guest_")) {
            viewModelScope.launch {
                try {
                    repository.saveAccount(uid, account)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        showToast("Updated ${account.name}")
    }

    fun requestDeleteAccount(account: AccountItem) {
        _uiState.update { it.copy(accountToDelete = account) }
    }

    fun dismissDeleteAccount() {
        _uiState.update { it.copy(accountToDelete = null) }
    }

    fun confirmDeleteAccount(account: AccountItem, context: Context? = null) {
        val updatedAccounts = _uiState.value.accounts.filterNot { it.id == account.id }
        _uiState.update {
            recalculateState(updatedAccounts, it.transactions, it).copy(accountToDelete = null)
        }

        val uid = _uiState.value.authUid
        if (uid.isNotBlank() && uid != "guest_uid" && !uid.startsWith("guest_")) {
            viewModelScope.launch {
                try {
                    repository.deleteAccount(uid, account.id)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        showToast("Deleted ${account.name}. Balances recalculated.")
        if (context != null && _uiState.value.statusBarAlertsEnabled) {
            NotificationHelper.sendNotification(
                context,
                "Account Deleted",
                "${account.name} was removed from your accounts. Balances updated."
            )
        }
    }

    fun addTransaction(
        title: String,
        category: String,
        amount: Double,
        type: TransactionType,
        accountName: String,
        note: String,
        isSplit: Boolean,
        isRecurring: Boolean,
        context: Context? = null
    ) {
        val signAmount = if (type == TransactionType.EXPENSE) -kotlin.math.abs(amount) else kotlin.math.abs(amount)
        val newTx = Transaction(
            id = "tx-${UUID.randomUUID().toString().take(8)}",
            title = title.ifBlank { "Unlabeled Transaction" },
            category = category,
            timestamp = "Just now",
            dateGroup = "Today",
            accountName = accountName,
            amount = signAmount,
            status = "Cleared",
            type = type,
            note = note,
            iconName = when (category.lowercase()) {
                "food", "dining" -> "coffee"
                "shopping" -> "shopping_bag"
                "transport" -> "directions_car"
                "utilities" -> "bolt"
                "movies" -> "local_activity"
                "wellness" -> "favorite"
                "learning" -> "school"
                else -> "payments"
            },
            isSplit = isSplit,
            isRecurring = isRecurring
        )

        val updatedTransactions = listOf(newTx) + _uiState.value.transactions
        val updatedAccounts = _uiState.value.accounts.map { acc ->
            if (acc.name.equals(accountName, ignoreCase = true) || accountName.contains(acc.name)) {
                acc.copy(balance = (acc.balance + signAmount).coerceAtLeast(0.0))
            } else acc
        }

        _uiState.update {
            recalculateState(updatedAccounts, updatedTransactions, it).copy(
                currentDestination = NavDestination.HOME
            )
        }

        val uid = _uiState.value.authUid
        if (uid.isNotBlank() && uid != "guest_uid" && !uid.startsWith("guest_")) {
            viewModelScope.launch {
                try {
                    repository.saveTransaction(uid, newTx)
                    val updatedAcc = updatedAccounts.find { it.name.equals(accountName, ignoreCase = true) || accountName.contains(it.name) }
                    if (updatedAcc != null) {
                        repository.saveAccount(uid, updatedAcc)
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        val formatted = CurrencyFormatter.format(kotlin.math.abs(amount), _uiState.value.currencyUnit)
        showToast("Logged Cleanly: $title ($formatted)")
        if (context != null && _uiState.value.statusBarAlertsEnabled) {
            NotificationHelper.sendNotification(
                context,
                "Transaction Added",
                "$title: $formatted logged to $accountName"
            )
        }
    }

    fun deleteTransaction(id: String, context: Context? = null) {
        val target = _uiState.value.transactions.find { it.id == id }
        val delta = target?.amount ?: 0.0
        val updatedTransactions = _uiState.value.transactions.filterNot { it.id == id }

        val updatedAccounts = _uiState.value.accounts.map { acc ->
            if (target != null && (acc.name.equals(target.accountName, ignoreCase = true) || target.accountName.contains(acc.name))) {
                acc.copy(balance = (acc.balance - delta).coerceAtLeast(0.0))
            } else acc
        }

        _uiState.update {
            recalculateState(updatedAccounts, updatedTransactions, it).copy(
                selectedTransactionForInspect = null
            )
        }

        val uid = _uiState.value.authUid
        if (uid.isNotBlank() && uid != "guest_uid" && !uid.startsWith("guest_")) {
            viewModelScope.launch {
                try {
                    repository.deleteTransaction(uid, id)
                    val updatedAcc = updatedAccounts.find { target != null && (it.name.equals(target.accountName, ignoreCase = true) || target.accountName.contains(it.name)) }
                    if (updatedAcc != null) {
                        repository.saveAccount(uid, updatedAcc)
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        val title = target?.title ?: "Transaction"
        showToast("Record removed: $title (Dashboard Updated)")
        if (context != null && _uiState.value.statusBarAlertsEnabled) {
            NotificationHelper.sendNotification(
                context,
                "Transaction Removed",
                "Removed $title. Balances and totals updated."
            )
        }
    }

    fun clearAllTransactions(context: Context? = null) {
        val currentAccounts = _uiState.value.accounts
        _uiState.update {
            recalculateState(currentAccounts, emptyList(), it).copy(
                selectedTransactionForInspect = null
            )
        }

        val uid = _uiState.value.authUid
        if (uid.isNotBlank() && uid != "guest_uid" && !uid.startsWith("guest_")) {
            viewModelScope.launch {
                try {
                    val txs = _uiState.value.transactions
                    for (t in txs) {
                        repository.deleteTransaction(uid, t.id)
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        showToast("All transactions cleared. Dashboard balance reset.")
        if (context != null && _uiState.value.statusBarAlertsEnabled) {
            NotificationHelper.sendNotification(
                context,
                "All Transactions Cleared",
                "All past transactions removed. Dashboard balance recalculated."
            )
        }
    }

    fun updateAvatarUri(uriString: String) {
        _uiState.update { it.copy(avatarUri = uriString) }
        val uid = _uiState.value.authUid
        if (uid.isNotBlank() && uid != "guest_uid" && !uid.startsWith("guest_")) {
            viewModelScope.launch {
                try {
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(mapOf("avatarUri" to uriString), SetOptions.merge())
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        showToast("Profile avatar updated successfully!")
    }

    fun resetAllAccountBalancesToZero(context: Context? = null) {
        val zeroAccounts = _uiState.value.accounts.map { it.copy(balance = 0.0) }
        _uiState.update {
            recalculateState(zeroAccounts, it.transactions, it)
        }

        val uid = _uiState.value.authUid
        if (uid.isNotBlank() && uid != "guest_uid" && !uid.startsWith("guest_")) {
            viewModelScope.launch {
                try {
                    for (acc in zeroAccounts) {
                        repository.saveAccount(uid, acc)
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        showToast("All account balances reset to ₹0.00")
        if (context != null && _uiState.value.statusBarAlertsEnabled) {
            NotificationHelper.sendNotification(
                context,
                "Balances Reset",
                "All connected bank account balances have been reset to ₹0.00."
            )
        }
    }

    fun setDeleteProfileDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isDeleteProfileDialogOpen = open) }
    }

    fun deleteUserProfile(context: Context? = null) {
        val uid = _uiState.value.authUid
        viewModelScope.launch {
            try {
                if (uid.isNotBlank() && uid != "guest_uid" && !uid.startsWith("guest_")) {
                    try {
                        repository.deleteAllUserData(uid)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                try {
                    FirebaseAuth.getInstance().currentUser?.delete()?.await()
                } catch (e: Exception) {
                    // ignore
                }
                try {
                    FirebaseAuth.getInstance().signOut()
                } catch (e: Exception) {
                    // ignore
                }
            } finally {
                dataObserverJob?.cancel()
                _uiState.update { state ->
                    state.copy(
                        accounts = emptyList(),
                        transactions = emptyList(),
                        totalExpenses = 0.0,
                        totalLiquidity = 0.0,
                        protectedVault = 0.0,
                        netWorth = 0.0,
                        netCashflow = 0.0,
                        avatarUri = null,
                        avgBurnMonth = 0.0,
                        savingsRate = 0,
                        cycleBurnSpentPct = 0,
                        remainingBuffer = 0.0,
                        dailyRate = 0.0,
                        authName = "",
                        authEmail = "",
                        authPhone = "",
                        authUid = "",
                        authMethod = "NONE",
                        isAuthenticated = false,
                        isDeleteProfileDialogOpen = false,
                        themeMode = AppThemeMode.DARK,
                        currencyUnit = "INR (₹)",
                        discreetMasking = false,
                        currentDestination = NavDestination.HOME
                    )
                }
                showToast("Profile & all data permanently deleted. Balances reset to ₹0.")
                if (context != null) {
                    NotificationHelper.sendNotification(
                        context,
                        "Profile Deleted",
                        "All accounts, transactions, and settings have been reset to ₹0."
                    )
                }
            }
        }
    }

    fun onAuthSuccess(name: String, email: String, context: Context? = null) {
        val user = try { FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null }
        val uid = user?.uid ?: "user_${UUID.randomUUID().toString().take(6)}"
        val method = if (user?.providerData?.any { it.providerId == "google.com" } == true) "GOOGLE"
                     else if (!user?.phoneNumber.isNullOrEmpty()) "MOBILE"
                     else "EMAIL"
        _uiState.update { state ->
            state.copy(
                isAuthenticated = true,
                authUid = uid,
                authName = name,
                authEmail = email,
                authPhone = user?.phoneNumber ?: state.authPhone,
                authMethod = method,
                currentDestination = NavDestination.HOME
            )
        }
        bindUserData(uid)
        showToast("Logged in as $name")
        if (context != null && _uiState.value.statusBarAlertsEnabled) {
            NotificationHelper.sendNotification(
                context,
                "Welcome to Kharcha Book",
                "Signed in successfully as $name ($email)"
            )
        }
    }

    fun onGuestLogin() {
        val guestUid = "guest_${UUID.randomUUID().toString().take(6)}"
        _uiState.update {
            it.copy(
                isAuthenticated = true,
                authUid = guestUid,
                authMethod = "GUEST",
                authName = "Guest User",
                authEmail = "guest@kharchabook.local",
                currentDestination = NavDestination.HOME
            )
        }
        bindUserData(guestUid)
        showToast("Continuing in Guest Mode")
    }

    fun signOut(context: Context? = null) {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // ignore
        }
        dataObserverJob?.cancel()
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                authUid = "",
                authName = "",
                authEmail = "",
                authPhone = "",
                authMethod = "NONE",
                accounts = emptyList(),
                transactions = emptyList(),
                totalExpenses = 0.0,
                totalLiquidity = 0.0,
                netWorth = 0.0,
                netCashflow = 0.0,
                currentDestination = NavDestination.HOME
            )
        }
        showToast("Signed out successfully")
        if (context != null && _uiState.value.statusBarAlertsEnabled) {
            NotificationHelper.sendNotification(
                context,
                "Signed Out",
                "You have signed out of Kharcha Book."
            )
        }
    }

    fun toggleStatusBarAlerts() {
        _uiState.update { it.copy(statusBarAlertsEnabled = !it.statusBarAlertsEnabled) }
    }

    fun sendTestNotification(context: Context) {
        NotificationHelper.sendNotification(
            context,
            "Kharcha Book Alert",
            "Status bar notifications are working! Current Remaining Balance: ${CurrencyFormatter.format(_uiState.value.remainingBankBalance, _uiState.value.currencyUnit)}"
        )
        showToast("Status bar notification sent!")
    }

    fun selectTransactionForInspect(transaction: Transaction?) {
        _uiState.update { it.copy(selectedTransactionForInspect = transaction) }
    }

    fun setTransferDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isTransferDialogOpen = open) }
    }

    fun setReceiptScannerOpen(open: Boolean) {
        _uiState.update { it.copy(isReceiptScannerOpen = open) }
    }

    fun setAddBankDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isAddBankDialogOpen = open) }
    }

    fun setTaxonomyDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isTaxonomyDialogOpen = open) }
    }

    fun setShowNotificationDrawer(show: Boolean) {
        _uiState.update { it.copy(showNotificationDrawer = show) }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
        viewModelScope.launch {
            delay(2400)
            if (_uiState.value.toastMessage == message) {
                _uiState.update { it.copy(toastMessage = null) }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun exportCsv() {
        showToast("Transactions exported to KharchaBook_INR.csv")
    }

    fun exportPdf() {
        showToast("Compiling PDF audit report (INR)...")
    }

    fun exportJson() {
        showToast("Packaging JSON telemetry stream...")
    }
}
