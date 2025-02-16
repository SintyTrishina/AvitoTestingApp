package com.example.avitotestingapp.frameworks

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()
    }


    private fun stopPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopSelf() // Останавливаем сервис
    }


    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                val intent = Intent("UPDATE_PROGRESS").apply {
                    putExtra("CURRENT_POSITION", it.currentPosition)
                    putExtra("DURATION", it.duration)
                }
                sendBroadcast(intent)
            }
            handler.postDelayed(this, 1000) // Обновляем каждую секунду
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "PAUSE" -> pausePlayback()
            "RESUME" -> resumePlayback()
            "SEEK_TO" -> {
                val progress = intent.getIntExtra("SEEK_TO", 0)
                mediaPlayer?.seekTo(progress)
            }
            "STOP" -> stopPlayback() // Обрабатываем команду STOP
            else -> {
                val url = intent?.getStringExtra("PREVIEW_URL")
                if (url != null) {
                    startPlayback(url)
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun startPlayback(url: String) {
        mediaPlayer?.reset()
        mediaPlayer?.setDataSource(url)
        mediaPlayer?.prepareAsync()
        mediaPlayer?.setOnPreparedListener {
            it.start()
            handler.post(updateProgressRunnable)
            sendPlaybackState(true)
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        handler.removeCallbacks(updateProgressRunnable)
        sendPlaybackState(false)
    }

    private fun resumePlayback() {
        mediaPlayer?.start()
        handler.post(updateProgressRunnable)
        sendPlaybackState(true)
    }

    private fun sendPlaybackState(isPlaying: Boolean) {
        val intent = Intent("PLAYBACK_STATE").apply {
            putExtra("IS_PLAYING", isPlaying)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}