package com.example.tugas3kelompok

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CarouselAdapter(
    private val images: List<Int>,
    private val titles: List<String>,
    private val subtitles: List<String>
) : RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder>() {

    inner class CarouselViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img = itemView.findViewById<ImageView>(R.id.imageView)
        val title = itemView.findViewById<TextView>(R.id.titleText)
        val subtitle = itemView.findViewById<TextView>(R.id.subText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carousel, parent, false)
        return CarouselViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        holder.img.setImageResource(images[position])
        holder.img.scaleType = ImageView.ScaleType.CENTER_CROP
        holder.title.text = titles[position]
        holder.subtitle.text = subtitles[position]
    }

    override fun getItemCount() = images.size
}
