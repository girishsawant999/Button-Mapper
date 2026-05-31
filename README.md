# Button Mapper for Android TV

**Button Mapper** is a native Android TV utility that lets you customize and remap remote control (or game controller) buttons to perform custom actions or launch specific applications. With a user interface tailored for D-pad navigation and a responsive design theme, managing your button layouts is seamless and direct.

---

## 🌟 Key Features

- **Global Key Interception**: Intercept physical button presses (Netflix button, Source, Guide, color keys, volume, etc.) globally using a lightweight Android `AccessibilityService`.
- **D-pad Optimized UI**: Beautiful glassmorphic/pastel design system optimized for Leanback and D-pad control, ensuring ease of use on TV devices.
- **Smart Auto-Detection**: Select the Key Code field in the UI and press any remote button to automatically capture and display its keycode.
- **Rich Action Library**:
  - **System Volume Control**: Remap buttons to increment or decrement the media stream volume directly.
  - **Self-Launch Utility**: Remap a key to instantly launch the Button Mapper configuration app.
  - **Target App Launcher**: Bind buttons to launch any installed TV or standard app (automatically resolves standard launch intents and Leanback launch intents).
- **Accidental Press Protection**: Features a custom *Hold to Delete* confirmation button with a smooth 1-second progress animation to prevent accidental deletion of key mappings.

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: Jetpack Compose (integrated with Material 3 for TV/Leanback support).
- **Service Layer**: Custom `AccessibilityService` (`ButtonMapperService`) that registers for `flagRequestFilterKeyEvents` and processes `KeyEvent` actions.
- **Persistence**: Fast SharedPreferences-backed caching layout (`StorageHelper`) using serialized JSON array format to read/write custom bindings.
- **Threading & Reactivity**: Kotlin Coroutines and StateFlow for modern state management.

---

## 📱 Required Permissions

To function correctly, Button Mapper requests the following permissions:
- **`android.permission.BIND_ACCESSIBILITY_SERVICE`**: Crucial for detecting and intercepting remote key presses globally, even when the app is in the background.
- **`android.permission.MODIFY_AUDIO_SETTINGS`**: Enables remapping buttons to system volume adjustments (Volume Up / Down).
- **`android.permission.QUERY_ALL_PACKAGES`**: Allows the app to query installed packages on the device so you can select and bind them to remote buttons.

---

## 🚀 Installation & Setup

1. **Build & Install**:
   Build the release or debug APK and install it on your Android TV:
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
2. **Enable Accessibility Service**:
   - Open the app, and you will see the **Accessibility Service** status in the bottom-left of the sidebar.
   - Click **Enable Accessibility** (this will open the Android system Accessibility Settings directly).
   - Locate **Button Mapper** and toggle the service to **Active**.
3. **Map a Button**:
   - Click **Add Mapping** in the main panel.
   - Click/Focus the **Key Code** input field.
   - Press the key on your remote you wish to map (e.g., a color button, search button, etc.) to auto-detect its code.
   - Select the desired action or application package from the dropdown.
   - Press **Add**.
4. **Delete a Mapping**:
   - Navigate to the mapping in the main list.
   - Click and hold the **Hold to Delete** button for 1 second. The background animation will fill up, and the mapping will be removed.

---

## 📂 Project Structure

```
Button-Mapper/
├── app/                         # Android application module
│   ├── src/main/
│   │   ├── java/com/example/buttonmapper/
│   │   │   ├── data/            # Data layer and repository pattern
│   │   │   ├── ui/              # Jetpack Compose UI
│   │   │   │   ├── main/        # Main screen, dialogs, and components
│   │   │   │   └── theme/       # Color palettes, styling, and typography
│   │   │   ├── ButtonMapperService.kt  # Key intercepting Accessibility Service
│   │   │   └── StorageHelper.kt # Configuration management (JSON/Prefs)
│   │   └── AndroidManifest.xml  # Permissions, services, and activity declarations
│   └── build.gradle.kts         # Module build configuration
├── build.gradle.kts             # Project-level build script
└── settings.gradle.kts          # Project settings and module inclusions
```
