package com.example.netardedrigger

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.netardedrigger.databinding.ActivityChatbotBinding
import java.io.File
import kotlin.math.sqrt
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.media.MediaPlayer
import android.media.MediaRecorder
import java.io.IOException



class ChatbotActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityChatbotBinding
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var dbHelper: DBHelper
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeThreshold = 15f
    private var lastShakeTime: Long = 0
    private val REQUEST_PERMISSION_BT = 1001

    // Constants for Camera
    private val CAMERA_PERMISSION_CODE = 100
    private val CAMERA_REQUEST_CODE = 101

    // Constants for Voice Note
    private var mediaRecorder: MediaRecorder? = null
    private var outputFilePath: String = ""
    private val RECORD_AUDIO_PERMISSION_CODE = 200

    private val TAG = "ChatbotActivity"
    private var isFabMenuOpen = false
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize components
        initializeComponents()
        setupFabMenu()
        setupClickListeners()
    }

    private fun initializeComponents() {
        // Initialize Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
        }

        // Register for Bluetooth discovery broadcasts
        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))

        // Initialize database helper
        dbHelper = DBHelper(this)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("ChatPrefs", MODE_PRIVATE)

        // Load saved username
        val savedUsername = sharedPreferences.getString("username", "")
        binding.usernameTextView.text = savedUsername

        // Initialize sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Load chat history
//        loadChatHistory()
    }

    private fun setupFabMenu() {
        // Initially hide all FABs except the menu FAB
        binding.clearChatFab.visibility = View.GONE
        binding.bluetoothFab.visibility = View.GONE
        binding.cameraFab.visibility = View.GONE
        binding.voiceNoteFab.visibility = View.GONE
    }

    private fun setupClickListeners() {
        // Menu FAB click listener
        binding.menuFab.setOnClickListener {
            toggleFabMenu()
        }

        // Clear Chat FAB click listener
        binding.clearChatFab.setOnClickListener {
            clearChat()
            toggleFabMenu()
        }

        // Bluetooth FAB click listener
        binding.bluetoothFab.setOnClickListener {
            val intent = Intent(this, DevicesActivity::class.java)
            startActivity(intent)
            toggleFabMenu()
        }

        // Camera FAB click listener
        binding.cameraFab.setOnClickListener {
            askCameraPermission()
            toggleFabMenu()
        }
        // Voice Note FAB click listener
        binding.voiceNoteFab.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                askAudioPermission()
            }
            isRecording = !isRecording
        }
        // Send button click listener
        binding.sendButton.setOnClickListener {
            sendMessage()
        }
    }


//start voice
    private var mediaPlayer: MediaPlayer? = null

    private fun askAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        outputFilePath = "${externalCacheDir?.absolutePath}/voice_note_${System.currentTimeMillis()}.3gp"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFilePath)
        }
        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            reset()
            release()
        }
        mediaRecorder = null
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()

        // Save the voice note path in the database
        val messageText = "You sent a voice note"
        dbHelper.insertMessage(messageText, outputFilePath)

        // Display the voice note in chat
        displayVoiceNoteMessage(messageText, outputFilePath)
    }

    private fun displayVoiceNoteMessage(message: String, filePath: String) {
        val textView = TextView(this).apply {
            text = message
            setBackgroundResource(R.drawable.bubble_sent)
            setPadding(16, 16, 16, 16)
            setOnClickListener {
                if (mediaPlayer?.isPlaying == true) {
                    stopVoiceNotePlayback()
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0)
                } else {
                    startVoiceNotePlayback(filePath)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0)
                }
            }
        }
