package com.example.temi_test

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.temi_test.databinding.ActivityMainBinding
import com.example.temi_test.model.MqttMessageData
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.TtsRequest.Language
import kotlinx.coroutines.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.ByteArrayOutputStream
import java.net.URI



class MainActivity : AppCompatActivity(), OnRobotReadyListener {

    private lateinit var temi: Robot
    private lateinit var binding: ActivityMainBinding
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private lateinit var imageReader: ImageReader
    private var webSocketClient: WebSocketClient? = null
    private lateinit var mqttClient: SimpleMqttClient


    private var frameSendingJob: Job? = null
    private var isWebSocketConnected = false
    private var isMonitoring = false

    private var cameraHandlerThread: HandlerThread? = null
    private var cameraHandler: Handler? = null

    private val REQUEST_CAMERA_PERMISSION = 2
    private val TAG = "TemiCamera"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        textureView = binding.textureView
        temi = Robot.getInstance()
        temi.addOnRobotReadyListener(this)

        mqttClient = SimpleMqttClient()
        mqttClient.connect()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            setupTextureView()
        }

        temi.speak(TtsRequest.create("The camera is starting, please wait.", false))

        binding.btnStart.setOnClickListener {
            startMonitoring()
        }

        binding.btnEnd.setOnClickListener {
            stopMonitoring()
        }
    }

    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        temi.speak(TtsRequest.create("Hi, I am Temi. We are starting the rice heating experiment.", false))
        startFrameSendingLoop()
        Log.d(TAG, "Monitoring started")
    }

    private fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        temi.stopMovement()
        stopFrameSendingLoop()
        temi.speak(TtsRequest.create("The experiment has been stopped.", false))
        Log.d(TAG, "Monitoring stopped")
    }

    private fun setupTextureView() {
        if (textureView.isAvailable) {
            startCamera(textureView.surfaceTexture!!)
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    startCamera(surface)
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }
        }
    }

    private fun startCamera(surfaceTexture: SurfaceTexture) {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: run {
                Toast.makeText(this, "No cameras available!", Toast.LENGTH_LONG).show()
                return
            }

            startCameraThread()

            imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.YUV_420_888, 2)

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    temi.speak(TtsRequest.create("Camera is ready.", false))
                    createCameraSession(camera, surfaceTexture)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    Toast.makeText(applicationContext, "Camera error: $error", Toast.LENGTH_LONG).show()
                }
            }, cameraHandler)

        } catch (e: Exception) {
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createCameraSession(camera: CameraDevice, surfaceTexture: SurfaceTexture) {
        try {
            surfaceTexture.setDefaultBufferSize(1920, 1080)
            val surface = Surface(surfaceTexture)
            val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
            }

            camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    session.setRepeatingRequest(captureRequestBuilder.build(), null, cameraHandler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(applicationContext, "Failed to configure camera session", Toast.LENGTH_SHORT).show()
                }
            }, cameraHandler)
        } catch (e: Exception) {
            Toast.makeText(this, "Error creating camera session: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendLiveFrame() {
        if (!textureView.isAvailable || webSocketClient?.isOpen != true) return

        val bitmap = textureView.bitmap ?: return

        lifecycleScope.launch {
            try {
                val base64Image = withContext(Dispatchers.IO) {
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val byteArray = outputStream.toByteArray()
                    bitmap.recycle()
                    Base64.encodeToString(byteArray, Base64.NO_WRAP)
                }

                webSocketClient?.send(base64Image)
                Log.d(TAG, "📤 Frame sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending frame: ${e.message}")
            }
        }
    }

    private fun startFrameSendingLoop() {
        if (frameSendingJob?.isActive == true) return

        frameSendingJob = lifecycleScope.launch {
            while (isActive) {
                sendLiveFrame()
                delay(1000)
            }
        }
    }

    private fun stopFrameSendingLoop() {
        frameSendingJob?.cancel()
        frameSendingJob = null
    }

    private fun startCameraThread() {
        cameraHandlerThread = HandlerThread("CameraThread").also {
            it.start()
            cameraHandler = Handler(it.looper)
        }
    }

    private fun stopCameraThread() {
        cameraHandlerThread?.quitSafely()
        cameraHandlerThread?.join()
        cameraHandlerThread = null
        cameraHandler = null
    }

    private fun closeCamera() {
        cameraDevice?.close()
        cameraDevice = null
        if (::imageReader.isInitialized) {
            imageReader.close()
        }
    }

    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            if (cameraDevice == null && textureView.isAvailable) {
                startCamera(textureView.surfaceTexture!!)
            }
            connectWebSocket()
        }
    }

    private fun connectWebSocket() {
        val uri = URI("ws://192.168.2.146:8000/ws")
        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                isWebSocketConnected = true
                Log.d(TAG, "WebSocket opened")
            }

            override fun onMessage(message: String?) {
                Log.d(TAG, "Received message: $message")
                handleWebSocketMessage(message)
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                isWebSocketConnected = false
                Log.w(TAG, "WebSocket closed: $reason, trying to reconnect...")
                reconnectWebSocket()
            }

            override fun onError(ex: Exception?) {
                isWebSocketConnected = false
                Log.e(TAG, "WebSocket error: ${ex?.message}, trying to reconnect...")
                reconnectWebSocket()
            }
        }
        webSocketClient?.connect()
    }

    private fun reconnectWebSocket() {
        if (isWebSocketConnected) return

        lifecycleScope.launch {
            delay(3000)
            Log.d(TAG, "🔄 Attempting to reconnect WebSocket...")
            connectWebSocket()
        }
    }

    private fun handleWebSocketMessage(message: String?) {
        message?.let { Log.d("test", it) }
        if (message != null) {
                when (message) {
                    "pot_detected" -> temi.speak(TtsRequest.create("Warning! A pot was detected.", false))
                    "cup_detected" -> temi.speak(TtsRequest.create("A cup was detected.", false))
                    "too_close" -> {
                        temi.speak(TtsRequest.create("Please step back so I can see the environment.", false))
                        temi.stopMovement()
                        temi.turnBy(180)
                    }

                    "no_pot" -> Log.d(TAG, "No pot detected.")
                    else -> Log.d(TAG, "Unknown message: $message")
                }
        }
    }

    override fun onResume() {
        super.onResume()
        if (textureView.isAvailable && cameraDevice == null) {
            startCamera(textureView.surfaceTexture!!)
        } else {
            setupTextureView()
        }
    }

    override fun onPause() {
        super.onPause()
        stopFrameSendingLoop()
        closeCamera()
        stopCameraThread()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFrameSendingLoop()
        closeCamera()
        stopCameraThread()
        temi.removeOnRobotReadyListener(this)
        temi.stopMovement()
        webSocketClient?.close()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupTextureView()
        } else {
            Toast.makeText(this, "Camera permission denied!", Toast.LENGTH_SHORT).show()
        }
    }

    fun scenario1(sensorData: MqttMessageData, message: String?){
        var potFound = false
        var plateReady = false
        var utensilReady = false
        var step = 1
        message?.let { Log.d("temi-log", it) }

        when (step) {
            1 -> {
                if (sensorData.device_name == "fridge-approached") {
                    temi.goTo("startpoint")
                }
                if (sensorData.device_name == "fridge_door" && sensorData.status) {
                    Log.d("temi-log", "Fridge door open")
                    step = 2
                }
            }
            2 -> {
                if (sensorData.device_name == "pot_in_fridge" && !sensorData.status) {
                    Log.d("temi-log", "metal pot detected")
                    temi.goTo("secondpoint")
                    potFound = true
                    step = 3
                }
            }

            3 -> {
                if (message == "metal_pot_on_counter") {
                    Log.d("temi-log", "סיר הונח על השיש")
                    step = 4
                }
            }

            4 -> {
                if (message == "plastic_plate" || message == "ceramic_bowl") {
                    Log.d("temi-log", "כלי חימום תקין זוהה")
                    plateReady = true
                    step = 5
                }
            }

            5 -> {
                if(sensorData.device_name == "drawer" && sensorData.status) {
                    if ((message == "fork" || message == "spoon")) {
                        Log.d("temi-log", "spoon detected")
                        utensilReady = true
                        step = 6
                    }
                }
            }

            6 -> {
                if (message == "metal_pot_and_plate_on_counter") {
                    Log.d("temi-log", "סיר וצלחת על השיש")
                    step = 7
                }
            }

            7 -> {
                if (sensorData.device_name == "micro_door" && sensorData.status) {
                    Log.d("temi-log", "מיקרוגל פתוח")
                    temi.goTo("thirdpoint")
                    step = 8
                }
            }

            8 -> {
                if (message == "plate_and_microwave_in_frame") {
                    Log.d("temi-log", "צלחת מוכנסת למיקרוגל")
                    if (message.contains("metal_pot")) {
                        temi.speak(TtsRequest.create("dangerous metal pot on microwave", false))
                        Log.d("temi-log", "❌ סכנת מתכת")
                    }
                    step = 9
                }
            }

            9 -> {
                if (sensorData.device_name == "micro_door" && !sensorData.status) {
                    Log.d("temi-log", "דלת נסגרה")
                    step = 10
                }
            }

            10 -> {
                Log.d("temi-log", "חימום פעיל... ממתין לסיום")
                step = 11
            }

            11 -> {
                if (message == "hand_and_plate_moving_away") {
                    Log.d("temi-log", "צלחת הוצאה מהמיקרוגל")
                    step = 12
                }
            }

            12 -> {
                if (message == "plate_on_counter") {
                    Log.d("temi-log", "סיום תהליך בהצלחה")
                    step = -1 // או reset לפי הצורך
                }
            }

            else -> Log.d("temi-log", "No active step")
        }
    }
}
