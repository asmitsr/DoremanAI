package com.sneha.ai.ai

/**
 * v1.2.0 — Siri-like local keyword fast-path.
 *
 * Parsed BEFORE the LLM call in MainViewModel.send(). A match returns a
 * Plan using only the existing Action types, so execution goes through the
 * existing ActionExecutor / CommsHelper paths — no new permissions are
 * invented here. Call, SMS and WhatsApp actions are NOT fast-pathed here:
 * their target/message parsing is ambiguous in free speech, so they stay
 * with the LLM JSON plan (which already handles phonetic names and
 * letter-spelling, e.g. "Bengali command Bengali command" -> "MA"). Call IS fast-pathed because its
 * target is a single contact name and placeCall() already handles
 * "not found / no permission" gracefully.
 *
 * Anything that does not match returns null and the LLM flow runs
 * exactly as before.
 */
object LocalCommands {

    fun parse(input: String, lang: String): Plan? {
        val raw = input.trim()
        if (raw.isEmpty()) return null
        val lower = raw.lowercase()
        // v1.2.1: a WhatsApp request must never be fast-pathed to a normal
        // phone call. Leave it to the LLM plan, which distinguishes a
        // WhatsApp message from a plain call. A plain call here only ever
        // returns Action.CallContact, never a WhatsApp action.
        if (lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ")) return null
        val ok = "Okay, doing that." // v2.0: visible replies are English; Bengali command matching below is kept for compatibility

        // --- v1.3.0: full-phone device control (checked BEFORE open-app,
        // so "Bengali command Bengali command" does not become an app search) ----------------
        // Flashlight / torch
        when {
            lower.contains("ফ্ল্যাশলাইট জ্বালাও") || lower.contains("ফ্ল্যাশলাইট জ্বালিয়ে দাও") ||
                lower.contains("ফ্ল্যাশলাইট চালু") || lower.contains("টর্চ জ্বালাও") ||
                lower.contains("flashlight on") || lower.contains("torch on") ->
                return Plan(ok, listOf(Action.Flashlight(true)), emptyList())
            lower.contains("ফ্ল্যাশলাইট বন্ধ") || lower.contains("ফ্ল্যাশলাইট নিভিয়ে দাও") ||
                lower.contains("টর্চ বন্ধ") || lower.contains("flashlight off") || lower.contains("torch off") ->
                return Plan(ok, listOf(Action.Flashlight(false)), emptyList())
        }
        // WiFi connect to a named network: "X Bengali command Bengali command Bengali command" /
        // "Bengali command Bengali command Bengali command X" / "connect wifi X"
        val wifiConnect = extractBeforeSuffix(lower, listOf(" ওয়াইফাইতে কানেক্ট করো", " ওয়াইফাই-তে কানেক্ট করো", " wifi তে কানেক্ট করো"))
            ?: extractAfterPrefix(lower, listOf("ওয়াইফাই কানেক্ট করো ", "wifi কানেক্ট করো ", "connect wifi ", "connect to wifi "))
        if (wifiConnect != null && wifiConnect.length in 1..60) {
            return Plan(ok, listOf(Action.Wifi("connect", wifiConnect)), emptyList())
        }
        when {
            lower.contains("ওয়াইফাই চালু") || lower.contains("ওয়াইফাই অন করো") || lower.contains("wifi on") ->
                return Plan(ok, listOf(Action.Wifi("on")), emptyList())
            lower.contains("ওয়াইফাই বন্ধ") || lower.contains("ওয়াইফাই অফ করো") || lower.contains("wifi off") ->
                return Plan(ok, listOf(Action.Wifi("off")), emptyList())
            lower.contains("ওয়াইফাই স্ট্যাটাস") || lower.contains("wifi status") ->
                return Plan(ok, listOf(Action.Wifi("status")), emptyList())
        }
        // Bluetooth connect: "Bengali command X Bengali command Bengali command" / "connect bluetooth X"
        val btConnect = extractBeforeSuffix(lower, listOf(" ব্লুটুথে কানেক্ট করো", " bluetooth এ কানেক্ট করো"))
            ?: extractAfterPrefix(lower, listOf("ব্লুটুথে কানেক্ট করো ", "connect bluetooth ", "bluetooth connect "))
        if (btConnect != null && btConnect.length in 1..60) {
            return Plan(ok, listOf(Action.Bluetooth("connect", btConnect)), emptyList())
        }
        when {
            lower.contains("ব্লুটুথ চালু") || lower.contains("ব্লুটুথ অন করো") || lower.contains("bluetooth on") ->
                return Plan(ok, listOf(Action.Bluetooth("on")), emptyList())
            lower.contains("ব্লুটুথ বন্ধ") || lower.contains("ব্লুটুথ অফ করো") || lower.contains("bluetooth off") ->
                return Plan(ok, listOf(Action.Bluetooth("off")), emptyList())
            lower.contains("পেয়ার করা ডিভাইস") || lower.contains("paired bluetooth") || lower.contains("bluetooth devices") ->
                return Plan(ok, listOf(Action.Bluetooth("list")), emptyList())
        }
        // Screen: "Bengali command Bengali command Bengali command" / "what's on my screen"
        if (lower.contains("স্ক্রিনে কী আছে") || lower.contains("স্ক্রিন পড়ো") ||
            lower.contains("what's on my screen") || lower.contains("what is on my screen") ||
            lower.contains("read the screen")) {
            return Plan(ok, listOf(Action.ReadScreen), emptyList())
        }
        // Stay awake: keep the foreground service + floating button alive
        when {
            lower.contains("সারাক্ষণ চালু রাখো") || lower.contains("ব্যাকগ্রাউন্ডে চালু রাখো") ||
                lower.contains("stay awake") || lower.contains("keep listening in background") ->
                return Plan(ok, listOf(Action.StayAwake(true)), emptyList())
            lower.contains("সারাক্ষণ বন্ধ করো") || lower.contains("ফ্লোটিং বোতাম বন্ধ") ->
                return Plan(ok, listOf(Action.StayAwake(false)), emptyList())
        }

        // --- Call a contact -------------------------------------------------
        // "Bengali command Bengali command Bengali command" / "Bengali command Bengali command Bengali command" / "call Baba" / "Baba Bengali command Bengali command Bengali command"
        val callTarget = extractCallTarget(raw, lower)
        if (callTarget != null) {
            return Plan(ok, listOf(Action.CallContact(callTarget)), emptyList())
        }

        // --- Open an app ----------------------------------------------------
        // "YouTube Bengali command" / "Bengali command YouTube" / "open YouTube"
        val openTarget = extractAfterAny(lower, listOf(" খোলো", " খুলো", " open "))
            ?: extractAfterPrefix(lower, listOf("খোলো ", "খুলো ", "open "))
            ?: extractBeforeSuffix(lower, listOf(" খোলো", " খুলো"))
        if (openTarget != null && openTarget.length in 2..60) {
            return Plan(ok, listOf(Action.OpenApp(openTarget)), emptyList())
        }

        // --- Play Store install / download ----------------------------------
        // "Free Fire Bengali command Bengali command" / "download Free Fire" / "install eFootball"
        val installQuery = extractBeforeSuffix(
            lower,
            listOf(" ডাউনলোড করো", " ডাউনলোড কর", " ইনস্টল করো", " ইনস্টল কর")
        ) ?: extractAfterPrefix(lower, listOf("download ", "install ", "play store "))
        if (installQuery != null && installQuery.length in 2..100) {
            return Plan(ok, listOf(Action.PlayStoreInstall(installQuery)), emptyList())
        }

        // --- YouTube search --------------------------------------------------
        // "Bengali command Bengali command Bengali command Bengali command" / "youtube search Arijit Singh"
        val ytQuery = extractAfterAny(lower, listOf("ইউটিউবে ", "youtube search ", "youtube "))
            ?.removeSuffix(" চালাও")?.removeSuffix(" চালান")?.removeSuffix(" play")?.trim()
        if (ytQuery != null && ytQuery.length in 2..120) {
            return Plan(ok, listOf(Action.YouTubeSearch(ytQuery, true)), emptyList())
        }

        // --- Volume ----------------------------------------------------------
        when {
            lower.contains("ভলিউম বাড়াও") || lower.contains("ভলিউম বাড়াও") ||
                lower.contains("volume up") -> return Plan(ok, listOf(Action.Volume("up")), emptyList())
            lower.contains("ভলিউম কমাও") || lower.contains("volume down") ->
                return Plan(ok, listOf(Action.Volume("down")), emptyList())
            lower.contains("ভলিউম মিউট") || lower.contains("volume mute") ||
                lower.contains("mute volume") -> return Plan(ok, listOf(Action.Volume("mute")), emptyList())
            lower.contains("ভলিউম চালু") || lower.contains("volume unmute") ->
                return Plan(ok, listOf(Action.Volume("unmute")), emptyList())
        }

        // --- Media (no query = media keys, not a search) ---------------------
        when {
            lower.contains("গান থামাও") || lower.contains("pause music") ||
                lower.contains("pause the music") -> return Plan(ok, listOf(Action.Media("pause")), emptyList())
            lower.contains("পরের গান") || lower.contains("next song") ->
                return Plan(ok, listOf(Action.Media("next")), emptyList())
            lower.contains("আগের গান") || lower.contains("previous song") ->
                return Plan(ok, listOf(Action.Media("previous")), emptyList())
            lower == "গান চালাও" || lower == "play music" ->
                return Plan(ok, listOf(Action.Media("play")), emptyList())
        }

        return null
    }

    private fun extractCallTarget(raw: String, lower: String): String? {
        // "X Bengali command Bengali command Bengali command / Bengali command Bengali command / Bengali command Bengali command" — X is the contact name.
        val suffixes = listOf("কে কল দাও", "কে কল করো", "কে ফোন করো", "কে ফোন দাও", "কে কল দাও।")
        for (s in suffixes) {
            val idx = lower.indexOf(s)
            if (idx > 0) {
                val name = raw.substring(0, idx).trim().trim(',', '।', '.')
                if (name.length in 1..80) return name
            }
        }
        val prefixes = listOf("call ", "কল দাও ", "কল করো ", "ফোন করো ", "ফোন দাও ")
        for (p in prefixes) {
            if (lower.startsWith(p)) {
                val name = raw.substring(p.length).trim().trim(',', '।', '.')
                if (name.length in 1..80) return name
            }
        }
        // "call Baba" inside a longer sentence, e.g. after the wake word strip.
        val callIdx = lower.indexOf("call ")
        if (callIdx >= 0) {
            val name = raw.substring(callIdx + 5).trim().trim(',', '।', '.')
            if (name.length in 1..80 && !name.contains(" ")) return name
        }
        return null
    }

    private fun extractAfterPrefix(lower: String, prefixes: List<String>): String? {
        for (p in prefixes) {
            if (lower.startsWith(p)) {
                val v = lower.substring(p.length).trim().trim(',', '।', '.')
                if (v.isNotBlank()) return v
            }
        }
        return null
    }

    private fun extractAfterAny(lower: String, needles: List<String>): String? {
        for (n in needles) {
            val idx = lower.indexOf(n)
            if (idx >= 0) {
                val v = lower.substring(idx + n.length).trim().trim(',', '।', '.')
                if (v.isNotBlank()) return v
            }
        }
        return null
    }

    private fun extractBeforeSuffix(lower: String, suffixes: List<String>): String? {
        for (s in suffixes) {
            val idx = lower.indexOf(s)
            if (idx > 0) {
                val v = lower.substring(0, idx).trim().trim(',', '।', '.')
                if (v.isNotBlank()) return v
            }
        }
        return null
    }
}
