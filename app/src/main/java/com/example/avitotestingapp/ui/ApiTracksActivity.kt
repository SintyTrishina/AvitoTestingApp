package com.example.avitotestingapp.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

class ApiTracksActivity : AppCompatActivity() {
    private var tracks = ArrayList<Track>()

    private lateinit var inputEditText: EditText
    private var userText: String = ""
    private lateinit var trackAdapter: TrackAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var isClickAllowed = true
    private lateinit var progressBar: ProgressBar
    private lateinit var placeholderMessage: TextView
    private lateinit var placeholderImage: ImageView
    private lateinit var updateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_tracks)
        progressBar = findViewById(R.id.progressBar)
        placeholderMessage = findViewById(R.id.placeholderMessage)
        placeholderImage = findViewById(R.id.placeholderImage)
        updateButton = findViewById(R.id.updateButton)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.audioPlayerActivity // Установите текущий элемент

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
        DeezerApi.create().getChart()
            .enqueue(object : Callback<ChartResponse> {
                override fun onResponse(
                    call: Call<ChartResponse>,
                    response: Response<ChartResponse>
                ) {

                    if (response.isSuccessful && response.body() != null) {
                        progressBar.visibility = View.GONE
                        response.body()?.tracks?.data?.let { data ->
                            tracks.clear()
                            tracks.addAll(data)
                            Log.d("AddResult", "$tracks") // Логируем данные
                            trackAdapter.updateTracks(tracks)
                        }
                        if (tracks.isEmpty()) {
                            placeholderImage.setImageResource(R.drawable.error)
                            placeholderImage.visibility = View.VISIBLE
                            updateButton.visibility = View.GONE
                            showMessage(getString(R.string.nothing_found), "")
                        } else {
                            placeholderImage.visibility = View.GONE
                            updateButton.visibility = View.GONE
                            showMessage("", "")
                        }
                    }
                    else {
                        placeholderImage.setImageResource(R.drawable.errorconnection)
                        placeholderImage.visibility = View.VISIBLE
                        updateButton.visibility = View.VISIBLE
                        showMessage(
                            getString(R.string.something_went_wrong), response.code().toString()
                        )
                    }
                }


                override fun onFailure(call: Call<ChartResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    placeholderImage.setImageResource(R.drawable.errorconnection)
                    placeholderImage.visibility = View.VISIBLE
                    updateButton.visibility = View.VISIBLE
                    showMessage(
                        getString(R.string.something_went_wrong), t.message.toString()
                    )
                }
            })
    }

    private fun search() {
        // Скрываем историю и показываем ProgressBar
        progressBar.visibility = View.VISIBLE
        hideSearchHistory()

        // Проверяем, есть ли текст для поиска
        if (inputEditText.text.isNotEmpty()) {
            DeezerApi.create().searchTracks(query = inputEditText.text.toString())
                .enqueue(object : Callback<SearchResponse> {
                    override fun onResponse(
                        call: Call<SearchResponse>, response: Response<SearchResponse>
                    ) {
                        progressBar.visibility = View.GONE // Скрываем ProgressBar после завершения
                        if (response.code() == 200) {
                            tracks.clear()
                            if (response.body()?.data?.isNotEmpty() == true) {
                                tracks.addAll(response.body()?.data!!)
                                showTracks()
                            } else {
                                placeholderImage.setImageResource(R.drawable.error)
                                placeholderImage.visibility = View.VISIBLE
                                updateButton.visibility = View.GONE
                                showMessage(getString(R.string.nothing_found), "")
                            }
                        } else {
                            placeholderImage.setImageResource(R.drawable.errorconnection)
                            placeholderImage.visibility = View.VISIBLE
                            updateButton.visibility = View.VISIBLE
                            showMessage(
                                getString(R.string.something_went_wrong), response.code().toString()
                            )
                        }
                    }

                    override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                        progressBar.visibility = View.GONE
                        placeholderImage.setImageResource(R.drawable.errorconnection)
                        placeholderImage.visibility = View.VISIBLE
                        updateButton.visibility = View.VISIBLE
                        showMessage(
                            getString(R.string.something_went_wrong), t.message.toString()
                        )
                    }
                })
        } else {
            // Если текст пустой, скрываем ProgressBar и очищаем историю
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