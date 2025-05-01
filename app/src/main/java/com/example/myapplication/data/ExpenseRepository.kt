package com.example.myapplication.data

class ExpenseRepository(private val dao: ExpenseDao) {
    suspend fun insertExpense(expense: Expense) = dao.insertExpense(expense)
    suspend fun getExpenses(userId: Int) = dao.getAllExpenses(userId)
    suspend fun getMonthlyTotal(month: String, year: String, userId: Int): Double {
        return dao.getMonthlyTotal(month, year, userId) ?: 0.0
    }
    suspend fun getExpensesByCategory(category: String, userId: Int) = dao.getExpensesByCategory(category, userId)
    suspend fun getExpensesSortedByAmountAsc(userId: Int) = dao.getExpensesSortedByAmountAsc(userId)
    suspend fun getExpensesSortedByAmountDesc(userId: Int) = dao.getExpensesSortedByAmountDesc(userId)
    suspend fun getExpensesByDateRange(startDate: String, endDate: String, userId: Int) = dao.getExpensesByDateRange(startDate, endDate, userId)
    suspend fun deleteExpense(expense: Expense) = dao.deleteExpense(expense)
}











