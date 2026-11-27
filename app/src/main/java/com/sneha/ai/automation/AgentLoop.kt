package com.sneha.ai.automation

import com.sneha.ai.ai.AiException
import com.sneha.ai.ai.AiProvider
import com.sneha.ai.ai.ChatMsg
import com.sneha.ai.ai.GenConfig
import com.sneha.ai.ai.PlanParser
import com.sneha.ai.ai.describe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * v2.2.0: ARTEMIS-style agent loop. Instead of one LLM plan executed
 * blindly, the model sees the current screen (SnehaAccessibilityService
 * .dumpScreen), picks ONE action, sees the result, and repeats until it
 * returns no actions (goal done / answer found) or a stop condition hits:
 * 20 steps max, 3 identical screens in a row, or cancellation.
 *
 * Every action still goes through ActionExecutor, so the package
 * deny-list, notifications and the Stop button all keep working.
 */
object AgentLoop {

    private const val MAX_STEPS = 20

    private val AGENT_SYSTEM = """
You control an Android phone to achieve the user's goal. Reply ONLY with JSON: {"reply":"...","actions":[ ONE action object ]}.
Action types (with their fields): open_app {app}, play_store_install {query}, call_contact {target}, send_sms {target, message}, whatsapp_msg {target, message}, youtube_search {query, play_first}, media {command: play|pause|next|previous}, volume {direction: up|down|mute|unmute}, system_setting {panel: wifi|bluetooth|sound|display|location|airplane}, clean_phone, lock_screen, power_menu, ui_tap_text {text}, ui_tap_xy {x, y}, ui_tap_index {index}, ui_tap_any {texts: [..]}, ui_scroll {direction: down|up}, ui_back, ui_home, ui_type {text}, ui_enter, ui_read, wait {ms}, remember {text}, flashlight {on}, wifi {mode: on|off|connect|status, ssid}, bluetooth {mode: on|off|connect|list, device}, stay_awake {on}, take_photo {front}, show_last_photo, screenshot, battery_status, device_info, open_notifications, open_quick_settings, recent_apps, ui_swipe {direction: up|down|left|right}, set_alarm {hour, minute, label}, set_timer {seconds, label}, open_url {url}, set_brightness {level}, vibrate {ms}, copy_text {text}, read_clipboard.
Rules: Prefer ui_tap_text with the exact visible text. For an icon button with empty text, use ui_tap_index with its number from the element list. Use ui_tap_xy with an element's center coordinates only when a text or index tap is impossible. Use open_app to launch apps. When the goal is fully achieved, or the answer is visible on screen, return actions: [] and put the final answer in "reply" in the same language as the goal (Bengali or English). Never invent screen content; act only on listed elements. One action per reply. Return ONLY the JSON object — no markdown fences, no explanation. Example: {"reply":"","actions":[{"type":"open_app","app":"Settings"}]}
""".trimIndent()

    suspend fun run(
        goal: String,
        provider: AiProvider,
        cfg: GenConfig,
        exec: ActionExecutor,
        svc: SnehaAccessibilityService,
        onStatus: (String) -> Unit
    ): String {
        val history = mutableListOf<String>()
        var lastRaw = ""
        var lastDumpKey = ""
        var sameCount = 0
        try {
            for (step in 1..MAX_STEPS) {
                if (!currentCoroutineContext().isActive) {
                    exec.cancel()
                    return "Stopped."
                }
                val dump = svc.dumpScreen()
                val dumpKey = dump.pkg + "|" + dump.elements.joinToString(",") { it.text }
                if (dumpKey == lastDumpKey) {
                    sameCount++
                    if (sameCount >= 3) {
                        return "I could not make progress — the screen stayed the same after repeated tries." +
                            if (lastRaw.isNotBlank()) "\nModel said: ${lastRaw.take(160)}" else ""
                    }
                } else {
                    sameCount = 1
                    lastDumpKey = dumpKey
                }

                val prompt = buildString {
                    append("Goal: ").append(goal).append('\n')
                    append("Step: ").append(step).append(" of ").append(MAX_STEPS).append('\n')
                    append("Current app: ").append(dump.pkg).append('\n')
                    append("Screen elements:\n")
                    dump.elements.forEach { e ->
                        val flags = listOfNotNull(
                            if (e.clickable) "clickable" else null,
                            if (e.editable) "editable" else null
                        ).joinToString(", ")
                        append(e.index).append(". \"").append(e.text).append('"')
                        if (flags.isNotEmpty()) append(" [").append(flags).append(']')
                        append(" @(").append(e.cx).append(',').append(e.cy).append(")\n")
                    }
                    append("Actions already taken:\n")
                    if (history.isEmpty()) append("(none yet)\n")
                    else history.forEach { append(it).append('\n') }
                    append("Reply with ONE action as JSON, or actions: [] if the goal is achieved.")
                }

                val res = provider.generate(AGENT_SYSTEM, listOf(ChatMsg("user", prompt)), cfg, true)
                val reply = res.getOrElse { e ->
                    if (e is CancellationException) throw e
                    return (e as? AiException)?.userMessage() ?: "Agent error: ${e.message}"
                }
                lastRaw = reply.text
                val plan = PlanParser.parse(reply.text)
                if (plan.actions.isEmpty()) {
                    // A non-blank reply with no actions = goal done / answer.
                    if (plan.reply.isNotBlank()) return plan.reply
                    // Blank reply + no actions after real steps = finished.
                    if (history.isNotEmpty() && plan.rejected.isEmpty()) return "Done."
                    // Otherwise the model failed to act (bad JSON or a
                    // rejected action type) — feed that back and retry
                    // instead of reporting a fake "Done."
                    val why = if (plan.rejected.isNotEmpty())
                        "Your action type(s) ${plan.rejected.joinToString()} are invalid. Use exactly one action with a type from the list."
                    else
                        "You returned no action. Return exactly one action as JSON."
                    history.add("Step $step: $why")
                    onStatus("Step $step: retrying…")
                    continue
                }

                val action = plan.actions.first()
                onStatus("Step $step: ${action.describe()}")

                // ActionExecutor.run() silently returns while busy, which
                // would hang the await below — wait for it to free up first.
                while (exec.busy) {
                    if (!currentCoroutineContext().isActive) {
                        exec.cancel()
                        return "Stopped."
                    }
                    delay(100)
                }
                val result = suspendCancellableCoroutine { cont ->
                    cont.invokeOnCancellation { exec.cancel() }
                    exec.run(listOf(action)) { log, ok ->
                        if (cont.isActive) {
                            cont.resume(log.joinToString("\n").ifBlank { if (ok) "OK" else "Failed" })
                        }
                    }
                }
                history.add("Step $step: $result")
                // The notification Stop button cancels the executor, not
                // this coroutine — honour it and end the loop.
                if (result.contains("Stopped")) return "Stopped."
                delay(600) // let the UI settle before the next screen dump
            }
        } catch (e: CancellationException) {
            exec.cancel()
            return "Stopped."
        }
        return "I tried $MAX_STEPS steps but could not finish the task."
    }
}
