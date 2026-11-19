# ТАБР — Black & Gold Edition
Android Kotlin/Jetpack Compose prototype. Source lives in the TabrAndroid subfolder on the tabr-android branch to avoid modifying the existing Domino app.

Implemented: 52-card deck, 2/4-player local turn-taking, six-card batches, four initial table cards, independent sum captures, round scoring, 31/51 targets, Tabr bonus capped at 21/33, clockwise rotating starter, and a manual missed-capture engine API.

Not yet implemented: computer AI, private online rooms, full UI for multi-group captures and missed claims, tested APK. The UI is a local prototype requiring manual turn-taking.
Build: from TabrAndroid run gradle :app:assembleDebug using Gradle 8.9, JDK 17, Android SDK 35.
