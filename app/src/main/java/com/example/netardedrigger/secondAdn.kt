package com.example.netardedrigger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class secondAdn : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second_adn)

        // Find the VideoView by its ID
        val videoView: VideoView = findViewById(R.id.videoView)

        // Set the video path from the raw directory
        val videoUri: Uri = Uri.parse("android.resource://" + packageName + "/" + R.raw.videotest2)

        // Set the video URI and start the video
        videoView.setVideoURI(videoUri)
        videoView.start()

        // Set up the Back to Main Menu button
        val backButton: Button = findViewById(R.id.button4)
        backButton.setOnClickListener {
            // Finish the current activity and go back to MainActivity
            finish()
        }

        // Set up the CHATBOT button
        val chatBotButton: Button = findViewById(R.id.button7)
        chatBotButton.setOnClickListener {
            // Create an Intent to navigate to ChatbotActivity
            val intent = Intent(this, ChatbotActivity::class.java)
            startActivity(intent)
        }
    }
}
