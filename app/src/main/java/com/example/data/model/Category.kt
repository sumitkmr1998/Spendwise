package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class ExpenseCategory(
    val displayName: String,
    val color: Color,
    val iconName: String
) {
    FOOD_DINING("Food & Dining", ColorFood, "restaurant"),
    GROCERIES("Groceries", ColorGroceries, "shopping_cart"),
    SHOPPING("Shopping", ColorShopping, "shopping_bag"),
    TRANSPORTATION("Transportation", ColorTransport, "directions_car"),
    BILLS_UTILITIES("Bills & Utilities", ColorBills, "receipt_long"),
    ENTERTAINMENT("Entertainment", ColorEntertainment, "movie"),
    HEALTH_MEDICAL("Health & Medical", ColorHealth, "local_hospital"),
    TRAVEL("Travel", ColorTravel, "flight"),
    SALARY_INCOME("Income / Salary", ColorSalary, "account_balance_wallet"),
    OTHER("Other", ColorOther, "category");

    companion object {
        fun fromString(name: String?): ExpenseCategory {
            if (name == null) return OTHER
            return entries.firstOrNull { 
                it.name.equals(name, ignoreCase = true) || 
                it.displayName.equals(name, ignoreCase = true) 
            } ?: OTHER
        }
    }
}

enum class TransactionType {
    EXPENSE,
    INCOME;

    companion object {
        fun fromString(type: String?): TransactionType {
            return if (type?.uppercase() == "INCOME") INCOME else EXPENSE
        }
    }
}
