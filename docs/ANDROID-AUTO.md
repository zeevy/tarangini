# Android Auto: how to test

Tarangini shows in Android Auto as a media app. The car draws the screens; the app gives it the session, the lists and the buttons. This page says how to see the app in a car screen on a desk, with the Desktop Head Unit (DHU) of Google.

The official pages are [Test Android apps for cars](https://developer.android.com/training/cars/testing) and [Desktop Head Unit](https://developer.android.com/training/cars/testing/dhu). Read them when a step here no longer fits.

## What you need

- A computer with a screen, Android Studio and `adb`
- An Android phone with the Android Auto app and USB debugging on
- A debug build of Tarangini on the phone: `./gradlew :app:installCoreDebug`

## One time setup

1. In Android Studio open the SDK Manager, the tab "SDK Tools", and install "Android Auto Desktop Head Unit Emulator". It lands in `extras/google/auto` of the Android SDK folder.
2. On the phone open Settings, search for "Android Auto" and open its settings page.
3. Scroll to "Version" and tap it ten times, then accept the developer mode.
4. Open the menu with the three dots and pick "Developer settings". Switch on "Unknown sources". Without it Android Auto hides every app that did not come from the Play Store, and a debug build of Tarangini does not show.

## Start the head unit

1. On the phone, in the Android Auto settings, open the three dot menu and pick "Start head unit server".
2. On the computer forward the port: `adb forward tcp:5277 tcp:5277`
3. Start the head unit from the `extras/google/auto` folder: `./desktop-head-unit` (on Windows `desktop-head-unit.exe`).
4. The car screen opens in a window. Open the app list there and pick Tarangini.

When the window stays black, stop the head unit server on the phone, start it again and repeat step 2.

## What to check

The checks are rows 65 to 82 of [MANUAL-TESTS.md](MANUAL-TESTS.md). Run them after every change to the player, the session or the browse tree, and write the run in the results table there.

## Check that other apps are refused

The [Media Controller Test](https://github.com/googlesamples/android-media-controller) app of Google connects to a media app the way a car does. Tarangini refuses it, because it is not one of the callers the app knows (see [ADR 0025](adr/0025-android-auto-setup.md)). The test app must list Tarangini and fail to connect. This needs no head unit.