//end voice
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.END
        textView.layoutParams = layoutParams
        binding.chatLayout.addView(textView)

        // Scroll to the bottom of the chat
        binding.chatScrollView.post {
            binding.chatScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun startVoiceNotePlayback(filePath: String) {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()
        }
    }

    private fun stopVoiceNotePlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        mediaPlayer?.release()
        mediaPlayer = null
    }




    private fun
            toggleFabMenu() {
        isFabMenuOpen = !isFabMenuOpen
        if (isFabMenuOpen) {
            showFabMenu()
        } else {
            closeFabMenu()
        }
    }

    private fun showFabMenu() {
        binding.clearChatFab.show()
        binding.bluetoothFab.show()
        binding.cameraFab.show()
        binding.voiceNoteFab.show()

        // Animasi membuka dari bawah ke atas
        binding.clearChatFab.animate().translationY(-120f)
        binding.bluetoothFab.animate().translationY(-240f)
        binding.cameraFab.animate().translationY(-360f)
        binding.voiceNoteFab.animate().translationY(-480f)
        // Rotasi ikon menu
        binding.menuFab.animate().rotation(45f)
        isFabMenuOpen = true
    }

    private fun closeFabMenu() {
        // Animasi menutup dari atas ke bawah
        binding.clearChatFab.animate().translationY(0f).withEndAction {
            binding.clearChatFab.hide()
        }
        binding.bluetoothFab.animate().translationY(0f).withEndAction {
            binding.bluetoothFab.hide()
        }
        binding.cameraFab.animate().translationY(0f).withEndAction {
            binding.cameraFab.hide()
        }
        binding.voiceNoteFab.animate().translationY(0f).withEndAction {
            binding.voiceNoteFab.hide()  // Hide voiceNoteFab
        }

        // Kembalikan rotasi ikon menu
        binding.menuFab.animate().rotation(0f)
        isFabMenuOpen = false
    }    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        try {
                            // Check if permissions are granted
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {

                                // Display the name of the discovered device
                                val deviceName = it.name ?: "Unknown device"
                                Toast.makeText(context, "Discovered device: $deviceName", Toast.LENGTH_SHORT).show()
                            } else {
                                // Handle case where permissions are not granted
                                Toast.makeText(context, "Bluetooth permissions are not granted", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: SecurityException) {
                            // Handle any SecurityException that may occur
                            Toast.makeText(context, "Error accessing Bluetooth device information: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Toast.makeText(this@ChatbotActivity, "Discovery finished.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun askCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            displayCapturedImage(imageBitmap)
        }
    }

    private fun displayCapturedImage(imageBitmap: Bitmap) {
        val imageView = ImageView(this)
        imageView.setImageBitmap(imageBitmap)

        // Save the image to the device and get its URI
        val imageUri = saveImageToStorage(imageBitmap)

        // Store the image URI in the database
        dbHelper.insertMessage("You sent an image", imageUri.toString())

        // Add the image to the chat layout
        binding.chatLayout.addView(imageView)

        // Optionally scroll to the bottom
        binding.chatScrollView.post {
            binding.chatScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun saveImageToStorage(image: Bitmap): Uri {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        val fos = openFileOutput(filename, Context.MODE_PRIVATE)
        image.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        fos.close()
        return Uri.fromFile(File(filesDir, filename))
    }

    private fun sendMessage() {
        val message = binding.inputBox.text.toString()
        if (message.isNotEmpty()) {
            // Save the message to the database
            dbHelper.insertMessage("You: $message", null)

            // Display the message
            displaySentMessage(message)

            // Clear the input field after sending
            binding.inputBox.text.clear()

            // Optionally scroll to the bottom
            binding.chatScrollView.post {
                binding.chatScrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    private fun displaySentMessage(message: String) {
        val textView = TextView(this)
        textView.text = message
        textView.setBackgroundResource(R.drawable.bubble_sent)
        textView.setPadding(16, 16, 16, 16)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.END
        textView.layoutParams = layoutParams
        binding.chatLayout.addView(textView)
    }

    private fun displayReceivedMessage(message: String) {
        val textView = TextView(this)
        textView.text = message
        textView.setBackgroundResource(R.drawable.bubble_received)
        textView.setPadding(16, 16, 16, 16)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.START
        textView.layoutParams = layoutParams
        binding.chatLayout.addView(textView)
    }

    private fun clearChat() {
        binding.chatLayout.removeAllViews()
        dbHelper.clearMessages()
    }

//    private fun loadChatHistory() {
//        val chatHistory = dbHelper.getAllMessages()
//        for (message in chatHistory) {
//            if (message.isSent) {
//                displaySentMessage(message.text)
//            } else {
//                displayReceivedMessage(message.text)
//            }
//        }
//    }

    // Sensor handling for shake detection
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val accelerationMagnitude = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

                if (accelerationMagnitude > shakeThreshold) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastShakeTime > 1000) {
                        lastShakeTime = currentTime
                        onShakeDetected()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun onShakeDetected() {
        Toast.makeText(this, "Shake detected!", Toast.LENGTH_SHORT).show()
        clearChat() // Example action: clear chat on shake
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}