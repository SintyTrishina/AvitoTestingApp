package com.example.avitotestingapp.data

import com.google.gson.annotations.SerializedName

data class Artist(
    val id: Int,
    val link: String,
    val name: String? = "",
    val picture: String,
    @SerializedName("picture_big") val pictureBig: String,
    @SerializedName("picture_medium") val pictureMedium: String,
    @SerializedName("picture_small") val pictureSmall: String,
    @SerializedName("picture_xl") val pictureXl: String,
    @SerializedName("radio") val radio: Boolean,
    @SerializedName("tracklist") val trackList: String,
    @SerializedName("type") val type: String
)