package com.example.avitotestingapp.data

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DeezerApi {

    @GET("/chart")
    fun getChart(): Call<ChartResponse>

    @GET("/search?q=query")
    fun searchTracks(@Query("q") query: String): Call<SearchResponse>

    @GET("/track/{id}")
    fun getTrackById(@Path("id") trackId: Long): Call<Track>

    companion object {
        private const val BASE_URL = "https://api.deezer.com"

        fun create(): DeezerApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DeezerApi::class.java)
        }
    }
}