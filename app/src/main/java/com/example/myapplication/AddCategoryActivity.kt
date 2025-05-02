package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.Category
import com.example.myapplication.data.CategoryDao
import kotlinx.coroutines.launch

class AddCategoryActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private var userId: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_category)

        db = AppDatabase.getDatabase(this)
        categoryDao = db.categoryDao()
        userId = intent.getIntExtra("USER_ID", -1)

        val etCategory = findViewById<EditText>(R.id.etCategoryName)
        val btnSave = findViewById<Button>(R.id.btnSaveCategory)

        btnSave.setOnClickListener {
            val category = etCategory.text.toString()
            if (category.isNotEmpty()) {
                lifecycleScope.launch {
                    val newCategory = Category(name = category, userId = userId)
                    categoryDao.insert(newCategory)

                    val all = categoryDao.getAllNamesForUser(userId) // Add this line
                    println("Categories for user: $userId")              // Debug output


                    runOnUiThread {
                        Toast.makeText(this@AddCategoryActivity, "Saved: $category", Toast.LENGTH_SHORT).show()
                        etCategory.text.clear()
                    }
                    finish()
                }
            } else {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
