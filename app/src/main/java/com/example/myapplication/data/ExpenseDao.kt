package com.example.myapplication.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllExpenses(userId: Int): List<Expense>

    @Query(
        """
        SELECT SUM(amount)
        FROM expenses
        WHERE strftime('%m', date) = :month
        AND strftime('%Y', date) = :year
        AND userId = :userId
        """
    )
    suspend fun getMonthlyTotal(month: String, year: String, userId: Int): Double?

    @Query("SELECT * FROM expenses WHERE category = :category AND userId = :userId ORDER BY date DESC")
    suspend fun getExpensesByCategory(category: String, userId: Int): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY amount ASC")
    suspend fun getExpensesSortedByAmountAsc(userId: Int): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY amount DESC")
    suspend fun getExpensesSortedByAmountDesc(userId: Int): List<Expense>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate AND userId = :userId ORDER BY date DESC")
    suspend fun getExpensesByDateRange(startDate: String, endDate: String, userId: Int): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date ASC")
    suspend fun getExpensesByDateAsc(userId: Int): List<Expense>

    @Query("SELECT * FROM expenses WHERE strftime('%m', date) = :month AND strftime('%Y', date) = :year AND userId = :userId")
    suspend fun getExpensesByMonth(month: String, year: String, userId: Int): List<Expense>

    //Gets the total amount spent by a user
    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId")
    suspend fun getTotalAmountSpent(userId: Int): Double?

    @Delete
    suspend fun deleteExpense(expense: Expense)
}

