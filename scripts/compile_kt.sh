#!/bin/bash
export ANDROID_HOME=/home/z/android-sdk
export ANDROID_SDK_ROOT=/home/z/android-sdk
export JAVA_HOME=/usr

cd /home/z/my-project/student-app-rust/android-wrapper

LOG=/tmp/compile_result.log
RESULT_FILE=/tmp/compile_done

rm -f "$RESULT_FILE"

echo "COMPILE STARTED at $(date)" > "$LOG"
/home/z/.gradle/wrapper/dists/gradle-8.7-bin/gradle-8.7/bin/gradle :app:compileReleaseKotlin --no-daemon >> "$LOG" 2>&1
EXIT_CODE=$?
echo "" >> "$LOG"
echo "COMPILE FINISHED at $(date) with exit code $EXIT_CODE" >> "$LOG"
echo "DONE" > "$RESULT_FILE"
