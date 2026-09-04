# OFC Movies — Netflix-Style Android App (Jetpack Compose)

A cinematic, modern movie streaming application built with **Kotlin** and **Jetpack Compose**, adhering strictly to a custom dark-theme design system, integrating directly with **MovieBox APIs** with client-side HMAC-MD5 request signing and token bootstrapping, and optimized to be **under 10 MB**.

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

## 🚀 Direct MovieBox Integration

The app connects **directly** to MovieBox upstream services without requiring any intermediate backend server:
* **Gateway Endpoint:** `https://api6.aoneroom.com` (with fallback to `api5.aoneroom.com`)
* **Security & Signing Layer (`MovieBoxSigner.kt` & `MovieBoxAuthInterceptor.kt`):**
  - Canonical request generation with alphabetically sorted query parameters.
  - Client-side HMAC-MD5 cryptographic signature (`x-tr-signature`).
  - Dynamic timestamp-hashed token (`X-Client-Token`).
  - Automatic guest token bootstrapping via `/wefeed-mobile-bff/tab-operating` with transparent 401 retry handling.
* **Direct Streaming (`VideoPlayerScreen.kt`):**
  - CloudFront signed cookie parser (`CloudFront-Policy`) resolving `.mpd` adaptive DASH streams from `sacdn.hakunaymatata.com`.
  - Media3 ExoPlayer with `DefaultHttpDataSource.Factory` injecting signed cookies and headers for direct, smooth 1080p/720p/480p playback.
  - Fallback to direct MP4/HLS stream URLs.

---

## 📱 Features & Screens

1. **Splash Screen (`SplashScreen.kt`):**
   - Cinematic `#0A0A0F` backdrop with pulsing OFC Movies logo.
   - Transparent session bootstrap in the background.
   - Smooth transition into Onboarding or Main screen.
2. **Onboarding Screen (`OnboardingScreen.kt`):**
   - 3-card horizontal carousel showcasing Blockbuster Entertainment, Ultra HD 4K DASH Streaming, and Zero Signup Access.
   - Dot indicator, Skip button, and Deep Red "Get Started" pill button.
3. **Home Screen (`HomeScreen.kt`):**
   - Floating top branding header with search & profile buttons.
   - Hero banner carousel with auto-rotation, Play and Details pill buttons.
   - Category filter pills ("All", "Action", "Drama", "Sci-Fi", "Comedy", "Animation", "Thriller").
   - Continue Watching row with real progress bar overlay and play icon.
   - Top 10 Today row with massive Netflix-style ranking numbers (`1`, `2`, `3`...).
   - Curated horizontal scrolling rows from official MovieBox feeds.
4. **Movie Detail Screen (`MovieDetailScreen.kt`):**
   - Parallax poster backdrop header fading seamlessly into `#0A0A0F`.
   - Title, Gold rating badge (`#FFD700`), release year, 4K Ultra HD badge, and genres.
   - Large Red "Play Now" pill button and "My List" toggle.
   - Multi-audio dub selector (Hindi, English, Tamil, Telugu, Spanish, etc.).
   - Season & Episode selector pills for TV series.
   - Storyline description with smooth expand/collapse animation.
   - Cast & Crew row with avatars and role names.
   - "More Like This" recommendations carousel.
5. **Video Player Screen (`VideoPlayerScreen.kt`):**
   - Built on Media3 ExoPlayer with native DASH playback and signed CloudFront cookie injector.
   - Auto-hiding custom controls overlay (3-second idle timer, tap to toggle).
   - Top bar: Back navigation, title, season/episode indicator, and stream quality pill.
   - Center: Rewind 10s, Play/Pause circle button, and Forward 10s.
   - Bottom bar: Current position, deep red scrubber slider, total duration, and stream quality dialog (1080P, 720P, 480P).
6. **Search Screen (`SearchScreen.kt`):**
   - Debounced search bar (400ms) with instant clear action.
   - Category exploration chips and Recent Searches with "Clear All".
   - 3-column responsive poster grid with empty state suggestions.
7. **My Library (`MyListScreen.kt`):**
   - Watchlist and History tabs with 3-column movie grid.
8. **Downloads Manager (`DownloadsScreen.kt`):**
   - Device storage breakdown bar (used vs free space).
   - Downloaded media list with quality tags and offline playback launcher.
9. **Profile & Settings (`ProfileScreen.kt`):**
   - Streaming quality preferences dialog.
   - Image & Stream cache cleaner (clears Coil disk & memory caches).
   - App version and direct MovieBox gateway status.
10. **Category Explorer (`CategoryScreen.kt`):**
    - Deep-filtered 3-column movie grid for specific genres.

---

## ⚡ Sub-10MB Size Optimization

* **R8 Minification & Code Shrinking:** `isMinifyEnabled = true` strips all unused classes, methods, and SDK code.
* **Resource Shrinking:** `isShrinkResources = true` removes all unreferenced drawables and layouts.
* **Locale Stripping:** `resourceConfigurations += listOf("en")` eliminates bloated multi-language string tables.
* **Lean Icons:** Standard lightweight vector drawables, eliminating the 30MB `material-icons-extended` dependency.
* **ProGuard Optimization:** Custom `proguard-rules.pro` tailored for Retrofit, OkHttp, Gson, Coil, and Media3 ExoPlayer.

---

## ⚙️ Cloud CI / GitHub Actions APK Build

The application is built automatically in the cloud via GitHub Actions. **No local compilation is required.**

1. On every commit pushed to `main`, GitHub Actions spins up an Ubuntu environment with JDK 17 and Android SDK 34.
2. The workflow executes `./gradlew assembleDebug --stacktrace`.
3. The workflow verifies that the compiled APK size is strictly **under 10 MB**.
4. The compiled APK is uploaded as a downloadable artifact: **`OFC-Movies-Android-APK`** under the Actions tab.
