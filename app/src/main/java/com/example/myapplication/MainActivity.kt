package com.example.myapplication

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.CategoryDao
import com.example.myapplication.data.Expense
import com.example.myapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var binding: ActivityMainBinding
    private var selectedAmount: Int = 0
    private lateinit var selectedCategory: String
    private var userId: Int = 0
    private val calendar: Calendar = Calendar.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        categoryDao = db.categoryDao()

        // grabs the userID from the intent
        userId = intent.getIntExtra("USER_ID", 0)
        if (userId == 0) {
            Toast.makeText(this, "Invalid user session", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Back button
        binding.toolBarHome.setNavigationOnClickListener {
            finish()
        }

        // grabs categories, declares categories and populates the spinner.
        lifecycleScope.launch {
            val categories = getCategoriesList(userId)
            val adapter =
                ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, categories)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinner.adapter = adapter

            binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, position: Int, id: Long
                ) {
                    selectedCategory = categories[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        // Declares and sets up the seekbar
        binding.seekAmount.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedAmount = progress
                val text = "Amount: R $selectedAmount"
                binding.tvAmount.text = text
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // sets up the date picker
        binding.editTextText.setOnClickListener {
            showDatePicker()
        }



        binding.btnAddCategory.setOnClickListener {
            val intent = Intent(this, AddCategoryActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        // When the save button is pressed, all the categories are checked and if everything is good,
        // then the expense is added to the database.
        binding.btnSaveExpense.setOnClickListener {
            val description = binding.editTextTextMultiLine.text.toString()
            val date = binding.editTextText.text.toString()

            // checks if either the description, date or category is blank.
            if (description.isBlank() || date.isBlank() || selectedCategory.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Adds expense to database
            val newExpense = Expense(
                amount = selectedAmount.toDouble(),
                category = selectedCategory,
                description = description,
                date = date,
                userId = userId
            )

            // Takes us back to the home screen
            val resultIntent = Intent()
            resultIntent.putExtra("NEW_EXPENSE", newExpense) // Parcelable
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCategorySpinner()
    }

    private fun refreshCategorySpinner() {
        lifecycleScope.launch {
            val categories = getCategoriesList(userId)
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, categories)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinner.adapter = adapter

            binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    selectedCategory = categories[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(Calendar.YEAR, selectedYear)
                calendar.set(Calendar.MONTH, selectedMonth)
                calendar.set(Calendar.DAY_OF_MONTH, selectedDay)

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.editTextText.setText(sdf.format(calendar.time))
            }, year, month, day)

        datePickerDialog.show()
    }


    private suspend fun getCategoriesList(userId: Int): List<String> {
        val out = categoryDao.getAllNamesForUser(userId)

        return out
    }
}