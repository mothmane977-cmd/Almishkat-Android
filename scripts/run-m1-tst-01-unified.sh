#!/usr/bin/env bash
# ADM-M1-037: evidence-only isolated instrumentation, not a branch protection gate.
# RED in baseline production MUST be surfaced (not silently treated as PASS).
set -uo pipefail
clazz="${1:?Android JUnit class required}"
suite="${2:?Suite label required}"
target="m1-tst-01-unified-output/$suite"
mkdir -p "$target"
adb shell getprop ro.build.version.sdk > "$target/device-api.txt"
adb shell getprop ro.build.fingerprint > "$target/device-fingerprint.txt"
gradle :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class="$clazz" \
  --no-daemon --stacktrace > "$target/gradle-connected.log" 2>&1
rc=$?
printf 'TEST_CLASS=%s\nSUITE=%s\nGRADLE_EXIT=%s\n' "$clazz" "$suite" "$rc" > "$target/status.txt"
adb logcat -d -s ADM_M1_028_DDL:I TestRunner:I SQLiteLog:E '*:S' > "$target/logcat.txt" || true
echo "M1-TST-01 suite $suite: exit=$rc; inspect JUnit XML for PASS/RED/BLOCKED."
exit "$rc"
