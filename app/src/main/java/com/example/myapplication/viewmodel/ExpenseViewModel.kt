package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.myapplication.data.Expense
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.data.AppDatabase
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).expenseDao()
    private val repo = ExpenseRepository(dao)

    private val _expenses = MutableLiveData<List<Expense>>()
    val expenses: LiveData<List<Expense>> = _expenses

    private val _monthlyTotal = MutableLiveData<Double>()
    val monthlyTotal: LiveData<Double> = _monthlyTotal

    fun addExpense(expense: Expense, userId: Int) {
        viewModelScope.launch {
            repo.insertExpense(expense)
            loadExpenses(userId)
        }
    }

    fun loadExpenses(userId: Int) {
        viewModelScope.launch {
            val expenseList = repo.getExpenses(userId)
            _expenses.value = expenseList
            // update total
            val calendar = Calendar.getInstance()
            val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
            val year = calendar.get(Calendar.YEAR).toString()
            val total = repo.getMonthlyTotal(month, year, userId)
            _monthlyTotal.value = total
        }
    }
}