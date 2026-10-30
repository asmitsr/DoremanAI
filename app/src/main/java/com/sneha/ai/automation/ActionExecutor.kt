package com.sneha.ai.automation

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.sneha.ai.SnehaApp
import com.sneha.ai.data.K
import com.sneha.ai.ai.Action
import com.sneha.ai.ai.describe
import com.sneha.ai.ai.isUi
import com.sneha.ai.media.YouTubeHandoff
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExecStatus(val running: Boolean = false, val text: String = "")

object Guard {
    private val deny = listOf(
        "bank", "paytm", "phonepe", "paisa", "bkash", "nagad", "razorpay", "wallet",
        "authenticator", "keepass", "bitwarden", "lastpass", "1password",
        "permissioncontroller"
    )
    fun isBlocked(pkg: String?): Boolean = pkg != null && deny.any { pkg.contains(it, true) }
}

private data class R(val ok: Boolean, val msg: String)

class ActionExecutor(private val app: SnehaApp) {
    val status = MutableStateFlow(ExecStatus())
    private var job: Job? = null
    val busy: Boolean get() = job?.isActive == true
    private val audio: AudioManager get() = app.getSystemService(AudioManager::class.java)

    fun run(actions: List<Action>, onDone: (List<String>, Boolean) -> Unit) {
        if (busy) return
        job = app.appScope.launch {
            val log = mutableListOf<String>()
            var ok = true
            val notify = app.prefs.current().notifyAutomation
            try {
                for ((i, a) in actions.withIndex()) {
                    val label = "${i + 1}/${actions.size}: ${a.describe()}"
                    status.value = ExecStatus(true, label)
                    if (notify) Notifier.show(app, label)
                    val r = step(a)
                    log += (if (r.ok) "✓ " else "✗ ") + a.describe() + " — " + r.msg
                    if (!r.ok) { ok = false; break }
                }
            } catch (e: CancellationException) {
                log += "■ Stopped"; ok = false
            } finally {
                status.value = ExecStatus()
                Notifier.cancel(app)
            }
            onDone(log, ok)
        }
    }

    fun cancel() { job?.cancel() }

    private suspend fun step(a: Action): R {
        // v1.3.0: device actions go through DeviceControl, not the UI path.
        when (a) {
            is Action.Flashlight -> {
                val r = DeviceControl.flashlight(app, a.on)
                return R(r.first, r.second)
            }
            is Action.Wifi -> {
                val r = DeviceControl.wifi(app, a, SnehaAccessibilityService.instance)
                return R(r.first, r.second)
            }
            is Action.Bluetooth -> {
                val r = DeviceControl.bluetooth(app, a, SnehaAccessibilityService.instance)
                return R(r.first, r.second)
            }
            is Action.StayAwake -> return stayAwake(a.on)
            else -> {}
        }
        if (a.isUi()) {
            val svc = SnehaAccessibilityService.instance
            if (svc == null) {
                return when (a) {
                    is Action.YouTubeSearch -> {
                        val r = YouTubeHandoff.search(app, a.query)
                        R(r.first, r.second)
                    }
                    is Action.PlayStoreInstall -> {
                        val r = app.launcher.openPlayStoreSearch(a.query)
                        R(r.first, r.second + " (turn on Accessibility to auto-tap Install)")
                    }
                    is Action.WhatsAppMsg -> {
                        val r = app.comms.openWhatsApp(a.target, a.message)
                        R(r.first, r.second)
                    }
                    else -> R(false, "Accessibility service is not on (turn it on in Settings > Screen Automation)")
                }
            }
            val pkg = svc.foregroundPackage()
            if (a !is Action.LockScreen && a !is Action.PowerMenu && a !is Action.Back && a !is Action.Home && Guard.isBlocked(pkg)) {
                return R(false, "Automation is blocked in this app for safety ($pkg)")
            }
            return withContext(Dispatchers.Default) { uiStep(a, svc) }
        }
        return when (a) {
            is Action.OpenApp -> openApp(a.app)
            is Action.CallContact -> {
                val r = app.comms.placeCall(a.target)
                R(r.first, r.second)
            }
            is Action.SendSms -> {
                val r = app.comms.sendSms(a.target, a.message, a.direct)
                R(r.first, r.second)
            }
            is Action.YouTubeSearch -> {
                val r = YouTubeHandoff.search(app, a.query)
                R(r.first, r.second)
            }
            is Action.Media -> media(a.cmd)
            is Action.Volume -> adjustVolume(a.dir)
            is Action.SystemSetting -> openSystemSetting(a.panel)
            Action.CleanPhone -> cleanPhone()
            is Action.Wait -> { delay(a.ms); R(true, "OK") }
            is Action.Remember -> {
                if (!app.prefs.current().memoryEnabled) R(false, "Memory is off")
                else {
                    val e = app.memory.add(a.text)
                    if (e == null) R(true, "Saved to memory") else R(false, e)
                }
            }
            else -> R(false, "Unsupported action")
        }
    }

