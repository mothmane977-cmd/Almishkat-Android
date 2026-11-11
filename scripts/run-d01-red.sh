#!/usr/bin/env bash
# ADM-M1-018 Historical RED, baseline branch only. PASS means RED evidence proven.
set -uo pipefail
mkdir -p adm-m1-012-output
adb shell getprop ro.build.version.sdk > adm-m1-012-output/device-api.txt
adb shell getprop ro.build.fingerprint > adm-m1-012-output/device-fingerprint.txt
gradle :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.almishkat.studio.D01HistoricalRedEvidenceTest \
  --no-daemon --stacktrace > adm-m1-012-output/gradle-connected.log 2>&1
status=$?
printf 'GRADLE_EXIT=%s\n' "$status" > adm-m1-012-output/status.txt
adb logcat -d -s ADM_M1_012:I '*:S' > adm-m1-012-output/logcat-d01.txt || true
echo "Historical RED JUnit exit=$status. PASS confirms the known baseline defect and rollback."
exit "$status"
