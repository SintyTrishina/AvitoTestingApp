package com.example.avitotestingapp.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
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
        bottomNav.selectedItemId = R.id.downloadedTracksActivity // Установите текущий элемент

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.audioPlayerActivity -> {
                    startActivity(Intent(this, ApiTracksActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) // Анимация перехода
                    true
                }
                else -> false
            }
        }


        val trackRecyclerView = findViewById<RecyclerView>(R.id.trackRecyclerView)
        if (clickDebounce()) {
            trackAdapter = TrackAdapter { track, position ->
                val trackIds = tracks.map { it.id } // Получаем список ID треков
                val intentAudioPlayerActivity = Intent(this, AudioPlayerActivity::class.java).apply {
                    putExtra("TRACK_ID", track.id)
                    putExtra("TRACK_IDS", trackIds.toLongArray()) // Передаем список ID треков
                    putExtra("CURRENT_TRACK_INDEX", position) // Передаем индекс текущего трека
                }
                startActivity(intentAudioPlayerActivity)}
        }

        trackRecyclerView.adapter = trackAdapter


        inputEditText = findViewById(R.id.inputEditText)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchDebounce()
            }

            override fun afterTextChanged(s: Editable?) {
                userText = s.toString()
                if (!s.isNullOrBlank()) {
                    searchDebounce()
                }
            }
        }

        inputEditText.addTextChangedListener(textWatcher)
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
        handler.postDelayed(searchRunnable, 1000L)
    }

    private val searchRunnable = Runnable { showTracks() }


    private fun showTracks() {
        if (tracks.isNotEmpty()) {
            trackAdapter.updateTracks(tracks)

        }
    }
}