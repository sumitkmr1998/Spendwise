package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.security.KeystoreCryptoManager

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["messageHash"]),
        Index(value = ["referenceId"]),
        Index(value = ["timestamp"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val currency: String = "$",
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val category: String = "OTHER",
    val merchant: String = "Unknown",
    val accountInfo: String = "", // Stored encrypted in DB
    val timestamp: Long = System.currentTimeMillis(),
    val rawMessage: String = "",  // Stored encrypted in DB
    val referenceId: String = "", // Stored encrypted in DB
    val messageHash: String = "", // SHA-256 for duplicate message filtering
    val notes: String = "",
    val isFlagged: Boolean = false
) {
    val isExpense: Boolean
        get() = type.equals("EXPENSE", ignoreCase = true)

    val expenseCategory: ExpenseCategory
        get() = ExpenseCategory.fromString(category)

    // Decrypted accessors
    val rawMessageDecrypted: String
        get() = KeystoreCryptoManager.decrypt(rawMessage)

    val accountInfoDecrypted: String
        get() = KeystoreCryptoManager.decrypt(accountInfo)

    val referenceIdDecrypted: String
        get() = KeystoreCryptoManager.decrypt(referenceId)

    companion object {
        /**
         * Factory method to create an ExpenseEntity with sensitive fields encrypted via Android Keystore
         */
        fun createEncrypted(
            id: Long = 0,
            amount: Double,
            currency: String = "$",
            type: String = "EXPENSE",
            category: String = "OTHER",
            merchant: String = "Unknown",
            accountInfo: String = "",
            timestamp: Long = System.currentTimeMillis(),
            rawMessage: String = "",
            referenceId: String = "",
            notes: String = "",
            isFlagged: Boolean = false
        ): ExpenseEntity {
            val hash = if (rawMessage.isNotBlank()) {
                KeystoreCryptoManager.computeMessageHash(rawMessage)
            } else ""

            return ExpenseEntity(
                id = id,
                amount = amount,
                currency = currency,
                type = type,
                category = category,
                merchant = merchant,
                accountInfo = KeystoreCryptoManager.encrypt(accountInfo),
                timestamp = timestamp,
                rawMessage = KeystoreCryptoManager.encrypt(rawMessage),
                referenceId = KeystoreCryptoManager.encrypt(referenceId),
                messageHash = hash,
                notes = notes,
                isFlagged = isFlagged
            )
        }
    }
}
