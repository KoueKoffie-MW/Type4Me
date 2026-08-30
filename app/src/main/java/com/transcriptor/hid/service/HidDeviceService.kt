package com.transcriptor.hid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.transcriptor.hid.TranscriptorApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Interface abstraction for partial wake lock management.
 */
interface WakeLockController {
    fun acquire(timeoutMs: Long)
    fun release()
    val isHeld: Boolean
}

/**
 * Android system implementation of [WakeLockController] using [PowerManager.PARTIAL_WAKE_LOCK].
 */
class SystemWakeLockController(context: Context) : WakeLockController {
    private var wakeLock: PowerManager.WakeLock? = null

    init {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                HidDeviceService.WAKE_LOCK_TAG
            )?.apply {
                setReferenceCounted(false)
            }
        } catch (_: Throwable) {}
    }

    override fun acquire(timeoutMs: Long) {
        try {
            wakeLock?.let {
                if (!it.isHeld) it.acquire(timeoutMs)
            }
        } catch (_: Throwable) {}
    }

    override fun release() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Throwable) {}
    }

    override val isHeld: Boolean
        get() = wakeLock?.isHeld == true
}

/**
 * Controller encapsulating foreground service business logic, state transitions,
 * notification formatting, and emergency key release handling.
 */
class HidDeviceController(
    var transport: HidTransport? = null,
    val wakeLockController: WakeLockController? = null,
    private val notificationUpdater: ((String) -> Unit)? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    var isForegroundActive: Boolean = false
        private set

    fun startService(autoInit: Boolean = true) {
        isForegroundActive = true
        if (autoInit) {
            coroutineScope.launch {
                transport?.initialize()
            }
        }
        observeTransportState()
    }

    fun stopService() {
        isForegroundActive = false
    }

    fun handleIntent(action: String?, deviceAddress: String? = null) {
        when (action) {
            HidDeviceService.ACTION_START -> startService()
            HidDeviceService.ACTION_STOP -> stopService()
            HidDeviceService.ACTION_CONNECT -> {
                // Connection by MAC address handled via service wrapper
            }
            HidDeviceService.ACTION_DISCONNECT -> {
                coroutineScope.launch {
                    transport?.disconnect()
                }
            }
            null -> startService()
        }
    }

    fun acquireWakeLock(timeoutMs: Long = 10000L) {
        wakeLockController?.acquire(timeoutMs)
    }

    fun releaseWakeLock() {
        wakeLockController?.release()
    }

    fun observeTransportState() {
        val currentTransport = transport ?: return
        coroutineScope.launch {
            currentTransport.connectionState.collect { state ->
                val deviceName = currentTransport.connectedDeviceName.value
                val content = when (state) {
                    HidConnectionState.CONNECTED -> "Connected to ${deviceName ?: "Host PC"}"
                    HidConnectionState.CONNECTING -> "Connecting to ${deviceName ?: "Host PC"}..."
                    HidConnectionState.DISCONNECTED -> "Bluetooth HID Keyboard idle"
                    HidConnectionState.ERROR -> "Bluetooth HID Error — check settings"
                }
                notificationUpdater?.invoke(content)
            }
        }
    }

    fun destroy() {
        // Emergency all-zero release report to prevent stuck host modifier keys (Edge Case E4)
        coroutineScope.launch {
            try {
                transport?.sendKeyboardReport(ByteArray(8))
            } catch (_: Throwable) {}
            try {
                transport?.release()
            } catch (_: Throwable) {}
        }
        releaseWakeLock()
        coroutineScope.cancel()
    }

    fun onTaskRemoved() {
        coroutineScope.launch {
            try {
                transport?.sendKeyboardReport(ByteArray(8))
            } catch (_: Throwable) {}
        }
    }
}

/**
 * Android 14/15 Foreground Service for Bluetooth and USB HID Keyboard Emulation.
 *
 * Runs with [ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE] to maintain
 * continuous low-latency HID connectivity and prevent process termination when the device is locked.
 */
class HidDeviceService : Service() {

