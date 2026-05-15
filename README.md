# Paryavaran-Kavalu – Community Geo-Tagging Platform for Waste Blackspot Reporting & Cleanup
## Paryavaran-Kavalu is an offline-first Android civic-tech application that empowers citizens to report illegal waste dumping sites with GPS location and photo evidence, visualize them on an interactive map, and verify cleanups — supporting India's Swachh Bharat Mission 2.0 through community-driven environmental action.

<h2>Table of Contents</h2>
<ul>
  <li> <a href="#about"> About </a></li>
  <ul>
    <li><a href="#wa"> What is Paryavaran-Kavalu? </a></li>
    <li><a href="#features"> Features </a></li>
    <li><a href="#why"> Why Paryavaran-Kavalu? </a></li>
  </ul>
  <li> <a href="#getting_started"> Getting Started </a></li>
  <ul>
    <li><a href="#prerequisites"> Prerequisites </a></li>
    <li><a href="#installation"> Installation </a></li>
    <li><a href="#setup"> Project Setup </a></li>
    <li><a href="#run"> Building & Running the App </a></li>
  </ul>
  <li> <a href="#tech_used"> TechStack Used </a></li>
  <li> <a href="#architecture"> System Architecture </a></li>
  <li> <a href="#screenshots"> Screenshots and App Demonstration </a></li>
  <li> <a href="#conclusion"> Conclusion </a></li>
  <li> <a href="#team"> Developed By </a></li>
</ul>

---

<section id="about">
  <h2> About </h2>

  <h3 id="wa"> What is Paryavaran-Kavalu? </h3>
  Paryavaran-Kavalu (meaning <strong>Environment Guardian</strong> in Kannada) is a native Android application built entirely with Jetpack Compose and Material 3. It enables citizens to geo-tag and report waste blackspots — illegal garbage dumping sites — with a photo and GPS coordinates. Reports are visualized on an OSMDroid-powered interactive map, and cleanup volunteers can verify site resolution by capturing a proof photo. The app runs fully offline using Room DB, making it accessible even in areas with weak network connectivity.

  <h3 id="features"> Features </h3>
  <ul>
    <li><strong>Quick Waste Blackspot Reporting</strong>
      <ul>
        <li>Capture a photo and auto-tag GPS coordinates in under 60 seconds.</li>
        <li>Select from 6 waste type categories: Plastic, Organic, Construction, E-Waste, Medical, Mixed.</li>
        <li>Awards +20 Eco-Karma points instantly on report submission.</li>
      </ul>
    </li>
    <br>
    <li><strong>Interactive Cleanliness Map</strong>
      <ul>
        <li>OSMDroid-powered map displaying all reported incidents as color-coded markers.</li>
        <li>Red markers = Pending (unreported/uncleaned), Green markers = Cleaned (verified).</li>
        <li>Distance filters to focus on nearby blackspots; tap any pin for full incident detail.</li>
      </ul>
    </li>
    <br>
    <li><strong>Cleanup Verification with Proof Photo</strong>
      <ul>
        <li>Volunteers capture an after-photo as proof when marking a site as cleaned.</li>
        <li>Room DB row updated instantly; map marker turns green via Kotlin Flow observation.</li>
        <li>Awards +30 Eco-Karma points; duplicate reward prevention built in.</li>
      </ul>
    </li>
    <br>
    <li><strong>Eco-Karma Points & Gamification</strong>
      <ul>
        <li>Tiered reward system: Seedling → Sprout → Sapling → Guardian → Forest Guardian.</li>
        <li>Leaderboard merging real user points with demo community entries for a live feel.</li>
        <li>Rewards redemption catalogue with transactional point deduction.</li>
      </ul>
    </li>
    <br>
    <li><strong>Offline-First Design</strong>
      <ul>
        <li>All reports, profile, and redemptions stored locally in Room DB on the device.</li>
        <li>26 demo seed pins auto-generated on first install for evaluation without a live community.</li>
        <li>Works fully without internet — map tiles cached via OSMDroid.</li>
      </ul>
    </li>
    <br>
    <li><strong>Profile & Leaderboard</strong>
      <ul>
        <li>Edit nickname and bio; profile nickname propagates across all existing report rows.</li>
        <li>Tabbed screen: Leaderboard / Redeem / Claimed rewards.</li>
        <li>Tap any leaderboard entry to view that user's cleanups filtered on the map.</li>
      </ul>
    </li>
    <br>
    <li><strong>Google Maps Navigation to Blackspot</strong>
      <ul>
        <li>Tap "Navigate" on any incident to open Google Maps with walking directions.</li>
        <li>Uses Android Intent — completely free, no API key required.</li>
      </ul>
    </li>
  </ul>

  <h3 id="why"> Why Paryavaran-Kavalu? </h3>
  <ul>
    <li><strong>Solves a Real Public Health Problem</strong>: Illegal garbage dumping creates vectors for Malaria, Dengue, and Cholera. This app gives citizens and volunteers the tool to act on it.</li>
    <li><strong>Offline-First for Indian Networks</strong>: All core flows — reporting, map, cleanup — work without internet. Designed for areas with weak connectivity.</li>
    <li><strong>Closes the Feedback Loop</strong>: Unlike a simple complaint form, this app lets reporters see their sites get cleaned and verified with proof photos — creating accountability.</li>
    <li><strong>Gamification That Sustains Engagement</strong>: Eco-Karma points, tier badges, and a leaderboard transform civic duty into a rewarding community activity.</li>
    <li><strong>Supports Swachh Bharat 2.0</strong>: Directly aligned with India's national cleanliness mission by digitizing the report-verify-clean feedback loop that currently doesn't exist in any standardized form.</li>
    <li><strong>Evaluator-Ready Demo Data</strong>: 26 seeded demo pins ensure the map and all flows work perfectly during evaluation without requiring real community reports.</li>
  </ul>
