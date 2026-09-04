# OFC Movies — Netflix-Style Android App (Jetpack Compose)

A cinematic, modern movie streaming application built with **Kotlin** and **Jetpack Compose**, adhering strictly to a custom dark-theme design system.

---

## 🎨 Design System

* **Theme:** Dark mode only, cinematic feel
* **Background:** `#0A0A0F` (near-black)
* **Surface/Card:** `#16161F`
* **Surface Elevated:** `#22222E`
* **Primary Accent:** `#E50914` (Netflix Deep Red) — Used strictly for CTAs, play buttons, active indicators, and badges
* **Secondary Accent:** `#FFD700` (Gold) — For IMDb ratings and premium tags
* **Typography:** Bold condensed headings (`letterSpacing = -0.6.sp`), clean readable body text
* **Corner Radii:** `8dp` for movie posters/cards, `24dp` for pill-shaped buttons
* **Spacing:** `16dp` base padding, `24dp` between sections
* **Micro-interactions:** Interactive press-down scale animations (`0.95f`), smooth crossfades, shimmer image loading

---

## 📱 Features & Components Built

* **`Color.kt`, `Type.kt`, `Theme.kt`:** Zero default Material3 purple/blue colors. Custom cinematic dark scheme.
* **`HeroBanner.kt`:** Auto-scrolling featured carousel with gradient overlays (`transparent` to `#0A0A0F`), title, pill-shaped Play CTA (`#E50914`), and Details button.
* **`MovieCard.kt`:** 2:3 aspect ratio, 8dp rounded corners, scale-up animation on press, gold rating badges, and dub tags.
* **`ContinueWatchingCard.kt`:** Horizontal card with real progress bar overlay and centered play icon.
* **`MovieRow.kt`:** Smooth horizontal scrolling (`LazyRow`) for curated movie rows.
* **`BottomNavBar.kt`:** Custom bottom navigation bar with red pill indicator for the active tab (Home, Search, Downloads, Profile).
* **`ShimmerLoading.kt`:** Shimmer placeholder effect while images load via Coil.
* **`HomeScreen.kt`:** Full featured home screen with top branding bar, Hero banner, Continue Watching, and categorized rows.

---

## ⚙️ Cloud CI / GitHub Actions APK Build

The application is built automatically in the cloud via GitHub Actions. **No local compilation is required.**

1. On every commit pushed to `main`, GitHub Actions spins up an Ubuntu environment with JDK 17 and Android SDK 34.
2. The workflow executes `./gradlew assembleDebug`.
3. The compiled APK is uploaded as a downloadable artifact: **`OFC-Movies-Android-APK`** under the Actions tab.
