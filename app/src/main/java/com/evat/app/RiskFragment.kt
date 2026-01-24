package com.evat.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.evat.app.databinding.FragmentRiskBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class RiskFragment : Fragment() {
    private var _binding: FragmentRiskBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationOverlay: MyLocationNewOverlay? = null
    
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                enableUserLocation()
            }
            else -> {
                Toast.makeText(
                    requireContext(),
                    "Location permission is required to show risk-prone areas near you",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiskBinding.inflate(inflater, container, false)
        
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        setupMap()
        setupMyLocationButton()
        addRiskProneAreas()
        
        // Check location settings when fragment opens
        checkLocationSettings()
        
        return binding.root
    }
    
    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(18)
            
            // Center on Greater Lagro, Quezon City
            controller.setCenter(GeoPoint(14.7176, 121.0664))
        }
    }
    
    private fun setupMyLocationButton() {
        binding.fabMyLocation.setOnClickListener {
            centerMapOnCurrentLocation()
        }
    }
    
    private fun centerMapOnCurrentLocation() {
        if (!hasLocationPermissions()) {
            requestLocationPermissions()
            return
        }
        
        try {
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                location?.let {
                    val userLocation = GeoPoint(it.latitude, it.longitude)
                    binding.mapView.controller.animateTo(userLocation)
                    Toast.makeText(
                        requireContext(),
                        "Centered on your location",
                        Toast.LENGTH_SHORT
                    ).show()
                } ?: run {
                    Toast.makeText(
                        requireContext(),
                        "Unable to get current location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Failed to get location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(
                requireContext(),
                "Location permission required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun addRiskProneAreas() {
        // Risk-prone areas in Greater Lagro, Quezon City
        val riskAreas = listOf(
            RiskArea(
                "Risk Area: Lagro Subdivision",
                GeoPoint(14.7205, 121.0653),
                listOf(
                    GeoPoint(14.7225, 121.0633),
                    GeoPoint(14.7225, 121.0673),
                    GeoPoint(14.7185, 121.0673),
                    GeoPoint(14.7185, 121.0633)
                )
            ),
            RiskArea(
                "Risk Area: Lagro Dulo Park",
                GeoPoint(14.7187, 121.0661),
                listOf(
                    GeoPoint(14.7207, 121.0641),
                    GeoPoint(14.7207, 121.0681),
                    GeoPoint(14.7167, 121.0681),
                    GeoPoint(14.7167, 121.0641)
                )
            ),
            RiskArea(
                "Risk Area: Ascencion Avenue",
                GeoPoint(14.7142, 121.0671),
                listOf(
                    GeoPoint(14.7162, 121.0651),
                    GeoPoint(14.7162, 121.0691),
                    GeoPoint(14.7122, 121.0691),
                    GeoPoint(14.7122, 121.0651)
                )
            )
        )
        
        riskAreas.forEach { area ->
            // Add polygon overlay for risk zone
            val polygon = Polygon(binding.mapView).apply {
                points = area.boundary
                fillColor = Color.parseColor("#40FF0000") // Semi-transparent red
                strokeColor = Color.parseColor("#FFFF0000") // Red border
                strokeWidth = 3f
            }
            binding.mapView.overlays.add(polygon)
            
            // Add marker for risk area
            val marker = Marker(binding.mapView).apply {
                position = area.center
                title = area.name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_nav_risk)
            }
            binding.mapView.overlays.add(marker)
        }
    }
    
    private fun checkLocationSettings() {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        
        if (!isGpsEnabled && !isNetworkEnabled) {
            showLocationSettingsDialog()
        } else {
            requestLocationPermissions()
        }
    }
    
    private fun showLocationSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Location Services Disabled")
            .setMessage("Please enable location services to see risk-prone areas near your location.")
            .setPositiveButton("Enable") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Not Now") { _, _ ->
                requestLocationPermissions()
            }
            .show()
    }
    
    private fun requestLocationPermissions() {
        when {
            hasLocationPermissions() -> {
                enableUserLocation()
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
    
    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun enableUserLocation() {
        if (!hasLocationPermissions()) return
        
        try {
            // Add location overlay with BLUE DOT icon
            locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), binding.mapView)
            locationOverlay?.apply {
                enableMyLocation()
                val blueDotIcon = ContextCompat.getDrawable(requireContext(), R.drawable.location_blue_dot)
                blueDotIcon?.let {
                    val bitmap = drawableToBitmap(it)
                    setPersonIcon(bitmap)
                    setDirectionIcon(bitmap)
                }
                enableFollowLocation()
            }
            binding.mapView.overlays.add(locationOverlay)
            
            // Try to get last known location first (faster)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLocation = GeoPoint(it.latitude, it.longitude)
                    binding.mapView.controller.animateTo(userLocation)
                    return@addOnSuccessListener
                }
                
                // If no last known location, get current location
                val cancellationTokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { currentLocation ->
                    currentLocation?.let {
                        val userLocation = GeoPoint(it.latitude, it.longitude)
                        binding.mapView.controller.animateTo(userLocation)
                    }
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(
                requireContext(),
                "Unable to access location",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationOverlay?.disableMyLocation()
        _binding = null
    }
    
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
    
    private data class RiskArea(
        val name: String,
        val center: GeoPoint,
        val boundary: List<GeoPoint>
    )
}
