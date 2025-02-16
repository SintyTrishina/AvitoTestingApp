package com.example.avitotestingapp.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DeezerApi {

    @GET("/chart")
    suspend fun getChart(): ChartResponse

    @GET("/search?q=query")
    suspend fun searchTracks(@Query("q") query: String): SearchResponse

    @GET("/track/{id}")
    suspend fun getTrackById(@Path("id") trackId: Long): Track

    companion object {
        private const val BASE_URL = "https://api.deezer.com"

        //Создание ретрофит
        fun create(): DeezerApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DeezerApi::class.java)
        }
    }
}