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
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import kotlinx.coroutines.*

class LocationFragment : Fragment() {
    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationOverlay: MyLocationNewOverlay? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val evacuationCenters = mutableListOf<EvacuationCenter>()
    private val markers = mutableListOf<Marker>()
    private var selectedMarker: Marker? = null
    private var routeOverlay: Polyline? = null
    private var currentSelectedCenter: EvacuationCenter? = null
    private val client = OkHttpClient()
    private val PREFS_FAVORITES = "evac_favorites"
    private val KEY_FAVORITES = "favorite_centers"
    private var favoriteCenters: MutableSet<String> = mutableSetOf()
    
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
        
        // Load favorites from SharedPreferences
        val prefs = requireContext().getSharedPreferences(PREFS_FAVORITES, Context.MODE_PRIVATE)
        favoriteCenters = prefs.getStringSet(KEY_FAVORITES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

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
                ),
                capacity = 1500,
                facilities = "Multi-purpose covered court, Barangay Hall, BPSO outpost, daycare center, and proximity to the Lagro Fire sub-station"
            )
        )
        
        evacuationCenters.add(
            EvacuationCenter(
                name = "Lagro High School",
                address = "P3G8+PJJ, Misa de Gallo, Novaliches, Quezon City, Metro Manila",
                coordinates = GeoPoint(14.727000, 121.066833),
                imageUrls = listOf(
                    "android.resource://com.evat.app/drawable/lagro_high_1",
                    "android.resource://com.evat.app/drawable/lagro_high_2",
                    "android.resource://com.evat.app/drawable/lagro_high_3"
                ),
                capacity = 4000,
                facilities = "1.3-hectare campus, multi-level buildings (SB Building, etc.), school gym/activity center, and restrooms",
                hotline = "(02) 8939-1092"
            )
        )
        
        evacuationCenters.add(
            EvacuationCenter(
                name = "Lagro Elementary School",
                address = "P3H9+M4V, Ascension Ave, Quezon City, 1100 Metro Manila",
                coordinates = GeoPoint(14.729167, 121.067667),
                imageUrls = listOf(
                    "android.resource://com.evat.app/drawable/lagro_elem_1",
                    "android.resource://com.evat.app/drawable/lagro_elem_2",
                    "android.resource://com.evat.app/drawable/lagro_elem_3"
                ),
                capacity = 3000,
                facilities = "Covered courts, school buildings, and open grounds",
                hotline = "(02) 8565-0623"
            )
        )
        
        evacuationCenters.add(
            EvacuationCenter(
                name = "Ascension of Our Lord Parish Church Patio",
                address = "P3M8+9WC, Ascension Avenue, corner Domingo de Ramos, Novaliches, Quezon City, 1100 Metro Manila",
                coordinates = GeoPoint(14.733167, 121.067333),
                imageUrls = listOf(
                    "android.resource://com.evat.app/drawable/church_1",
                    "android.resource://com.evat.app/drawable/church_2",
                    "android.resource://com.evat.app/drawable/church_3"
                ),
                capacity = 400,
                facilities = "Open-air patio, gated perimeter, proximity to church restrooms, and spiritual support services",
                hotline = "(02) 8939-3596"
            )
        )
        
        evacuationCenters.add(
            EvacuationCenter(
                name = "Our Lady Of Fatima University Quezon City",
                address = "1 Esperanza, Quezon City, 1118 Metro Manila",
                coordinates = GeoPoint(14.706944, 121.064722),
                imageUrls = listOf(
                    "android.resource://com.evat.app/drawable/olfu_1",
                    "android.resource://com.evat.app/drawable/olfu_2",
                    "android.resource://com.evat.app/drawable/olfu_3",
                    "android.resource://com.evat.app/drawable/olfu_4",
                    "android.resource://com.evat.app/drawable/olfu_5"
                ),
                capacity = 1000,
                facilities = "Multi-purpose halls (e.g., San Lorenzo Hall), basketball courts (Building 1-E), medical clinics/infirmaries, and the RISE Tower facilities",
                hotline = "(02) 8420-6003"
            )
        )
        
        evacuationCenters.add(
            EvacuationCenter(
                name = "Barangay Greater Lagro (BHERT)",
                address = "P3G8+GJ8 Greater Lagro, Quezon City, Metro Manila",
                coordinates = GeoPoint(14.726167, 121.066500),
                imageUrls = listOf(
                    "android.resource://com.evat.app/drawable/bhert_1",
                    "android.resource://com.evat.app/drawable/bhert_2",
                    "android.resource://com.evat.app/drawable/bhert_3"
                ),
                capacity = 150,
                facilities = "Medical supplies, first-aid equipment, emergency communication tools (two-way radios), and basic medical triage",
                hotline = "(02) 8711-6160"
            )
        )
        
        evacuationCenters.add(
            EvacuationCenter(
                name = "Brgy. Greater Lagro Centennial Park",
                address = "P398+99X, Flores de Mayo, Novaliches, Quezon City, Metro Manila",
                coordinates = GeoPoint(14.718667, 121.066000),
                imageUrls = listOf(
                    "android.resource://com.evat.app/drawable/park_1",
                    "android.resource://com.evat.app/drawable/park_2",
                    "android.resource://com.evat.app/drawable/park_3"
                ),
                capacity = 2000,
                facilities = "Large open space, children's playground, and a covered court"
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
            currentSelectedCenter?.let { center ->
                getDirectionsToEvacuationCenter(center)
            } ?: run {
                Toast.makeText(requireContext(), "Please select an evacuation center first", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupDropdown() {
        updateDropdownWithFavorites()
        
        binding.evacuationSiteDropdown.setOnItemClickListener { _, _, position, _ ->
            // Get the favorite center from the filtered list
            val favoriteCentersList = evacuationCenters.filter { favoriteCenters.contains(it.name) }
            if (position < favoriteCentersList.size) {
                val selectedCenter = favoriteCentersList[position]
                val originalIndex = evacuationCenters.indexOf(selectedCenter)
                centerMapOnEvacuationSite(selectedCenter, originalIndex)
            }
        }
    }
    
    private fun updateDropdownWithFavorites() {
        val favoriteCentersList = evacuationCenters.filter { favoriteCenters.contains(it.name) }
        val favoriteNames = favoriteCentersList.map { it.name }
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, favoriteNames)
        binding.evacuationSiteDropdown.setAdapter(adapter)
        
        // Clear the dropdown text if no favorites
        if (favoriteNames.isEmpty()) {
            binding.evacuationSiteDropdown.setText("", false)
        }
    }
    
    private fun setupMyLocationButton() {
        binding.fabMyLocation.setOnClickListener {
            centerMapOnCurrentLocation()
        }
        
        binding.fabShowFavorites.setOnClickListener {
            showFavoritedEvacuationCenters()
        }
    }
    
    /**
     * Shows only the favorited evacuation centers on the map.
     * Highlights favorite markers and centers the map to show all of them.
     */
    private fun showFavoritedEvacuationCenters() {
        if (favoriteCenters.isEmpty()) {
            Toast.makeText(requireContext(), "No favorites yet. Tap the star icon on an evacuation center to add it to favorites!", Toast.LENGTH_LONG).show()
            return
        }
        
        // Clear route overlay if any
        routeOverlay?.let {
            binding.mapView.overlays.remove(it)
            routeOverlay = null
        }
        
        // Reset all markers to normal icon first
        markers.forEach { marker ->
            marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_evacuation_marker)
        }
        
        // Find and highlight favorite markers
        val favoriteMarkers = mutableListOf<Marker>()
        val favoriteGeoPoints = mutableListOf<GeoPoint>()
        
        evacuationCenters.forEachIndexed { index, center ->
            if (favoriteCenters.contains(center.name)) {
                val marker = markers[index]
                // Highlight favorite markers with selected icon
                marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_evacuation_marker_selected)
                favoriteMarkers.add(marker)
                favoriteGeoPoints.add(center.coordinates)
            }
        }
        
        // Refresh the map
        binding.mapView.invalidate()
        
        // Center map to show all favorite locations
        if (favoriteGeoPoints.size == 1) {
            // If only one favorite, center on it
            binding.mapView.controller.animateTo(favoriteGeoPoints[0])
            binding.mapView.controller.setZoom(17.0)
        } else if (favoriteGeoPoints.size > 1) {
            // If multiple favorites, zoom to show all of them
            zoomToShowMultipleLocations(favoriteGeoPoints)
        }
        
        // Show success message
        val count = favoriteCenters.size
        val message = if (count == 1) {
            "Showing your favorite evacuation center"
        } else {
            "Showing your $count favorite evacuation centers"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Zooms the map to show multiple locations within the view.
     */
    private fun zoomToShowMultipleLocations(locations: List<GeoPoint>) {
        if (locations.isEmpty()) return
        
        var minLat = locations[0].latitude
        var maxLat = locations[0].latitude
        var minLon = locations[0].longitude
        var maxLon = locations[0].longitude
        
        locations.forEach { point ->
            if (point.latitude < minLat) minLat = point.latitude
            if (point.latitude > maxLat) maxLat = point.latitude
            if (point.longitude < minLon) minLon = point.longitude
            if (point.longitude > maxLon) maxLon = point.longitude
        }
        
        // Calculate center point
        val centerLat = (minLat + maxLat) / 2
        val centerLon = (minLon + maxLon) / 2
        
        binding.mapView.controller.setCenter(GeoPoint(centerLat, centerLon))
        
        // Calculate appropriate zoom level based on the span
        val latDiff = maxLat - minLat
        val lonDiff = maxLon - minLon
        val maxDiff = maxOf(latDiff, lonDiff)
        
        val zoom = when {
            maxDiff > 0.1 -> 11.0
            maxDiff > 0.05 -> 12.0
            maxDiff > 0.02 -> 13.0
            maxDiff > 0.01 -> 14.0
            maxDiff > 0.005 -> 15.0
            else -> 16.0
        }
        
        binding.mapView.controller.setZoom(zoom)
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
        currentSelectedCenter = center
        val bottomSheet = binding.root.findViewById<View>(R.id.bottomSheet)
        
        // Set evacuation center details
        bottomSheet.findViewById<TextView>(R.id.tvEvacuationName).text = center.name
        bottomSheet.findViewById<TextView>(R.id.tvEvacuationAddress).text = center.address
        
        // Set capacity and facilities
        val tvCapacity = bottomSheet.findViewById<TextView>(R.id.tvEvacuationCapacity)
        val tvFacilities = bottomSheet.findViewById<TextView>(R.id.tvEvacuationFacilities)
        val tvHotline = bottomSheet.findViewById<TextView>(R.id.tvEvacuationHotline)

        // Setup favorite button
        val btnFavorite = bottomSheet.findViewById<android.widget.ImageButton>(R.id.btnFavorite)
        val isFavorite = favoriteCenters.contains(center.name)
        btnFavorite.setColorFilter(
            ContextCompat.getColor(requireContext(),
                if (isFavorite) android.R.color.holo_orange_light else android.R.color.darker_gray)
        )
        btnFavorite.setOnClickListener {
            toggleFavorite(center, btnFavorite)
        }
        
        if (center.capacity > 0) {
            tvCapacity.text = "${center.capacity} people"
        } else {
            tvCapacity.text = "Information not available"
        }
        
        if (center.facilities.isNotEmpty()) {
            tvFacilities.text = center.facilities
        } else {
            tvFacilities.text = "Information not available"
        }
        
        if (center.hotline.isNotEmpty()) {
            tvHotline.text = center.hotline
        } else {
            tvHotline.text = "Not available"
        }
        
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

    private fun toggleFavorite(center: EvacuationCenter, btnFavorite: android.widget.ImageButton) {
        val prefs = requireContext().getSharedPreferences(PREFS_FAVORITES, Context.MODE_PRIVATE)
        val isFavorite = favoriteCenters.contains(center.name)
        if (isFavorite) {
            favoriteCenters.remove(center.name)
            btnFavorite.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show()
        } else {
            favoriteCenters.add(center.name)
            btnFavorite.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light))
            Toast.makeText(requireContext(), "Saved to favorites", Toast.LENGTH_SHORT).show()
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favoriteCenters).apply()
        
        // Update dropdown to reflect changes in favorites
        updateDropdownWithFavorites()
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
    
    private fun getDirectionsToEvacuationCenter(center: EvacuationCenter) {
        if (!hasLocationPermissions()) {
            Toast.makeText(requireContext(), "Location permission required for directions", Toast.LENGTH_SHORT).show()
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
                    fetchRoute(userLocation, center.coordinates)
                } ?: run {
                    Toast.makeText(
                        requireContext(),
                        "Unable to get current location. Please try again.",
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
    
    private fun fetchRoute(start: GeoPoint, end: GeoPoint) {
        // Clear any existing route
        routeOverlay?.let {
            binding.mapView.overlays.remove(it)
            routeOverlay = null
        }
        
        // Show loading
        Toast.makeText(requireContext(), "Fetching route...", Toast.LENGTH_SHORT).show()
        
        // Use OSRM (Open Source Routing Machine) API for routing
        val url = "https://router.project-osrm.org/route/v1/driving/${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=polyline"
        
        val request = Request.Builder()
            .url(url)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch route: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseBody ->
                    try {
                        val json = JSONObject(responseBody)
                        val routes = json.getJSONArray("routes")
                        
                        if (routes.length() > 0) {
                            val route = routes.getJSONObject(0)
                            val geometry = route.getString("geometry")
                            val distance = route.getDouble("distance") / 1000 // Convert to km
                            val duration = route.getDouble("duration") / 60 // Convert to minutes
                            
                            // Decode polyline
                            val routePoints = decodePolyline(geometry)
                            
                            activity?.runOnUiThread {
                                drawRoute(routePoints, distance, duration)
                            }
                        } else {
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    "No route found",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        activity?.runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Error parsing route: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        })
    }
    
    private fun drawRoute(routePoints: List<GeoPoint>, distance: Double, duration: Double) {
        // Create polyline overlay
        routeOverlay = Polyline().apply {
            setPoints(routePoints)
            outlinePaint.color = ContextCompat.getColor(requireContext(), R.color.route_color)
            outlinePaint.strokeWidth = 12f
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
        }
        
        // Add route to map
        binding.mapView.overlays.add(routeOverlay)
        binding.mapView.invalidate()
        
        // Show route info
        val distanceText = String.format("%.2f km", distance)
        val durationText = String.format("%.0f min", duration)
        Toast.makeText(
            requireContext(),
            "Route: $distanceText, ~$durationText",
            Toast.LENGTH_LONG
        ).show()
        
        // Zoom to show entire route
        zoomToRoute(routePoints)
    }
    
    private fun zoomToRoute(routePoints: List<GeoPoint>) {
        if (routePoints.isEmpty()) return
        
        var minLat = routePoints[0].latitude
        var maxLat = routePoints[0].latitude
        var minLon = routePoints[0].longitude
        var maxLon = routePoints[0].longitude
        
        routePoints.forEach { point ->
            if (point.latitude < minLat) minLat = point.latitude
            if (point.latitude > maxLat) maxLat = point.latitude
            if (point.longitude < minLon) minLon = point.longitude
            if (point.longitude > maxLon) maxLon = point.longitude
        }
        
        val centerLat = (minLat + maxLat) / 2
        val centerLon = (minLon + maxLon) / 2
        
        binding.mapView.controller.setCenter(GeoPoint(centerLat, centerLon))
        
        // Calculate appropriate zoom level
        val latDiff = maxLat - minLat
        val lonDiff = maxLon - minLon
        val maxDiff = maxOf(latDiff, lonDiff)
        
        val zoom = when {
            maxDiff > 0.1 -> 11.0
            maxDiff > 0.05 -> 12.0
            maxDiff > 0.02 -> 13.0
            maxDiff > 0.01 -> 14.0
            else -> 15.0
        }
        
        binding.mapView.controller.setZoom(zoom)
    }
    
    private fun decodePolyline(encoded: String): List<GeoPoint> {
        val poly = ArrayList<GeoPoint>()
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
            
            val p = GeoPoint(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }
        
        return poly
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
