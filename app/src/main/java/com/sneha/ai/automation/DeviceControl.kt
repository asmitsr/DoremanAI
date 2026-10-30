package com.sneha.ai.automation

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.provider.Settings as AndroidSettings
import com.sneha.ai.ai.Action
import kotlinx.coroutines.delay

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
}
