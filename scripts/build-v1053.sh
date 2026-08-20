#!/bin/bash
# Build DreamKorea v10.53.0 — duplicate Batch/QBank cards removed + scrollable
# block grid for >40 questions + unified BatchManager entry on home.
#
# Strategy:
#   1. Build :app:assembleDebug (faster than release, signed with debug keystore)
#   2. zipalign + apksigner (to produce a clean installable APK)
#   3. Copy to /home/z/my-project/download/DreamKorea-SmartClass-v10.53.0.apk
#
# Signal-proof: traps SIGTERM/SIGINT/SIGHUP so the build survives session
# disconnects.
trap '' SIGTERM SIGINT SIGHUP SIGQUIT

export ANDROID_HOME=/home/z/android-sdk
export ANDROID_SDK_ROOT=/home/z/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx2048m"
export PATH="/usr/lib/jvm/java-21-openjdk-amd64/bin:/usr/bin:/bin:$PATH"

LOG=/tmp/build_v1053.log
RESULT=/tmp/build_v1053_result.txt
echo "=== Build started at $(date) ===" > $LOG

cd /home/z/my-project/student-app-rust/android-wrapper
echo "Working dir: $(pwd)" >> $LOG
echo "Git status:" >> $LOG
git status -s 2>&1 | head -10 >> $LOG

# Use the extracted gradle for speed (no wrapper download)
GRADLE_BIN=/home/z/.gradle/wrapper/dists/gradle-8.7-bin/gradle-8.7/bin/gradle

echo "=== Running assembleDebug ===" >> $LOG
$GRADLE_BIN :app:assembleDebug --no-daemon --console=plain >> $LOG 2>&1
EXIT=$?
echo "=== assembleDebug exit=$EXIT at $(date) ===" >> $LOG

if [ $EXIT -ne 0 ]; then
    echo "FAILED:$EXIT" > $RESULT
    tail -50 $LOG
    exit $EXIT
fi

# Find the built APK
APK_SRC=$(find /home/z/my-project/student-app-rust/android-wrapper/app/build/outputs/apk/debug -name "*.apk" 2>/dev/null | head -1)
echo "APK source: $APK_SRC" >> $LOG

if [ -z "$APK_SRC" ]; then
    echo "NO_APK_FOUND" > $RESULT
    tail -50 $LOG
    exit 1
fi

# Align + sign with debug keystore (so it installs cleanly on devices)
KEYSTORE=/home/z/.android/debug.keystore
ALIGN_APK=/tmp/DreamKorea-v10.53.0-aligned.apk
FINAL_APK=/home/z/my-project/download/DreamKorea-SmartClass-v10.53.0.apk

echo "=== zipalign ===" >> $LOG
ZIPALIGN=$(find /home/z/android-sdk/build-tools -name zipalign 2>/dev/null | sort -V | tail -1)
echo "zipalign: $ZIPALIGN" >> $LOG
if [ -n "$ZIPALIGN" ]; then
    "$ZIPALIGN" -f 4 "$APK_SRC" "$ALIGN_APK" >> $LOG 2>&1
    echo "zipalign exit=$?" >> $LOG
else
    cp "$APK_SRC" "$ALIGN_APK"
    echo "zipalign not found — using unsigned copy" >> $LOG
fi

echo "=== apksigner ===" >> $LOG
APKSIGNER=$(find /home/z/android-sdk/build-tools -name apksigner 2>/dev/null | sort -V | tail -1)
echo "apksigner: $APKSIGNER" >> $LOG
if [ -n "$APKSIGNER" ]; then
    "$APKSIGNER" sign --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android \
        --out "$FINAL_APK" "$ALIGN_APK" >> $LOG 2>&1
    echo "apksigner exit=$?" >> $LOG
else
    cp "$ALIGN_APK" "$FINAL_APK"
    echo "apksigner not found — using aligned copy" >> $LOG
fi

ls -la "$FINAL_APK" >> $LOG
echo "=== Build complete at $(date) ===" >> $LOG
echo "SUCCESS:$FINAL_APK" > $RESULT
