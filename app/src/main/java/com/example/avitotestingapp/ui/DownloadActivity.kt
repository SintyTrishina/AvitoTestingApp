package com.example.avitotestingapp.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.example.avitotestingapp.R
import com.example.avitotestingapp.data.ChartResponse
import com.example.avitotestingapp.data.DeezerApi
import com.example.avitotestingapp.data.SearchResponse
import com.example.avitotestingapp.data.Track
import com.example.avitotestingapp.frameworks.TrackAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DownloadActivity : AppCompatActivity() {
    private var tracks = ArrayList<Track>()
    private lateinit var inputEditText: EditText
    private var userText: String = ""
    private lateinit var trackAdapter: TrackAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var isClickAllowed = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.downloadedTracksActivity

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.audioPlayerActivity -> {
                    startActivity(Intent(this, ApiTracksActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    true
                }

                else -> false
            }
        }

        val trackRecyclerView = findViewById<RecyclerView>(R.id.trackRecyclerView)
        trackAdapter = TrackAdapter { track, position ->
            if (clickDebounce()) {
                val trackIds = tracks.map { it.id }
                val intentAudioPlayerActivity =
                    Intent(this, AudioPlayerActivity::class.java).apply {
                        putExtra("TRACK_ID", track.id)
                        putExtra("TRACK_IDS", trackIds.toLongArray())
                        putExtra("CURRENT_TRACK_INDEX", position)
                    }
                startActivity(intentAudioPlayerActivity)
            }
        }
        trackRecyclerView.adapter = trackAdapter
        loadTracks()

        inputEditText = findViewById(R.id.inputEditText)
        inputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                userText = s?.toString() ?: ""
                if (userText.isEmpty()) {
                    loadTracks() // Загружаем все треки
                } else {
                    searchDebounce()
                }
            }
            override fun afterTextChanged(s: Editable?) {
                userText = s.toString()
                if (!s.isNullOrBlank()) {
                    searchDebounce()
                }
            }
        })
    }

    private fun loadTracks() {
        val sharedPrefs = getSharedPreferences("Downloaded tracks", MODE_PRIVATE)
        val allEntries = sharedPrefs.all
        val loadedTracks = mutableListOf<Track>()

        for ((key, value) in allEntries) {
            val trackJson = value as String
            val track = Gson().fromJson(trackJson, Track::class.java)
            loadedTracks.add(track)
        }

        this.tracks.clear()
        this.tracks.addAll(loadedTracks)
        trackAdapter.updateTracks(this.tracks)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(searchRunnable)
    }

    private fun clickDebounce(): Boolean {
        val current = isClickAllowed
        if (isClickAllowed) {
            isClickAllowed = false
            handler.postDelayed({ isClickAllowed = true }, 1000L)
        }
        return current
    }

    private fun searchDebounce() {
        handler.removeCallbacks(searchRunnable)
        if (inputEditText.text.isNotEmpty()) { // Запускаем поиск только если текст не пустой
            handler.postDelayed(searchRunnable, 1000L)
        }
    }

    private val searchRunnable = Runnable { showTracks() }

    private fun showTracks() {
        if (userText.isEmpty()) {
            // Если текст пустой, показываем все треки
            loadTracks()
        } else {
            // Если текст не пустой, фильтруем треки
            val filteredTracks = tracks.filter { track ->
                track.title.contains(userText, ignoreCase = true) ||
                        track.artist.name.contains(userText, ignoreCase = true)
            }
            trackAdapter.updateTracks(filteredTracks)
        }
    }
}