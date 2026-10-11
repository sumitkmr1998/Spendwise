package com.example.data.repository

import com.example.data.db.CategoryDao
import com.example.data.db.ExpenseDao
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseEntity
import com.example.security.KeystoreCryptoManager
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) {

    // --- Expense Operations ---

    fun getAllExpensesFlow(): Flow<List<ExpenseEntity>> = expenseDao.getAllExpensesFlow()

    fun getExpensesBetweenFlow(startTime: Long, endTime: Long): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesBetweenFlow(startTime, endTime)

    suspend fun getExpensesBetween(startTime: Long, endTime: Long): List<ExpenseEntity> =
        expenseDao.getExpensesBetween(startTime, endTime)

    suspend fun getAllExpenses(): List<ExpenseEntity> = expenseDao.getAllExpenses()

    suspend fun insert(expense: ExpenseEntity): Long = expenseDao.insert(expense)

    suspend fun insertAll(expenses: List<ExpenseEntity>): List<Long> = expenseDao.insertAll(expenses)

    suspend fun update(expense: ExpenseEntity) = expenseDao.update(expense)

    suspend fun delete(expense: ExpenseEntity) = expenseDao.delete(expense)

    suspend fun deleteById(id: Long) = expenseDao.deleteById(id)

    suspend fun deleteAll() = expenseDao.deleteAll()

    // --- Category Operations ---

    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow()

    suspend fun getAllCategories(): List<CategoryEntity> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insert(category)

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.update(category)

    suspend fun deleteCategory(category: CategoryEntity) {
        // Reassign expenses using this category to "Other"
        expenseDao.reassignCategory(category.name, "Other")
        categoryDao.delete(category)
    }

    // --- Duplicate Detection ---

    suspend fun checkIsDuplicate(
        rawMessage: String,
        referenceId: String,
        amount: Double,
        merchant: String,
        timestamp: Long
    ): ExpenseEntity? {
        // 1. Check exact SHA-256 message hash
        if (rawMessage.isNotBlank()) {
            val hash = KeystoreCryptoManager.computeMessageHash(rawMessage)
            val match = expenseDao.findByMessageHash(hash)
            if (match != null) return match
        }

        // 2. Check reference ID if present
        if (referenceId.isNotBlank()) {
            val all = expenseDao.getAllExpenses()
            val match = all.firstOrNull { it.referenceIdDecrypted.equals(referenceId, ignoreCase = true) }
            if (match != null) return match
        }

        // 3. Check duplicate (same amount, same merchant, within 4 hours)
        val window = 4L * 60 * 60 * 1000
        val candidate = expenseDao.findPossibleDuplicate(
            amount = amount,
            merchant = merchant,
            startTime = timestamp - window,
            endTime = timestamp + window
        )
        if (candidate != null) return candidate

        return null
    }

    // --- Seed Initial Data ---

    suspend fun seedDefaultCategoriesIfEmpty() {
        if (categoryDao.getCategoriesCount() == 0) {
            val defaults = listOf(
                CategoryEntity(name = "Food & Dining", colorHex = "#F97316", iconName = "restaurant", keywords = "starbucks, mcdonald, diner, burger, pizza, cafe, restaurant, doordash, ubereats", isDefault = true),
                CategoryEntity(name = "Groceries", colorHex = "#10B981", iconName = "shopping_cart", keywords = "walmart, target, trader joe, costco, whole foods, grocery, supermarket", isDefault = true),
                CategoryEntity(name = "Shopping", colorHex = "#EC4899", iconName = "shopping_bag", keywords = "amazon, ebay, best buy, store, zara, nike, apple, mall", isDefault = true),
                CategoryEntity(name = "Transportation", colorHex = "#3B82F6", iconName = "directions_car", keywords = "uber, lyft, gas, fuel, shell, chevron, transit, metro, taxi", isDefault = true),
                CategoryEntity(name = "Bills & Utilities", colorHex = "#EAB308", iconName = "receipt_long", keywords = "electric, energy, water, gas, at&t, verizon, wifi, internet, power", isDefault = true),
                CategoryEntity(name = "Entertainment", colorHex = "#8B5CF6", iconName = "movie", keywords = "netflix, spotify, cinema, movie, tickets, hulu, disney, steam", isDefault = true),
                CategoryEntity(name = "Health & Medical", colorHex = "#EF4444", iconName = "local_hospital", keywords = "cvs, walgreens, pharmacy, doctor, dental, hospital, clinic", isDefault = true),
                CategoryEntity(name = "Travel", colorHex = "#06B6D4", iconName = "flight", keywords = "flight, airline, hotel, airbnb, delta, booking, travel", isDefault = true),
                CategoryEntity(name = "Income / Salary", colorHex = "#059669", iconName = "account_balance_wallet", keywords = "salary, payroll, deposit, dividend, bonus, direct dep", isDefault = true),
                CategoryEntity(name = "Other", colorHex = "#64748B", iconName = "category", keywords = "", isDefault = true)
            )
            categoryDao.insertAll(defaults)
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        seedDefaultCategoriesIfEmpty()

        if (expenseDao.getExpensesCount() == 0) {
            val initialList = mutableListOf<ExpenseEntity>()

            val sampleItems = listOf(
                Triple("Trader Joe's", 86.45, "Groceries"),
                Triple("Starbucks", 6.75, "Food & Dining"),
                Triple("Amazon.com", 149.99, "Shopping"),
                Triple("Uber Ride", 32.40, "Transportation"),
                Triple("Con Edison Electric", 115.50, "Bills & Utilities"),
                Triple("Netflix", 15.49, "Entertainment"),
                Triple("CVS Pharmacy", 48.20, "Health & Medical"),
                Triple("Shell Oil", 52.00, "Transportation"),
                Triple("Chipotle Grill", 18.25, "Food & Dining"),
                Triple("Target Superstore", 74.80, "Groceries"),
                Triple("Nike Store", 120.00, "Shopping"),
                Triple("ACME Corp Payroll", 3850.00, "Income / Salary"),

                Triple("Whole Foods", 142.30, "Groceries"),
                Triple("Delta Airlines", 310.00, "Travel"),
                Triple("Starbucks", 14.50, "Food & Dining"),
                Triple("Best Buy Electronics", 249.99, "Shopping"),
                Triple("Internet Comcast", 89.99, "Bills & Utilities"),
                Triple("Spotify Subscription", 10.99, "Entertainment"),
                Triple("Uber Airport", 45.00, "Transportation"),
                Triple("Dine at Olive Garden", 62.40, "Food & Dining"),
                Triple("ACME Corp Payroll", 3850.00, "Income / Salary"),

                Triple("Costco Wholesale", 215.60, "Groceries"),
                Triple("Marriott Hotel", 280.00, "Travel"),
                Triple("Amazon Prime", 139.00, "Shopping"),
                Triple("City Water & Power", 95.00, "Bills & Utilities"),
                Triple("Cinema AMC Tickets", 36.00, "Entertainment"),
                Triple("Chevron Gas", 58.50, "Transportation"),
                Triple("Starbucks", 8.25, "Food & Dining"),
                Triple("ACME Corp Payroll", 3850.00, "Income / Salary")
            )

            var offsetDays = 1
            for (item in sampleItems) {
                val calendar = Calendar.getInstance()
                val isIncome = item.third == "Income / Salary"
                val monthShift = when {
                    offsetDays <= 12 -> 0
                    offsetDays <= 21 -> -1
                    else -> -2
                }
                calendar.add(Calendar.MONTH, monthShift)
                calendar.set(Calendar.DAY_OF_MONTH, ((offsetDays * 3) % 27) + 1)

                val rawMsg = "Transaction alert: $${item.second} at ${item.first}"
                initialList.add(
                    ExpenseEntity.createEncrypted(
                        amount = item.second,
                        currency = "$",
                        type = if (isIncome) "INCOME" else "EXPENSE",
                        category = item.third,
                        merchant = item.first,
                        accountInfo = if (isIncome) "Direct Dep *3490" else "Card *4821",
                        timestamp = calendar.timeInMillis,
                        rawMessage = rawMsg,
                        referenceId = "REF${100000 + offsetDays * 123}",
                        notes = "Auto-imported SMS"
                    )
                )
                offsetDays++
            }
            expenseDao.insertAll(initialList)
        }
    }
}