    private suspend fun uiStep(a: Action, s: SnehaAccessibilityService): R = when (a) {
        is Action.TapText -> changed(s) { s.tapByText(a.text) }
        is Action.TapXY -> changed(s) { s.tapXY(a.x, a.y) }
        is Action.Scroll -> changed(s) { s.scroll(a.down) }
        Action.Back -> { val ok = s.back(); R(ok, if (ok) "Back sent" else "Back failed") }
        Action.Home -> { val ok = s.home(); R(ok, if (ok) "Home sent" else "Home failed") }
        Action.LockScreen -> {
            val ok = s.lockScreen()
            R(ok, if (ok) "Phone screen locked" else "Could not lock the screen")
        }
        Action.PowerMenu -> {
            val ok = s.openPowerDialog()
            R(ok, if (ok) "Power menu opened" else "Could not open the power menu")
        }
        is Action.TypeText -> { val e = s.typeText(a.text); if (e == null) R(true, "Text entered") else R(false, e) }
        Action.Enter -> { val ok = s.pressEnter(); R(ok, if (ok) "Enter sent" else "Enter could not be sent") }
        Action.ReadScreen -> { val l = s.readScreen().ifEmpty { SnehaAccessibilityService.lastScreen.value.texts }; R(true, if (l.isEmpty()) "No labels found" else "\n" + l.joinToString("\n")) }
        is Action.YouTubeSearch -> playFirst(a, s)
        is Action.PlayStoreInstall -> installFromPlayStore(a.query, s)
        is Action.WhatsAppMsg -> autoSendWhatsApp(a.target, a.message, s)
        else -> R(false, "Unsupported action")
    }

    private suspend fun installFromPlayStore(query: String, s: SnehaAccessibilityService): R {
        val h = app.launcher.openPlayStoreSearch(query)
        if (!h.first) return R(false, h.second)
        delay(3200)
        var installed = false
        repeat(3) {
            if (s.tapByText("Install") || s.tapByText("ইনস্টল করুন") || s.tapByText("ইনস্টল")) {
                installed = true
                return@repeat
            }
            s.tapPlayStoreInstall(query)
            delay(2200)
            if (s.tapByText("Install") || s.tapByText("ইনস্টল করুন") || s.tapByText("ইনস্টল")) {
                installed = true
                return@repeat
            }
        }
        return if (installed) {
            R(true, "'$query' was searched in Play Store and the Install button was tapped!")
        } else {
            R(true, "Opened '$query' in Play Store")
        }
    }

    private suspend fun autoSendWhatsApp(target: String, message: String, s: SnehaAccessibilityService): R {
        val res = app.comms.openWhatsApp(target, message)
        if (!res.first) return R(false, res.second)

        if (res.second.startsWith("DIRECT_CHAT:")) {
            delay(2500)
            repeat(4) {
                if (s.tapWhatsAppSend()) {
                    return R(true, "On WhatsApp, $target was sent a message!")
                }
                delay(1000)
            }
            return R(true, "On WhatsApp, $target's chat opened")
        } else {
            delay(2600)
            var chatOpened = s.tapByText(target)
            if (!chatOpened) {
                s.tapByText("Search") || s.tapByText("খুঁজুন")
                delay(900)
                s.typeText(target)
                delay(1400)
                chatOpened = s.tapByText(target)
            }
            if (chatOpened) {
                delay(1400)
                s.typeText(message)
                delay(800)
                if (s.tapWhatsAppSend()) {
                    return R(true, "On WhatsApp, '$target' was found and the message was sent!")
                }
            }
            return R(true, "WhatsApp opened")
        }
    }

    private suspend fun cleanPhone(): R {
        return withContext(Dispatchers.Default) {
            try {
                app.cacheDir.deleteRecursively()
                Runtime.getRuntime().gc()

                val am = app.getSystemService(ActivityManager::class.java)
                val miBefore = ActivityManager.MemoryInfo()
                am.getMemoryInfo(miBefore)

                val pmOpen = app.launcher.open("Phone Master")
                if (pmOpen.ok) {
                    delay(2400)
                    SnehaAccessibilityService.instance?.let { svc ->
                        svc.tapByText("Clean") || svc.tapByText("Boost") || svc.tapByText("Junk")
                    }
                    return@withContext R(true, "Cache cleared and Phone Master cleaner launched!")
                }

                val freeMb = miBefore.availMem / (1024 * 1024)
                R(true, "Phone optimisation and cache cleaning done (free RAM: ${freeMb}MB)")
            } catch (e: Exception) {
                R(false, "Could not clean: ${e.message}")
            }
        }
    }

