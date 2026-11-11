#!/usr/bin/env bash
# M1-TST-01: candidate DB-matrix proof. No protected-branch gate activation.
set -uo pipefail
mkdir -p m1-tst-01-output
adb shell getprop ro.build.version.sdk > m1-tst-01-output/device-api.txt
adb shell getprop ro.build.fingerprint > m1-tst-01-output/device-fingerprint.txt
gradle :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.almishkat.studio.V1V10SchemaMatrixTest \
  --no-daemon --stacktrace > m1-tst-01-output/gradle-connected.log 2>&1
rc=$?
printf 'GRADLE_EXIT=%s\n' "$rc" > m1-tst-01-output/status.txt
adb logcat -d -s TestRunner:I SQLiteLog:E '*:S' > m1-tst-01-output/logcat-db-matrix.txt || true
echo "db-tests candidate: gradle_exit=$rc; compare JUnit XML per version"
exit "$rc"