</section>

---

<section id="getting_started">
  <h2> Getting Started </h2>

  <h3 id="prerequisites"> Prerequisites </h3>
  <p>Before you begin, ensure the following are installed and set up in your development environment:</p>

  <h4>Development Environment:</h4>
  <ul>
    <li><strong>Android Studio Hedgehog or later</strong>: Required for building and running the project.
      <ul>
        <li><a href="https://developer.android.com/studio">Download Android Studio</a></li>
      </ul>
    </li>
    <li><strong>JDK 11+</strong>: Required for Kotlin compilation (bundled with Android Studio).</li>
    <li><strong>Android SDK</strong>: compileSdk 35, minSdk 24 (Android 7.0+).</li>
    <li><strong>Google Play Services</strong>: Required on device/emulator for FusedLocationProviderClient GPS.</li>
  </ul>

  <h4>Device / Emulator:</h4>
  <ul>
    <li><strong>Physical Android device</strong> (API 24+) — recommended for camera and GPS testing.</li>
    <li><strong>Android Emulator</strong> — supported; app includes emulator offset helper for GPS simulation.</li>
    <li>Ensure <strong>Google Maps</strong> is installed on device for navigation intent to work.</li>
  </ul>

  <h3 id="installation"> Installation </h3>

  <h4>Clone the Repository:</h4>

