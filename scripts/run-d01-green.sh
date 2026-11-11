#!/usr/bin/env bash
# ADM-M1-018: tests only, no production data. Preserve logs if GREEN fails.
set -uo pipefail
mkdir -p adm-m1-018-output
adb shell getprop ro.build.version.sdk > adm-m1-018-output/device-api.txt
adb shell getprop ro.build.fingerprint > adm-m1-018-output/device-fingerprint.txt
gradle :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.almishkat.studio.D01MigrationInstrumentedTest \
  --no-daemon --stacktrace > adm-m1-018-output/gradle-connected.log 2>&1
status=$?
printf 'GRADLE_EXIT=%s\n' "$status" > adm-m1-018-output/status.txt
adb logcat -d -s ADM_M1_012:I '*:S' > adm-m1-018-output/logcat-d01.txt || true
echo "D01 GREEN exit code: $status; inspect the individual JUnit XML results."
exit "$status"
