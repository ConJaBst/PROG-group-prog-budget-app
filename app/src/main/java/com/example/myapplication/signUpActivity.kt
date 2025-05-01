package com.example.myapplication



import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.User
import com.example.myapplication.data.UserDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt

class signUpActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Importing database as db and userDAO as userDAO
        db = AppDatabase.getDatabase(this)
        userDao = db.userDao()

        // Linking buttons on the view with declarations
        val btnSignUp = findViewById<Button>(R.id.btnCreateAccount)
        val etNewUsername = findViewById<EditText>(R.id.etNewUsername)
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)


        // When clicking the sign up button the program will read the username and password and create an account,
        // then it will go to the login activity.
        btnSignUp.setOnClickListener {
            val username = etNewUsername.text.toString().trim()
            val password = etNewPassword.text.toString().trim()

            // if either the username or password fields are empty then the sign up wont work.
            if (username.isNotEmpty() && password.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    // Checking if the username already exists and
                    try {
                        val existingUser = userDao.getUserByUsername(username)

                        // If everything is correct then the program will create the user and
                        // store the hashed password in the database. Then it will go to the login activity.
                        if (existingUser == null) {
                            val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
                            userDao.insertUser(User(username = username, password = hashedPassword))

                            withContext(Dispatchers.Main) {
                                Toast.makeText(applicationContext, "Account created!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@signUpActivity, loginActivity::class.java))
                                finish()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(applicationContext, "Username already exists", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(applicationContext, "Something went wrong. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
