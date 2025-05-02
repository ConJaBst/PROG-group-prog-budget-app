package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.viewmodel.ExpenseViewModel
import com.google.android.material.appbar.MaterialToolbar

class homeActivity : AppCompatActivity() {

    private lateinit var homeToolbar: MaterialToolbar
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSummary: TextView
    private lateinit var btnSetBudget: Button

    private var budgetAmount = 0
    private var userId: Int = -1

    private lateinit var addExpenseLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // The userID is passed over from the login activity and stored for future use.
        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Toast.makeText(this, "Invalid user. Please log in again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Linking buttons on the view with declarations
        homeToolbar = findViewById(R.id.toolBarHome)
        progressBar = findViewById(R.id.progressBar)
        tvSummary = findViewById(R.id.tvSummary)
        btnSetBudget = findViewById(R.id.btnSetBudget)

        // sets up the toolbar
        setSupportActionBar(homeToolbar)
        setupToolbarMenu()

        // grabs the viewmodel
        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        // gets and sets the budget amount from the shared preferences
        budgetAmount = BudgetPrefs.getBudget(this)
        viewModel.monthlyTotal.observe(this) { total ->
            updateBudgetUI(total ?: 0.0)
        }

        // loads expenses for the current user
        viewModel.loadExpenses(userId)

        // open the budget dialog
        btnSetBudget.setOnClickListener {
            showSetBudgetDialog()
        }


        addExpenseLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.loadExpenses(userId)
            }
        }
    }

    // Toolbar code, this is used to navigate the app.
    private fun setupToolbarMenu() {
        homeToolbar.setNavigationOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.menuInflater.inflate(R.menu.main_menu, popupMenu.menu)

            // sets up the fields
            try {
                val fields = popupMenu.javaClass.declaredFields
                for (field in fields) {
                    if (field.name == "mPopup") {
                        field.isAccessible = true
                        val menuPopupHelper = field.get(popupMenu)
                        val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                        val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
                        setForceIcons.invoke(menuPopupHelper, true)
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_settings -> {
                        // TODO: Implement settings in part 3
                        Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_rewards -> {
                        // TODO: Implement rewards and gamify in part 3
                        Toast.makeText(this, "Rewards clicked", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_logout -> {
                        // Takes the user to the login screen.
                        startActivity(Intent(this, loginActivity::class.java))
                        finish()
                        true
                    }
                    R.id.action_add_expense -> {
                        // Takes the user to the add expense screen.
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("USER_ID", userId)
                        addExpenseLauncher.launch(intent)
                        true
                    }
                    R.id.action_expense_list -> {
                        // Takes the user to the expense list screen.
                        val intent = Intent(this, ExpenseListActivity::class.java)
                        intent.putExtra("USER_ID", userId)
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateBudgetUI(totalSpent: Double) {
        budgetAmount = BudgetPrefs.getBudget(this)

        if (budgetAmount <= 0) {
            progressBar.progress = 0
            val text = "You have not set a budget yet. Set your budget to start tracking."
            tvSummary.text = text
            return
        }

        val percent = ((totalSpent / budgetAmount) * 100).toInt().coerceAtMost(100)
        progressBar.progress = percent

        val colorRes = when {
            percent < 50 -> R.color.green
            percent < 90 -> R.color.yellow
            else -> R.color.red
        }
        progressBar.progressDrawable.setTint(ContextCompat.getColor(this, colorRes))

        val remaining = budgetAmount - totalSpent
        val text = "You've spent R${String.format("%.2f", totalSpent)} " +
                "of your R${String.format("%.2f", budgetAmount.toDouble())} budget. Remaining: R${String.format("%.2f", remaining)}"
        tvSummary.text = text
    }

    private fun showSetBudgetDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Monthly Budget")

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Enter amount (e.g., 5000)"
        }
        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val newBudget = input.text.toString().toIntOrNull()
            if (newBudget != null && newBudget > 0) {
                BudgetPrefs.setBudget(this, newBudget)
                viewModel.loadExpenses(userId)
            } else {
                Toast.makeText(this, "Please enter a valid budget.", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }



}
