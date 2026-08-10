#!/bin/bash
# Build script that runs detached and saves results
export ANDROID_HOME=/home/z/android-sdk
export ANDROID_SDK_ROOT=/home/z/android-sdk
export JAVA_HOME=/usr

cd /home/z/my-project/student-app-rust/android-wrapper

LOG=/tmp/build_result.log
RESULT_FILE=/tmp/build_done

rm -f "$RESULT_FILE"

echo "BUILD STARTED at $(date)" > "$LOG"
/home/z/.gradle/wrapper/dists/gradle-8.7-bin/gradle-8.7/bin/gradle :app:assembleRelease --no-daemon >> "$LOG" 2>&1
EXIT_CODE=$?
echo "" >> "$LOG"
echo "BUILD FINISHED at $(date) with exit code $EXIT_CODE" >> "$LOG"
echo "DONE" > "$RESULT_FILE"
