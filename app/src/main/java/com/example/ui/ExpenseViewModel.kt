package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseEntity
import com.example.data.repository.ExpenseRepository
import com.example.parser.ParsedSmsResult
import com.example.parser.SmsParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class DateRangeFilter(val displayName: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_90_DAYS("Last 90 Days"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time"),
    CUSTOM("Custom Range")
}

data class CategorySpendItem(
    val categoryName: String,
    val color: Color,
    val totalAmount: Double,
    val percentage: Float,
    val count: Int
)

data class MonthlyTrendItem(
    val monthYearKey: String, // e.g. "2026-10"
    val displayLabel: String, // e.g. "Oct 26"
    val totalSpent: Double,
    val count: Int
)

data class UiSummaryReport(
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netBalance: Double = 0.0,
    val dailyAverage: Double = 0.0,
    val topCategory: String = "Other",
    val topCategoryAmount: Double = 0.0,
    val transactionCount: Int = 0
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    private val _selectedFilter = MutableStateFlow(DateRangeFilter.ALL_TIME)
    val selectedFilter: StateFlow<DateRangeFilter> = _selectedFilter.asStateFlow()

    private val _customDateRange = MutableStateFlow(Pair(
        System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
        System.currentTimeMillis()
    ))
    val customDateRange: StateFlow<Pair<Long, Long>> = _customDateRange.asStateFlow()

    // Search query & category filter for transactions tab
    val searchQuery = MutableStateFlow("")
    val filterCategoryName = MutableStateFlow<String?>(null)

    // Current SMS parsing state
    val smsInputText = MutableStateFlow("")
    val lastParsedResult = MutableStateFlow<ParsedSmsResult?>(null)
    val isScanningInbox = MutableStateFlow(false)
    val inboxScanResultNotice = MutableStateFlow<String?>(null)

    // Category Manager Dialog State
    val showCategoryManagerDialog = MutableStateFlow(false)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(db.expenseDao(), db.categoryDao())
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.getAllExpensesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered by selected date range
    val filteredExpenses: StateFlow<List<ExpenseEntity>> = combine(
        allExpenses,
        _selectedFilter,
        _customDateRange
    ) { expenses, filter, customRange ->
        filterExpensesByDate(expenses, filter, customRange)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Summary KPIs
    val summaryReport: StateFlow<UiSummaryReport> = filteredExpenses.map { list ->
        calculateSummaryReport(list, _selectedFilter.value, _customDateRange.value)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiSummaryReport())

    // Category Breakdown for Visualizations (resolving custom colors)
    val categoryBreakdown: StateFlow<List<CategorySpendItem>> = combine(
        filteredExpenses,
        categories
    ) { list, catList ->
        val expenseItems = list.filter { it.isExpense }
        val totalSpent = expenseItems.sumOf { it.amount }
        val grouped = expenseItems.groupBy { it.category }

        grouped.map { (catName, items) ->
            val matchedCat = catList.firstOrNull { it.name.equals(catName, ignoreCase = true) }
            val color = matchedCat?.composeColor ?: ExpenseCategory.fromString(catName).color
            val amount = items.sumOf { it.amount }
            val pct = if (totalSpent > 0) ((amount / totalSpent) * 100).toFloat() else 0f
            CategorySpendItem(
                categoryName = catName,
                color = color,
                totalAmount = amount,
                percentage = pct,
                count = items.size
            )
        }.sortedByDescending { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Spending Trends for Visualizations
    val monthlyTrends: StateFlow<List<MonthlyTrendItem>> = allExpenses.map { list ->
        calculateMonthlyTrends(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions list filtered by search and category filter
    val displayTransactions: StateFlow<List<ExpenseEntity>> = combine(
        filteredExpenses,
        searchQuery,
        filterCategoryName
    ) { list, query, catName ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.merchant.contains(query, ignoreCase = true) ||
                    item.notes.contains(query, ignoreCase = true) ||
                    item.rawMessageDecrypted.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true)
            val matchesCat = catName == null || item.category.equals(catName, ignoreCase = true)
            matchesQuery && matchesCat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDateFilter(filter: DateRangeFilter) {
        _selectedFilter.value = filter
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customDateRange.value = Pair(start, end)
        _selectedFilter.value = DateRangeFilter.CUSTOM
    }

    fun analyzeCurrentSms() {
        val text = smsInputText.value
        val parsed = SmsParser.parse(text, customCategories = categories.value)

        if (parsed.isTransaction) {
            viewModelScope.launch {
                val duplicateMatch = repository.checkIsDuplicate(
                    rawMessage = parsed.rawMessage,
                    referenceId = parsed.referenceId,
                    amount = parsed.amount,
                    merchant = parsed.merchant,
                    timestamp = parsed.timestamp
                )

                if (duplicateMatch != null) {
                    val dateFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(duplicateMatch.timestamp))
                    lastParsedResult.value = parsed.copy(
                        isDuplicate = true,
                        duplicateMatchReason = "Duplicate Detected: Already saved as $${duplicateMatch.amount} at ${duplicateMatch.merchant} on $dateFormatted."
                    )
                } else {
                    lastParsedResult.value = parsed.copy(isDuplicate = false)
                }
            }
        } else {
            lastParsedResult.value = parsed
        }
    }

    fun setSampleSms(text: String) {
        smsInputText.value = text
        analyzeCurrentSms()
    }

    fun saveParsedResult(result: ParsedSmsResult, customNotes: String = "") {
        if (!result.isTransaction) return
        viewModelScope.launch {
            // Secure creation with Android Keystore encryption
            val entity = ExpenseEntity.createEncrypted(
                amount = result.amount,
                currency = result.currency,
                type = result.type.name,
                category = result.categoryName,
                merchant = result.merchant,
                accountInfo = result.accountInfo,
                timestamp = result.timestamp,
                rawMessage = result.rawMessage,
                referenceId = result.referenceId,
                notes = customNotes
            )
            repository.insert(entity)
            smsInputText.value = ""
            lastParsedResult.value = null
        }
    }

    fun addManualTransaction(
        amount: Double,
        type: String,
        categoryName: String,
        merchant: String,
        notes: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val entity = ExpenseEntity.createEncrypted(
                amount = amount,
                currency = "$",
                type = type,
                category = categoryName,
                merchant = merchant.ifBlank { "Manual Entry" },
                accountInfo = "Manual",
                timestamp = timestamp,
                notes = notes
            )
            repository.insert(entity)
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun toggleFlag(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.update(expense.copy(isFlagged = !expense.isFlagged))
        }
    }

    // --- Custom Category CRUD ---

    fun addCustomCategory(name: String, colorHex: String, iconName: String, keywords: String) {
        viewModelScope.launch {
            val entity = CategoryEntity(
                name = name,
                colorHex = colorHex,
                iconName = iconName,
                keywords = keywords,
                isDefault = false
            )
            repository.insertCategory(entity)
        }
    }

    fun editCustomCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCustomCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun resetDataWithSamples() {
        viewModelScope.launch {
            repository.deleteAll()
            repository.seedInitialDataIfEmpty()
        }
    }

    fun scanInboxSms(context: Context) {
        viewModelScope.launch {
            isScanningInbox.value = true
            inboxScanResultNotice.value = null
            try {
                val uri = Uri.parse("content://sms/inbox")
                val cursor = context.contentResolver.query(
                    uri,
                    arrayOf("body", "date"),
                    null,
                    null,
                    "date DESC LIMIT 100"
                )

                var foundCount = 0
                var duplicateSkippedCount = 0
                val newExpenses = mutableListOf<ExpenseEntity>()
                val currentCats = categories.value

                cursor?.use {
                    val bodyIndex = it.getColumnIndex("body")
                    val dateIndex = it.getColumnIndex("date")
                    while (it.moveToNext()) {
                        val body = if (bodyIndex != -1) it.getString(bodyIndex) ?: "" else ""
                        val date = if (dateIndex != -1) it.getLong(dateIndex) else System.currentTimeMillis()
                        val parsed = SmsParser.parse(body, defaultTimestamp = date, customCategories = currentCats)
                        if (parsed.isTransaction && parsed.amount > 0) {
                            // Check for duplicates before importing!
                            val isDup = repository.checkIsDuplicate(
                                rawMessage = parsed.rawMessage,
                                referenceId = parsed.referenceId,
                                amount = parsed.amount,
                                merchant = parsed.merchant,
                                timestamp = parsed.timestamp
                            ) != null

                            if (isDup) {
                                duplicateSkippedCount++
                            } else {
                                newExpenses.add(
                                    ExpenseEntity.createEncrypted(
                                        amount = parsed.amount,
                                        currency = parsed.currency,
                                        type = parsed.type.name,
                                        category = parsed.categoryName,
                                        merchant = parsed.merchant,
                                        accountInfo = parsed.accountInfo,
                                        timestamp = parsed.timestamp,
                                        rawMessage = parsed.rawMessage,
                                        referenceId = parsed.referenceId,
                                        notes = "Imported from Inbox"
                                    )
                                )
                                foundCount++
                            }
                        }
                    }
                }

                if (newExpenses.isNotEmpty()) {
                    repository.insertAll(newExpenses)
                    inboxScanResultNotice.value = "Imported $foundCount transactions (Filtered out $duplicateSkippedCount duplicates)!"
                } else if (duplicateSkippedCount > 0) {
                    inboxScanResultNotice.value = "All $duplicateSkippedCount inbox messages were already saved (duplicates filtered)."
                } else {
                    inboxScanResultNotice.value = "Scan complete: No financial transaction SMS messages found."
                }
            } catch (e: Exception) {
                inboxScanResultNotice.value = "Could not read inbox: ${e.message}"
            } finally {
                isScanningInbox.value = false
            }
        }
    }

    private fun filterExpensesByDate(
        expenses: List<ExpenseEntity>,
        filter: DateRangeFilter,
        customRange: Pair<Long, Long>
    ): List<ExpenseEntity> {
        return when (filter) {
            DateRangeFilter.ALL_TIME -> expenses
            DateRangeFilter.THIS_MONTH -> {
                val startCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                expenses.filter { it.timestamp >= startCal.timeInMillis }
            }
            DateRangeFilter.LAST_MONTH -> {
                val startCal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                expenses.filter { it.timestamp >= startCal.timeInMillis && it.timestamp < endCal.timeInMillis }
            }
            DateRangeFilter.LAST_30_DAYS -> {
                val threshold = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                expenses.filter { it.timestamp >= threshold }
            }
            DateRangeFilter.LAST_90_DAYS -> {
                val threshold = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
                expenses.filter { it.timestamp >= threshold }
            }
            DateRangeFilter.THIS_YEAR -> {
                val startCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                expenses.filter { it.timestamp >= startCal.timeInMillis }
            }
            DateRangeFilter.CUSTOM -> {
                val start = customRange.first
                val end = customRange.second + 24L * 60 * 60 * 1000
                expenses.filter { it.timestamp in start..end }
            }
        }
    }

    private fun calculateSummaryReport(
        list: List<ExpenseEntity>,
        filter: DateRangeFilter,
        customRange: Pair<Long, Long>
    ): UiSummaryReport {
        if (list.isEmpty()) return UiSummaryReport()

        val expenseList = list.filter { it.isExpense }
        val incomeList = list.filter { !it.isExpense }

        val totalSpent = expenseList.sumOf { it.amount }
        val totalIncome = incomeList.sumOf { it.amount }
        val netBalance = totalIncome - totalSpent

        // Top category
        val catGroups = expenseList.groupBy { it.category }
        val topCatEntry = catGroups.maxByOrNull { entry -> entry.value.sumOf { it.amount } }
        val topCat = topCatEntry?.key ?: "Other"
        val topCatAmt = topCatEntry?.value?.sumOf { it.amount } ?: 0.0

        val daysSpan = when (filter) {
            DateRangeFilter.THIS_MONTH -> Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
            DateRangeFilter.LAST_30_DAYS -> 30
            DateRangeFilter.LAST_90_DAYS -> 90
            DateRangeFilter.LAST_MONTH -> 30
            DateRangeFilter.THIS_YEAR -> Calendar.getInstance().get(Calendar.DAY_OF_YEAR).coerceAtLeast(1)
            DateRangeFilter.CUSTOM -> {
                val diffDays = ((customRange.second - customRange.first) / (1000 * 60 * 60 * 24)).toInt()
                diffDays.coerceAtLeast(1)
            }
            DateRangeFilter.ALL_TIME -> {
                val minTs = list.minOfOrNull { it.timestamp } ?: System.currentTimeMillis()
                val diffDays = ((System.currentTimeMillis() - minTs) / (1000 * 60 * 60 * 24)).toInt()
                diffDays.coerceAtLeast(1)
            }
        }

        val dailyAvg = if (daysSpan > 0) totalSpent / daysSpan else 0.0

        return UiSummaryReport(
            totalSpent = totalSpent,
            totalIncome = totalIncome,
            netBalance = netBalance,
            dailyAverage = dailyAvg,
            topCategory = topCat,
            topCategoryAmount = topCatAmt,
            transactionCount = list.size
        )
    }

    private fun calculateMonthlyTrends(list: List<ExpenseEntity>): List<MonthlyTrendItem> {
        val expenseList = list.filter { it.isExpense }
        val sdfKey = SimpleDateFormat("yyyy-MM", Locale.US)
        val sdfLabel = SimpleDateFormat("MMM yy", Locale.US)

        val grouped = expenseList.groupBy {
            sdfKey.format(Date(it.timestamp))
        }

        val sortedKeys = grouped.keys.sorted()
        return sortedKeys.map { key ->
            val items = grouped[key] ?: emptyList()
            val sampleTimestamp = items.firstOrNull()?.timestamp ?: System.currentTimeMillis()
            MonthlyTrendItem(
                monthYearKey = key,
                displayLabel = sdfLabel.format(Date(sampleTimestamp)),
                totalSpent = items.sumOf { it.amount },
                count = items.size
            )
        }
    }
}
