package com.sneha.ai.ai

import org.json.JSONArray
import org.json.JSONObject

sealed class Action {
    data class OpenApp(val app: String) : Action()
    data class PlayStoreInstall(val query: String) : Action()
    data class CallContact(val target: String) : Action()
    data class SendSms(val target: String, val message: String, val direct: Boolean = true) : Action()
    data class WhatsAppMsg(val target: String, val message: String) : Action()
    data class YouTubeSearch(val query: String, val playFirst: Boolean) : Action()
    data class Media(val cmd: String) : Action()
    data class Volume(val dir: String) : Action()
    data class SystemSetting(val panel: String) : Action()
    object CleanPhone : Action()
    object LockScreen : Action()
    object PowerMenu : Action()
    data class TapText(val text: String) : Action()
    data class TapXY(val x: Int, val y: Int) : Action()
    data class Scroll(val down: Boolean) : Action()
    object Back : Action()
    object Home : Action()
    data class TypeText(val text: String) : Action()
    object Enter : Action()
    object ReadScreen : Action()
    data class Wait(val ms: Long) : Action()
    data class Remember(val text: String) : Action()
    // v1.3.0 — full-phone device control (see DeviceControl.kt for limits).
    data class Flashlight(val on: Boolean) : Action()
    data class Wifi(val mode: String, val ssid: String? = null) : Action()
    data class Bluetooth(val mode: String, val device: String? = null) : Action()
    data class StayAwake(val on: Boolean) : Action()
}

fun Action.isUi(): Boolean = when (this) {
    is Action.TapText, is Action.TapXY, is Action.Scroll, is Action.TypeText -> true
    Action.Back, Action.Home, Action.Enter, Action.ReadScreen, Action.LockScreen, Action.PowerMenu -> true
    is Action.YouTubeSearch -> playFirst
    is Action.PlayStoreInstall -> true
    is Action.WhatsAppMsg -> true
    else -> false
}

fun Action.describe(): String = when (this) {
    is Action.OpenApp -> "Opening app: $app"
    is Action.PlayStoreInstall -> "Downloading / installing from Play Store: $query"
    is Action.CallContact -> "Calling: $target"
    is Action.SendSms -> "Sending SMS ($target): \"$message\""
    is Action.WhatsAppMsg -> "WhatsApp message ($target): \"$message\""
    is Action.YouTubeSearch -> "YouTube song / video: \"$query\"" + if (playFirst) " (playing directly)" else ""
    is Action.Media -> "Media command: $cmd"
    is Action.Volume -> "Volume change: $dir"
    is Action.SystemSetting -> "Opening system settings: $panel"
    Action.CleanPhone -> "Cleaning / boosting phone"
    Action.LockScreen -> "Locking phone screen"
    Action.PowerMenu -> "Opening power / shutdown menu"
    is Action.TapText -> "Tap: \"$text\""
    is Action.TapXY -> "Tap: ($x,$y)"
    is Action.Scroll -> "Scroll " + if (down) "down" else "up"
    Action.Back -> "Back pressed"
    Action.Home -> "Going Home"
    is Action.TypeText -> "Typing: \"$text\""
    Action.Enter -> "Enter pressed"
    Action.ReadScreen -> "Reading screen labels"
    is Action.Wait -> "Waiting ${ms}ms"
    is Action.Remember -> "Remembering: \"$text\""
    is Action.Flashlight -> if (on) "Turning flashlight on" else "Turning flashlight off"
    is Action.Wifi -> when (mode) {
        "connect" -> "Connecting to WiFi: ${ssid ?: ""}"
        "status" -> "Showing WiFi status"
        else -> "WiFi $mode"
    }
    is Action.Bluetooth -> when (mode) {
        "connect" -> "Connecting to Bluetooth: ${device ?: ""}"
        "list" -> "Showing paired Bluetooth devices"
        else -> "Bluetooth $mode"
    }
    is Action.StayAwake -> if (on) "Stay awake (foreground service + floating button)" else "Stay awake turned off"
}

data class Plan(val reply: String, val actions: List<Action>, val rejected: List<String>) {
    fun needsConfirm(): Boolean = false
}

