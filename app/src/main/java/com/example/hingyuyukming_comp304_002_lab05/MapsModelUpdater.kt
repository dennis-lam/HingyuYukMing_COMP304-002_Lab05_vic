package com.example.hingyuyukming_comp304_002_lab05

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.AsyncTask
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request

class MapsModelUpdater(val context: Context) {
    // Declare maps models
    private var placeMarker: Marker? = null
    private var userMarker: Marker? = null
    private var routePolyline: Polyline? = null

    // Status
    var hasUpdated: Boolean = false
    private set

    // API keys
    private val apiKey = context.packageManager
        .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        .metaData.getString("com.google.android.geo.API_KEY")!!

    init {
        // Initialize places
        if (!Places.isInitialized()) {
            Places.initialize(context, apiKey)
        }
    }

    fun update(maps: GoogleMap, placeName: String, placeLatLng: LatLng, userLatLng: LatLng?) {
        with (maps) {
            // Set place marker
            placeMarker?.remove()
            placeMarker = addMarker(
                MarkerOptions()
                    .position(placeLatLng)
                    .title(placeName)
            )
            // Set user marker
            userMarker?.remove()
            if (userLatLng != null) {
                userMarker = addMarker(
                    MarkerOptions()
                        .position(userLatLng)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.user_marker))
                )
                // Set route
                val urll = getDirectionURL(userLatLng, placeLatLng)
                GetDirection(urll, this).execute()
            }
        }
        // Mark as updated
        hasUpdated = true
    }

    private fun getDirectionURL(origin:LatLng, dest:LatLng) : String{
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&sensor=false" +
                "&mode=driving" +
                "&key=$apiKey"
    }


    fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val latLng = LatLng((lat.toDouble() / 1E5),(lng.toDouble() / 1E5))
            poly.add(latLng)
        }
        return poly
    }

    class MapData {
        var routes = ArrayList<Routes>()
    }

    class Routes {
        var legs = ArrayList<Legs>()
    }

    class Legs {
        var distance = Distance()
        var duration = Duration()
        var end_address = ""
        var start_address = ""
        var end_location =Location()
        var start_location = Location()
        var steps = ArrayList<Steps>()
    }

    class Steps {
        var distance = Distance()
        var duration = Duration()
        var end_address = ""
        var start_address = ""
        var end_location =Location()
        var start_location = Location()
        var polyline = PolyLine()
        var travel_mode = ""
        var maneuver = ""
    }

    class Duration {
        var text = ""
        var value = 0
    }

    class Distance {
        var text = ""
        var value = 0
    }

    class PolyLine {
        var points = ""
    }

    class Location{
        var lat =""
        var lng =""
    }

    @SuppressLint("StaticFieldLeak")
    private inner class GetDirection(val url : String, val map: GoogleMap) : AsyncTask<Void, Void, List<List<LatLng>>>(){
        override fun doInBackground(vararg params: Void?): List<List<LatLng>> {

            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body!!.string()

            val result =  ArrayList<List<LatLng>>()
            try{
                val respObj = Gson().fromJson(data,MapData::class.java)
                val path =  ArrayList<LatLng>()
                for (i in 0 until respObj.routes[0].legs[0].steps.size){
                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }
                result.add(path)
            }catch (e:Exception){
                e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: List<List<LatLng>>) {
            val lineoption = PolylineOptions()
            for (i in result.indices){
                lineoption.addAll(result[i])
                lineoption.width(10f)
                lineoption.color(Color.BLUE)
                lineoption.geodesic(true)
            }
            routePolyline?.remove()
            routePolyline = map.addPolyline(lineoption)
        }
    }
}