package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityExpenseListBinding
import com.example.myapplication.viewmodel.ExpenseViewModel
import com.example.myapplication.data.Expense
import java.text.SimpleDateFormat
import java.util.*

class ExpenseListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseListBinding
    private val expenseViewModel: ExpenseViewModel by viewModels()

    private lateinit var expenseAdapter: ExpenseAdapter
    private var currentExpenseList: List<Expense> = emptyList()
    private var userId: Int = 0

    private lateinit var addExpenseLauncher: ActivityResultLauncher<Intent>

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Gets the user ID from intent
        userId = intent.getIntExtra("USER_ID", 0)
        if (userId == 0) {
            Toast.makeText(this, "Invalid user. Please log in again.", Toast.LENGTH_LONG).show()
            finish()
        }

        // Gets the expense adapter
        expenseAdapter = ExpenseAdapter()
        binding.recyclerViewExpenses.apply {
            layoutManager = LinearLayoutManager(this@ExpenseListActivity)
            adapter = expenseAdapter
        }

        // Gets the expenses from the view model
        expenseViewModel.expenses.observe(this) { expenseList ->
            currentExpenseList = expenseList
            expenseAdapter.submitList(currentExpenseList)
        }
        expenseViewModel.loadExpenses(userId)

        addExpenseLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                data?.let {
                    val newExpense = it.getParcelableExtra("NEW_EXPENSE", Expense::class.java)
                    newExpense?.let { expense ->
                        expenseViewModel.addExpense(expense, userId)
                    }
                }
            }
        }


        // back button
        binding.toolBarHome.setNavigationOnClickListener {
            finish()
        }

        // Takes the user to the add expense page.
        binding.fabAddExpense.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("USER_ID", userId)
            addExpenseLauncher.launch(intent)
        }
    }

    // Creates the hamburger menu for sorting
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_expense_list, menu)
        return true
    }

    // Sorts the expenses by date or amount
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort_newest -> sortByDate(true)
            R.id.sort_oldest -> sortByDate(false)
            R.id.sort_highest -> sortByAmount(true)
            R.id.sort_lowest -> sortByAmount(false)
        }
        return true
    }

    private fun sortByDate(descending: Boolean) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sorted = currentExpenseList.sortedWith { a, b ->
            val dateA = sdf.parse(a.date) ?: Date(0)
            val dateB = sdf.parse(b.date) ?: Date(0)
            dateA.compareTo(dateB)
        }
        if (descending) expenseAdapter.submitList(sorted.reversed()) else expenseAdapter.submitList(sorted)
    }

    private fun sortByAmount(descending: Boolean) {
        val sorted = currentExpenseList.sortedBy { it.amount }
        if (descending) expenseAdapter.submitList(sorted.reversed()) else expenseAdapter.submitList(sorted)
    }
}