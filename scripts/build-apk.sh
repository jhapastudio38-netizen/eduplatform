#!/bin/bash
# Build the DreamKorea Android APK
set -e

export ANDROID_HOME=/home/z/android-sdk
export ANDROID_SDK_ROOT=/home/z/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
export GRADLE_OPTS="-Xmx2048m -Dorg.gradle.jvmargs=-Xmx2048m -Dorg.gradle.daemon=false -Dorg.gradle.parallel=true"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

cd /home/z/my-project/student-app-rust/android-wrapper

echo "=== Starting Gradle build ==="
# Use the extracted gradle directly for speed
/home/z/.gradle/wrapper/dists/gradle-8.7-bin/gradle-8.7/bin/gradle assembleDebug \
  --no-daemon \
  --offline 2>/dev/null || \
/home/z/.gradle/wrapper/dists/gradle-8.7-bin/gradle-8.7/bin/gradle assembleDebug \
  --no-daemon

echo "=== Build exit code: $? ==="
echo "=== Build outputs ==="
find /home/z/my-project/student-app-rust/android-wrapper/app/build/outputs/ -name "*.apk" 2>&1 || echo "No APK found"
