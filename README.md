# Android WebView starter

A minimal Android Studio project that wraps a website you own or are authorized to distribute.

## Configure
1. Open `app/src/main/java/com/example/webviewstarter/MainActivity.java`.
2. Replace `https://example.com` in `START_URL` with your authorized website URL.
3. Change the app label in `app/src/main/AndroidManifest.xml`.
4. Optionally change `applicationId` and `namespace` in `app/build.gradle`.

## Build with GitHub Actions
1. Create a new GitHub repository.
2. Upload the contents of this ZIP (including the hidden `.github` folder).
3. Push to the `main` branch, or open **Actions → Build Android APK → Run workflow**.
4. Open the completed workflow run and download the `android-debug-apk` artifact.

## Notes
- This is a basic WebView wrapper, not a copy of the website's backend.
- The website must permit embedding in a WebView and may block it with security headers.
- This starter intentionally uses `example.com`; only configure a site you own or have permission to package.
