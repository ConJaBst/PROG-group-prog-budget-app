package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.UserDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt

class loginActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Importing database as db and userDAO as userDAO
        db = AppDatabase.getDatabase(this)
        userDao = db.userDao()

        // Linking buttons on the view with declarations
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnSignUp = findViewById<Button>(R.id.button)

        // When clicking the login button the program will read the username and password and check if it is correct,
        // Then it will take the user into the program.
        btnLogin.setOnClickListener {
            val username = findViewById<EditText>(R.id.etUsername).text.toString()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()

            // if either the username or password fields are empty then the login wont work.
            if (username.isNotEmpty() && password.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val user = userDao.getUserByUsername(username)
                    withContext(Dispatchers.Main) {
                        // Checks if the entered password's hash matched with the stored hash in the database.
                        if (user != null && BCrypt.checkpw(password, user.password)) {
                            Toast.makeText(applicationContext, "Login successful!", Toast.LENGTH_SHORT).show()

                            // If everything is correct then the program will go to the home activity with the userID stored.
                            val intent = Intent(this@loginActivity,  homeActivity::class.java)
                            intent.putExtra("USER_ID", user.id)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(applicationContext, "Invalid credentials", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // If the sign up button is pressed the user is taken to the sign up view.
        btnSignUp.setOnClickListener {
            startActivity(Intent(this, signUpActivity::class.java))
        }
    }
}


