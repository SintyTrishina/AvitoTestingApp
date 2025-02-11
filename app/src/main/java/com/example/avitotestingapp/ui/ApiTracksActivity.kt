package com.example.avitotestingapp.ui

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
import com.example.avitotestingapp.data.Track
import com.example.avitotestingapp.data.DeezerApi
import com.example.avitotestingapp.data.SearchResponse
import com.example.avitotestingapp.frameworks.TrackAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ApiTracksActivity : AppCompatActivity() {
    private var tracks = ArrayList<Track>()
    private var results = ArrayList<Track>()
    private lateinit var inputEditText: EditText
    private var userText: String = ""
    private lateinit var trackAdapter: TrackAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var isClickAllowed = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_tracks)


        val trackRecyclerView = findViewById<RecyclerView>(R.id.trackRecyclerView)
        trackAdapter = TrackAdapter()
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
                            results.clear()
                            results.addAll(data)
                            Log.d("AddResult", "$results") // Логируем данные
                            trackAdapter.updateTracks(results)
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
                            results.clear()
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