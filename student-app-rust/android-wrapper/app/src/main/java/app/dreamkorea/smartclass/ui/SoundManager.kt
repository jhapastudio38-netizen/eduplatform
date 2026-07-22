package app.dreamkorea.smartclass.ui

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * SoundManager — plays click, success, error, and other UI sounds.
 * Uses Android's SoundPool for low-latency playback.
 *
 * Since we don't have bundled sound files, we synthesize simple tones
 * using AudioTrack (built-in, no assets needed).
 */
class SoundManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())

    /** Play a short click sound (synthesized). */
    fun click() {
        playTone(800.0, 30) // Short high-pitched click
    }

    /** Play a success sound (ascending two-tone). */
    fun success() {
        playTone(523.0, 80) // C5
        handler.postDelayed({ playTone(784.0, 120) }, 80) // G5
    }

    /** Play an error sound (descending two-tone). */
    fun error() {
        playTone(440.0, 100) // A4
        handler.postDelayed({ playTone(311.0, 150) }, 100) // Eb4
    }

    /** Play a "ding" for correct answer. */
    fun correct() {
        playTone(880.0, 60) // A5
        handler.postDelayed({ playTone(1318.0, 100) }, 60) // E6
    }

    /** Play a "buzz" for wrong answer. */
    fun wrong() {
        playTone(200.0, 200) // Low buzz
    }

    /** Play a swoosh for navigation. */
    fun swoosh() {
        playToneSweep(400.0, 1200.0, 100)
    }

    /**
     * Play a simple sine tone using AudioTrack.
     * No external sound files needed — fully synthesized.
     */
    private fun playTone(freq: Double, durationMs: Int) {
        try {
            val sampleRate = 44100
            val numSamples = (durationMs * sampleRate / 1000.0).toInt()
            val buffer = ShortArray(numSamples)
            val amplitude = 32767 * 0.15 // 15% volume (not too loud)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // Fade in/out envelope to avoid clicks
                val envelope = when {
                    i < numSamples * 0.1 -> i.toDouble() / (numSamples * 0.1)
                    i > numSamples * 0.9 -> (numSamples - i).toDouble() / (numSamples * 0.1)
                    else -> 1.0
                }
                val sample = (amplitude * envelope * Math.sin(2 * Math.PI * freq * t)).toInt()
                buffer[i] = sample.toShort()
            }

            val audioTrack = android.media.AudioTrack(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                android.media.AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                numSamples * 2,
                android.media.AudioTrack.MODE_STATIC,
                android.media.AudioManager.STREAM_MUSIC
            )
            audioTrack.write(buffer, 0, numSamples)
            audioTrack.play()
            // Release after playback
            handler.postDelayed({
                try { audioTrack.stop(); audioTrack.release() } catch (_: Exception) {}
            }, (durationMs + 50).toLong())
        } catch (_: Exception) {
            // Silently fail — sound is non-critical
        }
    }

    /** Play a frequency sweep (for swoosh effects). */
    private fun playToneSweep(startFreq: Double, endFreq: Double, durationMs: Int) {
        try {
            val sampleRate = 44100
            val numSamples = (durationMs * sampleRate / 1000.0).toInt()
            val buffer = ShortArray(numSamples)
            val amplitude = 32767 * 0.1

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val freq = startFreq + (endFreq - startFreq) * progress
                val t = i.toDouble() / sampleRate
                val envelope = when {
                    i < numSamples * 0.2 -> i.toDouble() / (numSamples * 0.2)
                    i > numSamples * 0.8 -> (numSamples - i).toDouble() / (numSamples * 0.2)
                    else -> 1.0
                }
                val sample = (amplitude * envelope * Math.sin(2 * Math.PI * freq * t)).toInt()
                buffer[i] = sample.toShort()
            }

            val audioTrack = android.media.AudioTrack(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                android.media.AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                numSamples * 2,
                android.media.AudioTrack.MODE_STATIC,
                android.media.AudioManager.STREAM_MUSIC
            )
            audioTrack.write(buffer, 0, numSamples)
            audioTrack.play()
            handler.postDelayed({
                try { audioTrack.stop(); audioTrack.release() } catch (_: Exception) {}
            }, (durationMs + 50).toLong())
        } catch (_: Exception) {}
    }
}

/** Returns a SoundManager scoped to the current composition. */
@Composable
fun rememberSoundManager(): SoundManager {
    val context = LocalContext.current
    return remember { SoundManager(context) }
}
