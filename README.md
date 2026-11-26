# Sanatan Wallpaper (pure Kotlin + Jetpack Compose)

Catalog: https://wallpaper-assets-5l7.pages.dev/wallpapers.json

## Features
- Remote catalog loader + offline JSON cache (`data/remote`, `data/local`)
- Mixed Explore feed + interest algorithm (`domain/ExploreFeedEngine.kt`)
- Typo-tolerant search (`domain/SmartSearchEngine.kt`) + search history with remove (X)
- Clean warm-white theme with soft yellow accents (from the Om icon) + dark mode (Settings > Appearance), 2 / 4 images per row view
- Expanding pill bottom navigation: Home, Liked, Settings
- Tap a wallpaper: half-screen preview, direct Download, share app link, View full screen
- Downloads named `<Category> Wallpaper 0.1`, `0.2` ... (Pictures folder)
- Liked wallpapers are stored on the phone and work offline
- Slow background prefetch + 600 MB disk image cache (Coil)
- Yellow "no signal" error screen with your saved wallpapers below, smart bottom-sheet popups
- Settings: Rate Us, Privacy Policy, About Us, Help (10 FAQs), Contact Us, Share App

## Offline (local) wallpapers
Put images in `app/src/main/assets/localwallpaper/` (jpg, jpeg, png, webp).
The first word of the file name becomes the category, e.g. `mahadev_1.jpg` -> Mahadev.

## App link used for sharing
`util/AppInfo.kt` -> `APP_LINK` (change it when the app has a real Play Store link).

## Build
Push to GitHub -> Actions -> artifact `SanatanWallpaper-build` (debug APK, release APK, AAB).
