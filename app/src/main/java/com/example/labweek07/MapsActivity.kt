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
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // Fused Location Provider Client untuk mendapatkan lokasi perangkat
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

        // Register ActivityResultLauncher untuk handle permission request
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, ambil lokasi
                getLastLocation()
            } else {
                // Permission denied, tampilkan rationale dialog
                showPermissionRationale {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Check apakah user sudah memberikan permission lokasi
        if (hasLocationPermission()) {
            getLastLocation()
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Jika user pernah deny permission, tampilkan rationale
            showPermissionRationale {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            // Request permission untuk pertama kali
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Function untuk cek apakah permission lokasi sudah granted
    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    // Function untuk menampilkan rationale dialog
    private fun showPermissionRationale(positiveAction: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Location permission")
            .setMessage("This app will not work without knowing your current location")
            .setPositiveButton(android.R.string.ok) { _, _ -> positiveAction() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    // Function untuk mendapatkan lokasi pengguna
    private fun getLastLocation() {
        Log.d("MapsActivity", "getLastLocation called")

        if (hasLocationPermission()) {
            try {
                // Coba ambil lokasi terakhir dari FusedLocationProviderClient
                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            // Jika lokasi tersedia, tampilkan marker
                            val userLocation = LatLng(location.latitude, location.longitude)
                            updateMapLocation(userLocation)
                            addMarkerAtLocation(userLocation, "You")
                            Log.d("MapsActivity", "Location found: ${location.latitude}, ${location.longitude}")
                        } else {
                            // Jika lokasi null (emulator belum diset lokasi), gunakan request update
                            Log.d("MapsActivity", "Last location is null, requesting location updates")
                            requestNewLocation()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MapsActivity", "Failed to get location: ${e.message}")
                        // Fallback ke lokasi default jika gagal
                        val defaultLocation = LatLng(-7.165, 113.482) // Pamekasan
                        updateMapLocation(defaultLocation)
                        addMarkerAtLocation(defaultLocation, "Default Location (Pamekasan)")
                    }
            } catch (e: SecurityException) {
                Log.e("MapsActivity", "SecurityException: ${e.message}")
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Function untuk request lokasi baru secara realtime (jika lastLocation null)
    private fun requestNewLocation() {
        if (hasLocationPermission()) {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 1000 // Update setiap 1 detik
                fastestInterval = 500
                numUpdates = 1 // Hanya sekali saja
            }

            try {
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            super.onLocationResult(locationResult)
                            val location = locationResult.lastLocation
                            if (location != null) {
                                val userLocation = LatLng(location.latitude, location.longitude)
                                updateMapLocation(userLocation)
                                addMarkerAtLocation(userLocation, "You")
                                Log.d("MapsActivity", "New location: ${location.latitude}, ${location.longitude}")
                            } else {
                                // Jika masih null, gunakan default
                                val defaultLocation = LatLng(-7.165, 113.482)
                                updateMapLocation(defaultLocation)
                                addMarkerAtLocation(defaultLocation, "Default Location (Pamekasan)")
                            }
                            // Stop location updates setelah dapat lokasi
                            fusedLocationProviderClient.removeLocationUpdates(this)
                        }
                    },
                    null
                )
            } catch (e: SecurityException) {
                Log.e("MapsActivity", "SecurityException during location update: ${e.message}")
            }
        }
    }

    // Function untuk memindahkan kamera map ke lokasi tertentu
    private fun updateMapLocation(location: LatLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }

    // Function untuk menambahkan marker di lokasi tertentu
    private fun addMarkerAtLocation(location: LatLng, title: String) {
        mMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
        )
    }
}
