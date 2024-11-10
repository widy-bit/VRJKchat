package com.example.netardedrigger

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
//    private lateinit var chatBotButton: Button // Declare the Chatbot button

    private lateinit var dbHelper: DBHelper
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        loginButton = findViewById(R.id.buttonLogin)
        registerButton = findViewById(R.id.buttonRegister)
//        chatBotButton = findViewById(R.id.buttonChatbot)

        // Initialize SharedPreferences and DBHelper
        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        dbHelper = DBHelper(this)

        // Retrieve the saved username if it exists
        val savedUsername = sharedPreferences.getString("username", "")
        val savedPassword = sharedPreferences.getString("password", "")
        if (!savedUsername.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
            editTextUsername.setText(savedUsername)
            editTextPassword.setText(savedPassword)
            Toast.makeText(this, "Welcome back, $savedUsername!", Toast.LENGTH_SHORT).show()
        }

        // Handle login button click
        loginButton.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            } else {
                val hashedPassword = hashPassword(password)
                if (dbHelper.checkUser(username, hashedPassword)) {
                    // Save the username in SharedPreferences for "Remember Username"
                    val editor: SharedPreferences.Editor = sharedPreferences.edit()
                    editor.putString("username", username)
                    editor.putString("password", password)
                    editor.apply()

                    // Show welcome message
                    Toast.makeText(this, "Welcome, $username!", Toast.LENGTH_SHORT).show()

                    // Navigate to the next activity
                    val intent = Intent(this, secondAdn::class.java)
                    startActivity(intent)

                } else {
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Handle register button click
        registerButton.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                val hashedPassword = hashPassword(password)
                if (dbHelper.insertUser(username, hashedPassword)) {
                    Toast.makeText(this, "User registered successfully!", Toast.LENGTH_SHORT).show()

                    val editor = sharedPreferences.edit()
                    editor.putString("username", username)
                    editor.putString("password", password)
                    editor.apply()
                } else {
                    Toast.makeText(this, "Registration failed, user may already exist", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a username and password", Toast.LENGTH_SHORT).show()
            }
        }
//on pai
        // Chatbot button click
//        chatBotButton.setOnClickListener {
//            val intent = Intent(this, ChatbotActivity::class.java) // Start ChatbotActivity
//            startActivity(intent)
//        }
    }



    // Function to hash passwords using SHA-256
    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(password.toByteArray())
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }
}
