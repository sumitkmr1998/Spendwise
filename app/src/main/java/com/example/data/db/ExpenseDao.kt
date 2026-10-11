package com.example.data.db

import androidx.room.*
import com.example.data.model.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>): List<Long>

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpensesFlow(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getExpensesBetweenFlow(startTime: Long, endTime: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getExpensesBetween(startTime: Long, endTime: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpenses(): List<ExpenseEntity>

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getExpensesCount(): Int

    // Duplicate detection queries
    @Query("SELECT * FROM expenses WHERE messageHash = :hash LIMIT 1")
    suspend fun findByMessageHash(hash: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE referenceId = :refId AND referenceId != '' LIMIT 1")
    suspend fun findByReferenceId(refId: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE amount = :amount AND LOWER(merchant) = LOWER(:merchant) AND timestamp >= :startTime AND timestamp <= :endTime LIMIT 1")
    suspend fun findPossibleDuplicate(amount: Double, merchant: String, startTime: Long, endTime: Long): ExpenseEntity?

    @Query("UPDATE expenses SET category = :newCategory WHERE category = :oldCategory")
    suspend fun reassignCategory(oldCategory: String, newCategory: String)
}
