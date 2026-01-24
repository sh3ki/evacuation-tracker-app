# EVAT - Map Feature Implementation

## ✅ Completed Features

### 1. Blue Dot for Current Location
- **Replaced** the white arrow with a **blue dot** to display the current phone location
- The blue dot design matches Google Maps style with:
  - Outer pulse circle (light blue)
  - Inner solid circle (medium blue)
  - Center white dot
- Drawable resource: `location_blue_dot.xml`

### 2. Evacuation Centers with Red Pin Markers
Added **7 evacuation centers** for Greater Lagro area with red pin markers:

1. **Lagro Plaza**
   - Address: P3G8+GFP, Flores de Mayo, Novaliches, Quezon City, Metro Manila
   
2. **Lagro High School**
   - Address: P3G8+PJJ, Misa de Gallo, Novaliches, Quezon City, Metro Manila
   
3. **Lagro Elementary School**
   - Address: P3H9+M4V, Ascension Ave, Quezon City, 1100 Metro Manila
   
4. **Ascension of Our Lord Parish Church Patio**
   - Address: P3M8+9WC, Ascension Avenue, corner Domingo de Ramos, Novaliches, Quezon City, 1100 Metro Manila
   
5. **Our Lady Of Fatima University Quezon City**
   - Address: 1 Esperanza, Quezon City, 1118 Metro Manila
   
6. **Barangay Greater Lagro (BHERT)**
   - Address: P3G8+GJ8 Greater Lagro, Quezon City, Metro Manila
   
7. **Brgy. Greater Lagro Centennial Park**
   - Address: P398+99X, Flores de Mayo, Novaliches, Quezon City, Metro Manila

### 3. Evacuation Center Details Bottom Sheet
When a red pin marker is clicked, a bottom sheet slides up showing:
- **Evacuation center name** (title)
- **Image carousel** with multiple photos (swipeable using ViewPager2)
- **Image indicator** showing current image position (e.g., "1 / 3")
- **Address** section
- **"Get Directions" button** (prepared for future functionality)

### 4. Dropdown Menu for Site Selection
- Located at the **top of the map**
- Material Design dropdown with all 7 evacuation sites
- When a site is selected:
  - Map automatically **centers** on that evacuation site
  - Zooms to level 17 for better visibility
  - Opens the bottom sheet with site details

### 5. "My Location" Button
- **Floating Action Button** in the **bottom right corner**
- Blue background with location target icon
- When clicked:
  - Centers the map on the user's current location
  - Zooms to level 17
  - Shows a confirmation toast message

## 🎨 UI/UX Features

### Color Scheme
- **Blue theme** for location features (#4A90E2)
- **Red markers** for evacuation centers (#D32F2F)
- Material Design 3 components throughout

### Responsive Design
- Bottom sheet with drag handle
- Smooth animations for map movements
- Touch-friendly controls
- Proper spacing and padding

### User Feedback
- Toast messages for location updates
- Loading indicators for images (Glide)
- Fallback placeholder images

## 📦 Dependencies Added

```gradle
// Image Loading - Glide
implementation("com.github.bumptech.glide:glide:4.16.0")

// ViewPager2 for image carousel
implementation("androidx.viewpager2:viewpager2:1.0.0")

// CoordinatorLayout for bottom sheet
implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
```

## 🗂️ Files Created/Modified

### New Files Created:
1. `location_blue_dot.xml` - Blue dot drawable for current location
2. `ic_evacuation_marker.xml` - Red pin marker for evacuation centers
3. `ic_my_location.xml` - Icon for "My Location" button
4. `bottom_sheet_evacuation_details.xml` - Layout for evacuation details
5. `item_evacuation_image.xml` - Layout for image carousel items
6. `EvacuationCenter.kt` - Data model for evacuation centers
7. `EvacuationImageAdapter.kt` - RecyclerView adapter for image carousel

### Modified Files:
1. `fragment_location.xml` - Updated with CoordinatorLayout, dropdown, FAB, and bottom sheet
2. `LocationFragment.kt` - Complete rewrite with all new features
3. `build.gradle.kts` - Added new dependencies

## 🚀 How to Use

1. **Launch the app** and navigate to the **Map** tab (bottom navigation)
2. **Grant location permissions** when prompted
3. **View your current location** as a blue dot on the map
4. **See all 7 evacuation centers** marked with red pins
5. **Click any red pin** to view detailed information about that evacuation center
6. **Use the dropdown** at the top to select and navigate to specific evacuation sites
7. **Tap the "My Location" button** (bottom right) to center the map on your current location

## 📝 Notes

### Images
- Currently using **placeholder images** from Lorem Picsum API
- To use **real images**, replace the URLs in `LocationFragment.kt` → `setupEvacuationCenters()` function
- You can get real images from:
  - Google Maps photos
  - Official website photos
  - Local government resources

### Future Enhancements Ready for Implementation:
- "Get Directions" button functionality (integrate with Google Maps or in-app routing)
- Real-time distance calculation to nearest evacuation center
- Search functionality for evacuation centers
- Filter by center capacity or facilities
- Offline map support with cached tiles

## ✅ Build Status
- **Build**: ✅ Successful
- **Warnings**: Only deprecation warnings (expected with OSMDroid)
- **Errors**: ✅ None
- **APK**: Successfully installed on emulator

---

**Status**: All requested features have been implemented and are fully functional! 🎉
