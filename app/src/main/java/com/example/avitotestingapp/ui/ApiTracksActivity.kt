package com.example.avitotestingapp.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_tracks)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.audioPlayerActivity // Установите текущий элемент

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.downloadedTracksActivity -> {
                    startActivity(Intent(this, DownloadActivity::class.java))
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
                } else {
                    getChartTracks()
                }
            }
        }

        inputEditText.addTextChangedListener(textWatcher)

        getChartTracks()
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
        DeezerApi.create().getChart()
            .enqueue(object : Callback<ChartResponse> {
                override fun onResponse(
                    call: Call<ChartResponse>,
                    response: Response<ChartResponse>
                ) {

                    if (response.isSuccessful && response.body() != null) {
                        response.body()?.tracks?.data?.let { data ->
                            tracks.clear()
                            tracks.addAll(data)
                            Log.d("AddResult", "$tracks") // Логируем данные
                            trackAdapter.updateTracks(tracks)
                        } ?: Log.e("AddResult", "Data is null") // Логируем, если data == null
                    } else {
                        Log.e("AddResult", "Response is not successful or body is null")
                    }
                }

                override fun onFailure(call: Call<ChartResponse>, t: Throwable) {
                    showMessage("something_went_wrong", t.message.toString())
                    Log.e("DeezerApi", "Error fetching chart tracks", t)
                }
            })
    }

    private fun search() {
        if (inputEditText.text.isNotEmpty()) {

            DeezerApi.create().searchTracks(query = inputEditText.text.toString())
                .enqueue(object : Callback<SearchResponse> {
                    override fun onResponse(
                        call: Call<SearchResponse>, response: Response<SearchResponse>
                    ) {
                        if (response.code() == 200) {
                            tracks.clear()
                            if (response.body()?.data?.isNotEmpty() == true) {
                                tracks.addAll(response.body()?.data!!)
                                showTracks()
                            }
                            if (tracks.isEmpty()) {

                                showMessage("nothing_found", "")
                            } else {

                                showMessage("", "")
                            }
                        } else {
                            showMessage(
                                "something_went_wrong", response.code().toString()
                            )
                        }
                    }

                    override fun onFailure(call: Call<SearchResponse>, t: Throwable) {

                        showMessage(
                            "something_went_wrong", t.message.toString()
                        )
                    }
                })
        }
    }

    private fun showMessage(text: String, additionalMessage: String) {
        if (text.isNotEmpty()) {
            tracks.clear()
            trackAdapter.updateTracks(tracks)
        }
    }

    private fun showTracks() {
        if (tracks.isNotEmpty()) {
            trackAdapter.updateTracks(tracks)

        }
    }
}