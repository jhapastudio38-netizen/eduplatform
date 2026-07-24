#!/bin/bash
# Trap all signals and ignore them — keep the build alive
trap '' SIGTERM SIGINT SIGHUP SIGQUIT

export ANDROID_HOME=/home/z/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx1024m"
export PATH="/usr/lib/jvm/java-21-openjdk-amd64/bin:/usr/bin:/bin:$PATH"

LOG=/tmp/gradle-trap.log
echo "=== Build started at $(date) ===" > $LOG

cd /home/z/my-project/student-app-rust/android-wrapper
./gradlew :app:assembleDebug --no-daemon --console=plain >> $LOG 2>&1
EXIT=$?

echo "=== Build finished at $(date) exit=$EXIT ===" >> $LOG

if [ $EXIT -eq 0 ]; then
    APK=$(find app/build/outputs/ -name "*.apk" 2>/dev/null | head -1)
    if [ -n "$APK" ]; then
        cp "$APK" /home/z/my-project/download/DreamKorea-SmartClass-v1.1.0.apk
        echo "SUCCESS" > /tmp/build_result.txt
    else
        echo "NO_APK" > /tmp/build_result.txt
    fi
else
    echo "FAILED:$EXIT" > /tmp/build_result.txt
fi
