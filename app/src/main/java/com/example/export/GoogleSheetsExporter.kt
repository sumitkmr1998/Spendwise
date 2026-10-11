package com.example.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.ExpenseEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object GoogleSheetsExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val fullDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    /**
     * Builds full Google Sheets compatible CSV with Detailed Transactions & Summary Report.
     */
    fun generateCsvContent(
        expenses: List<ExpenseEntity>,
        dateRangeLabel: String = "All Time"
    ): String {
        val sb = StringBuilder()

        val totalExpenses = expenses.filter { it.isExpense }.sumOf { it.amount }
        val totalIncome = expenses.filter { !it.isExpense }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpenses

        // 1. Executive Summary Table for Google Sheets
        sb.append("=== SPENDWISE EXPENDITURE REPORT FOR GOOGLE SHEETS ===\n")
        sb.append("Report Date,${escapeCsv(fullDateTimeFormat.format(Date()))}\n")
        sb.append("Date Range Filter,${escapeCsv(dateRangeLabel)}\n")
        sb.append("Total Transactions,${expenses.size}\n")
        sb.append("Total Expenses,${String.format(Locale.US, "%.2f", totalExpenses)}\n")
        sb.append("Total Income,${String.format(Locale.US, "%.2f", totalIncome)}\n")
        sb.append("Net Balance,${String.format(Locale.US, "%.2f", netBalance)}\n\n")

        // 2. Category Summary Section
        sb.append("CATEGORY,TOTAL SPENT,PERCENTAGE,COUNT\n")
        val categoryGroups = expenses.filter { it.isExpense }.groupBy { it.category }
        for ((category, list) in categoryGroups.entries.sortedByDescending { it.value.sumOf { e -> e.amount } }) {
            val catTotal = list.sumOf { it.amount }
            val pct = if (totalExpenses > 0) (catTotal / totalExpenses) * 100 else 0.0
            sb.append("${escapeCsv(category)},${String.format(Locale.US, "%.2f", catTotal)},${String.format(Locale.US, "%.1f%%", pct)},${list.size}\n")
        }
        sb.append("\n")

        // 3. Detailed Transactions Table
        sb.append("Date,Time,Merchant,Category,Type,Amount,Currency,Account/Method,Reference ID,Notes,Raw SMS Alert\n")
        for (item in expenses.sortedByDescending { it.timestamp }) {
            val dateStr = dateFormat.format(Date(item.timestamp))
            val timeStr = timeFormat.format(Date(item.timestamp))
            sb.append("${escapeCsv(dateStr)},")
            sb.append("${escapeCsv(timeStr)},")
            sb.append("${escapeCsv(item.merchant)},")
            sb.append("${escapeCsv(item.category)},")
            sb.append("${escapeCsv(item.type)},")
            sb.append("${String.format(Locale.US, "%.2f", item.amount)},")
            sb.append("${escapeCsv(item.currency)},")
            sb.append("${escapeCsv(item.accountInfoDecrypted)},")
            sb.append("${escapeCsv(item.referenceIdDecrypted)},")
            sb.append("${escapeCsv(item.notes)},")
            sb.append("${escapeCsv(item.rawMessageDecrypted)}\n")
        }

        return sb.toString()
    }

    /**
     * Builds Tab-Separated Values (TSV) which pastes directly into Google Sheets cells cleanly.
     */
    fun generateTsvForClipboard(expenses: List<ExpenseEntity>): String {
        val sb = StringBuilder()
        sb.append("Date\tTime\tMerchant\tCategory\tType\tAmount\tCurrency\tAccount\tRef ID\tNotes\n")
        for (item in expenses.sortedByDescending { it.timestamp }) {
            val dateStr = dateFormat.format(Date(item.timestamp))
            val timeStr = timeFormat.format(Date(item.timestamp))
            sb.append("$dateStr\t")
            sb.append("$timeStr\t")
            sb.append("${item.merchant}\t")
            sb.append("${item.category}\t")
            sb.append("${item.type}\t")
            sb.append("${String.format(Locale.US, "%.2f", item.amount)}\t")
            sb.append("${item.currency}\t")
            sb.append("${item.accountInfoDecrypted}\t")
            sb.append("${item.referenceIdDecrypted}\t")
            sb.append("${item.notes}\n")
        }
        return sb.toString()
    }

    /**
     * Writes CSV file to cache and returns Intent for sharing / opening with Google Sheets.
     */
    fun exportAndShareGoogleSheets(
        context: Context,
        expenses: List<ExpenseEntity>,
        dateRangeLabel: String = "All Time"
    ): Intent {
        val csvData = generateCsvContent(expenses, dateRangeLabel)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "SpendWise_Expenses_$timeStamp.csv"

        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val file = File(exportDir, fileName)
        FileWriter(file).use { writer ->
            writer.write(csvData)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "SpendWise Expense Report ($dateRangeLabel)")
            putExtra(Intent.EXTRA_TEXT, "Here is the expenditure report exported for Google Sheets from SpendWise.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(intent, "Open or Export with Google Sheets")
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Google Sheets TSV") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    private fun escapeCsv(value: String): String {
        var str = value.replace("\r", " ").replace("\n", " ")
        if (str.contains(",") || str.contains("\"")) {
            str = str.replace("\"", "\"\"")
            return "\"$str\""
        }
        return str
    }
}