```bash
git clone https://github.com/Suhas-Varna/Paryavaran-Kavalu.git
cd Paryavaran-Kavalu
```

  <h3 id="setup"> Project Setup </h3>

  <ol>
    <li>
      <p><strong>Open in Android Studio:</strong></p>
      <pre><code>File → Open → Select the cloned Paryavaran-Kavalu folder</code></pre>
      <p>Wait for Gradle sync to complete.</p>
    </li>
    <li>
      <p><strong>Add your Google Maps API Key</strong> in <code>AndroidManifest.xml</code>:</p>

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_GOOGLE_MAPS_API_KEY_HERE" />
```

  <p>Get a free API key from <a href="https://console.cloud.google.com/">Google Cloud Console</a> → Enable Maps SDK for Android.</p>
    </li>
    <li>
      <p><strong>Room DB</strong> is pre-configured. On first launch, the app auto-seeds:</p>
      <ul>
        <li>Default user profile (userId = 1)</li>
        <li>Rewards redemption catalogue</li>
        <li>26 demo waste blackspot pins around your current location</li>
      </ul>
    </li>
    <li>
      <p><strong>Permissions required</strong> — the app will request these at runtime:</p>
      <ul>
        <li><code>ACCESS_FINE_LOCATION</code> — for GPS coordinates on report submission</li>
        <li><code>CAMERA</code> — for capturing report and cleanup proof photos</li>
        <li><code>INTERNET</code> — for OSMDroid map tile loading</li>
        <li><code>ACCESS_NETWORK_STATE</code> — for network-aware map caching</li>
      </ul>
    </li>
  </ol>

  <h3 id="run"> Building & Running the App </h3>
  <ol>
    <li><strong>Connect your Android device</strong> via USB (enable Developer Options + USB Debugging) or launch an emulator.</li>
    <li><strong>Click Run ▶</strong> in Android Studio or use:</li>
  </ol>

```bash
./gradlew assembleDebug
```

  <p>The app will install and launch on your device/emulator. The map loads with demo pins immediately on first install.</p>
</section>

---

<section id="tech_used">
  <h2> TechStack — Built With </h2>

  <p><strong>Kotlin:</strong> Core programming language for all Android development — coroutines for async operations, null safety, and Google-recommended for modern Android.</p>

  <p><strong>Jetpack Compose + Material 3:</strong> Entire UI built declaratively with Compose. No XML layouts — all screens are Composable functions styled with Material 3 components and theming.</p>

  <p><strong>Navigation Compose (2.7.7):</strong> Single-activity navigation between all screens (Home, Map, Report, Cleanup, Karma, Leaderboard, Profile) using a NavGraph.</p>

  <p><strong>Room DB (2.7.0) + KSP:</strong> Local SQLite database storing all reports, user profile, rewards catalogue, and redemption transactions. Version 9 with documented migrations.</p>

  <p><strong>OSMDroid (6.1.16):</strong> OpenStreetMap-based interactive map library. Displays incident markers, supports offline tile caching, and handles distance-based filtering.</p>

  <p><strong>FusedLocationProviderClient (Play Services 21.3.0):</strong> GPS coordinate capture for every report submission. Includes emulator offset helper for testing.</p>

  <p><strong>Coil (coil-compose):</strong> Efficient image loading for report photos and cleanup proof images throughout the app.</p>

  <p><strong>Kotlin Coroutines + Flow:</strong> All database operations and location fetching run on background threads. UI updates reactively via StateFlow observed in Composables.</p>

  <p><strong>ViewModel (AndroidViewModel):</strong> Single activity-wide WasteReportViewModel shared across all screens — map, report form, leaderboard, and profile all share the same data flows.</p>
</section>

---

<section id="architecture">
  <h2> System Architecture </h2>

  <h3>🏗️ High-Level Architecture</h3>

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Paryavaran-Kavalu APP                              │
│  ┌─────────────────┐  ┌──────────────────┐  ┌───────────────────────────┐   │
│  │   Home / Splash │→ │  Report & Camera │→ │  Map & Incident Detail    │   │
│  └─────────────────┘  └──────────────────┘  └───────────────────────────┘   │
│           ↓                    ↓                          ↓                 │
│  ┌─────────────────┐  ┌──────────────────┐  ┌───────────────────────────┐   │
│  │ Leaderboard &   │  │ Cleanup Camera & │  │  Profile & Eco-Karma      │   │
│  │ Rewards Redeem  │  │ Verification     │  │  Points Dashboard         │   │
│  └─────────────────┘  └──────────────────┘  └───────────────────────────┘   │
│           ↓                    ↓                          ↓                 │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │              WasteReportViewModel (Activity-scoped)                 │    │
│  │  • Exposes StateFlow / Flow for reports, profile, leaderboard       │    │
│  │  • Handles report insert, cleanup update, karma award logic         │    │
│  │  • Demo seed logic + emulator GPS offset                            │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│           ↓                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      Room Database (paryavaran.db)                  │    │
│  │  ReportEntity | UserEntity | RedeemItemEntity | RedemptionEntity    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
```

  <h3>📊 Data Flow Diagram</h3>

