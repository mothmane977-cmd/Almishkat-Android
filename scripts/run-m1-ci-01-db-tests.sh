#!/usr/bin/env bash
# ADM-M1-039 / M1-CI-01 TRIAL ONLY.
# Required acceptance suites, NOT Historical RED evidence.
set -uo pipefail
out="m1-ci-01-output"
mkdir -p "$out"
adb shell getprop ro.build.version.sdk > "$out/device-api.txt"
adb shell getprop ro.build.fingerprint > "$out/device-fingerprint.txt"

# Verify exact M1-TST-01 fixture path; these are not authenticated old databases.
for v in {1..10}; do
  test -s "app/src/androidTest/assets/db/v$v.sql" || {
    echo "MISSING_FIXTURE_v$v" >&2
    exit 2
  }
  if [ "$v" -lt 10 ]; then
    grep -q "RECONSTRUCTED" "app/src/androidTest/assets/db/v$v.sql" || exit 3
  else
    grep -q "DERIVED FROM CURRENT" "app/src/androidTest/assets/db/v$v.sql" || exit 3
  fi
done

# Strict acceptance suites; RED historical classes are excluded.
classes="com.almishkat.studio.FixtureLoaderVersionTest,com.almishkat.studio.V1V10SchemaMatrixTest,com.almishkat.studio.D01MigrationInstrumentedTest"
start_epoch=$(date +%s)
start_iso=$(date -u +%Y-%m-%dT%H:%M:%SZ)
gradle :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class="$classes" \
  --no-daemon --stacktrace > "$out/gradle-connected.log" 2>&1
gradle_exit=$?
end_epoch=$(date +%s)
end_iso=$(date -u +%Y-%m-%dT%H:%M:%SZ)
printf 'SUITE=required-db-acceptance\nCLASS_FILTER=%s\nSTART_UTC=%s\nEND_UTC=%s\nELAPSED_SECONDS=%s\nGRADLE_EXIT=%s\n' \
  "$classes" "$start_iso" "$end_iso" "$((end_epoch-start_epoch))" "$gradle_exit" > "$out/status.txt"
adb logcat -d -s ADM_M1_028_DDL:I TestRunner:I SQLiteLog:E '*:S' > "$out/logcat-db-tests.txt" || true

# An exit=0 without real JUnit / a required test class is NOT a success.
python3 - "$out/junit-summary.json" <<'PY'
import glob,json,sys,xml.etree.ElementTree as ET
paths=sorted(glob.glob("app/build/outputs/androidTest-results/**/*.xml",recursive=True))
classes={"FixtureLoaderVersionTest","V1V10SchemaMatrixTest","D01MigrationInstrumentedTest"}
tests=[]
errors=[]
for path in paths:
    try:
        root=ET.parse(path).getroot()
        for t in root.iter("testcase"):
            name=t.get("name","")
            klass=t.get("classname","").rsplit(".",1)[-1]
            state=("FAIL" if t.find("failure") is not None else
                   "ERROR" if t.find("error") is not None else
                   "SKIP" if t.find("skipped") is not None else "PASS")
            tests.append({"class":klass,"test":name,"status":state,"xml":path})
    except Exception as e: errors.append({"file":path,"error":str(e)})
observed={t["class"] for t in tests}
missing=sorted(classes-observed)
counts={s:sum(x["status"]==s for x in tests) for s in ["PASS","FAIL","ERROR","SKIP"]}
ok=(not errors and not missing and bool(tests) and not counts["FAIL"]
    and not counts["ERROR"] and not counts["SKIP"])
result={"files":paths,"cases":tests,"counts":counts,"missing_classes":missing,"xml_errors":errors,"junit_gate_pass":ok}
with open(sys.argv[1],"w") as f:json.dump(result,f,indent=2,ensure_ascii=False)
print("db-tests JUnit:",json.dumps({"cases":len(tests),"counts":counts,
    "missing_classes":missing,"errors":errors,"gate_pass":ok}))
sys.exit(0 if ok else 1)
PY
junit_exit=$?
printf 'JUNIT_VALIDATION_EXIT=%s\n' "$junit_exit" >> "$out/status.txt"
echo "db-tests gradle_exit=$gradle_exit junit_validation_exit=$junit_exit"
if [ "$gradle_exit" -ne 0 ]; then exit "$gradle_exit"; fi
exit "$junit_exit"
