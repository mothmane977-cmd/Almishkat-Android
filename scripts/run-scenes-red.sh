#!/usr/bin/env bash
# ADM-M1-028 standalone evidence-only runner. Not a required CI gate.
set -uo pipefail
suite="${2:-scenes-red}"
clazz="${1:-com.almishkat.studio.StudioScenesHistoricalRedTest}"
mkdir -p "adm-m1-028-output/$suite"
adb shell getprop ro.build.version.sdk > "adm-m1-028-output/$suite/device-api.txt"
adb shell getprop ro.build.fingerprint > "adm-m1-028-output/$suite/device-fingerprint.txt"
gradle :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class="$clazz" \
  --no-daemon --stacktrace > "adm-m1-028-output/$suite/gradle-connected.log" 2>&1
rc=$?
printf 'SUITE=%s\nCLASS=%s\nGRADLE_EXIT=%s\n' "$suite" "$clazz" "$rc" > "adm-m1-028-output/$suite/status.txt"
adb logcat -d -s ADM_M1_028_DDL:I SQLiteLog:E TestRunner:I '*:S' > "adm-m1-028-output/$suite/logcat.txt" || true
echo "ADM-M1-028 $suite exit=$rc; JUnit XML is required to classify RED vs BLOCKED"
exit "$rc"
