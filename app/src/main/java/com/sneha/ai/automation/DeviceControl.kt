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
            } ?: return Pair(false, "ফ্ল্যাশলাইট আছে এমন ক্যামেরা পাওয়া যায়নি")
            cm.setTorchMode(id, on)
            Pair(true, if (on) "ফ্ল্যাশলাইট জ্বালানো হয়েছে" else "ফ্ল্যাশলাইট নিভিয়ে দেওয়া হয়েছে")
        } catch (e: SecurityException) {
            Pair(false, "ফ্ল্যাশলাইটের জন্য Camera অনুমতি দিন (Settings > Permission Setup Hub)")
        } catch (e: Exception) {
            Pair(false, "ফ্ল্যাশলাইট চালানো যায়নি: ${e.message}")
        }
    }

    private fun wifiManager(ctx: Context): WifiManager =
        ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun wifiStatus(ctx: Context): Pair<Boolean, String> {
        return try {
            val wm = wifiManager(ctx)
            val ssid = try { wm.connectionInfo?.ssid?.removeSurrounding("\"") } catch (_: Exception) { null }
            if (!wm.isWifiEnabled) Pair(true, "WiFi এখন বন্ধ আছে")
            else if (!ssid.isNullOrBlank() && ssid != "<unknown ssid>") Pair(true, "WiFi চালু আছে, কানেক্টেড: $ssid")
            else Pair(true, "WiFi চালু আছে, কিন্তু কোনো নেটওয়ার্কে কানেক্টেড দেখাচ্ছে না")
        } catch (e: Exception) {
            Pair(false, "WiFi স্ট্যাটাস পড়া যায়নি: ${e.message}")
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
                val wm = try { wifiManager(ctx) } catch (e: Exception) { return Pair(false, "WiFi ম্যানেজার পাওয়া যায়নি") }
                val wantOn = a.mode == "on"
                if (wm.isWifiEnabled == wantOn) {
                    return Pair(true, if (wantOn) "WiFi আগেই চালু আছে" else "WiFi আগেই বন্ধ আছে")
                }
                // Android 10+: setWifiEnabled() always fails for normal apps.
                // Do NOT silently pretend it worked — open the panel instead.
                val opened = openPanel(ctx, AndroidSettings.ACTION_WIFI_SETTINGS)
                if (opened) Pair(
                    true,
                    "Android 10+ এ অ্যাপ সরাসরি WiFi " + (if (wantOn) "চালু" else "বন্ধ") +
                        " করতে পারে না, তাই WiFi সেটিংস খুলে দিয়েছি — উপরের টগলটা একবার চাপুন"
                ) else Pair(false, "WiFi সেটিংস খোলা যায়নি")
            }
            "connect" -> {
                val ssid = a.ssid?.trim().orEmpty()
                if (ssid.isEmpty()) return Pair(false, "কোন WiFi-তে কানেক্ট করব, নামটা বলুন (যেমন: আমার WiFi-তে কানেক্ট করো)")
                if (svc == null) {
                    val opened = openPanel(ctx, AndroidSettings.ACTION_WIFI_SETTINGS)
                    return if (opened) Pair(true, "WiFi সেটিংস খুলেছি — তালিকা থেকে $ssid বেছে নিন (স্বয়ংক্রিয় ট্যাপের জন্য Accessibility চালু করুন)")
                    else Pair(false, "WiFi সেটিংস খোলা যায়নি")
                }
                openPanel(ctx, AndroidSettings.ACTION_WIFI_SETTINGS)
                delay(2200)
                val tapped = svc.tapByText(ssid)
                delay(1600)
                if (!tapped) return Pair(true, "WiFi সেটিংস খুলেছি, কিন্তু তালিকায় $ssid পাইনি — নিজে বেছে নিন")
                // If it connected without a password prompt, the SSID is saved/open.
                // If a password prompt appeared, typeText() refuses password
                // fields by design, so the user must type it once themselves.
                Pair(true, "$ssid-এ ট্যাপ করা হয়েছে। পাসওয়ার্ড চাইলে নিজে টাইপ করে Connect চাপুন, সেভ করা নেটওয়ার্ক হলে নিজেই কানেক্ট হবে")
            }
            else -> Pair(false, "অজানা WiFi কমান্ড: ${a.mode}")
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
            ?: return Pair(false, "এই ফোনে Bluetooth পাওয়া যায়নি")
        return when (a.mode) {
            "list" -> {
                val names = try { adapter.bondedDevices.mapNotNull { it.name }.filter { it.isNotBlank() } } catch (_: SecurityException) {
                    return Pair(false, "পেয়ার করা ডিভাইস দেখতে Bluetooth অনুমতি দিন (Settings > Permission Setup Hub)")
                } catch (_: Exception) { emptyList() }
                if (names.isEmpty()) Pair(true, "কোনো পেয়ার করা Bluetooth ডিভাইস পাওয়া যায়নি")
                else Pair(true, "পেয়ার করা ডিভাইস: " + names.joinToString(", "))
            }
            "on" -> {
                if (adapter.isEnabled) return Pair(true, "Bluetooth আগেই চালু আছে")
                val ok = try { adapter.enable() } catch (_: SecurityException) {
                    return Pair(false, "Bluetooth চালু করতে Bluetooth অনুমতি দিন (Settings > Permission Setup Hub)")
                } catch (_: Exception) { false }
                if (ok) return Pair(true, "Bluetooth চালু করা হচ্ছে")
                // Fallback: the system enable dialog (user taps Allow once).
                try {
                    ctx.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    Pair(true, "Bluetooth চালুর অনুমতি চাওয়া হয়েছে — Allow চাপুন")
                } catch (_: Exception) {
                    val opened = openPanel(ctx, AndroidSettings.ACTION_BLUETOOTH_SETTINGS)
                    if (opened) Pair(true, "Bluetooth সেটিংস খুলেছি — উপরের টগলটা চাপুন") else Pair(false, "Bluetooth চালু করা যায়নি")
                }
            }
            "off" -> {
                if (!adapter.isEnabled) return Pair(true, "Bluetooth আগেই বন্ধ আছে")
                val ok = try { adapter.disable() } catch (_: SecurityException) {
                    return Pair(false, "Bluetooth বন্ধ করতে Bluetooth অনুমতি দিন")
                } catch (_: Exception) { false }
                if (ok) Pair(true, "Bluetooth বন্ধ করা হচ্ছে")
                else {
                    val opened = openPanel(ctx, AndroidSettings.ACTION_BLUETOOTH_SETTINGS)
                    if (opened) Pair(true, "Bluetooth সেটিংস খুলেছি — উপরের টগলটা চাপুন") else Pair(false, "Bluetooth বন্ধ করা যায়নি")
                }
            }
            "connect" -> {
                val device = a.device?.trim().orEmpty()
                if (device.isEmpty()) return Pair(false, "কোন ডিভাইসে কানেক্ট করব, নামটা বলুন")
                if (!adapter.isEnabled) return Pair(false, "আগে Bluetooth চালু করুন, তারপর ডিভাইসে কানেক্ট করব")
                if (svc == null) {
                    val opened = openPanel(ctx, AndroidSettings.ACTION_BLUETOOTH_SETTINGS)
                    return if (opened) Pair(true, "Bluetooth সেটিংস খুলেছি — তালিকা থেকে $device বেছে নিন (স্বয়ংক্রিয় ট্যাপের জন্য Accessibility চালু করুন)")
                    else Pair(false, "Bluetooth সেটিংস খোলা যায়নি")
                }
                openPanel(ctx, AndroidSettings.ACTION_BLUETOOTH_SETTINGS)
                delay(2200)
                val tapped = svc.tapByText(device)
                delay(2000)
                val connected = try { adapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED } catch (_: Exception) { false }
                if (tapped && connected) Pair(true, "$device-এ কানেক্ট করা হয়েছে")
                else if (tapped) Pair(true, "$device-এ ট্যাপ করা হয়েছে — ডিভাইসে Confirm/Pair চাইলে সেটা চাপুন")
                else Pair(true, "Bluetooth সেটিংস খুলেছি, কিন্তু তালিকায় $device পাইনি — পেয়ার করা আছে কিনা দেখুন")
            }
            else -> Pair(false, "অজানা Bluetooth কমান্ড: ${a.mode}")
        }
    }
}
