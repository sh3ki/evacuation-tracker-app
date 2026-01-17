package com.evat.app

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.evat.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Check location settings when app starts
        checkLocationSettings()

        if (savedInstanceState == null) {
            switchFragment(HomeFragment(), getString(R.string.title_home))
            binding.bottomNav.selectedItemId = R.id.nav_home
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(HomeFragment(), getString(R.string.title_home))
                R.id.nav_location -> switchFragment(LocationFragment(), getString(R.string.title_location))
                R.id.nav_risk -> switchFragment(RiskFragment(), getString(R.string.title_risk))
                R.id.nav_guidelines -> switchFragment(GuidelinesFragment(), getString(R.string.title_guidelines))
                R.id.nav_hotlines -> switchFragment(HotlinesFragment(), getString(R.string.title_hotlines))
                else -> false
            }
        }
    }
    
    private fun checkLocationSettings() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        
        if (!isGpsEnabled && !isNetworkEnabled) {
            showLocationSettingsDialog()
        }
    }
    
    private fun showLocationSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Services Disabled")
            .setMessage("This app requires location services to show evacuation centers and risk-prone areas near you. Would you like to enable location?")
            .setPositiveButton("Enable") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Not Now", null)
            .show()
    }

    private fun switchFragment(fragment: Fragment, title: String): Boolean {
        binding.headerTitle.text = title
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        return true
    }
}
