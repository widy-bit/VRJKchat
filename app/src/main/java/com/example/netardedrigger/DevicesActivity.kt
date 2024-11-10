package com.example.netardedrigger

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class DevicesActivity : AppCompatActivity() {


    // Core Bluetooth components
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var devicesListView: ListView
    private lateinit var devicesListAdapter: ArrayAdapter<String>
    private lateinit var statusTextView: TextView
    private val devicesList = ArrayList<String>()
    private val discoveredDevices = HashMap<String, BluetoothDevice>()

    // Collections to store discovered devices
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {

                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        try {
                            if (hasRequiredPermissions()) {

                                val deviceName = if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                    it.name ?: "Unknown device"
                                } else {
                                    "Unknown device"
                                }
                                val deviceAddress = it.address
                                val deviceDisplayString = "$deviceName\n$deviceAddress"

                                discoveredDevices[deviceAddress] = it

                                if (!devicesList.contains(deviceDisplayString)) {
                                    devicesList.add(deviceDisplayString)
                                    devicesListAdapter.notifyDataSetChanged()
                                    statusTextView.text = "Found ${devicesList.size} devices"
                                }
                            }
                        } catch (e: SecurityException) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                // Handle completion of device discovery process
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    findViewById<Button>(R.id.scanButton).isEnabled = true
                    if (devicesList.isEmpty()) {
                        statusTextView.text = "No devices found"
                    }
                    Toast.makeText(context, "Search completed", Toast.LENGTH_SHORT).show()
                }

                // Handle changes in device bonding/pairing state
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {

                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)) {
                        BluetoothDevice.BOND_BONDED -> {
                            // Successfully paired with device
                            val deviceName = if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                device?.name ?: "Unknown device"
                            } else {
                                "Unknown device"
                            }
                            statusTextView.text = "Connected with $deviceName"
                            Toast.makeText(context, "Successfully connected with $deviceName", Toast.LENGTH_SHORT).show()
                        }
                        BluetoothDevice.BOND_BONDING -> {
                            statusTextView.text = "Connecting..."
                        }
                        BluetoothDevice.BOND_NONE -> {
                            statusTextView.text = "Failed to connect"
                            Toast.makeText(context, "Failed to connect", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.devices)
        // Initialize UI components
        statusTextView = findViewById(R.id.statusTextView)
        devicesListView = findViewById(R.id.devicesListView)

        // Initialize Bluetooth adapter
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (bluetoothManager == null) {
            statusTextView.text = "Bluetooth service not available"
            Toast.makeText(this, "Bluetooth service not available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        bluetoothAdapter = bluetoothManager.adapter
        // Check if device supports Bluetooth
        if (bluetoothAdapter == null) {
            statusTextView.text = "Bluetooth is not available on this device"
            Toast.makeText(this, "Bluetooth is not available on this device", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check and request Bluetooth activation if needed
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            if (!bluetoothAdapter.isEnabled) {
                statusTextView.text = "Activating Bluetooth..."
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        } else {
            requestBluetoothPermissions()
        }

        // Setup ListView and adapter for showing discovered devices
        devicesListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devicesList)
        devicesListView.adapter = devicesListAdapter

        // Handle device selection from list
        devicesListView.setOnItemClickListener { _, _, position, _ ->
            val deviceAddress = devicesList[position].split("\n")[1]
            val device = discoveredDevices[deviceAddress]
            device?.let {

                val deviceName = if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    it.name ?: "Unknown device"
                } else {
                    "Unknown device"
                }
                statusTextView.text = "Trying to connect with $deviceName..."
                connectToDevice(it)
            }
        }

        // Register for Bluetooth-related broadcasts
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        registerReceiver(receiver, filter)

        findViewById<Button>(R.id.scanButton).setOnClickListener {
            it.isEnabled = false
            devicesList.clear()
            devicesListAdapter.notifyDataSetChanged()
            statusTextView.text = "Searching for available devices..."
            startDiscovery()
        }

        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }

        statusTextView.text = "Ready to search for devices..."
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        ActivityCompat.requestPermissions(this, permissions, REQUEST_BLUETOOTH_PERMISSIONS)
    }

    private fun startDiscovery() {
        if (!hasRequiredPermissions()) {
            statusTextView.text = "Permission required to search for devices"
            requestBluetoothPermissions()
            return
        }

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                }

                findViewById<Button>(R.id.scanButton).isEnabled = false
                bluetoothAdapter.startDiscovery()
                Toast.makeText(this, "Starting search...", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            statusTextView.text = "Error: ${e.message}"
            Toast.makeText(this, "Error starting search: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Initiates connection to selected Bluetooth device
    private fun connectToDevice(device: BluetoothDevice) {
        try {
            if (hasRequiredPermissions()) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter.cancelDiscovery()
                }

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    if (device.bondState != BluetoothDevice.BOND_BONDED) {
                        device.createBond()
                    } else {
                        statusTextView.text = "Connected to ${device.name ?: "Unknown device"}"
                        Toast.makeText(this, "Connected to ${device.name ?: "Unknown device"}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                statusTextView.text = "Permission required to connect to devices"
                requestBluetoothPermissions()
            }
        } catch (e: SecurityException) {
            statusTextView.text = "Connection error: ${e.message}"
            Toast.makeText(this, "Connection error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    //Handles result of Bluetooth enable request
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                statusTextView.text = "Bluetooth enabled, ready to search"
                startDiscovery()
            } else {
                statusTextView.text = "Bluetooth must be enabled to use this feature"
                Toast.makeText(this, "Bluetooth must be enabled", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // Handles permission request results
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_BLUETOOTH_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    statusTextView.text = "Permissions granted, starting search"
                    startDiscovery()
                } else {
                    statusTextView.text = "Bluetooth permissions are required to use this feature"
                    Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(receiver)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                }
            }
        } catch (e: Exception) {

        }
    }

    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 1
        private const val REQUEST_ENABLE_BT = 2
    }
}