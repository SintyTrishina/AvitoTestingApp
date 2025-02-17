package com.example.avitotestingapp.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.avitotestingapp.R
import com.example.avitotestingapp.data.DeezerApi
import com.example.avitotestingapp.data.Track
import com.example.avitotestingapp.frameworks.MusicService
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class AudioPlayerActivity : AppCompatActivity() {

    private lateinit var buttonBack: ImageView
    private lateinit var buttonPlay: ImageButton
    private lateinit var buttonPrevious: ImageButton
    private lateinit var buttonNext: ImageButton
    private lateinit var timePlay: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var downloadButton: ImageView
    private lateinit var currentTrack: Track

    private var playerState = STATE_DEFAULT
    private var previewUrl: String? = null
    private var artistName: String? = null
    private var trackName: String? = null
    private var imageMusic: String? = null
    private var collectionName: String? = null
    private var trackId: Long? = null

    private val handler = Handler(Looper.getMainLooper())

    private var trackIds = longArrayOf()
    private var currentTrackIndex = 0
    private val searchList = ArrayList<Track>()

    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)

        sharedPrefs = getSharedPreferences("Downloaded tracks", MODE_PRIVATE)

        initViews() //инициализируем вьюшки

        trackIds = intent.getLongArrayExtra("TRACK_IDS") ?: longArrayOf() //получаем id треков
        trackId = intent.getLongExtra("TRACK_ID", -1) //получаем id трека
        currentTrackIndex = trackIds.indexOf(trackId!!) //получаем индекс трека

        getTrack() //загружаем данные трека
    }

    private fun initViews() {
        buttonBack = findViewById(R.id.back)
        buttonPlay = findViewById(R.id.buttonPlay)
        timePlay = findViewById(R.id.timePlay)
        buttonNext = findViewById(R.id.buttonNext)
        buttonPrevious = findViewById(R.id.buttonPrevious)
        seekBar = findViewById(R.id.seekBar)
        downloadButton = findViewById(R.id.download)

        buttonBack.setOnClickListener {
            val intent = Intent(this@AudioPlayerActivity, MusicService::class.java).apply {
                action = "STOP"
            }
            startService(intent) // используем startService для отправки команды
            finish()
        }
        buttonPlay.setOnClickListener { playbackControl() }
        buttonPrevious.setOnClickListener { playPreviousTrack() }
        buttonNext.setOnClickListener { playNextTrack() }

        downloadButton.setOnClickListener {
            if (::currentTrack.isInitialized) {
                addTrack(currentTrack)
            } else {
                Log.e("AudioPlayerActivity", "currentTrack is not initialized")
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val intent = Intent(this@AudioPlayerActivity, MusicService::class.java).apply {
                        action = "SEEK_TO"
                        putExtra("SEEK_TO", progress)
                    }
                    startService(intent)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }


    private fun addTrack(track: Track) {
        Log.d("AudioPlayerActivity", "Adding track: ${track.title}")
        if (searchList.contains(track)) {
            searchList.remove(track)
            Log.d("AudioPlayerActivity", "Track removed from searchList")
        }

        searchList.add(0, track)
        Log.d("AudioPlayerActivity", "Track added to searchList")

        saveToSharedPrefs(track)
    }

    private fun saveToSharedPrefs(track: Track) {
        val json = Gson().toJson(track)
        if (json != null) {
            val key = track.id.toString()
            sharedPrefs.edit()
                .putString(key, json)
                .apply()
            Log.d("AudioPlayerActivity", "Track saved to SharedPreferences: $key")
        } else {
            Log.e("AudioPlayerActivity", "Failed to convert track to JSON")
        }
    }

    private fun getTrack() {
        if (trackIds.isNotEmpty() && currentTrackIndex in trackIds.indices) {
            val trackId = trackIds[currentTrackIndex]
            lifecycleScope.launch {
                try {
                    val track = DeezerApi.create().getTrackById(trackId) // Вызов suspend-функции
                    previewUrl = track.preview
                    artistName = track.artist.name
                    trackName = track.title
                    imageMusic = track.album.coverBig
                    collectionName = track.album.title

                    setupTrackInfo(track)
                    currentTrack = track
                    previewUrl?.let { url -> startMusicService(url) } ?: Log.e(
                        "AudioPlayerActivity",
                        "Failed to get track preview URL"
                    )
                } catch (e: Exception) {
                    Log.e("AudioPlayerActivity", "Failed to load track data", e)
                }
            }
        }
    }

    private fun startMusicService(url: String) {
        val intent = Intent(this, MusicService::class.java).apply {
            putExtra("PREVIEW_URL", url)
            putExtra("TRACK_IDS", trackIds)
            putExtra("CURRENT_TRACK_INDEX", currentTrackIndex)
        }
        startService(intent)
    }

    private val updatingTime = object : Runnable {
        override fun run() {
            if (playerState == STATE_PLAYING) {
                handler.postDelayed(this, UPDATE_TIME)
            }
        }
    }

    private fun playbackControl() {
        lifecycleScope.launch {
            val intent = Intent(this@AudioPlayerActivity, MusicService::class.java).apply {
                action = if (playerState == STATE_PLAYING) "PAUSE" else "RESUME"
            }
            startService(intent)
        }
    }


    private fun playPreviousTrack() {
        if (trackIds.isNotEmpty()) {
            currentTrackIndex = (currentTrackIndex - 1 + trackIds.size) % trackIds.size
            getTrack()
        }
    }

    private fun playNextTrack() {
        if (trackIds.isNotEmpty()) {
            currentTrackIndex = (currentTrackIndex + 1) % trackIds.size
            getTrack()
        }
    }

    private fun setupTrackInfo(track: Track?) {
        track?.let {
            findViewById<TextView>(R.id.trackName).text = track.title
            findViewById<TextView>(R.id.artistName).text = track.artist.name
            val collectionNameView = findViewById<TextView>(R.id.collectionName)
            // проверяем, есть ли URL изображения
            val imageUrl = track.album.coverBig

            // если URL есть, загружаем изображение
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder) // устанавливаем placeholder
                .error(R.drawable.placeholder) // устанавливаем placeholder в случае ошибки
                .into(findViewById(R.id.imageMusic))

            if (collectionName == null) {
                collectionNameView.visibility = View.GONE
            }
            collectionNameView.text = track.album.title
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updatingTime)
        val intent = Intent(this@AudioPlayerActivity, MusicService::class.java).apply {
            action = "STOP"
        }
        startService(intent)
    }

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "UPDATE_PROGRESS" -> {
                    val currentPosition = intent.getIntExtra("CURRENT_POSITION", 0)
                    val duration = intent.getIntExtra("DURATION", 0)
                    updateSeekBar(currentPosition, duration)
                }

                "PLAYBACK_STATE" -> {
                    val isPlaying = intent.getBooleanExtra("IS_PLAYING", false)
                    updatePlayButton(isPlaying)
                }
            }
        }
    }

    private fun updateSeekBar(currentPosition: Int, duration: Int) {
        seekBar.max = duration
        seekBar.progress = currentPosition
        timePlay.text = SimpleDateFormat("mm:ss", Locale.getDefault()).format(currentPosition)
    }

    private fun updatePlayButton(isPlaying: Boolean) {
        if (isPlaying) {
            buttonPlay.setImageResource(R.drawable.pause_button)
            playerState = STATE_PLAYING
        } else {
            buttonPlay.setImageResource(R.drawable.black_button)
            playerState = STATE_PAUSED
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction("UPDATE_PROGRESS")
            addAction("PLAYBACK_STATE")
        }
        registerReceiver(updateReceiver, filter, RECEIVER_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(updateReceiver)
    }

    companion object {
        private const val STATE_DEFAULT = 0
        private const val STATE_PLAYING = 2
        private const val STATE_PAUSED = 3
        private const val UPDATE_TIME = 300L
    }
}