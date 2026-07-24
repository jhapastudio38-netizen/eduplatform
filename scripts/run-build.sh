#!/bin/bash
# Build APK script — fully self-contained
export ANDROID_HOME=/home/z/android-sdk
export ANDROID_SDK_ROOT=/home/z/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx2048m"
export PATH="/usr/lib/jvm/java-21-openjdk-amd64/bin:/usr/bin:/bin:$PATH"

LOG=/tmp/build_apk.log
echo "=== Build started at $(date) ===" > $LOG
cd /home/z/my-project/student-app-rust/android-wrapper
echo "Working dir: $(pwd)" >> $LOG
echo "Java: $(which java)" >> $LOG
echo "Gradle: /home/z/.gradle/wrapper/dists/gradle-8.7-bin/gradle-8.7/bin/gradle" >> $LOG

/home/z/.gradle/wrapper/dists/gradle-8.7-bin/gradle-8.7/bin/gradle :app:assembleDebug --no-daemon --console=plain >> $LOG 2>&1
EXIT=$?
echo "=== Build finished at $(date) with exit code $EXIT ===" >> $LOG
echo "EXIT_CODE=$EXIT" > /tmp/build_apk_exit.txt

# Copy APK if built
APK=$(find /home/z/my-project/student-app-rust/android-wrapper/app/build/outputs/ -name "*.apk" 2>/dev/null | head -1)
if [ -n "$APK" ]; then
    cp "$APK" /home/z/my-project/download/DreamKorea-SmartClass-v1.1.0.apk
    echo "APK copied to download folder" >> $LOG
fi
