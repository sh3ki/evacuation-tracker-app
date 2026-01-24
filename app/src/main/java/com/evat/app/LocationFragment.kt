package com.evat.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.evat.app.databinding.FragmentLocationBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class LocationFragment : Fragment() {
    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationOverlay: MyLocationNewOverlay? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val evacuationCenters = mutableListOf<EvacuationCenter>()
    private val markers = mutableListOf<Marker>()
    private var selectedMarker: Marker? = null
    
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
                    "Location permission is required to show your position on the map",
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
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        setupMap()
        setupEvacuationCenters()
        setupBottomSheet()
        setupDropdown()
        setupMyLocationButton()
        addEvacuationMarkers()
        
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
    
    private fun setupEvacuationCenters() {
        evacuationCenters.clear()
        
        // Add the 7 evacuation centers for Greater Lagro
        // Note: Image URLs are placeholders. Replace with actual images from Google Maps or local resources
        evacuationCenters.add(
            EvacuationCenter(
                name = "Lagro Plaza",
                address = "P3G8+GFP, Flores de Mayo, Novaliches, Quezon City, Metro Manila",
                coordinates = GeoPoint(14.726333, 121.066167),
                imageUrls = listOf(
                    "https://picsum.photos/400/300?random=1",
                    "https://picsum.photos/400/300?random=2"
                )
            )
        )
        
        evacuationCenters.add(
            EvacuationCenter(
                name = "Lagro High School",
                address = "P3G8+PJJ, Misa de Gallo, Novaliches, Quezon City, Metro Manila",
                coordinates = GeoPoint(14.727000, 121.066833),
                imageUrls = listOf(
                    "https://picsum.photos/400/300?random=3",
                    "https://picsum.photos/400/300?random=4"
                )
            )
        )
        
        evacuationCenters.add(
            EvacuationCenter(
                name = "Lagro Elementary School",
                address = "P3H9+M4V, Ascension Ave, Quezon City, 1100 Metro Manila",
                coordinates = GeoPoint(14.729167, 121.067667),
                imageUrls = listOf(
                    "https://picsum.photos/400/300?random=5",
                    "https://picsum.photos/400/300?random=6"
                )
            )
        )
        
        evacuationCenters.add(
            EvacuationCenter(
                name = "Ascension of Our Lord Parish Church Patio",
                address = "P3M8+9WC, Ascension Avenue, corner Domingo de Ramos, Novaliches, Quezon City, 1100 Metro Manila",
                coordinates = GeoPoint(14.733167, 121.067333),
                imageUrls = listOf(
                    "https://picsum.photos/400/300?random=7",
                    "https://picsum.photos/400/300?random=8",
                    "https://picsum.photos/400/300?random=9"
                )
            )
        )
        
        evacuationCenters.add(
            EvacuationCenter(
                name = "Our Lady Of Fatima University Quezon City",
                address = "1 Esperanza, Quezon City, 1118 Metro Manila",
                coordinates = GeoPoint(14.706944, 121.064722),
                imageUrls = listOf(
                    "https://picsum.photos/400/300?random=10",
                    "https://picsum.photos/400/300?random=11"
                )
            )
        )
        
        evacuationCenters.add(
            EvacuationCenter(
                name = "Barangay Greater Lagro (BHERT)",
                address = "P3G8+GJ8 Greater Lagro, Quezon City, Metro Manila",
                coordinates = GeoPoint(14.726167, 121.066500),
                imageUrls = listOf(
                    "https://picsum.photos/400/300?random=12",
                    "https://picsum.photos/400/300?random=13"
                )
            )
        )
        
        evacuationCenters.add(
            EvacuationCenter(
                name = "Brgy. Greater Lagro Centennial Park",
                address = "P398+99X, Flores de Mayo, Novaliches, Quezon City, Metro Manila",
                coordinates = GeoPoint(14.718667, 121.066000),
                imageUrls = listOf(
                    "https://picsum.photos/400/300?random=14",
                    "https://picsum.photos/400/300?random=15",
                    "https://picsum.photos/400/300?random=16"
                )
            )
        )
    }
    
    private fun setupBottomSheet() {
        val bottomSheet = binding.root.findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        
        // Setup Get Directions button
        val btnGetDirections = bottomSheet.findViewById<MaterialButton>(R.id.btnGetDirections)
        btnGetDirections.setOnClickListener {
            Toast.makeText(requireContext(), "Get Directions feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupDropdown() {
        val evacuationNames = evacuationCenters.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, evacuationNames)
        binding.evacuationSiteDropdown.setAdapter(adapter)
        
        binding.evacuationSiteDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedCenter = evacuationCenters[position]
            centerMapOnEvacuationSite(selectedCenter, position)
        }
    }
    
    private fun setupMyLocationButton() {
        binding.fabMyLocation.setOnClickListener {
            centerMapOnCurrentLocation()
        }
    }
    
    private fun addEvacuationMarkers() {
        markers.clear()
        
        evacuationCenters.forEachIndexed { index, center ->
            val marker = Marker(binding.mapView).apply {
                position = center.coordinates
                title = center.name
                snippet = center.address
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_evacuation_marker)
                
                setOnMarkerClickListener { _, _ ->
                    selectMarker(this, index)
                    showEvacuationDetails(center)
                    true
                }
            }
            
            binding.mapView.overlays.add(marker)
            markers.add(marker)
        }
        
        binding.mapView.invalidate()
    }
    
    private fun showEvacuationDetails(center: EvacuationCenter) {
        val bottomSheet = binding.root.findViewById<View>(R.id.bottomSheet)
        
        // Set evacuation center details
        bottomSheet.findViewById<TextView>(R.id.tvEvacuationName).text = center.name
        bottomSheet.findViewById<TextView>(R.id.tvEvacuationAddress).text = center.address
        
        // Setup image ViewPager
        val imageViewPager = bottomSheet.findViewById<ViewPager2>(R.id.imageViewPager)
        val imageAdapter = EvacuationImageAdapter(center.imageUrls)
        imageViewPager.adapter = imageAdapter
        
        // Setup image indicator
        val tvImageIndicator = bottomSheet.findViewById<TextView>(R.id.tvImageIndicator)
        tvImageIndicator.text = "1 / ${center.imageUrls.size}"
        
        imageViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tvImageIndicator.text = "${position + 1} / ${center.imageUrls.size}"
            }
        })
        
        // Show the bottom sheet
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.peekHeight = 500
    }
    
    private fun centerMapOnEvacuationSite(center: EvacuationCenter, index: Int) {
        binding.mapView.controller.animateTo(center.coordinates)
        
        // Highlight the selected marker
        if (index in markers.indices) {
            selectMarker(markers[index], index)
        }
        
        // Optionally show the details
        showEvacuationDetails(center)
    }
    
    private fun selectMarker(marker: Marker, index: Int) {
        // Reset previous selected marker to normal icon
        selectedMarker?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_evacuation_marker)
        
        // Set new selected marker to highlighted icon
        marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_evacuation_marker_selected)
        selectedMarker = marker
        
        // Update dropdown to show this selection
        binding.evacuationSiteDropdown.setText(evacuationCenters[index].name, false)
        
        // Refresh map
        binding.mapView.invalidate()
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
            .setMessage("Please enable location services to see your current position on the map.")
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
            // Add location overlay with BLUE DOT icon (not white arrow)
            locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), binding.mapView)
            locationOverlay?.apply {
                enableMyLocation()
                
                // Create blue dot bitmap
                val blueDotIcon = ContextCompat.getDrawable(requireContext(), R.drawable.location_blue_dot)
                blueDotIcon?.let { 
                    val bitmap = drawableToBitmap(it)
                    setPersonIcon(bitmap)
                    setDirectionIcon(bitmap) // Replace direction arrow with blue dot too
                }
                
                // Disable the direction arrow to show only the blue dot
                enableFollowLocation()
            }
            binding.mapView.overlays.add(locationOverlay)
            
            // Try to get last known location first (faster display)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLocation = GeoPoint(it.latitude, it.longitude)
                    binding.mapView.controller.animateTo(userLocation)
                    Toast.makeText(
                        requireContext(),
                        "Location detected",
                        Toast.LENGTH_SHORT
                    ).show()
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
                        Toast.makeText(
                            requireContext(),
                            "Location detected",
                            Toast.LENGTH_SHORT
                        ).show()
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
}
