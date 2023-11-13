package com.example.hingyuyukming_comp304_002_lab05

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import com.example.hingyuyukming_comp304_002_lab05.databinding.ActivityMapsBinding
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.slider.Slider

class MapsActivity : AppCompatActivity(),
    OnMapReadyCallback,
    CompoundButton.OnCheckedChangeListener {

    private lateinit var binding: ActivityMapsBinding

    private var theMap: GoogleMap? = null

    private lateinit var theCategory: String
    private lateinit var landmarks: Landmarks
    private lateinit var thePlaces: Map<String, Place>
    private lateinit var thePlaceNames: List<String>
    private var selectedPlaceName: String = ""
    private var mapMarker: Marker? = null
    private var zoom = 15.0f
    private var tilt = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        theCategory = intent.getStringExtra("category") ?: "Attractions"
        landmarks = (application as LandmarkApplication).landmarks
        thePlaces = landmarks.getPlaces(theCategory)
        thePlaceNames = thePlaces.keys.toList()
        selectedPlaceName = intent.getStringExtra("place") ?: thePlaceNames.first()

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
            it.setSelection(thePlaceNames.indexOf(selectedPlaceName))
            it.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    changeMarkerTo(thePlaceNames[position])
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
        }
        binding.sliderTilt.addOnChangeListener { slider: Slider, value: Float, fromUser: Boolean ->
            tilt = value
            changeCamera(tilt = tilt)
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

    private fun changeMarkerTo(placeName: String) {
        val pos = thePlaceNames.indexOf(placeName)
        if (pos >= 0) {
            binding.tvAddress.text = thePlaces[placeName]?.address
            val coord = thePlaces[placeName]?.coordinates ?: listOf(0.0, 0.0)
            val latLng = LatLng(coord[0], coord[1])
            theMap?.apply {
                val firstTimeAddMarker = mapMarker == null
                mapMarker?.remove()
                mapMarker = addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(placeName)
                )
                changeCamera(
                    lat = coord[0],
                    lng = coord[1],
                    bearing = 0.0f,
                    tilt = tilt,
                    zoom = if (firstTimeAddMarker) zoom else null,
                    dontAnimate = firstTimeAddMarker
                )
            }
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


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        theMap = googleMap
        theMap!!.uiSettings.apply {
            isZoomControlsEnabled = true
            // isTiltGesturesEnabled = true
        }

        // Add a marker in Sydney and move the camera
        changeMarkerTo(selectedPlaceName)
    }
}