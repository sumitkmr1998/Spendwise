package com.example.parser

import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

data class ParsedSmsResult(
    val isTransaction: Boolean,
    val amount: Double = 0.0,
    val currency: String = "$",
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryName: String = "Other",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val merchant: String = "Unknown Merchant",
    val accountInfo: String = "",
    val referenceId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val rawMessage: String = "",
    val confidence: Float = 0f,
    val rejectionReason: String? = null,
    val isDuplicate: Boolean = false,
    val duplicateMatchReason: String? = null
)

object SmsParser {

    // Detection for non-transaction messages like OTPs, promos without amounts
    private val OTP_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(otp|verification code|one time password|security code|login code|secret code)\\b"),
        Pattern.compile("(?i)\\b(do not share|don't share|valid for \\d+ mins?)\\b")
    )

    // Expense indicators
    private val EXPENSE_KEYWORDS = listOf(
        "debited", "spent", "paid", "charged", "purchase", "purchased", "sent", "withdrawn",
        "deducted", "order placed", "txn", "dr", "billing", "autopay", "spent on", "payment to"
    )

    // Income indicators
    private val INCOME_KEYWORDS = listOf(
        "credited", "received", "deposited", "salary", "refund", "cashback", "cr", "added to",
        "transferred to you", "reward", "reimbursed"
    )

    // Amount extraction regexes
    private val AMOUNT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:[$€£₹]|USD|INR|EUR|GBP|Rs\\.?|CAD|AUD)\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)(?:spent|paid|debited|charged|amount(?: of)?|sum of|txn of)\\s*[:\\-]?\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:[$€£₹]|USD|INR|EUR|GBP|Rs\\.?)?"),
        Pattern.compile("(?i)([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:USD|EUR|GBP|INR|dollars|bucks)")
    )

    // Currency symbol match
    private val CURRENCY_PATTERNS = mapOf(
        Pattern.compile("(?i)([$]|USD|dollars?)") to "$",
        Pattern.compile("(?i)([€]|EUR|euros?)") to "€",
        Pattern.compile("(?i)([£]|GBP|pounds?)") to "£",
        Pattern.compile("(?i)([₹]|INR|Rs\\.?)") to "₹",
        Pattern.compile("(?i)(CAD)") to "CA$",
        Pattern.compile("(?i)(AUD)") to "A$"
    )

    // Account / Card patterns
    private val ACCOUNT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:a/c|acct|account|card)(?:\\s+(?:ending|no|number|xx|\\*+))?\\s*[:\\-]?\\s*([*xX\\d]{3,8})"),
        Pattern.compile("(?i)(?:chase|wells fargo|citi|bofa|bank of america|hdfc|icici|amex|apple card|discover|capital one)\\b(?:\\s+(?:card|a/c|ending)?\\s*[*xX\\d]{2,6})?")
    )

    // Reference ID patterns
    private val REF_PATTERNS = listOf(
        Pattern.compile("(?i)(?:ref(?:erence)?(?: id| no| num)?|txn(?: id| no)?|utr|rrn|auth(?: code)?)\\s*[:\\-]?\\s*([a-zA-Z0-9]{5,20})")
    )

    // Merchant extraction regexes
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:at|to|for|vpa|info|towards|merchant)\\s+([a-zA-Z0-9&'._\\- ]{2,30}?)(?:\\s+(?:on|using|via|with|ref|avail|bal|dated|from|card|a/c|at)|[.,;\\n]|$)"),
        Pattern.compile("(?i)(?:purchase of [^ ]+ at)\\s+([a-zA-Z0-9&'._\\- ]{2,30}?)(?:[.,;\\n]|$)")
    )

    // Common known merchants for instant high-confidence classification
    private val KNOWN_MERCHANT_CATEGORIES = mapOf(
        listOf("starbucks", "mcdonald", "subway", "burger king", "dominos", "pizza", "doordash", "ubereats", "grubhub", "dunkin", "chipotle", "wendy", "taco bell", "kfc", "cafe", "restaurant", "bistro", "bakery", "diner")
                to ExpenseCategory.FOOD_DINING,

        listOf("walmart", "target", "costco", "trader joe", "whole foods", "kroger", "safeway", "aldi", "publix", "supermarket", "grocery", "instacart", "sprouts", "heb", "meijer")
                to ExpenseCategory.GROCERIES,

        listOf("amazon", "ebay", "apple store", "best buy", "zara", "nike", "adidas", "h&m", "ikea", "nordstrom", "macy", "shein", "walmart.com", "target.com", "etsy", "aliexpress", "sephora")
                to ExpenseCategory.SHOPPING,

        listOf("uber", "lyft", "shell", "chevron", "exxon", "bp gas", "mobil", "transit", "subway metro", "parking", "toll", "airline", "gas station", "fuel", "amtrak", "hertz", "enterprise")
                to ExpenseCategory.TRANSPORTATION,

        listOf("electric", "water", "gas bill", "at&t", "verizon", "t-mobile", "xfinity", "comcast", "spectrum", "con edison", "pge", "energy", "utility", "internet", "broadband", "rent")
                to ExpenseCategory.BILLS_UTILITIES,

        listOf("netflix", "spotify", "disney", "hulu", "hbo", "cinema", "amc", "regal", "steam", "playstation", "xbox", "nintendo", "ticketmaster", "youtube premium", "audible", "twitch")
                to ExpenseCategory.ENTERTAINMENT,

        listOf("cvs", "walgreens", "pharmacy", "clinic", "hospital", "doctor", "dental", "dentist", "optometry", "kaiser", "quest diag", "health", "medicare", "urgent care")
                to ExpenseCategory.HEALTH_MEDICAL,

        listOf("delta", "united airlines", "american airlines", "southwest", "airbnb", "hotel", "marriott", "hilton", "hyatt", "booking.com", "expedia", "hostel", "flight")
                to ExpenseCategory.TRAVEL,

        listOf("payroll", "salary", "bonus", "dividend", "acme corp", "employer", "deposit", "direct dep")
                to ExpenseCategory.SALARY_INCOME
    )

    fun parse(
        message: String,
        defaultTimestamp: Long = System.currentTimeMillis(),
        customCategories: List<CategoryEntity> = emptyList()
    ): ParsedSmsResult {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            return ParsedSmsResult(
                isTransaction = false,
                rawMessage = message,
                rejectionReason = "Empty message"
            )
        }

        // Check if message is strictly an OTP
        var isOtp = false
        for (pattern in OTP_PATTERNS) {
            if (pattern.matcher(trimmed).find()) {
                isOtp = true
                break
            }
        }

        // Check amount
        var extractedAmount: Double? = null
        var matchedCurrency = "$"

        for ((pattern, symbol) in CURRENCY_PATTERNS) {
            if (pattern.matcher(trimmed).find()) {
                matchedCurrency = symbol
                break
            }
        }

        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(trimmed)
            if (matcher.find()) {
                val group = matcher.group(1) ?: matcher.group(0)
                val clean = group?.replace(",", "")?.replace("$", "")?.replace("USD", "")?.replace("₹", "")?.replace("EUR", "")?.trim()
                val parsedVal = clean?.toDoubleOrNull()
                if (parsedVal != null && parsedVal > 0) {
                    extractedAmount = parsedVal
                    break
                }
            }
        }

        if (isOtp && extractedAmount == null) {
            return ParsedSmsResult(
                isTransaction = false,
                rawMessage = trimmed,
                rejectionReason = "Message identified as one-time verification code (OTP)."
            )
        }

        if (extractedAmount == null) {
            return ParsedSmsResult(
                isTransaction = false,
                rawMessage = trimmed,
                rejectionReason = "No monetary transaction amount found in text."
            )
        }

        // Determine Transaction Type
        val lowerText = trimmed.lowercase()
        var type = TransactionType.EXPENSE
        var hasExpenseWord = false
        var hasIncomeWord = false

        for (kw in EXPENSE_KEYWORDS) {
            if (lowerText.contains(kw)) {
                hasExpenseWord = true
                break
            }
        }

        for (kw in INCOME_KEYWORDS) {
            if (lowerText.contains(kw)) {
                hasIncomeWord = true
                break
            }
        }

        if (hasIncomeWord && !hasExpenseWord) {
            type = TransactionType.INCOME
        } else if (hasExpenseWord) {
            type = TransactionType.EXPENSE
        }

        // 1. First, check user-defined Custom Categories (Keywords & Category Name matching)
        var customMatchedCategory: CategoryEntity? = null
        for (customCat in customCategories) {
            // Check custom keywords
            for (keyword in customCat.keywordList) {
                if (lowerText.contains(keyword)) {
                    customMatchedCategory = customCat
                    break
                }
            }
            if (customMatchedCategory != null) break

            // Check custom category name itself if not generic
            val catNameLower = customCat.name.lowercase()
            if (catNameLower != "other" && lowerText.contains(catNameLower)) {
                customMatchedCategory = customCat
                break
            }
        }

        // Extract Merchant
        var merchant = "Unknown Merchant"
        var identifiedCategory: ExpenseCategory? = null

        for ((merchantList, cat) in KNOWN_MERCHANT_CATEGORIES) {
            for (m in merchantList) {
                if (lowerText.contains(m)) {
                    merchant = m.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                    identifiedCategory = cat
                    break
                }
            }
            if (identifiedCategory != null) break
        }

        if (merchant == "Unknown Merchant") {
            for (pattern in MERCHANT_PATTERNS) {
                val matcher = pattern.matcher(trimmed)
                if (matcher.find()) {
                    val candidate = matcher.group(1)?.trim()
                    if (!candidate.isNullOrBlank() && candidate.length > 2 && !candidate.equals("the", ignoreCase = true)) {
                        merchant = candidate.take(30).replace(Regex("[^a-zA-Z0-9&' ]"), "").trim()
                        break
                    }
                }
            }
        }

        if (merchant == "Unknown Merchant" || merchant.isBlank()) {
            merchant = if (type == TransactionType.INCOME) "Income Deposit" else "Card Expense"
        }

        // If a custom category matched, use it! Otherwise fallback to default category inference
        val finalCategoryName: String
        val finalDefaultCategory: ExpenseCategory

        if (customMatchedCategory != null) {
            finalCategoryName = customMatchedCategory.name
            finalDefaultCategory = ExpenseCategory.fromString(customMatchedCategory.name)
        } else {
            val fallback = identifiedCategory ?: inferCategory(lowerText, merchant, type)
            finalCategoryName = fallback.displayName
            finalDefaultCategory = fallback
        }

        // Account info
        var accountInfo = ""
        for (pattern in ACCOUNT_PATTERNS) {
            val matcher = pattern.matcher(trimmed)
            if (matcher.find()) {
                accountInfo = matcher.group(0)?.take(24)?.trim() ?: ""
                break
            }
        }
        if (accountInfo.isBlank()) {
            if (lowerText.contains("card")) accountInfo = "Card"
            else if (lowerText.contains("a/c") || lowerText.contains("account")) accountInfo = "Bank A/c"
            else accountInfo = "Account"
        }

        // Reference ID
        var referenceId = ""
        for (pattern in REF_PATTERNS) {
            val matcher = pattern.matcher(trimmed)
            if (matcher.find()) {
                referenceId = matcher.group(1)?.trim() ?: ""
                break
            }
        }

        val parsedDate = extractDate(trimmed) ?: defaultTimestamp

        val confidence = when {
            customMatchedCategory != null -> 0.99f
            identifiedCategory != null && referenceId.isNotEmpty() -> 0.98f
            identifiedCategory != null || hasExpenseWord || hasIncomeWord -> 0.90f
            else -> 0.75f
        }

        return ParsedSmsResult(
            isTransaction = true,
            amount = extractedAmount,
            currency = matchedCurrency,
            type = type,
            categoryName = finalCategoryName,
            category = finalDefaultCategory,
            merchant = merchant,
            accountInfo = accountInfo,
            referenceId = referenceId,
            timestamp = parsedDate,
            rawMessage = trimmed,
            confidence = confidence
        )
    }

    private fun inferCategory(text: String, merchant: String, type: TransactionType): ExpenseCategory {
        if (type == TransactionType.INCOME) {
            return ExpenseCategory.SALARY_INCOME
        }

        val combined = "$text $merchant".lowercase()
        return when {
            combined.contains("food") || combined.contains("dine") || combined.contains("eat") || combined.contains("meal") || combined.contains("coffee") -> ExpenseCategory.FOOD_DINING
            combined.contains("grocery") || combined.contains("market") || combined.contains("fresh") -> ExpenseCategory.GROCERIES
            combined.contains("shop") || combined.contains("store") || combined.contains("retail") || combined.contains("mall") -> ExpenseCategory.SHOPPING
            combined.contains("fuel") || combined.contains("gas") || combined.contains("ride") || combined.contains("cab") || combined.contains("car") || combined.contains("transit") -> ExpenseCategory.TRANSPORTATION
            combined.contains("bill") || combined.contains("power") || combined.contains("electric") || combined.contains("water") || combined.contains("wifi") || combined.contains("recharge") -> ExpenseCategory.BILLS_UTILITIES
            combined.contains("movie") || combined.contains("cinema") || combined.contains("game") || combined.contains("play") -> ExpenseCategory.ENTERTAINMENT
            combined.contains("med") || combined.contains("pharmacy") || combined.contains("clinic") || combined.contains("health") -> ExpenseCategory.HEALTH_MEDICAL
            combined.contains("flight") || combined.contains("hotel") || combined.contains("trip") || combined.contains("tour") -> ExpenseCategory.TRAVEL
            else -> ExpenseCategory.OTHER
        }
    }

    private fun extractDate(text: String): Long? {
        val dateFormats = listOf(
            "dd-MMM-yyyy", "dd-MMM-yy", "dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "dd-MM-yyyy"
        )
        val dateRegex = Pattern.compile("(?i)\\b(\\d{1,2}[\\-/](?:[a-zA-Z]{3}|\\d{1,2})[\\-/]\\d{2,4})\\b")
        val matcher = dateRegex.matcher(text)
        if (matcher.find()) {
            val dateStr = matcher.group(1) ?: return null
            for (format in dateFormats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    sdf.isLenient = false
                    val date = sdf.parse(dateStr)
                    if (date != null) return date.time
                } catch (_: Exception) {}
            }
        }
        return null
    }
}
