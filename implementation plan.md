# Implementation Plan - Native Android TV Button Mapper & Scheduler

We will create a lightweight native Android TV application in Kotlin that runs on Android 12 (Google TV). It will provide key remapping via an Accessibility Service, automated time-based action triggers using `AlarmManager`, and persistent settings using `SharedPreferences`.

## Proposed Features

1. **Accessibility Service (`ButtonMapperService`)**:
   - Intercepts system-wide key presses on the remote (such as custom app keys, media keys, back, etc.).
   - Consumes and remaps key events to launch our app or trigger system actions (volume, brightness).
   - Persists key mapping configurations in `SharedPreferences`.

2. **Scheduler & Alarm Receiver (`ActionReceiver` & `BootReceiver`)**:
   - `AlarmManager` registers time-based triggers.
   - `ActionReceiver` runs directly inside the broadcast receiver to adjust volume or brightness instantly when the alarm fires, keeping the app lightweight and avoiding complex background services.
   - `BootReceiver` listens for `ACTION_BOOT_COMPLETED` and re-schedules all registered alarms upon TV restart.

3. **User Interface (`MainActivity`)**:
   - Optimized for TV navigation (D-pad focus states).
   - Quick settings to grant permissions: Accessibility Service, Modify System Settings (`WRITE_SETTINGS`), and Post Notifications.
   - List and customize button mappings.
   - List and customize scheduled triggers.

4. **Extremely Lightweight Architecture**:
   - Minimal dependencies to keep APK size small.
   - Standard Kotlin, Jetpack Compose for UI (lightweight standard elements), and `SharedPreferences` for storage (no heavy databases like Room).

---

## User Review Required

> [!IMPORTANT]
> **Accessibility Service Activation**:
> For key interception to work, the user must manually enable the app's Accessibility Service in TV settings (_Settings -> System -> Accessibility -> Button Mapper_). We will add a button in the UI that directly takes the user to this settings page.

> [!IMPORTANT]
> **Write Settings Permission**:
> To adjust screen brightness, the user must grant "Modify System Settings" (`WRITE_SETTINGS`) permission. We will provide a button in the UI to navigate to the system authorization screen.

---

## Proposed Changes

### Project Initialization

We will initialize the project using the native template:

```bash
android create empty-activity --name="ButtonMapper" --output=.
```

### File Structure

We will create the following files:

#### [NEW] `StorageHelper.kt` (Storage & Configuration)

- Manages serialization/deserialization of mappings and scheduled alarms to `SharedPreferences`.

#### [NEW] `ButtonMapperService.kt` (Accessibility Service)

- Extends `AccessibilityService`.
- Filters and overrides key events.
- Performs actions: open app, change volume, change brightness.

#### [NEW] `res/xml/accessibility_service_config.xml`

- Configuration file for the Accessibility Service specifying that it filters key events (`android:accessibilityFlags="flagRequestFilterKeyEvents"`).

#### [NEW] `ActionReceiver.kt` (Alarm Broadcast Receiver)

- Handles scheduled triggers from `AlarmManager`.
- Updates volume using `AudioManager` and brightness using `Settings.System`.

#### [NEW] `BootReceiver.kt` (Boot Receiver)

- Listens for `android.intent.action.BOOT_COMPLETED`.
- Reads saved triggers and schedules them with `AlarmManager` on boot.

#### [MODIFY] `AndroidManifest.xml`

- Declarations for `AccessibilityService`, `BootReceiver`, `ActionReceiver`, and permissions (`BIND_ACCESSIBILITY_SERVICE`, `RECEIVE_BOOT_COMPLETED`, `WRITE_SETTINGS`, `MODIFY_AUDIO_SETTINGS`, `SCHEDULE_EXACT_ALARM`).

#### [MODIFY] `MainActivity.kt`

- Implements the TV-optimized UI using Jetpack Compose.
- Connects permission requests, mapping creation, and schedule configuration.

---

## Verification Plan

### Automated Verification

- Build and compile the APK using `./gradlew assembleDebug` to verify compilation.
- Ensure the APK is lightweight (typically under 3MB).

### Manual Verification

- Deploy to an Android TV emulator/device.
- Test Accessibility Service activation and button remapping.
- Test scheduling volume/brightness change and verify that the trigger runs at the set time.
- Restart the device and confirm that the alarms and Accessibility Service auto-start.
