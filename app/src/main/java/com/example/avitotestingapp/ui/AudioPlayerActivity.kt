package com.example.avitotestingapp.ui

import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.avitotestingapp.R
import com.example.avitotestingapp.data.DeezerApi
import com.example.avitotestingapp.data.Track
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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

    private var mediaPlayer: MediaPlayer? = null
    private var playerState = STATE_DEFAULT
    private var previewUrl: String? = null
    private var artistName: String? = null
    private var trackName: String? = null
    private var imageMusic: String? = null
    private var trackId: Long? = null
    private val handler = Handler(Looper.getMainLooper())
    private var trackIds = longArrayOf()
    private var currentTrackIndex = 0

    lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)
        sharedPrefs = getSharedPreferences("Downloaded tracks", MODE_PRIVATE)
        initViews()
        initMediaPlayer()

        trackIds = intent.getLongArrayExtra("TRACK_IDS") ?: longArrayOf()
        trackId = intent.getLongExtra("TRACK_ID", 0L)
        currentTrackIndex = intent.getIntExtra("CURRENT_TRACK_INDEX", 0)

        loadTrackData()
    }

    private fun initViews() {
        buttonBack = findViewById(R.id.back)
        buttonPlay = findViewById(R.id.buttonPlay)
        timePlay = findViewById(R.id.timePlay)
        buttonNext = findViewById(R.id.buttonNext)
        buttonPrevious = findViewById(R.id.buttonPrevious)
        seekBar = findViewById(R.id.seekBar)
        downloadButton = findViewById(R.id.download)

        buttonBack.setOnClickListener { finish() }
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
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private val searchList = ArrayList<Track>()

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

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setOnPreparedListener {
                buttonPlay.isEnabled = true
                playerState = STATE_PREPARED
                seekBar.max = it.duration
                startPlayer()
            }
            setOnCompletionListener {
                playerState = STATE_PREPARED
                buttonPlay.setImageResource(R.drawable.black_button)
                handler.removeCallbacks(updatingTime)
                playNextTrack()
            }
        }
    }

    private fun loadTrackData() {
        if (trackIds.isNotEmpty() && currentTrackIndex in trackIds.indices) {
            val trackId = trackIds[currentTrackIndex]
            getTrack(trackId)
        }
    }

    private fun getTrack(trackId: Long) {
        DeezerApi.create().getTrackById(trackId = trackId).enqueue(object : Callback<Track> {
            override fun onResponse(call: Call<Track>, response: Response<Track>) {
                if (response.isSuccessful) {
                    response.body()?.let { track ->
                        previewUrl = track.preview
                        artistName = track.artist.name
                        trackName = track.title
                        imageMusic = track.album.cover_big

                        setupTrackInfo(track)
                        currentTrack = track
                        previewUrl?.let { url -> preparePlayer(url) }
                    }
                } else {
                    Log.e("AudioPlayerActivity", "Failed to get track: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Track>, t: Throwable) {
                Log.e("AudioPlayerActivity", "Failed to get track", t)
            }
        })
    }

    private fun preparePlayer(url: String) {
        mediaPlayer?.reset()
        mediaPlayer?.setDataSource(url)
        mediaPlayer?.prepareAsync()
    }

    private val updatingTime = object : Runnable {
        override fun run() {
            if (playerState == STATE_PLAYING) {
                mediaPlayer?.currentPosition?.let {
                    timePlay.text = SimpleDateFormat("mm:ss", Locale.getDefault()).format(it)
                    seekBar.progress = it
                }
                handler.postDelayed(this, UPDATE_TIME)
            }
        }
    }

    private fun startPlayer() {
        mediaPlayer?.start()
        buttonPlay.setImageResource(R.drawable.pause_button)
        playerState = STATE_PLAYING
        handler.post(updatingTime)
    }

    private fun pausePlayer() {
        mediaPlayer?.pause()
        buttonPlay.setImageResource(R.drawable.black_button)
        playerState = STATE_PAUSED
        handler.removeCallbacks(updatingTime)
    }

    private fun playbackControl() {
        when (playerState) {
            STATE_PLAYING -> pausePlayer()
            STATE_PREPARED, STATE_PAUSED -> startPlayer()
        }
    }

    private fun playPreviousTrack() {
        if (trackIds.isNotEmpty()) {
            currentTrackIndex = (currentTrackIndex - 1 + trackIds.size) % trackIds.size
            loadTrackData()
        }
    }

    private fun playNextTrack() {
        if (trackIds.isNotEmpty()) {
            currentTrackIndex = (currentTrackIndex + 1) % trackIds.size
            loadTrackData()
        }
    }

    private fun setupTrackInfo(track: Track?) {
        track?.let {
            findViewById<TextView>(R.id.trackName).text = track.title
            findViewById<TextView>(R.id.artistName).text = track.artist.name

            Glide.with(this)
                .load(track.album.cover_big)
                .placeholder(R.drawable.placeholder)
                .into(findViewById(R.id.imageMusic))
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updatingTime)
        pausePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updatingTime)
        mediaPlayer?.release()
        mediaPlayer = null
    }

    companion object {
        private const val STATE_DEFAULT = 0
        private const val STATE_PREPARED = 1
        private const val STATE_PLAYING = 2
        private const val STATE_PAUSED = 3
        private const val UPDATE_TIME = 300L
    }
}