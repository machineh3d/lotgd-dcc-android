# LotGD-DCC Native Android Client (`lotgd-dcc-android`)

A native **Kotlin + Jetpack Compose** Android client built specifically for **Dungeon Crawler World: Earth (`lotgd-dcc`)** running on [NB-Core LotGD](https://github.com/NB-Core/lotgd).

## Features
- **Configurable Server URL:** Connect to your Unraid server (HTTPS domain or local LAN IP) and automatically remember your server and PHP session across restarts.
- **Pinned Syndicate Top HUD:** Always-visible Floor/Level badge, HP bar, Stamina (Turns), Gold Credits, Sponsor Tokens, and Syndicate Comms unread mail counter.
- **Context-Aware Action Deck:** Thumb-friendly **Attack / Specialties / Flee** controls during combat, categorized action cards in the Safe Room Hub, and one-tap `badnav.php` recovery.
- **Native LotGD Color & Form Rendering:** Renders LotGD color spans, System AI achievement/loot callouts, and interactive server forms (shops, Bopca quests, registration, feedback) natively in Material 3.
- **Automatic APK Builds:** GitHub Actions compiles a downloadable `lotgd-dcc-android-debug.apk` on every push to `main` and publishes a GitHub Release on `v*` tags.

## Step 1: Install the Server JSON Bridge on Your `lotgd-dcc` Container
Extract `lotgd-dcc-server-bridge.zip` onto your Unraid server and run:
```bash
chmod +x install-bridge.sh
./install-bridge.sh dcc-web-prod
```
*(Regular web browsers remain 100% untouched; JSON is returned only when `X-DCC-Client: 1` is sent.)*

## Step 2: Push to a New GitHub Repo & Auto-Build Your APK (Windows PowerShell)
1. Create an empty repository named **`lotgd-dcc-android`** on [GitHub](https://github.com/new).
2. Extract `lotgd-dcc-android-v0.1.0-alpha.zip` and push:
```powershell
mkdir C:\git\lotgd-dcc-android
Expand-Archive -Path "$env:USERPROFILE\Downloads\lotgd-dcc-android-v0.1.0-alpha.zip" -DestinationPath C:\git\lotgd-dcc-android-tmp -Force
robocopy C:\git\lotgd-dcc-android-tmp\lotgd-dcc-android C:\git\lotgd-dcc-android /E /XD .git | Out-Null
Remove-Item C:\git\lotgd-dcc-android-tmp -Recurse -Force

cd C:\git\lotgd-dcc-android
git init
git branch -M main
git add .
git commit -m "Initial v0.1.0-alpha: Native Kotlin Compose client for lotgd-dcc"
git remote add origin https://github.com/<your-github-username>/lotgd-dcc-android.git
git push -u origin main
git tag -a v0.1.0-alpha -m "Release v0.1.0-alpha"
git push origin v0.1.0-alpha
```
3. Open the **Actions** (or **Releases**) tab on GitHub and download **`lotgd-dcc-android-debug.apk`** straight to your Android phone!

## Optional: Local Development in Android Studio
Open `C:\git\lotgd-dcc-android` in Android Studio (Ladybug or newer, JDK 17) — Gradle 8.9 will sync automatically and let you run on an emulator or USB-connected Android device.
