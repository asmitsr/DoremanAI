package com.sneha.ai.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechInput(private val ctx: Context) {
    private var rec: SpeechRecognizer? = null
    private val audio: AudioManager? = ctx.getSystemService(AudioManager::class.java)
    private var mutedBeep = false

    fun available() = SpeechRecognizer.isRecognitionAvailable(ctx)

    private fun muteBeepSound() {
        if (mutedBeep) return
        try {
            audio?.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
            audio?.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
            mutedBeep = true
        } catch (_: Exception) {}
    }

    private fun unmuteBeepSound() {
        if (!mutedBeep) return
        try {
            audio?.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
            audio?.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
        } catch (_: Exception) {}
        mutedBeep = false
    }

    fun start(
        lang: String,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit,
        onLevel: (Float) -> Unit,
    ) {
        release()
        if (!available()) {
            onError("এই ফোনে স্পিচ রিকগনিশন সার্ভিস নেই")
            return
        }
        muteBeepSound()
        val r = SpeechRecognizer.createSpeechRecognizer(ctx)
        rec = r
        // Accumulated words from this session. Android ends a recognizer
        // session on silence/end-of-speech, so long speech must not be lost
        // when that happens (see onError below).
        var lastPartial = ""
        var delivered = false
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {
                unmuteBeepSound()
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rms: Float) { onLevel(((rms + 2f) / 12f).coerceIn(0f, 1f)) }
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {
                muteBeepSound()
            }
            override fun onError(code: Int) {
                release()
                unmuteBeepSound()
                // v1.2.0: a timeout AFTER words were already recognised is not
                // "nothing heard" — deliver the recognised words as the final
                // result instead of dropping them. Only a timeout/no-match
                // with no recognised words at all is reported as an error,
                // and in that case MainViewModel relistening drops nothing.
                if (!delivered && (code == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                        code == SpeechRecognizer.ERROR_NO_MATCH) && lastPartial.isNotBlank()
                ) {
                    delivered = true
                    onFinal(lastPartial)
                    return
                }
                onError(
                    when (code) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কিছু শুনতে পাইনি"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "নেটওয়ার্ক সমস্যা"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন অনুমতি নেই"
                        else -> "ত্রুটি ($code)"
                    }
                )
            }
            override fun onResults(b: Bundle?) {
                val t = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                release()
                unmuteBeepSound()
                delivered = true
                if (t.isBlank() && lastPartial.isBlank()) onError("কিছু শুনতে পাইনি")
                else onFinal(if (t.isNotBlank()) t else lastPartial)
            }
            override fun onPartialResults(b: Bundle?) {
                val p = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!p.isNullOrBlank()) {
                    // Partials within one session are cumulative on most
                    // devices; keep the longest recognised form so the UI
                    // and a later timeout-delivery never go backwards.
                    if (p.length >= lastPartial.length) lastPartial = p
                    onPartial(lastPartial)
                }
            }
            override fun onEvent(t: Int, b: Bundle?) {}
        })
        val tag = if (lang == "bn") "bn-BD" else "en-US"
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, tag)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // v1.2.0: give long / slow speech room before the platform ends
            // the session. Complete-silence 2500ms, possibly-complete 1200ms,
            // minimum length 1000ms. Note: these are hints — some devices /
            // recognizer services may ignore them, which is why the
            // timeout-delivery above is kept as a second safety net.
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
        r.startListening(i)
    }

    fun stop() {
        muteBeepSound()
        rec?.stopListening()
        unmuteBeepSound()
    }

    fun release() {
        try {
            rec?.destroy()
        } catch (_: Exception) {}
        rec = null
        unmuteBeepSound()
    }
}
