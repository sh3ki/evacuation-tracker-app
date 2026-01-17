# EVAT (Evacuation Tracker)

Kotlin Android app with a splash screen and a single home screen showing a header, map placeholder, and bottom navigation items (Location, Risk Prone Areas, Safety Guidelines, Hotlines). Built for emulator `emulator-5554`.

## Quick start
1. Ensure Android SDK/Android Studio is installed.
2. Place the provided logo file at `app/src/main/res/drawable/evat_logo.png` (use the supplied image). The layouts currently reference `@drawable/logo_placeholder`; swap it to your logo once added.
3. From project root, run `./gradlew.bat assembleDebug` (Windows) to build, or use Android Studio to run on `emulator-5554`.
4. Launch the app: the splash shows the logo/title, then navigates to the main screen with header, map placeholder, and bottom nav items.

## Map integration (next step)
- Replace the map placeholder with Google Maps SDK for Android when you have an API key. Insert the `MapView`/`SupportMapFragment` into `activity_main.xml` and add the key in the manifest.

## Fonts
- Using `sans-serif-medium`/`sans-serif-black` (system modern/professional). If you prefer a specific font, add it to `res/font` and set it in styles/text appearances.
