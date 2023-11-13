package com.example.hingyuyukming_comp304_002_lab05

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hingyuyukming_comp304_002_lab05.databinding.ActivityLandmarkBinding

class LandmarkActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLandmarkBinding
    private lateinit var landmarks: Landmarks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLandmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        landmarks = (application as LandmarkApplication).landmarks

        binding.rvCategories.let {
            val layoutManager = LinearLayoutManager(this)
            layoutManager.orientation = LinearLayoutManager.VERTICAL
            it.layoutManager = layoutManager
            it.adapter = RvAdapter(landmarks.getCategories())
        }
    }

    class RvAdapter(private val categories: List<String>): RecyclerView.Adapter<RvAdapter.ViewHolder>() {

        class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
            val textView: TextView
            init {
                textView = view.findViewById(android.R.id.text1)
                textView.run {
                    textSize = 32.0f
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    setTextColor(Color.MAGENTA)
                    setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
                }
                textView.setOnClickListener {
                    val category = (it as TextView).text
                    val intent = Intent(it.context, PlacesActivity::class.java)
                    intent.putExtra("category", category)
                    it.context.startActivity(intent)
                }
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvAdapter.ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    android.R.layout.simple_list_item_1, parent, false)
            )
        }
        override fun onBindViewHolder(holder: RvAdapter.ViewHolder, position: Int) {
            holder.textView.text = categories[position]
        }
        override fun getItemCount(): Int {
            return categories.size
        }
    }
}


