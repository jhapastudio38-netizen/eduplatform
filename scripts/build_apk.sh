#!/bin/bash
# Build the DreamKorea SmartClass APK with bigger fonts/images
set -e

export ANDROID_HOME=/home/z/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
cd /home/z/my-project/student-app-rust/android-wrapper

echo "=== Building APK v10.3.5 (versionCode 168) ==="
echo "Changes: bigger question text (17sp), bigger question images (260-280dp), bigger option text (16sp), bigger option images (96dp), bigger option number circles"

chmod +x gradlew
./gradlew assembleDebug --no-daemon 2>&1 | tail -30

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    DEST="/home/z/my-project/download/DreamKorea-SmartClass-v10.3.5.apk"
    mkdir -p /home/z/my-project/download
    cp "$APK_PATH" "$DEST"
    echo "=== APK BUILT SUCCESSFULLY ==="
    echo "Path: $DEST"
    ls -la "$DEST"
else
    echo "=== BUILD FAILED — APK not found ==="
    exit 1
fi
