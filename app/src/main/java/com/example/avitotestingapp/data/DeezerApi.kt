package com.example.avitotestingapp.data

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface DeezerApi {

    @GET("/chart")
    suspend fun getChart(): Call<ChartResponse>

    @GET("/search?q=query")
    suspend fun searchTracks(@Query("q") query: String): Call<SearchResponse>

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