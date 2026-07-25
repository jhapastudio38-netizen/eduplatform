#!/bin/bash
trap '' SIGTERM SIGINT SIGHUP SIGQUIT

export ANDROID_HOME=/home/z/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx1024m"
export PATH="/usr/lib/jvm/java-21-openjdk-amd64/bin:/usr/bin:/bin:$PATH"

LOG=/tmp/gradle-v120.log
RESULT=/tmp/build_v120_result.txt
APK_OUT=/home/z/my-project/download/DreamKorea-SmartClass-v1.3.1.apk
PROJECT_DIR=/home/z/my-project/student-app-rust/android-wrapper

echo "=== Build started at $(date) ===" > $LOG
echo "Working dir: $PROJECT_DIR" >> $LOG

cd "$PROJECT_DIR" || { echo "FAILED:cd" > $RESULT; exit 1; }
bash ./gradlew :app:assembleDebug --no-daemon --console=plain --max-workers=1 >> $LOG 2>&1
EXIT=$?

echo "=== Build finished at $(date) exit=$EXIT ===" >> $LOG

if [ $EXIT -eq 0 ]; then
    APK=$(find "$PROJECT_DIR/app/build/outputs/" -name "*.apk" 2>/dev/null | head -1)
    if [ -n "$APK" ]; then
        cp "$APK" "$APK_OUT"
        echo "SUCCESS" > $RESULT
    else
        echo "NO_APK" > $RESULT
    fi
else
    echo "FAILED:$EXIT" > $RESULT
fi
