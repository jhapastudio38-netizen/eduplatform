#!/bin/bash
# Build APK — fully detached, writes progress to a status file
STATUS=/tmp/build_status.txt
LOG=/tmp/build_full.log

echo "RUNNING" > $STATUS
echo "=== Build started at $(date) ===" > $LOG

export ANDROID_HOME=/home/z/android-sdk
export ANDROID_SDK_ROOT=/home/z/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx2048m"
export PATH="/usr/lib/jvm/java-21-openjdk-amd64/bin:/usr/bin:/bin:$PATH"

cd /home/z/my-project/student-app-rust/android-wrapper >> $LOG 2>&1

# Run the build
/home/z/.gradle/wrapper/dists/gradle-8.7-bin/gradle-8.7/bin/gradle :app:assembleDebug --no-daemon --console=plain >> $LOG 2>&1
EXIT=$?

if [ $EXIT -eq 0 ]; then
    APK=$(find /home/z/my-project/student-app-rust/android-wrapper/app/build/outputs/ -name "*.apk" 2>/dev/null | head -1)
    if [ -n "$APK" ]; then
        cp "$APK" /home/z/my-project/download/DreamKorea-SmartClass-v1.1.0.apk
        echo "SUCCESS: APK at /home/z/my-project/download/DreamKorea-SmartClass-v1.1.0.apk" > $STATUS
    else
        echo "FAILED: No APK produced despite exit 0" > $STATUS
    fi
else
    echo "FAILED: Gradle exit code $EXIT" > $STATUS
fi

echo "=== Build finished at $(date) ===" >> $LOG
