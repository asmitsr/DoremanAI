package com.sneha.ai.automation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sneha.ai.MainActivity

/**
 * v1.3.0 — "stay awake" foreground service.
 *
 * This is what stops Doreman from dying when you leave the app for
 * WhatsApp / Settings / anything else:
 *  1. A foreground service (microphone type) with an ongoing notification
 *     keeps the process alive and exempt from the usual background kill.
 *     Tapping the notification re-opens Doreman.
 *  2. A floating "Doreman" button (TYPE_APPLICATION_OVERLAY, needs the
 *     "Display over other apps" permission) sits on top of other apps.
 *     Tapping it brings Doreman to the front, and MainActivity's
 *     ON_RESUME -> MainViewModel.onAppForegrounded() re-arms listening
 *     (v1.2.1), so the next command works without saying "Doreman" again.
 *
 * What this does NOT do: it does not run SpeechRecognizer invisibly in
 * the background forever. Android does not allow a normal app to listen
 * continuously from the background without a visible foreground service,
 * and a hidden always-listening mic would also drain the battery the
 * user is already short on. The honest contract is: service + floating
 * button = one tap to resume, never a silent restart of the whole flow.
 */
class DoremanVoiceService : Service() {

    companion object {
        const val ACTION_START = "com.sneha.ai.action.VOICE_START"
        const val ACTION_STOP = "com.sneha.ai.action.VOICE_STOP"
        private const val CH = "doreman_voice"
        private const val ID = 4202

        fun start(ctx: Context) {
            val i = Intent(ctx, DoremanVoiceService::class.java).setAction(ACTION_START)
            try { ContextCompat.startForegroundService(ctx, i) } catch (_: Exception) { }
        }

        fun stop(ctx: Context) {
            try { ctx.stopService(Intent(ctx, DoremanVoiceService::class.java)) } catch (_: Exception) { }
        }
    }

    private var wm: WindowManager? = null
    private var button: Button? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(ID, buildNotification())
        showFloatingButton()
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CH, "Doreman is listening", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, DoremanVoiceService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Doreman is running")
            .setContentText("Even in another app, tap the floating button to be heard")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_delete, "Stop", stop)
            .build()
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } catch (_: Exception) {
                // Already in startForeground() from onStartCommand on some
                // paths; the notification itself is what matters here.
            }
        }
        return n
    }

    private fun showFloatingButton() {
        if (!Settings.canDrawOverlays(this)) return
        if (button != null) return
        try {
            val w = getSystemService(WINDOW_SERVICE) as WindowManager
            val b = Button(this).apply {
                text = "Doreman"
                setOnClickListener {
                    val i = Intent(this@DoremanVoiceService, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(i)
                }
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 16
                y = 320
            }
            w.addView(b, params)
            wm = w
            button = b
        } catch (_: Exception) {
            // Overlay denied at runtime: the notification tap still works.
        }
    }

    override fun onDestroy() {
        try {
            val b = button
            if (b != null) wm?.removeView(b)
        } catch (_: Exception) { }
        button = null
        wm = null
        super.onDestroy()
    }
}
