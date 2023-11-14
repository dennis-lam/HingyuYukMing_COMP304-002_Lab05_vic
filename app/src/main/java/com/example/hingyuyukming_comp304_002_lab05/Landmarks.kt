package com.example.hingyuyukming_comp304_002_lab05

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson

data class Place(
    val type: String,
    val name: String,
    val coordinates: List<Double>,
    val address: String,
) {
    // Obtain LatLng
    fun latLngOrDefault() = if (coordinates.size != 2) {
        LatLng(0.0,0.0)
    } else {
        LatLng(coordinates[0], coordinates[1])
    }
}

class Landmarks (context: Context) {
    private var landmarks: Map<String, Map<String, Place>>
    init {
        context.assets.open("places.json").use { inputStream ->
            val jsonStr = String(inputStream.readBytes())
            val placeArray: Array<Place> = Gson().fromJson(jsonStr, Array<Place>::class.java)
            landmarks = placeArray.groupBy {
                it.type
            }.map { (key, placeList) ->
                key to placeList.associateBy { it.name }
            }.toMap()
        }
    }

    fun getPlaces(category: String): Map<String, Place> {
        return landmarks.getOrDefault(category, emptyMap())
    }

    fun getCategories(): List<String> {
        return landmarks.keys.toList()
    }
}
