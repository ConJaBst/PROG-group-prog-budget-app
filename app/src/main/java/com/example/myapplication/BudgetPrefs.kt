package com.example.myapplication

import android.content.Context

object BudgetPrefs {
    private const val PREF_NAME = "budget_prefs"
    private const val KEY_BUDGET = "budget_amount"

    fun setBudget(context: Context, amount: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_BUDGET, amount).apply()
    }

    fun getBudget(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_BUDGET, 0)
    }
}
