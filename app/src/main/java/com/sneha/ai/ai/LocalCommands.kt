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
 * letter-spelling, e.g. "এম এ" -> "MA"). Call IS fast-pathed because its
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
        val bn = lang == "bn"
        val ok = if (bn) "ঠিক আছে, করছি।" else "Okay, doing that."

        // --- Call a contact -------------------------------------------------
        // "বাবাকে কল দাও" / "বাবাকে ফোন করো" / "call Baba" / "Baba কে কল দাও"
        val callTarget = extractCallTarget(raw, lower)
        if (callTarget != null) {
            return Plan(ok, listOf(Action.CallContact(callTarget)), emptyList())
        }

        // --- Open an app ----------------------------------------------------
        // "YouTube খোলো" / "খোলো YouTube" / "open YouTube"
        val openTarget = extractAfterAny(lower, listOf(" খোলো", " খুলো", " open "))
            ?: extractAfterPrefix(lower, listOf("খোলো ", "খুলো ", "open "))
            ?: extractBeforeSuffix(lower, listOf(" খোলো", " খুলো"))
        if (openTarget != null && openTarget.length in 2..60) {
            return Plan(ok, listOf(Action.OpenApp(openTarget)), emptyList())
        }

        // --- Play Store install / download ----------------------------------
        // "Free Fire ডাউনলোড করো" / "download Free Fire" / "install eFootball"
        val installQuery = extractBeforeSuffix(
            lower,
            listOf(" ডাউনলোড করো", " ডাউনলোড কর", " ইনস্টল করো", " ইনস্টল কর")
        ) ?: extractAfterPrefix(lower, listOf("download ", "install ", "play store "))
        if (installQuery != null && installQuery.length in 2..100) {
            return Plan(ok, listOf(Action.PlayStoreInstall(installQuery)), emptyList())
        }

        // --- YouTube search --------------------------------------------------
        // "ইউটিউবে অরিজিৎ সিং চালাও" / "youtube search Arijit Singh"
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
        // "X কে কল দাও / ফোন করো / কল করো" — X is the contact name.
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
