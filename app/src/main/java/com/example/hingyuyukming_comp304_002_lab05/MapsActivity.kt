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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.hingyuyukming_comp304_002_lab05.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.slider.Slider

data class MapDisplay(var placeName: String, var userLatLng: LatLng?)

class MapsActivity : AppCompatActivity(),
    OnMapReadyCallback,
    CompoundButton.OnCheckedChangeListener {

    private lateinit var binding: ActivityMapsBinding

    private var theMap: GoogleMap? = null

    private lateinit var theCategory: String
    private lateinit var thePlaces: Map<String, Place>
    private lateinit var thePlaceNames: List<String>
    private var mapMarker: Marker? = null
    private var userMarker: Marker? = null
    private var zoom = 15.0f
    private var tilt = 0.0f

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationRequestCode = 12345
    private lateinit var currentDisplay: MapDisplay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        theCategory = intent.getStringExtra("category") ?: "Attractions"
        val landmarks = (application as LandmarkApplication).landmarks
        thePlaces = landmarks.getPlaces(theCategory)
        thePlaceNames = thePlaces.keys.toList()
        currentDisplay = MapDisplay(
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
            it.setSelection(thePlaceNames.indexOf(currentDisplay.placeName))
            it.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    currentDisplay.placeName = thePlaceNames[position]
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
            updateCurrentLocation()
        }
    }

    // Function to get requested permissions result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            locationRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    updateCurrentLocation()
                } else {
                    Toast.makeText(this, getString(R.string.get_user_location_error_message), Toast.LENGTH_SHORT).show()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun updateCurrentLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener {
                if (it == null) {
                    Toast.makeText(this, getString(R.string.get_user_location_error_message), Toast.LENGTH_SHORT).show()
                } else {
                    currentDisplay.userLatLng = LatLng(it.latitude, it.longitude)
                    updateDisplay()
                }
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
        val place = thePlaces[currentDisplay.placeName] ?: throw IllegalArgumentException("Invalid place: ${currentDisplay.placeName}")
        val placeLatLng = place.latLngOrDefault()
        val userLatLng = currentDisplay.userLatLng
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
}