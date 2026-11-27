package com.sneha.ai.automation

import android.Manifest
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.StatFs
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings as AndroidSettings
import android.view.Surface
import androidx.core.content.ContextCompat
import com.sneha.ai.SnehaApp
import com.sneha.ai.ai.Action
import com.sneha.ai.data.K
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

/**
 * v1.3.0 — full-phone device control, the way the LIA / private-assistant
 * videos do it: Accessibility drives the Settings UI, because Android
 * itself blocks silent toggles.
 *
 * HONEST LIMITS (do not promise more than this in the UI):
 * - WiFi ON/OFF: on Android 10+ (API 29+) WifiManager.setWifiEnabled()
 *   always returns false for a normal third-party app (only Device Owner /
 *   Profile Owner / system apps are exempt). So "wifi on/off" opens the
 *   WiFi settings panel and tells the user to tap the toggle once.
 * - WiFi CONNECT: works without a password only for a saved / open
 *   network. A secured network's password field is a password field, and
 *   SnehaAccessibilityService.typeText() deliberately refuses password
 *   fields, so the app taps the SSID and asks the user to type the
 *   password + Connect themselves. Never speak a WiFi password aloud.
 * - Bluetooth ON: uses the system enable dialog (ACTION_REQUEST_ENABLE)
 *   when adapter.enable() is refused. CONNECT taps the paired device in
 *   Bluetooth settings and then checks the A2DP connection state.
 * - Flashlight: CameraManager torch mode, needs the CAMERA runtime
 *   permission. No FLASHLIGHT permission exists; CAMERA is the one.
 */
object DeviceControl {

