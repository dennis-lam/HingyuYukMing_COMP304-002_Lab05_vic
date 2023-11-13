package com.example.hingyuyukming_comp304_002_lab05

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hingyuyukming_comp304_002_lab05.databinding.ActivityPlacesBinding

class PlacesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlacesBinding
    private lateinit var landmarks: Landmarks
    private lateinit var category: String
    private lateinit var places: Map<String, Place>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlacesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        category = intent.getStringExtra("category") ?: "Attraction"
        landmarks = (application as LandmarkApplication).landmarks
        places = landmarks.getPlaces(category)

        binding.tvTitle.text = "Toronto\n$category"

        binding.rvPlaces.let {
            val layoutManager = LinearLayoutManager(this)
            layoutManager.orientation = LinearLayoutManager.VERTICAL
            it.layoutManager = layoutManager
            it.adapter = RvAdapter(category, places.keys.toList())
        }
    }

    class RvAdapter(private val category: String, private val places: List<String>): RecyclerView.Adapter<RvAdapter.ViewHolder>() {

        class ViewHolder(category: String, view: View): RecyclerView.ViewHolder(view) {
            val textView: TextView
            init {
                textView = view.findViewById(android.R.id.text1)
                textView.run {
                    textSize = 24.0f
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    setTextColor(ContextCompat.getColor(context, R.color.purple_200))
                    setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
                }
                textView.setOnClickListener {
                    val place = (it as TextView).text
                    val intent = Intent(it.context, MapsActivity::class.java)
                    intent.putExtra("category", category)
                    intent.putExtra("place", place)
                    it.context.startActivity(intent)
                }
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                category,
                LayoutInflater.from(parent.context).inflate(
                    android.R.layout.simple_list_item_1, parent, false)
            )
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = places[position]
        }
        override fun getItemCount(): Int {
            return places.size
        }
    }
}