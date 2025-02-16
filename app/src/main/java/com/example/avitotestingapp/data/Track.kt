package com.example.avitotestingapp.data

data class Track(
    val album: Album,
    val artist: Artist,
    val duration: Int,
    val id: Long,
    val position: Int,
    val preview: String,
    val title: String,
)