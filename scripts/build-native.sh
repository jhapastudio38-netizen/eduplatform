#!/usr/bin/env bash
# Build native Android (.apk / .aab) and iOS (.ipa) binaries from this Next.js app.
#
# Prerequisites:
#   - Node 20+, Bun, and this project's deps installed
#   - For Android:  Android Studio + JDK 17 + Android SDK (compileSdk 34)
#   - For iOS:      macOS 13+ with Xcode 15+ and CocoaPods
#
# Usage:
#   ./scripts/build-native.sh android-debug
#   ./scripts/build-native.sh android-release
#   ./scripts/build-native.sh ios-debug
#   ./scripts/build-native.sh ios-release
#   ./scripts/build-native.sh pwa
#
# Output:
#   - Android:  android/app/build/outputs/apk/{debug,release}/app-{debug,release}.apk
#   - iOS:      ios/App/build/outputs/iphoneos/App.ipa   (release only)
#   - PWA:      ./out/   (static export, deployable to S3/CloudFront)

set -euo pipefail

cd "$(dirname "$0")/.."

MODE="${1:-pwa}"
NATIVE_INSTALLED="$(test -d node_modules/@capacitor/core && echo yes || echo no)"

ensure_capacitor() {
  if [ "$NATIVE_INSTALLED" = "no" ]; then
    echo "→ Installing Capacitor dependencies…"
    bun add -D @capacitor/cli @capacitor/core @capacitor/android @capacitor/ios
    bun add @capacitor/splash-screen @capacitor/local-notifications @capacitor/push-notifications
  fi
}

build_web() {
  echo "→ Building static export of Next.js app…"
  # Next.js static export — produces ./out
  NEXT_EXPORT=1 bun run build
}

case "$MODE" in
  pwa)
    build_web
    echo "✓ PWA export ready in ./out/  — deploy to S3+CloudFront"
    ;;

  android-debug|android-release)
    ensure_capacitor
    if [ ! -d android ]; then
      echo "→ Adding Android platform…"
      bunx cap add android
    fi
    build_web
    bunx cap sync android
    if [ "$MODE" = "android-release" ]; then
      echo "→ Building release .apk / .aab (requires signing config in capacitor.config.json)"
      cd android && ./gradlew assembleRelease bundleRelease && cd ..
      echo "✓ Outputs:"
      echo "  android/app/build/outputs/apk/release/app-release.apk"
      echo "  android/app/build/outputs/bundle/release/app-release.aab"
    else
      cd android && ./gradlew assembleDebug && cd ..
      echo "✓ Output: android/app/build/outputs/apk/debug/app-debug.apk"
    fi
    ;;

  ios-debug|ios-release)
    if [[ "$(uname)" != "Darwin" ]]; then
      echo "✗ iOS builds require macOS + Xcode. Run this on a Mac."
      exit 1
    fi
    ensure_capacitor
    if [ ! -d ios ]; then
      echo "→ Adding iOS platform…"
      bunx cap add ios
    fi
    build_web
    bunx cap sync ios
    if [ "$MODE" = "ios-release" ]; then
      echo "→ Building release .ipa via xcodebuild (requires signing identity + provisioning profile)"
      cd ios/App && xcodebuild -workspace App.xcworkspace -scheme App -configuration Release -archivePath build/App.xcarchive archive
      xcodebuild -exportArchive -archivePath build/App.xcarchive -exportPath build/ -exportOptionsPlist ExportOptions.plist
      cd ../..
      echo "✓ Output: ios/App/build/App.ipa"
    else
      cd ios/App && xcodebuild -workspace App.xcworkspace -scheme App -configuration Debug -destination 'generic/platform=iOS Simulator' build && cd ../..
      echo "✓ Built for iOS Simulator"
    fi
    ;;

  *)
    echo "Usage: $0 {pwa|android-debug|android-release|ios-debug|ios-release}"
    exit 1
    ;;
esac