    companion object {
        const val CHANNEL_ID = "transcriptor_hid_service_channel"
        const val CHANNEL_NAME = "Transcriptor HID Keyboard"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.transcriptor.hid.action.START_SERVICE"
        const val ACTION_STOP = "com.transcriptor.hid.action.STOP_SERVICE"
        const val ACTION_CONNECT = "com.transcriptor.hid.action.CONNECT"
        const val ACTION_DISCONNECT = "com.transcriptor.hid.action.DISCONNECT"
        const val EXTRA_DEVICE_ADDRESS = "com.transcriptor.hid.extra.DEVICE_ADDRESS"
        const val WAKE_LOCK_TAG = "transcriptor:hid_wake_lock"
    }

    inner class LocalBinder : Binder() {
        fun getService(): HidDeviceService = this@HidDeviceService
        fun getController(): HidDeviceController = this@HidDeviceService.controller
    }

    private val binder = LocalBinder()
    lateinit var controller: HidDeviceController

    var transport: HidTransport?
        get() = if (::controller.isInitialized) controller.transport else null
        set(value) {
            if (::controller.isInitialized) controller.transport = value
        }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val sharedTransport = (applicationContext as? TranscriptorApp)?.bluetoothHidTransport
            ?: BluetoothHidTransport(applicationContext)
        controller = HidDeviceController(
            transport = sharedTransport,
            wakeLockController = SystemWakeLockController(this),
            notificationUpdater = { content -> updateNotificationContent(content) }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!::controller.isInitialized) {
            val sharedTransport = (applicationContext as? TranscriptorApp)?.bluetoothHidTransport
                ?: BluetoothHidTransport(applicationContext)
            controller = HidDeviceController(
                transport = sharedTransport,
                wakeLockController = SystemWakeLockController(this),
                notificationUpdater = { content -> updateNotificationContent(content) }
            )
        }

        when (intent?.action) {
            ACTION_START -> {
                startForegroundServiceInternal()
                controller.startService()
            }
            ACTION_STOP -> {
                controller.stopService()
                stopForegroundServiceInternal()
            }
            ACTION_CONNECT -> {
                val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (address != null) {
                    connectToDeviceAddress(address)
                }
            }
            ACTION_DISCONNECT -> {
                controller.handleIntent(ACTION_DISCONNECT)
            }
            null -> {
                startForegroundServiceInternal()
                controller.startService()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    /**
     * Connects to a paired Bluetooth device by MAC address.
     */
    fun connectToDeviceAddress(address: String): Boolean {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter() ?: return false
        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(address)
        } catch (_: Exception) {
            return false
        }
        val btTransport = transport as? BluetoothHidTransport ?: return false
        return btTransport.connectHost(device)
    }

    /**
     * Starts the service in the foreground with connectedDevice type.
     */
    fun startForegroundServiceInternal() {
        val notification = buildNotification(
            title = "Transcriptor HID Active",
            content = "Bluetooth HID Keyboard ready for voice typing"
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (_: Exception) {}
        }
    }

    /**
     * Stops the foreground service and removes notification.
     */
    fun stopForegroundServiceInternal() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                stopForeground(true)
            } catch (_: Exception) {}
        }
        stopSelf()
    }

    fun acquireWakeLock(timeoutMs: Long = 10000L) {
        controller.acquireWakeLock(timeoutMs)
    }

    fun releaseWakeLock() {
        controller.releaseWakeLock()
    }

    private fun updateNotificationContent(content: String) {
        if (!controller.isForegroundActive) return
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val notification = buildNotification(
                title = "Transcriptor HID Keyboard",
                content = content
            )
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains persistent Bluetooth HID connection with host PC"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun buildNotification(title: String, content: String): Notification {
        val launchIntent = packageManager?.getLaunchIntentForPackage(packageName)
        val pendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            null
        }

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        return builder.build()
    }

    override fun onDestroy() {
        if (::controller.isInitialized) {
            controller.destroy()
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (::controller.isInitialized) {
            controller.onTaskRemoved()
        }
        super.onTaskRemoved(rootIntent)
    }
}
