package com.example.labweek07

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    // This is the variable through which we will launch the permission request and track user responses
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // A google play location service which helps us interact with Google's Fused Location Provider API
    // The API intelligently provides us with the device location information
    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // This is used to register for activity result
        // The activity result will be used to handle the permission request to the user
        // It accepts an ActivityResultContract as a parameter which in this case we're using the RequestPermission() ActivityResultContract
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // If granted by the user, execute the necessary function
                getLastLocation()
            } else {
                // If not granted, show a rationale dialog
                // A rationale dialog is used for a warning to the user that the app will not work without the required permission
                showPermissionRationale {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // OnMapReady is called when the map is ready to be used
        // The code below is used to check for the location permission for the map functionality to work
        // If it's not granted yet, then the rationale dialog will be brought up
        if (hasLocationPermission()) {
            getLastLocation()
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // shouldShowRequestPermissionRationale automatically checks if the user has denied the permission before
            // If it has, then the rationale dialog will be brought up
            showPermissionRationale {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // This is used to check if the user already has the permission granted
    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    // This is used to bring up a rationale dialog which will be used to ask the user for permission again
    // A rationale dialog is used for a warning to the user that the app will not work without the required permission
    // Usually it's brought up when the user denies the needed permission in the previous permission request
    private fun showPermissionRationale(positiveAction: () -> Unit) {
        // Create a pop up alert dialog that's used to ask for the required permission again to the user
        AlertDialog.Builder(this)
            .setTitle("Location permission")
            .setMessage("This app will not work without knowing your current location")
            .setPositiveButton(android.R.string.ok) { _, _ -> positiveAction() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    // Executed when the location permission has been granted by the user
    private fun getLastLocation() {
        Log.d("MapsActivity", "getLastLocation called.")

        if (hasLocationPermission()) {
            try {
                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            val userLocation = LatLng(location.latitude, location.longitude)
                            updateMapLocation(userLocation)
                            addMarkerAtLocation(userLocation, "You")
                        }
                    }
            } catch (e: SecurityException) {
                Log.e("MapsActivity", e.message ?: "SecurityException")
            }
        } else {
            // If permission was rejected
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun updateMapLocation(location: LatLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }

    private fun addMarkerAtLocation(location: LatLng, title: String) {
        mMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
        )
    }
}