```
  CITIZEN SEES A WASTE BLACKSPOT
                │
                ▼
┌─────────────────────────────────────────────────────┐
│              Paryavaran-Kavalu App                  │
│  • Opens camera → captures photo                    │
│  • Selects waste type(s)                            │
│  • GPS auto-fetched via FusedLocationProvider       │
└───────────────────┬─────────────────────────────────┘
                    │  ViewModel.submitReport()
                    ▼
┌─────────────────────────────────────────────────────┐
│              WasteReportViewModel                   │
│  • Inserts ReportEntity into Room DB                │
│  • Awards +20 Eco-Karma points to UserEntity        │
│  • Triggers demo seed if needed                     │
└───────────────────┬─────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│              Room DB (Local, On-Device)             │
│  • ReportEntity saved with status = Pending         │
│  • Flow emits updated report list                   │
└───────────────────┬─────────────────────────────────┘
                    │  Flow observed in MapScreen
                    ▼
┌─────────────────────────────────────────────────────┐
│              OSMDroid Map Screen                    │
│  • New Red marker appears at reported GPS location  │
│  • Volunteer taps marker → Incident Detail          │
│  • Captures proof photo → markReportCleaned()       │
│  • Marker turns Green → +30 Eco-Karma awarded       │
└─────────────────────────────────────────────────────┘
```

  <h3>🗂️ Project Structure</h3>

```
Paryavaran-Kavalu/
│
├── app/
│   ├── src/main/
│   │   ├── java/com/example/paryavaran_kavalu/
│   │   │   ├── MainActivity.kt               # Single activity; sets AppNavigation
│   │   │   ├── ParyavaranApplication.kt      # App class; Room init, OSMDroid config, seed
│   │   │   ├── NavGraph.kt                   # Navigation routes (AppNavigation)
│   │   │   ├── WasteReportViewModel.kt       # Activity-scoped ViewModel; all logic
│   │   │   │
│   │   │   ├── ui/                           # All Composable screens
│   │   │   │   ├── SplashScreen.kt
│   │   │   │   ├── HomeScreen.kt             # Carousel guide + entry points
│   │   │   │   ├── MapScreen.kt              # OSMDroid map + markers + filters
│   │   │   │   ├── ReportScreen.kt           # Report form + waste type chips
│   │   │   │   ├── CameraScreen.kt           # Photo capture for report
│   │   │   │   ├── CleanupCameraScreen.kt    # Proof photo capture for cleanup
│   │   │   │   ├── ReportSuccessScreen.kt    # Celebration after report submit
│   │   │   │   ├── CleanupSuccessScreen.kt   # Celebration after cleanup verify
│   │   │   │   ├── IncidentDetailScreen.kt   # Full incident view + navigate button
│   │   │   │   ├── LeaderboardScreen.kt      # Leaderboard / Redeem / Claimed tabs
│   │   │   │   └── ProfileScreen.kt          # Edit nickname, bio, view karma
│   │   │   │
│   │   │   ├── data/                         # Room DB layer
│   │   │   │   ├── AppDatabase.kt            # Room DB v9 + migrations
│   │   │   │   ├── ReportEntity.kt
│   │   │   │   ├── UserEntity.kt
│   │   │   │   ├── RedeemItemEntity.kt
│   │   │   │   ├── RedemptionTransactionEntity.kt
│   │   │   │   ├── ReportDao.kt
│   │   │   │   ├── UserDao.kt
│   │   │   │   ├── RedeemItemDao.kt
│   │   │   │   └── RedemptionDao.kt
│   │   │   │
│   │   │   └── util/                         # Helpers
│   │   │       ├── MockReportsSeed.kt        # 26 demo pin seeding logic
│   │   │       └── GeoUtils.kt               # distanceMeters, offsetLatLon, emulator helper
│   │   │
│   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── assets/
│   │
│   └── build.gradle.kts
│
└── README.md
```

  <h3>🔐 Security & Privacy</h3>
  <ul>
    <li><strong>Data Privacy</strong>:
      <ul>
        <li>All data (reports, profile, rewards) stored locally on-device only — no backend server, no cloud upload.</li>
        <li>Photos stored as URI references in Room; actual files remain in device local storage.</li>
        <li>No personally identifiable information transmitted externally in v1.0.</li>
      </ul>
    </li>
    <li><strong>Permission Handling</strong>:
      <ul>
        <li>Runtime permission requests with rationale dialogs for Camera and Location.</li>
        <li>Graceful degradation if permissions are denied — app continues functioning with manual fallbacks.</li>
      </ul>
    </li>
    <li><strong>GPS Usage</strong>:
      <ul>
        <li>One-shot location request per report — no continuous background tracking.</li>
        <li>Battery-efficient by design using FusedLocationProviderClient.</li>
      </ul>
    </li>
  </ul>

  <h3>⚡ Performance Optimizations</h3>
  <ul>
    <li><strong>Database</strong>:
      <ul>
        <li>Room Flows with stateIn / SharingStarted for efficient reactive UI updates.</li>
        <li>Viewport-based marker queries to avoid loading all pins at low zoom.</li>
        <li>DB migrations (v1–v9) documented; fallbackToDestructiveMigration as safety net.</li>
      </ul>
    </li>
    <li><strong>Map & UI</strong>:
      <ul>
        <li>OSMDroid tile caching for offline map rendering.</li>
        <li>Coil for memory-efficient photo loading across report cards and detail screens.</li>
        <li>Compose recomposition minimized via stable state hoisting in ViewModel.</li>
      </ul>
    </li>
  </ul>
