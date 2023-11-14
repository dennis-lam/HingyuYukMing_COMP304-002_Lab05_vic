package com.example.hingyuyukming_comp304_002_lab05

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Toast
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.AsyncTask
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.hingyuyukming_comp304_002_lab05.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.material.slider.Slider
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request

data class MarkersData(var placeName: String, var userLatLng: LatLng?)

class MapsActivity : AppCompatActivity(),
    OnMapReadyCallback,
    CompoundButton.OnCheckedChangeListener {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var apiKey: String

    private var theMap: GoogleMap? = null

    private lateinit var theCategory: String
    private lateinit var thePlaces: Map<String, Place>
    private lateinit var thePlaceNames: List<String>
    private var mapMarker: Marker? = null
    private var userMarker: Marker? = null
    private var routePolyline: com.google.android.gms.maps.model.Polyline? = null
    private var zoom = 15.0f
    private var tilt = 0.0f

    private lateinit var markersData: MarkersData

    // Declare user location information
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val locationRequestCode = 12345



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        theCategory = intent.getStringExtra("category") ?: "Attractions"
        val landmarks = (application as LandmarkApplication).landmarks
        thePlaces = landmarks.getPlaces(theCategory)
        thePlaceNames = thePlaces.keys.toList()
        markersData = MarkersData(
            intent.getStringExtra("place") ?: thePlaceNames.first(),
            null
        )

        actionBar?.title = theCategory
        supportActionBar?.title = theCategory

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.fragMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.cbSatelliteMap.setOnCheckedChangeListener(this)
        binding.cbHybridMap.setOnCheckedChangeListener(this)

        binding.spinPlaces.let {
            it.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                thePlaceNames
            )
            it.setSelection(thePlaceNames.indexOf(markersData.placeName))
            it.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    markersData.placeName = thePlaceNames[position]
                    updateDisplay()
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
        }
        binding.sliderTilt.addOnChangeListener { slider: Slider, value: Float, fromUser: Boolean ->
            tilt = value
            changeCamera(tilt = tilt)
        }

        // Prepare location retrieval
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION), locationRequestCode)
        } else {
            startUserLocationScheduler()
        }

        // Location callback
        locationCallback = object : LocationCallback() {
             override fun onLocationResult(locationResult: LocationResult) {
                 val lastLocation = locationResult.lastLocation ?: return
                 markersData.userLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
                 updateDisplay()
            }
        }

        // Setup Places API
        apiKey = applicationContext.packageManager
            .getApplicationInfo(applicationContext.packageName, PackageManager.GET_META_DATA)
            .metaData.getString("com.google.android.geo.API_KEY")!!
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }
    }

    // Function to get requested permissions result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            locationRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    startUserLocationScheduler()
                } else {
                    Toast.makeText(this, getString(R.string.get_user_location_error_message), Toast.LENGTH_SHORT).show()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startUserLocationScheduler() {
        // Set location settings
        val locationRequest = LocationRequest.Builder(LocationRequest.PRIORITY_HIGH_ACCURACY, 5000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            // Start request update
            fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper())
        }
        task.addOnFailureListener { exception ->
            Toast.makeText(this, getString(R.string.get_user_location_error_message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCameraPosition(
        lat: Double? = null,
        lng: Double? = null,
        zoom: Float? = null,
        bearing: Float? = null,
        tilt: Float? = null,
    ): CameraPosition? {
        if (theMap != null) {
            val camPos = theMap!!.cameraPosition
            val newLat = lat ?: camPos.target.latitude
            val newLng = lng ?: camPos.target.longitude
            val newZoom = zoom ?: camPos.zoom
            val newBearing = bearing ?: camPos.bearing
            val newTilt = tilt ?: camPos.tilt
            return CameraPosition.Builder()
                .target(LatLng(newLat, newLng))
                .zoom(newZoom)
                .bearing(newBearing)
                .tilt(newTilt)
                .build()
        }
        return null
    }

    private fun changeCamera(
        lat: Double? = null,
        lng: Double? = null,
        zoom: Float? = null,
        bearing: Float? = null,
        tilt: Float? = null,
        dontAnimate: Boolean = true,
    ) {
        val cameraPos = getCameraPosition(lat, lng, zoom, bearing, tilt)
        if (cameraPos != null) {
            if (dontAnimate) {
                theMap!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPos))
            } else {
                theMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPos))
            }
        }
    }

    private fun updateDisplay() {
        // Obtain display information
        val place = thePlaces[markersData.placeName] ?: throw IllegalArgumentException("Invalid place: ${markersData.placeName}")
        val placeLatLng = place.latLngOrDefault()
        val userLatLng = markersData.userLatLng
        // Set address
        binding.tvAddress.text = place.address
        // Set map
        theMap?.apply {
            val firstTimeAddMarker = mapMarker == null
            mapMarker?.remove()
            mapMarker = addMarker(
                MarkerOptions()
                    .position(placeLatLng)
                    .title(place.name)
            )
            userMarker?.remove()
            if (userLatLng != null) {
                userMarker = addMarker(
                    MarkerOptions()
                        .position(userLatLng)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.user_marker))
                )
                val urll = getDirectionURL(userLatLng, placeLatLng, apiKey)
                GetDirection(urll).execute()
            }
            changeCamera(
                lat = placeLatLng.latitude,
                lng = placeLatLng.longitude,
                bearing = 0.0f,
                tilt = tilt,
                zoom = if (firstTimeAddMarker) zoom else null,
                dontAnimate = firstTimeAddMarker
            )
        }
    }

   override fun onCheckedChanged(button: CompoundButton?, isChecked: Boolean) {
        var mapType = GoogleMap.MAP_TYPE_NORMAL
        when (button?.id) {
            R.id.cbSatelliteMap -> {
                if (isChecked) mapType = GoogleMap.MAP_TYPE_SATELLITE
            }
            R.id.cbHybridMap -> {
                if (isChecked) mapType = GoogleMap.MAP_TYPE_HYBRID
            }
        }
        theMap?.mapType = mapType

        // Temporarily disable the listeners
        binding.cbSatelliteMap.setOnCheckedChangeListener(null)
        binding.cbHybridMap.setOnCheckedChangeListener(null)

        binding.cbSatelliteMap.isChecked = (mapType == GoogleMap.MAP_TYPE_SATELLITE)
        binding.cbHybridMap.isChecked = (mapType == GoogleMap.MAP_TYPE_HYBRID)

        // Enable the listeners
        binding.cbSatelliteMap.setOnCheckedChangeListener(this)
        binding.cbHybridMap.setOnCheckedChangeListener(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        theMap = googleMap
        theMap!!.uiSettings.apply {
            isZoomControlsEnabled = true
        }
        updateDisplay()
    }

    private fun getDirectionURL(origin:LatLng, dest:LatLng, secret: String) : String{
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&sensor=false" +
                "&mode=driving" +
                "&key=$secret"
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
    private inner class GetDirection(val url : String) : AsyncTask<Void, Void, List<List<LatLng>>>(){
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
            routePolyline = theMap!!.addPolyline(lineoption)
        }
    }
}