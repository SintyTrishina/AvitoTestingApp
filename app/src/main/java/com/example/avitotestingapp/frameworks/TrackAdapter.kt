package com.example.avitotestingapp.frameworks


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.avitotestingapp.R
import com.example.avitotestingapp.data.Track

class TrackAdapter(
    private val onClick: (Track, Int) -> Unit
) : RecyclerView.Adapter<TrackViewHolder>() {

    private var tracks: ArrayList<Track> = ArrayList()


    fun updateTracks(newTracks: List<Track>) {
        tracks.clear()
        tracks.addAll(newTracks)
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.track_view, parent, false)
        return TrackViewHolder(view)
    }

    override fun getItemCount(): Int {
        return tracks.size
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]
        holder.bind(track)
        holder.itemView.setOnClickListener {
            onClick(track, position)
        }
    }
}