    private suspend fun changed(s: SnehaAccessibilityService, act: suspend () -> Boolean): R {
        val before = s.readScreen().hashCode()
        if (!act()) return R(false, "Element not found")
        delay(800)
        val after = s.readScreen().hashCode()
        return if (before != after) R(true, "Done")
        else R(true, "Tap sent")
    }

    private suspend fun playFirst(a: Action.YouTubeSearch, s: SnehaAccessibilityService): R {
        val wasPlaying = audio.isMusicActive
        val h = YouTubeHandoff.search(app, a.query)
        if (!h.first) return R(false, h.second)
        delay(3800)
        if (audio.isMusicActive && !wasPlaying) {
            return R(true, "Playback started")
        }
        var clicked = false
        repeat(3) {
            if (!clicked) {
                clicked = s.tapFirstYouTubeResult(a.query)
                if (!clicked) delay(1400)
            }
        }
        if (!clicked) return R(false, "YouTube opened but the video could not be tapped")
        repeat(14) {
            delay(500)
            if (audio.isMusicActive) {
                return R(true, "Music started")
            }
        }
        return R(true, "Video started")
    }

    private fun stayAwake(on: Boolean): R {
        // v1.3.1: remember the choice so MainViewModel can restore the
        // service after the process is recreated.
        app.appScope.launch {
            try { app.prefs.set(K.stayAwake, on) } catch (_: Exception) { }
        }
        return try {
            if (on) {
                DoremanVoiceService.start(app)
                R(true, "Stay awake is on — even in another app, tap the floating Doreman button to be heard (if overlay permission is missing, tap the notification)")
            } else {
                DoremanVoiceService.stop(app)
                R(true, "Stay awake turned off")
            }
        } catch (e: Exception) {
            R(false, "Could not turn on stay awake: " + e.message)
        }
    }

    private suspend fun openApp(name: String): R {
        val r = app.launcher.open(name, app.prefs.current().favorites)
        return R(r.ok, r.msg)
    }

    private fun adjustVolume(dir: String): R {
        return try {
            val am = audio
            when (dir) {
                "up" -> {
                    am.adjustSuggestedStreamVolume(AudioManager.ADJUST_RAISE, AudioManager.STREAM_MUSIC, AudioManager.FLAG_SHOW_UI)
                    R(true, "Volume increased")
                }
                "down" -> {
                    am.adjustSuggestedStreamVolume(AudioManager.ADJUST_LOWER, AudioManager.STREAM_MUSIC, AudioManager.FLAG_SHOW_UI)
                    R(true, "Volume decreased")
                }
                "mute" -> {
                    am.adjustSuggestedStreamVolume(AudioManager.ADJUST_MUTE, AudioManager.STREAM_MUSIC, AudioManager.FLAG_SHOW_UI)
                    R(true, "Audio muted")
                }
                else -> {
                    am.adjustSuggestedStreamVolume(AudioManager.ADJUST_UNMUTE, AudioManager.STREAM_MUSIC, AudioManager.FLAG_SHOW_UI)
                    R(true, "Audio unmuted")
                }
            }
        } catch (e: Exception) {
            R(false, "Could not change volume")
        }
    }

    private fun openSystemSetting(panel: String): R {
        val action = when (panel) {
            "wifi" -> AndroidSettings.ACTION_WIFI_SETTINGS
            "bluetooth" -> AndroidSettings.ACTION_BLUETOOTH_SETTINGS
            "sound" -> AndroidSettings.ACTION_SOUND_SETTINGS
            "display" -> AndroidSettings.ACTION_DISPLAY_SETTINGS
            "location" -> AndroidSettings.ACTION_LOCATION_SOURCE_SETTINGS
            "airplane" -> AndroidSettings.ACTION_AIRPLANE_MODE_SETTINGS
            else -> AndroidSettings.ACTION_SETTINGS
        }
        return try {
            app.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            R(true, "$panel settings opened")
        } catch (e: Exception) {
            R(false, "Could not open settings")
        }
    }

    private suspend fun media(cmd: String): R {
        val code = when (cmd) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            else -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
        }
        val am = audio
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        delay(800)
        return R(true, "Media command sent")
    }
}

object Notifier {
    private const val CH = "doreman_automation"
    private const val ID = 4201

    fun show(ctx: Context, text: String) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CH, "Doreman Automation", NotificationManager.IMPORTANCE_LOW))
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val stop = PendingIntent.getBroadcast(
            ctx, 1, Intent(ctx, StopReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Doreman is working")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_delete, "Stop", stop)
            .build()
        try { nm.notify(ID, n) } catch (_: SecurityException) {}
    }

    fun cancel(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).cancel(ID)
    }
}

class StopReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        (c.applicationContext as SnehaApp).executor.cancel()
    }
}
