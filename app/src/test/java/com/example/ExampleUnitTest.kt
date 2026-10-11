package com.example

import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseEntity
import com.example.data.model.TransactionType
import com.example.export.GoogleSheetsExporter
import com.example.parser.SmsParser
import com.example.security.KeystoreCryptoManager
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun smsParser_parsesDebitExpenseCorrectly() {
        val message = "Chase Alert: You spent $86.45 at Trader Joe's on 08-Oct-2026 with debit card ending 4821. Ref: CH982314."
        val result = SmsParser.parse(message)

        assertTrue(result.isTransaction)
        assertEquals(86.45, result.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(ExpenseCategory.GROCERIES, result.category)
        assertTrue(result.merchant.contains("Trader Joe", ignoreCase = true))
        assertEquals("CH982314", result.referenceId)
    }

    @Test
    fun smsParser_matchesCustomUserDefinedCategoryKeywords() {
        val customCats = listOf(
            CategoryEntity(
                name = "Fitness & Gym",
                colorHex = "#10B981",
                iconName = "fitness",
                keywords = "planet fitness, gold's gym, workout, protein, equinox",
                isDefault = false
            ),
            CategoryEntity(
                name = "Pet Care",
                colorHex = "#F97316",
                iconName = "pets",
                keywords = "chewy, petco, petsmart, vet clinic",
                isDefault = false
            )
        )

        val gymMessage = "Alert: Paid $45.00 to Planet Fitness with Card 8821 on 10-Oct. Ref: PF1029."
        val gymResult = SmsParser.parse(gymMessage, customCategories = customCats)

        assertTrue(gymResult.isTransaction)
        assertEquals(45.00, gymResult.amount, 0.001)
        assertEquals("Fitness & Gym", gymResult.categoryName)

        val petMessage = "Your card was debited for $78.20 at Chewy.com on 09-Oct. Ref: CHW8812."
        val petResult = SmsParser.parse(petMessage, customCategories = customCats)

        assertTrue(petResult.isTransaction)
        assertEquals(78.20, petResult.amount, 0.001)
        assertEquals("Pet Care", petResult.categoryName)
    }

    @Test
    fun keystoreCryptoManager_encryptsAndDecryptsAccurately() {
        val sensitiveSms = "Confidential Bank Alert: A/c 98214 debited for $1,250.00 at Apple Store. Balance: $14,200. Ref: APPL8831."
        val encrypted = KeystoreCryptoManager.encrypt(sensitiveSms)

        assertNotNull(encrypted)
        assertNotEquals(sensitiveSms, encrypted) // Ciphertext is not plaintext

        val decrypted = KeystoreCryptoManager.decrypt(encrypted)
        assertEquals(sensitiveSms, decrypted)
    }

    @Test
    fun keystoreCryptoManager_duplicateHashDetectsIdenticalMessages() {
        val msg1 = "Chase: $42.50 spent at Uber on 10/10/2026 with Card 1234."
        val msg2 = "  Chase: $42.50 spent at Uber on 10/10/2026 with Card 1234.  \n"

        val hash1 = KeystoreCryptoManager.computeMessageHash(msg1)
        val hash2 = KeystoreCryptoManager.computeMessageHash(msg2)

        assertEquals(hash1, hash2) // Normalization detects identical messages
    }

    @Test
    fun smsParser_rejectsOtpMessageWithoutAmount() {
        val message = "Your verification code is 639102. Valid for 5 minutes. Do not share this OTP with anyone."
        val result = SmsParser.parse(message)

        assertFalse(result.isTransaction)
        assertNotNull(result.rejectionReason)
    }

    @Test
    fun googleSheetsExporter_generatesCsvAndTsvProperly() {
        val expense = ExpenseEntity.createEncrypted(
            id = 1,
            amount = 45.50,
            type = "EXPENSE",
            category = "Food & Dining",
            merchant = "Starbucks",
            accountInfo = "Card *1234",
            timestamp = 1760000000000L,
            rawMessage = "Spent $45.50 at Starbucks",
            referenceId = "REF123"
        )

        val csv = GoogleSheetsExporter.generateCsvContent(listOf(expense), "This Month")
        assertTrue(csv.contains("Starbucks"))
        assertTrue(csv.contains("45.50"))
        assertTrue(csv.contains("Food & Dining"))
        assertTrue(csv.contains("SPENDWISE EXPENDITURE REPORT"))

        val tsv = GoogleSheetsExporter.generateTsvForClipboard(listOf(expense))
        assertTrue(tsv.contains("Starbucks\t"))
        assertTrue(tsv.contains("45.50\t"))
    }
}
