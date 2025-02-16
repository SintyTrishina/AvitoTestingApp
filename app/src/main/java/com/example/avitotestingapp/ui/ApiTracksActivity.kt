package com.example.avitotestingapp.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.avitotestingapp.R
import com.example.avitotestingapp.data.DeezerApi
import com.example.avitotestingapp.data.Track
import com.example.avitotestingapp.frameworks.TrackAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class ApiTracksActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var placeholderMessage: TextView
    private lateinit var placeholderImage: ImageView
    private lateinit var updateButton: Button
    private lateinit var inputEditText: EditText

    private var tracks = ArrayList<Track>()
    private var userText: String = ""

    private lateinit var trackAdapter: TrackAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var isClickAllowed = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_tracks)

        progressBar = findViewById(R.id.progressBar)
        placeholderMessage = findViewById(R.id.placeholderMessage)
        placeholderImage = findViewById(R.id.placeholderImage)
        updateButton = findViewById(R.id.updateButton)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.audioPlayerActivity //устанавливаем текущее состояние

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.downloadedTracksActivity -> {
                    startActivity(Intent(this, DownloadActivity::class.java))
                    overridePendingTransition(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    ) // Анимация перехода
                    true
                }

                else -> false
            }
        }

        val trackRecyclerView = findViewById<RecyclerView>(R.id.trackRecyclerView)

        if (clickDebounce()) {
            trackAdapter = TrackAdapter { track, position ->
                val trackIds = tracks.map { it.id } // Получаем список ID треков
                val intentAudioPlayerActivity =
                    Intent(this, AudioPlayerActivity::class.java).apply {
                        putExtra("TRACK_ID", track.id)
                        putExtra("TRACK_IDS", trackIds.toLongArray()) // Передаем список ID треков
                        putExtra("CURRENT_TRACK_INDEX", position) // Передаем индекс текущего трека
                        putExtra("PREVIEW_URL", track.preview)
                    }
                startActivity(intentAudioPlayerActivity)
            }
        }

        trackRecyclerView.adapter = trackAdapter

        inputEditText = findViewById(R.id.inputEditText)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                userText = s?.toString() ?: ""
                if (userText.isEmpty()) {
                    // Если текст пустой, очищаем историю и загружаем чарт
                    hideSearchHistory()
                    getChartTracks()
                } else {
                    // Если текст не пустой, запускаем поиск с задержкой
                    searchDebounce()
                }
                if (s.isNullOrEmpty()) {
                    placeholderImage.visibility = View.GONE
                    placeholderMessage.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {
                userText = s.toString()
                if (!s.isNullOrBlank()) {
                    searchDebounce()
                }
            }
        }

        inputEditText.addTextChangedListener(textWatcher)

        updateButton.setOnClickListener {
            searchDebounce()
        }
        getChartTracks()
    }

    private fun hideSearchHistory() {
        placeholderImage.visibility = View.GONE
        updateButton.visibility = View.GONE
        placeholderMessage.visibility = View.GONE
        tracks.clear()
        trackAdapter.updateTracks(tracks)
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

    private val searchRunnable = Runnable { search() }

    private fun getChartTracks() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = DeezerApi.create().getChart() // Вызов suspend-функции
                if (response.tracks.data.isNotEmpty()) {
                    tracks.clear()
                    tracks.addAll(response.tracks.data)
                    trackAdapter.updateTracks(tracks)
                    placeholderImage.visibility = View.GONE
                    updateButton.visibility = View.GONE
                    showMessage("", "")
                } else {
                    placeholderImage.setImageResource(R.drawable.error)
                    placeholderImage.visibility = View.VISIBLE
                    updateButton.visibility = View.GONE
                    showMessage(getString(R.string.nothing_found), "")
                }
            } catch (e: Exception) {
                placeholderImage.setImageResource(R.drawable.errorconnection)
                placeholderImage.visibility = View.VISIBLE
                updateButton.visibility = View.VISIBLE
                showMessage(getString(R.string.something_went_wrong), e.message.toString())
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun search() {
        progressBar.visibility = View.VISIBLE
        hideSearchHistory()

        val query = inputEditText.text.toString()
        if (query.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val response = DeezerApi.create().searchTracks(query) // Вызов suspend-функции
                    if (response.data.isNotEmpty()) {
                        tracks.clear()
                        tracks.addAll(response.data)
                        showTracks()
                    } else {
                        placeholderImage.setImageResource(R.drawable.error)
                        placeholderImage.visibility = View.VISIBLE
                        updateButton.visibility = View.GONE
                        showMessage(getString(R.string.nothing_found), "")
                    }
                } catch (e: Exception) {
                    placeholderImage.setImageResource(R.drawable.errorconnection)
                    placeholderImage.visibility = View.VISIBLE
                    updateButton.visibility = View.VISIBLE
                    showMessage(getString(R.string.something_went_wrong), e.message.toString())
                } finally {
                    progressBar.visibility = View.GONE
                }
            }
        } else {
            progressBar.visibility = View.GONE
            hideSearchHistory()
        }
    }

    private fun showMessage(text: String, additionalMessage: String) {
        if (text.isNotEmpty()) {
            placeholderMessage.visibility = View.VISIBLE
            tracks.clear()
            trackAdapter.updateTracks(tracks)
            placeholderMessage.text = text
            if (additionalMessage.isNotEmpty()) {
                Toast.makeText(applicationContext, additionalMessage, Toast.LENGTH_LONG).show()
            }
        } else {
            placeholderMessage.visibility = View.GONE
        }
    }


    private fun showTracks() {
        if (tracks.isNotEmpty()) {
            trackAdapter.updateTracks(tracks)
            placeholderImage.visibility = View.GONE
            placeholderMessage.visibility = View.GONE
            updateButton.visibility = View.GONE
        }
    }
}