object PlanParser {
    fun parse(raw: String): Plan {
        val cleaned = extractJsonObject(raw)
        val o = try {
            if (cleaned != null) JSONObject(cleaned) else JSONObject(raw.trim())
        } catch (_: Exception) {
            return Plan(cleanPlainReply(raw), emptyList(), emptyList())
        }

        val reply = o.optString("reply").trim().ifBlank {
            o.optString("text").trim().ifBlank { o.optString("message").trim() }
        }
        val arr = o.optJSONArray("actions") ?: JSONArray()
        val ok = mutableListOf<Action>()
        val rej = mutableListOf<String>()

        for (i in 0 until minOf(arr.length(), 10)) {
            val a = arr.optJSONObject(i) ?: continue
            val t = a.optString("type").trim()
            val act: Action? = when (t) {
                "open_app" -> a.optString("app").trim().takeIf { it.isNotBlank() }?.let { Action.OpenApp(it.take(60)) }
                "play_store_install" -> a.optString("query").trim().ifBlank { a.optString("app").trim() }
                    .takeIf { it.isNotBlank() }?.let { Action.PlayStoreInstall(it.take(100)) }
                "call_contact" -> a.optString("target").trim().ifBlank { a.optString("name").trim() }
                    .takeIf { it.isNotBlank() }?.let { Action.CallContact(it.take(80)) }
                "send_sms" -> {
                    val target = a.optString("target").trim().ifBlank { a.optString("name").trim() }
                    val msg = a.optString("message").trim().ifBlank { a.optString("text").trim() }
                    if (target.isNotBlank() && msg.isNotBlank()) {
                        Action.SendSms(target.take(80), msg.take(400), a.optBoolean("direct", true))
                    } else null
                }
                "whatsapp_msg" -> {
                    val target = a.optString("target").trim().ifBlank { a.optString("name").trim() }
                    val msg = a.optString("message").trim().ifBlank { a.optString("text").trim() }
                    if (msg.isNotBlank()) Action.WhatsAppMsg(target.take(80), msg.take(500)) else null
                }
                "youtube_search" -> a.optString("query").trim().takeIf { it.isNotBlank() }
                    ?.let { Action.YouTubeSearch(it.take(120), a.optBoolean("play_first", true)) }
                "media" -> a.optString("command").trim().lowercase()
                    .takeIf { it in setOf("play", "pause", "next", "previous") }
                    ?.let { Action.Media(it) }
                "volume" -> a.optString("direction").trim().lowercase()
                    .takeIf { it in setOf("up", "down", "mute", "unmute") }
                    ?.let { Action.Volume(it) }
                "system_setting" -> a.optString("panel").trim().lowercase()
                    .takeIf { it in setOf("wifi", "bluetooth", "sound", "display", "location", "airplane") }
                    ?.let { Action.SystemSetting(it) }
                "clean_phone" -> Action.CleanPhone
                "lock_screen" -> Action.LockScreen
                "power_menu" -> Action.PowerMenu
                "ui_tap_text" -> a.optString("text").trim().takeIf { it.isNotBlank() }?.let { Action.TapText(it.take(80)) }
                "ui_tap_xy" -> {
                    val x = a.optInt("x", -1)
                    val y = a.optInt("y", -1)
                    if (x in 0..5000 && y in 0..5000) Action.TapXY(x, y) else null
                }
                "ui_scroll" -> when (a.optString("direction").trim().lowercase()) {
                    "down" -> Action.Scroll(true)
                    "up" -> Action.Scroll(false)
                    else -> null
                }
                "ui_back" -> Action.Back
                "ui_home" -> Action.Home
                "ui_type" -> a.optString("text").takeIf { it.isNotEmpty() }?.let { Action.TypeText(it.take(500)) }
                "ui_enter" -> Action.Enter
                "ui_read" -> Action.ReadScreen
                "wait" -> Action.Wait(a.optLong("ms", 1000).coerceIn(200, 5000))
                "remember" -> a.optString("text").trim().takeIf { it.isNotBlank() }?.let { Action.Remember(it.take(300)) }
                // v1.3.0
                "flashlight" -> {
                    val on = when {
                        a.has("on") -> a.optBoolean("on", true)
                        a.optString("state").trim().lowercase() in setOf("off", "বন্ধ") -> false
                        else -> true
                    }
                    Action.Flashlight(on)
                }
                "wifi" -> {
                    val mode = a.optString("mode").trim().lowercase().ifBlank { a.optString("action").trim().lowercase() }
                    if (mode in setOf("on", "off", "connect", "status")) Action.Wifi(mode, a.optString("ssid").trim().takeIf { it.isNotBlank() }) else null
                }
                "bluetooth" -> {
                    val mode = a.optString("mode").trim().lowercase().ifBlank { a.optString("action").trim().lowercase() }
                    if (mode in setOf("on", "off", "connect", "list")) Action.Bluetooth(mode, a.optString("device").trim().takeIf { it.isNotBlank() }) else null
                }
                "stay_awake" -> Action.StayAwake(a.optBoolean("on", true))
                else -> null
            }
            if (act == null) rej.add(t.ifBlank { "?" }) else ok.add(act)
        }
        return Plan(reply, ok, rej)
    }

    private fun extractJsonObject(input: String): String? {
        val s = input.trim()
        val start = s.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until s.length) {
            val ch = s[i]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == '"') {
                    inString = false
                }
            } else {
                when (ch) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            return s.substring(start, i + 1)
                        }
                    }
                }
            }
        }
        return null
    }

    private fun cleanPlainReply(raw: String): String {
        return raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