</section>

---

<section id="screenshots">
  <h2> App Demonstration </h2>
  <h2> Screenshots </h2>
  <p><i>Screenshots will be added after final build. The app includes the following screens:</i></p>
  <ul>
    <li>🏠 Home Screen with carousel guide</li>
    <li>🗺️ Cleanliness Map with red/green incident markers</li>
    <li>📷 Report Screen with waste type chips and GPS card</li>
    <li>✅ Report Success celebration screen (+20 Eco-Karma)</li>
    <li>🧹 Cleanup Verification with proof photo capture</li>
    <li>🏆 Leaderboard, Redeem, and Claimed rewards tabs</li>
    <li>👤 Profile Screen with Eco-Karma tier badge</li>
  </ul>
</section>

---

<section id="conclusion">
  <h2> Conclusion </h2>
  <p>
    Paryavaran-Kavalu successfully delivers a complete offline-first civic-tech platform for community waste blackspot reporting and cleanup verification. By combining Jetpack Compose UI, OSMDroid map visualization, Room DB persistence, FusedLocationProvider GPS, and an Eco-Karma gamification system, the app closes the report-verify-clean feedback loop that currently does not exist in any standardized form for Swachh Bharat volunteers. The demo seed data, migration-safe database, and single-ViewModel architecture make it both evaluator-ready and production-extensible. Overall, Paryavaran-Kavalu demonstrates how mobile technology can directly empower citizens to take ownership of their environment and drive measurable public health improvement in Indian cities.
  </p>
</section>

---

<section id="team">
  <h2> Developed By </h2>

  <h3> Suhas Varna </h3>
  <p align="left">
    <a href="https://github.com/Suhas-Varna" style="text-decoration: none;" target="_blank" rel="nofollow">
      <img src="https://img.shields.io/badge/GitHub-black?style=flat&logo=github" alt="GitHub" />
    </a>
    <a href="https://www.linkedin.com/in/suhas-varna2003/" style="text-decoration: none;" target="_blank">
      <img src="https://img.shields.io/badge/LinkedIn-blue?style=flat&logo=linkedin" alt="LinkedIn" />
    </a>
  </p>
</section>