    fun flashlight(ctx: Context, on: Boolean): Pair<Boolean, String> {
        return try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull { id ->
                try { cm.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } catch (_: Exception) { false }
            } ?: return Pair(false, "No camera with a flashlight was found")
            cm.setTorchMode(id, on)
            Pair(true, if (on) "Flashlight turned on" else "Flashlight turned off")
        } catch (e: SecurityException) {
            Pair(false, "Allow the Camera permission for the flashlight (Settings > Permission Setup Hub)")
        } catch (e: Exception) {
            Pair(false, "Could not turn on the flashlight: ${e.message}")
        }
    }

    private fun wifiManager(ctx: Context): WifiManager =
        ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun wifiStatus(ctx: Context): Pair<Boolean, String> {
        return try {
            val wm = wifiManager(ctx)
            val ssid = try { wm.connectionInfo?.ssid?.removeSurrounding("\"") } catch (_: Exception) { null }
            if (!wm.isWifiEnabled) Pair(true, "WiFi is currently off")
            else if (!ssid.isNullOrBlank() && ssid != "<unknown ssid>") Pair(true, "WiFi is on, connected to: $ssid")
            else Pair(true, "WiFi is on, but no network appears connected")
        } catch (e: Exception) {
            Pair(false, "Could not read WiFi status: ${e.message}")
        }
    }

    private fun openPanel(ctx: Context, action: String): Boolean {
        return try {
            ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: Exception) { false }
    }

    suspend fun wifi(ctx: Context, a: Action.Wifi, svc: SnehaAccessibilityService?): Pair<Boolean, String> {
        return when (a.mode) {
            "status" -> wifiStatus(ctx)
            "on", "off" -> {
                val wm = try { wifiManager(ctx) } catch (e: Exception) { return Pair(false, "WiFi manager not found") }
                val wantOn = a.mode == "on"
                if (wm.isWifiEnabled == wantOn) {
                    return Pair(true, if (wantOn) "WiFi is already on" else "WiFi is already off")
                }
                // Android 10+: setWifiEnabled() always fails for normal apps.
                // Do NOT silently pretend it worked — open the panel instead.
                val opened = openPanel(ctx, AndroidSettings.ACTION_WIFI_SETTINGS)
                if (opened) Pair(
                    true,
                    "On Android 10+, an app cannot switch WiFi " + (if (wantOn) "on" else "off") +
                        " directly, so I opened WiFi settings — tap the toggle at the top once"
                ) else Pair(false, "Could not open WiFi settings")
            }
            "connect" -> {
                val ssid = a.ssid?.trim().orEmpty()
                if (ssid.isEmpty()) return Pair(false, "Which WiFi network should I connect to? Tell me its name (for example: connect to my WiFi)")
                if (svc == null) {
                    val opened = openPanel(ctx, AndroidSettings.ACTION_WIFI_SETTINGS)
                    return if (opened) Pair(true, "I opened WiFi settings — pick $ssid from the list (turn on Accessibility for auto-tap)")
                    else Pair(false, "Could not open WiFi settings")
                }
                openPanel(ctx, AndroidSettings.ACTION_WIFI_SETTINGS)
                delay(2200)
                val tapped = svc.tapByText(ssid)
                delay(1600)
                if (!tapped) return Pair(true, "I opened WiFi settings, but $ssid was not in the list — pick it yourself")
                // If it connected without a password prompt, the SSID is saved/open.
                // If a password prompt appeared, typeText() refuses password
                // fields by design, so the user must type it once themselves.
                Pair(true, "$ssid was tapped. If a password is asked, type it yourself and tap Connect; a saved network will connect on its own")
            }
            else -> Pair(false, "Unknown WiFi command: ${a.mode}")
        }
    }

    suspend fun bluetooth(ctx: Context, a: Action.Bluetooth, svc: SnehaAccessibilityService?): Pair<Boolean, String> {
        // v1.3.0 fix (run #46): BluetoothAdapter.getDefaultAdapter() is a
        // platform type, and declaring it BluetoothAdapter? made every later
        // adapter.xxx call fail with "Only safe (?.) or non-null asserted
        // (!!.) calls are allowed on a nullable receiver". Keep the null
        // check, but bind the result to a NON-null val so the rest of this
        // function compiles without ?. on every call.
        val rawAdapter: BluetoothAdapter? = try { BluetoothAdapter.getDefaultAdapter() } catch (_: Exception) { null }
        val adapter: BluetoothAdapter = rawAdapter
            ?: return Pair(false, "Bluetooth was not found on this phone")
        return when (a.mode) {
            "list" -> {
                val names = try { adapter.bondedDevices.mapNotNull { it.name }.filter { it.isNotBlank() } } catch (_: SecurityException) {
                    return Pair(false, "Allow the Bluetooth permission to see paired devices (Settings > Permission Setup Hub)")
                } catch (_: Exception) { emptyList() }
                if (names.isEmpty()) Pair(true, "No paired Bluetooth devices found")
                else Pair(true, "Paired devices: " + names.joinToString(", "))
            }
            "on" -> {
                if (adapter.isEnabled) return Pair(true, "Bluetooth is already on")
                val ok = try { adapter.enable() } catch (_: SecurityException) {
                    return Pair(false, "Allow the Bluetooth permission to turn on Bluetooth (Settings > Permission Setup Hub)")
                } catch (_: Exception) { false }
                if (ok) return Pair(true, "Turning Bluetooth on")
                // Fallback: the system enable dialog (user taps Allow once).
                try {
                    ctx.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    Pair(true, "Bluetooth permission was requested — tap Allow")
                } catch (_: Exception) {
                    val opened = openPanel(ctx, AndroidSettings.ACTION_BLUETOOTH_SETTINGS)
                    if (opened) Pair(true, "I opened Bluetooth settings — tap the toggle at the top") else Pair(false, "Could not turn on Bluetooth")
                }
            }
            "off" -> {
                if (!adapter.isEnabled) return Pair(true, "Bluetooth is already off")
                val ok = try { adapter.disable() } catch (_: SecurityException) {
                    return Pair(false, "Allow the Bluetooth permission to turn off Bluetooth")
                } catch (_: Exception) { false }
                if (ok) Pair(true, "Turning Bluetooth off")
                else {
                    val opened = openPanel(ctx, AndroidSettings.ACTION_BLUETOOTH_SETTINGS)
                    if (opened) Pair(true, "I opened Bluetooth settings — tap the toggle at the top") else Pair(false, "Could not turn off Bluetooth")
                }
            }
            "connect" -> {
                val device = a.device?.trim().orEmpty()
                if (device.isEmpty()) return Pair(false, "Which device should I connect to? Tell me its name")
                if (!adapter.isEnabled) return Pair(false, "Turn on Bluetooth first, then I will connect to the device")
                if (svc == null) {
                    val opened = openPanel(ctx, AndroidSettings.ACTION_BLUETOOTH_SETTINGS)
                    return if (opened) Pair(true, "I opened Bluetooth settings — pick $device from the list (turn on Accessibility for auto-tap)")
                    else Pair(false, "Bluetooth Could not open settings")
                }
                openPanel(ctx, AndroidSettings.ACTION_BLUETOOTH_SETTINGS)
                delay(2200)
                val tapped = svc.tapByText(device)
                delay(2000)
                val connected = try { adapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED } catch (_: Exception) { false }
                if (tapped && connected) Pair(true, "$device connected")
                else if (tapped) Pair(true, "$device tapped — if the device asks for Confirm/Pair, tap that")
                else Pair(true, "I opened Bluetooth settings, but $device was not in the list — check whether it is paired")
            }
            else -> Pair(false, "Unknown Bluetooth command: ${a.mode}")
        }
    }

    // ---- v2.2.3: instant device answers + in-app camera -------------------

    fun battery(ctx: Context): Pair<Boolean, String> {
        return try {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (pct < 0) Pair(false, "Could not read battery level")
            else Pair(true, "Battery is $pct%" + if (bm.isCharging) ", charging" else ", not charging")
        } catch (e: Exception) {
            Pair(false, "Could not read battery: ${e.message}")
        }
    }

    fun deviceInfo(ctx: Context): Pair<Boolean, String> {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val freeGb = stat.availableBytes / (1024.0 * 1024 * 1024)
            val totalGb = stat.totalBytes / (1024.0 * 1024 * 1024)
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            val ramGb = mem.totalMem / (1024.0 * 1024 * 1024)
            Pair(true, "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}. " +
                "Storage free %.1f GB of %.1f GB. RAM %.1f GB.".format(freeGb, totalGb, ramGb))
        } catch (e: Exception) {
            Pair(false, "Could not read device info: ${e.message}")
        }
    }

    fun vibrate(ctx: Context, ms: Long): Pair<Boolean, String> {
        return try {
            val vib: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            Pair(true, "Vibrating")
        } catch (e: Exception) {
            Pair(false, "Could not vibrate: ${e.message}")
        }
    }

    fun setBrightness(ctx: Context, level: Int): Pair<Boolean, String> {
        return try {
            if (!AndroidSettings.System.canWrite(ctx)) {
                ctx.startActivity(
                    Intent(AndroidSettings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return Pair(false, "Brightness needs one special permission — I opened the screen, turn on 'Allow modifying system settings' for Doreman, then ask again")
            }
            val v = (level.coerceIn(0, 100) * 255 / 100).coerceIn(1, 255)
            AndroidSettings.System.putInt(ctx.contentResolver, AndroidSettings.System.SCREEN_BRIGHTNESS_MODE, AndroidSettings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            AndroidSettings.System.putInt(ctx.contentResolver, AndroidSettings.System.SCREEN_BRIGHTNESS, v)
            Pair(true, "Brightness set to $level%")
        } catch (e: Exception) {
            Pair(false, "Could not set brightness: ${e.message}")
        }
    }

    fun setAlarm(ctx: Context, a: Action.SetAlarm): Pair<Boolean, String> {
        return try {
            val i = Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, a.hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, a.minute)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (a.label.isNotBlank()) i.putExtra(AlarmClock.EXTRA_MESSAGE, a.label)
            ctx.startActivity(i)
            Pair(true, "Alarm set for %02d:%02d".format(a.hour, a.minute))
        } catch (e: Exception) {
            Pair(false, "Could not set alarm: ${e.message}")
        }
    }

    fun setTimer(ctx: Context, a: Action.SetTimer): Pair<Boolean, String> {
        return try {
            val i = Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, a.seconds)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (a.label.isNotBlank()) i.putExtra(AlarmClock.EXTRA_MESSAGE, a.label)
            ctx.startActivity(i)
            Pair(true, "Timer set for ${a.seconds} seconds")
        } catch (e: Exception) {
            Pair(false, "Could not set timer: ${e.message}")
        }
    }

    fun openUrl(ctx: Context, url: String): Pair<Boolean, String> {
        return try {
            var u = url.trim()
            if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Pair(true, "Opening $u")
        } catch (e: Exception) {
            Pair(false, "Could not open link: ${e.message}")
        }
    }

    fun copyText(ctx: Context, text: String): Pair<Boolean, String> {
        return try {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Doreman", text))
            Pair(true, "Copied to clipboard")
        } catch (e: Exception) {
            Pair(false, "Could not copy: ${e.message}")
        }
    }

    fun readClipboard(ctx: Context): Pair<Boolean, String> {
        return try {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val t = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
            if (t.isBlank()) Pair(true, "Clipboard is empty") else Pair(true, "Clipboard: $t")
        } catch (e: Exception) {
            Pair(false, "Could not read clipboard: ${e.message}")
        }
    }

    // In-app photo capture (Camera2, no external dependency): no
    // Camera-app UI guessing, so a selfie works even when the camera
    // app's buttons have no labels.
    suspend fun takePhoto(app: SnehaApp, front: Boolean): Pair<Boolean, String> {
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return Pair(false, "Camera permission is off — turn it on in Settings > Permission Setup Hub")
        }
        val uri = try {
            captureJpeg(app, front)
        } catch (e: Exception) {
            return Pair(false, "Could not take photo: ${e.message}")
        } ?: return Pair(false, "Could not take photo")
        try { app.prefs.set(K.lastPhoto, uri.toString()) } catch (_: Exception) {}
        return Pair(true, if (front) "Selfie saved to gallery (Pictures/Doreman)" else "Photo saved to gallery (Pictures/Doreman)")
    }

    private suspend fun captureJpeg(ctx: Context, front: Boolean): Uri? = suspendCancellableCoroutine { cont ->
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val wantFacing = if (front) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
        var cameraId: String? = null
        var jpegSize: android.util.Size? = null
        var sensorOrientation = 0
        try {
            for (id in cm.cameraIdList) {
                val ch = cm.getCameraCharacteristics(id)
                if (ch.get(CameraCharacteristics.LENS_FACING) == wantFacing) {
                    cameraId = id
                    sensorOrientation = ch.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                    val map = ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val sizes = map?.getOutputSizes(ImageFormat.JPEG)?.sortedByDescending { it.width * it.height }
                    jpegSize = sizes?.firstOrNull { it.width <= 4000 } ?: sizes?.firstOrNull()
                    break
                }
            }
        } catch (_: Exception) {}
        val id = cameraId
        val size = jpegSize
        if (id == null || size == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        val thread = HandlerThread("doreman-cam").apply { start() }
        val handler = Handler(thread.looper)
        val reader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 1)
        val dummyTex = SurfaceTexture(0).apply { setDefaultBufferSize(640, 480) }
        val dummySurface = Surface(dummyTex)
        var device: CameraDevice? = null
        var session: CameraCaptureSession? = null
        fun cleanup() {
            try { session?.close() } catch (_: Exception) {}
            try { device?.close() } catch (_: Exception) {}
            try { reader.close() } catch (_: Exception) {}
            try { dummySurface.release() } catch (_: Exception) {}
            try { dummyTex.release() } catch (_: Exception) {}
            try { thread.quitSafely() } catch (_: Exception) {}
        }
        fun fail() {
            cleanup()
            if (cont.isActive) cont.resume(null)
        }
        handler.postDelayed({ fail() }, 12000)
        reader.setOnImageAvailableListener({ r ->
            val image = try { r.acquireLatestImage() } catch (_: Exception) { null }
            if (image == null) {
                fail()
                return@setOnImageAvailableListener
            }
            try {
                val buf = image.planes[0].buffer
                val bytes = ByteArray(buf.remaining()).also { buf.get(it) }
                image.close()
                val uri = saveJpeg(ctx, bytes)
                cleanup()
                if (cont.isActive) cont.resume(uri)
            } catch (_: Exception) {
                try { image.close() } catch (_: Exception) {}
                fail()
            }
        }, handler)
        try {
            cm.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    device = camera
                    try {
                        @Suppress("DEPRECATION")
                        camera.createCaptureSession(
                            listOf(dummySurface, reader.surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(s: CameraCaptureSession) {
                                    session = s
                                    try {
                                        val preview = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                            addTarget(dummySurface)
                                            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                        }
                                        s.setRepeatingRequest(preview.build(), null, handler)
                                        handler.postDelayed({
                                            try {
                                                val shot = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                                    addTarget(reader.surface)
                                                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                                    set(CaptureRequest.JPEG_ORIENTATION, sensorOrientation)
                                                }
                                                s.capture(shot.build(), null, handler)
                                            } catch (_: Exception) {
                                                fail()
                                            }
                                        }, 700)
                                    } catch (_: Exception) {
                                        fail()
                                    }
                                }

                                override fun onConfigureFailed(s: CameraCaptureSession) {
                                    fail()
                                }
                            },
                            handler
                        )
                    } catch (_: Exception) {
                        fail()
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    fail()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    fail()
                }
            }, handler)
        } catch (_: Exception) {
            fail()
        }
        cont.invokeOnCancellation { cleanup() }
    }

    private fun saveJpeg(ctx: Context, bytes: ByteArray): Uri? {
        return try {
            val name = "Doreman_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Doreman")
            }
            val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            uri
        } catch (_: Exception) {
            null
        }
    }
}
