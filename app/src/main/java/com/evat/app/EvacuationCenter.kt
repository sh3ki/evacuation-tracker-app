package com.evat.app

import org.osmdroid.util.GeoPoint

data class EvacuationCenter(
    val name: String,
    val address: String,
    val coordinates: GeoPoint,
    val imageUrls: List<String>
)
