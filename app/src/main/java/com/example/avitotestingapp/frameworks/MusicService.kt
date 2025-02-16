package com.example.avitotestingapp.frameworks

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {

            "PAUSE" -> pausePlayback() //если действие пауза, ставим на паузу

            "RESUME" -> resumePlayback() //возобновление воспроизведения

            "SEEK_TO" -> {
                val progress = intent.getIntExtra("SEEK_TO", 0)
                mediaPlayer?.seekTo(progress)
            } //перемещает позицию воспроизведения на указанное количество миллисекунд

            "STOP" -> stopPlayback() //останавливаем воспроизведение
            else -> {
                val url = intent?.getStringExtra("PREVIEW_URL")
                if (url != null) {
                    startPlayback(url) //воспроизведение по url
                }
            }
        }
        return START_NOT_STICKY //автоматически сервис не перезапускается
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressRunnable) //отменяем выполнение всех заданий
        mediaPlayer?.release() //освобождаем ресурсы
        mediaPlayer = null
    }

    //обновляем прогресс воспроизведения
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                val intent = Intent("UPDATE_PROGRESS").apply {
                    putExtra("CURRENT_POSITION", it.currentPosition)
                    putExtra("DURATION", it.duration)
                }
                sendBroadcast(intent) //отправляем широковещательное сообщение
            }
            handler.postDelayed(this, 1000) // обновляем каждую секунду
        }
    }


    private fun stopPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopSelf() // останавливаем сервис
    }

    //подготавливаем и запускаем медиаплеер
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
        sendPlaybackState(false) // пауза
    }

    private fun resumePlayback() {
        mediaPlayer?.start()
        handler.post(updateProgressRunnable)
        sendPlaybackState(true) // возобновление
    }

    private fun sendPlaybackState(isPlaying: Boolean) {
        val intent = Intent("PLAYBACK_STATE").apply {
            putExtra("IS_PLAYING", isPlaying)
        }
        sendBroadcast(intent) //отправляем состояние плеера
    }
}