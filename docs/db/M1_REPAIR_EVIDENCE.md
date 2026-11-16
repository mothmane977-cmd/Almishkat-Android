# إصلاحات قاعدة البيانات M1 | M1 database repairs

> نتائج الجولة الأولى أدناه تخص الالتزام `1a807e1489f6f4790db984340b14d666a4f8a8fd`. جولة التحقق الموسعة موثقة في القسم الأخير. / Initial results below apply to `1a807e1`; expanded validation is recorded in the final section.

## النطاق والمصدر | Scope and source

- المرحلة: **M1 — Database Repairs & Regression Tests**.
- المهام المرتبطة: **M1-FIX-01 / D01**، **M1-FIX-02 / D02**، واختبارات محدودة تخص **M1-TST-01** و**M1-CI-01** ([Issue #17](https://github.com/mothmane977-cmd/Almishkat-Android/issues/17)). إصلاح جدول المشاهد المفقود موثق هنا دون اختراع رقم مهمة مستقل.
- الأساس: `develop` عند `22dac68190250f093bcca044509fbfa1e32a8327`.
- الفرع: `fix/M1-db-upgrade-background-regressions`، بطلب صاحب المشروع إصلاح نتائج الفحص. لا يتضمن ذلك إذن دمج.

Related work: M1-FIX-01/D01, M1-FIX-02/D02, bounded M1-TST-01 and M1-CI-01 coverage. Missing-scene recovery has no newly invented task ID. This branch starts at the exact `develop` SHA above; owner authorization covers preparing fixes, not merging them.

## التغييرات | Changes

1. فحص الأعمدة الموجودة قبل إضافة روابط المقاطع الصوتية؛ يظل ترحيل v4 يضيف الأعمدة الناقصة، ولا يعيد إضافة الأعمدة المنشأة بالفعل لمسارات v1–v3.
2. رفع إصدار SQLite إلى **11** وإنشاء `studio_scenes` وفهرسه إذا كانا مفقودين. يشمل ذلك قواعد v10 المثبتة التي كانت لن تستدعي `onUpgrade` دون رفع الإصدار. لا يُعاد إنشاء الجداول الموجودة ولا تُحذف سجلاتها.
3. حفظ الخلفية داخل transaction: اختيار أحدث سجل للمفتاح المنطقي وتحديثه، أو إدراج سجل عند غيابه. مفتاح الخلفية العامة يستخدم `scene_id IS NULL`. تبقى السجلات المكررة القديمة محفوظة دون تحديثها أو حذفها؛ توقف هذه الدالة إنشاء مكررات جديدة. تختار القراءة `updated_at DESC, rowid DESC`، ويحافظ الحفظ على ترتيب السجل حتى بعد تصحيح ساعة الجهاز.
4. اختبارات JVM باستخدام Robolectric وSQLite الأصلي على **API 26 وAPI 35**، وتشغيلها في Workflow البناء الحالي قبل إنشاء APK مع إرفاق تقارير JUnit. اسم الفحص الإلزامي الحالي محفوظ؛ لم تُغيَّر قواعد الحماية ولم يُنشأ ادعاء باكتمال بوابة `db-tests` المستقلة.

The audio migration adds only absent link columns. SQLite v11 additively repairs missing scene tables/indexes, including installed v10 databases. Background saves serialize lookup/update/insert and update only the newest matching row; older duplicates remain intact for a separately reviewed cleanup. Reads resolve duplicates deterministically, including timestamp ties and clock corrections. The existing build workflow now runs native-SQLite Robolectric tests before building the APK and publishes JUnit reports; branch rules remain unchanged.

## مصدر ملفات الاختبار | Fixture provenance

نُسخت الملفات التالية دون تغيير من الالتزام `5ffb077c02d66dcf7388218f8902a890856cd390` الخاص بـ[PR #18](https://github.com/mothmane977-cmd/Almishkat-Android/pull/18)، من `app/src/androidTest/assets/db/` إلى `app/src/test/resources/db/`:

| الملف / File | Git blob SHA-1 | الحالة / Status |
| --- | --- | --- |
| `v1.sql` | `93bc560cca6e091ce9263c137caf0bc5d60fbdeb` | RECONSTRUCTED |
| `v2.sql` | `b85097208ed0d24a69b55c02afb16912f0a23427` | RECONSTRUCTED |
| `v3.sql` | `9ad8e237290da0005ce7985273fffb8867eb5dba` | RECONSTRUCTED |
| `v4.sql` | `5e03abcd282b24753f878238296b7113d298e48e` | RECONSTRUCTED |
| `v5.sql` | `7163c3b95e169a178fee777e12d2aa86d75ddf5b` | RECONSTRUCTED |
| `v6.sql` | `ff50c97a27cb52411c171acb9fedb18277a09f9a` | RECONSTRUCTED |
| `v7.sql` | `bf9efcd23b3f9db5cd643ed022887e277d0509af` | RECONSTRUCTED |
| `v8.sql` | `4b1096ac5e538a84e41ae3f5d3192a27baa2ad08` | RECONSTRUCTED |
| `v9.sql` | `5bd2ea4898f583585d8cc1d4bee464add2560820` | RECONSTRUCTED |
| `v10.sql` | `b58dd442899c2ad1be27b2a2c65d3afb21444189` | DERIVED FROM CURRENT onCreate |

هذه بصمات ملفات اختبار، وليست بصمات قواعد مستخدمين أصلية. لم يُسترجع DDL أصلي لـv1–v9. سيناريو v10 المفقود منه جدول المشاهد يُحاكي عيب الترقية المؤكد، وليس قاعدة مسترجعة من جهاز. جميع القيم المزروعة اصطناعية `TEST_ONLY`؛ لا تتضمن نصوصًا قرآنية معتمدة.

These unchanged test fixtures retain PR #18 provenance labels. v1–v9 are reconstructed scenarios; v10 derives from the baseline source, not an authenticated device snapshot. Removing the scene table simulates the observed faulty upgrade state. Test hashes are not historical database hashes. No religious-content validation is claimed.

## التحقق | Verification

الأوامر القابلة لإعادة التشغيل بعد تثبيت JDK 17 وGradle 8.11.1 وAndroid SDK 36:

```sh
git clone --branch fix/M1-db-upgrade-background-regressions https://github.com/mothmane977-cmd/Almishkat-Android.git
cd Almishkat-Android
gradle --no-daemon --stacktrace :app:testDebugUnitTest :app:assembleDebug
```

The command above requires JDK 17, Gradle 8.11.1 and Android SDK 36. Gradle supplies its required Build Tools; no Gradle Wrapper was introduced. Local verification uses the same Gradle tasks through a workspace-only launcher for the managed network proxy and CA trust.

| التشغيل / Run | النتيجة / Result |
| --- | --- |
| Baseline `:app:testDebugUnitTest` before production patch | **FAIL (RED)**: 28 tests, 26 failed, 0 errors, 0 skipped; exit 1 |
| Fixed `:app:testDebugUnitTest :app:assembleDebug` | **PASS**: 28/28 tests, APK generated; exit 0 |
| Fixed `:app:test` (full unit suite) | **PASS**: Debug 28/28 + Release 28/28; 0 failures/errors/skips; exit 0 |
| `git diff --check` | **PASS** |
| Device/emulator instrumentation, API 36, authentic v1–v9 databases | **NOT RUN** |

نسخة APK المحلية التجريبية / Local debug APK: `app/build/outputs/apk/debug/app-debug.apk`, **17,806,756 bytes**. SHA-256: `da9507263a3033bfd213717430b4561bef529116e81569c5acbd3412d1618dae`.

حالات RED المؤكدة / Observed baseline failures include: `duplicate column name: project_id`, `no such table: studio_scenes`, duplicate background count growth, stale background reads, and failure to clear saved media. The rollback test also fails on baseline because REPLACE bypasses the intended update path. Two background isolation cases already passed; they are regression guards, not newly fixed behavior.

السجلات الكاملة محفوظة في مساحة العمل / Full local logs and JUnit files retained in the task workspace (not committed):

| الملف / Workspace file | SHA-256 |
| --- | --- |
| `/workspace/scratch/m1-red.log` | `547b19f59119222631c01b4c08c5df6728ec3a34fce5a3f00d3de0baf2f42408` |
| `/workspace/scratch/m1-red.xml` | `3ebb2999fb386d60c1b07255fc2f05c11ff5fe4d910511b18bce1aab0dacbcad` |
| `/workspace/scratch/m1-green.log` | `1ba8c284c0ebad791bef0935123e6f96a2fa4896612eaa440530bcba62651e92` |
| `/workspace/scratch/m1-green.xml` | `093a59b725c407e5fa0ded2fefb54001d1bff4276a3ff348596c89849969d0d8` |
| `/workspace/scratch/m1-full-tests.log` | `94b2f1922495bc12f396334cff65f9707febcfdb6adbc4cd7ac2130110ca8383` |

نتيجة GitHub CI تُراجع على SHA طلب الدمج نفسه، ولا تُستبدل بنتائج محلية. الاختبارات الحالية ليست شهادة إصدار أو قبول جميع الترقيات التاريخية. ظهرت تحذيرات Kotlin/Compose قديمة أثناء البناء، دون فشل البناء.

GitHub CI must pass on the PR head itself. Local results do not replace the required check. Existing Kotlin/Compose warnings remain; neither successful compilation nor this test suite establishes production-release readiness.


## الحدود والعمل المتبقي | Limits and remaining work

- هذه إصلاحات محدودة؛ لا تغلق M1 أو Issue #17 ولا تثبت قبول DB-001…DB-015 كاملًا.
- لم تُختبر قواعد مستخدمين أصلية. تشمل الجولة الموسعة سيناريوهات معاد بناؤها لـv1–v9، ويلزم استرجاع المخططات الأصلية قبل اعتماد توافق تاريخي شامل.
- نتائج Robolectric منفصلة عن نتائج المحاكيات؛ حالة API 26/36 الفعلية موثقة في الجولة الموسعة أدناه.
- لا يوجد قيد UNIQUE جديد للخلفيات العامة؛ تبقى المكررات القديمة، وأي SQL مباشر خارج دالة الحفظ يمكنه إنشاء مكررات. تحتاج معالجة هذه السجلات وخطة القيد إلى مراجعة تحفظ جميع البيانات.
- أي دمج يحتاج مراجعة التغييرات والفحوص الإلزامية وموافقة المالك وفق سياسة المستودع. PR #18 و#19 و#22 لم تُدمج ولم تُعدَّل بهذا العمل.

This bounded repair does not complete M1, Issue #17, or the full DB-001…DB-015 matrix. Authentic historical databases remain unverified; expanded tests cover reconstructed v1–v9 scenarios. Native-SQLite Robolectric API26/35 coverage is separate from the API26/36 emulator results recorded below. No new uniqueness constraint or destructive cleanup was introduced; direct SQL can still bypass the application save path. Existing PRs remain untouched, and merging requires the repository's normal review and approval gates.

## جولة التحقق الموسعة | Expanded validation

طلب المالك متابعة العمل بعد الجولة الأولى. جُمعت التأكيدات في `DatabaseRegressionContract` واستخدمتها واجهتا تشغيل: Robolectric على API 26/35 وAndroidJUnit4 على API 26/36. تستخدم اختبارات Android ملفات SQLite معزولة داخل مجلد cache فريد؛ لا تفتح قاعدة التطبيق الحقيقية ولا تحذفها. لا تغيير إضافي في كود الإنتاج منذ `1a807e1`.

The owner's continuation request authorizes this M1 verification round. Both runners share the same **19 test methods**, including eleven upgrade/recovery cases and eight background cases. Device tests redirect every helper database-open path to a unique test-cache file. Production code is unchanged from `1a807e1`.

- سيناريوهات v1–v10، والتحقق من إصدار الإدخال، وحفظ القيم القديمة، وإعادة الفتح مرتين، و`integrity_check` و`foreign_key_check`.
- مقارنة بنية الترقية بتثبيت جديد: أسماء الأعمدة وأنواعها وNULL/default/PK، وخصائص الفهارس وترتيب أعمدتها. تتجاهل المقارنة ترتيب الأعمدة الفيزيائي؛ لا تثبت تكافؤ CHECK أو triggers أو collations أو كل دلالات DDL الخام.
- نفس ملفات SQL العشرة تُحمَّل كموارد JVM وassets لاختبارات Android، دون نسخ مختلفة قابلة للتباعد.
- مسار GitHub مؤقت ومحـدود بفرع الإصلاح؛ لا يؤسس بنية M2 دائمة أو يغيّر Required Checks. تُجمع تقارير JUnit وlogcat وخصائص الجهاز، وتفشل الجولة عند غياب التقارير أو وجود skip أو عدد حالات غير 19 لكل API.

The upgrade cases preserve seeded values and compare column/index metadata against a fresh installation, then verify integrity and two reopens. This excludes physical column order and does not establish CHECK/trigger/collation/raw-DDL equivalence. Both runners consume the same fixture files. The existing build job compiles the instrumentation APK alongside the unit suite. A temporary branch-scoped GitHub workflow provides bounded M1 device evidence, with exact test counts and fail-closed report checks; this is not a new permanent M2 gate.

### النتائج الموسعة | Expanded results

| الجولة / Round | النتيجة / Result |
| --- | --- |
| Expanded `:app:test` | **PASS**, Debug 38/38 + Release 38/38, 0 failures/errors/skips, exit 0 |
| `:app:assembleDebug :app:assembleDebugAndroidTest` | **PASS**, exit 0; both APKs generated |
| Expanded baseline RED replay at `22dac681` | **FAIL (RED)**: 38 tests, 36 failures, 0 errors/skips; exit 1. Fixed source restored automatically. |
| API26 and API36 emulator runs | **PASS**, 19/19 each; no failures/errors/skips. See SHA-bound evidence below. |

Local command: `gradle --no-daemon :app:test :app:assembleDebug :app:assembleDebugAndroidTest`. Full log retained as `/workspace/scratch/m1-expanded-green.log` (not committed). The debug app APK remains byte-identical to the first round (`da950726…18dae`); test-only source changes do not modify production output. Instrumentation APK: 388,130 bytes, SHA-256 `166c5be6540bf880102afc524da6af15581e096a0ea2bd9e440034f526f66032`.

هذه نتائج بناء واختبارات محلية، وليست بعدُ نتائج أجهزة. / These are local unit/build results, not device execution results.

تشخيص إعداد المحاكي / Emulator setup diagnosis: [run 43177285232](https://github.com/mothmane977-cmd/Almishkat-Android/actions/runs/43177285232) stopped before Android execution at an immediate KVM permission check. The retained artifact reports `STATUS=NOT_RUN` and later `crw-rw-rw-` permissions on `/dev/kvm`, identifying an asynchronous udev rule application race. Commit `8813fa51c8aeeba6b1278f9e85173f5681e55f6a` waits for `udevadm settle --timeout=30` before checking access. This is infrastructure failure evidence, not a failed application assertion or device PASS.

تحقق منفصل من فشل مُحلل JUnit / Report-parser failure verification: the actual embedded workflow Python was executed with controlled XML. Missing reports, 18 cases, a skipped case, a failed case, or an error all returned exit 1; exactly 19 clean cases returned exit 0. No partial suite or skipped test is accepted as PASS.

السجلات المحلية الموسعة / Retained expanded local logs:

| ملف / File | SHA-256 |
| --- | --- |
| `/workspace/scratch/m1-expanded-red.log` | `83554afb91adf7c3e3678066b450e047edb18210b6e153e27db20e5a24234598` |
| `/workspace/scratch/m1-expanded-red.xml` | `cecbb1b8e6c63deff86b02a2a20da92d3d5c405956c7789489b24ffc14b03825` |
| `/workspace/scratch/m1-expanded-final-green.log` | `1454b6df6f10f2ef880e7ed43e18481b14ddd4bd02bc8b8da86b34c23427fce9` |

محاولة التشغيل الثانية / Second device attempt: [run 43177719490](https://github.com/mothmane977-cmd/Almishkat-Android/actions/runs/43177719490) on `8813fa51` passed KVM and booted API26, but the API26 image rejected `adb logcat -c` before Gradle ran. Log clearing was changed to best-effort diagnostics only; boot/API checks, Gradle exit status and exact 19-case JUnit acceptance remain strict. No API26 test PASS is inferred from this failed setup attempt.

API36 evidence from [job 131045909785](https://github.com/mothmane977-cmd/Almishkat-Android/actions/runs/43177719490/job/131045909785) at `8813fa51`: emulator boot recorded, Gradle `BUILD SUCCESSFUL`, **19 tests / 0 failures / 0 errors / 0 skips**. The run as a whole failed because its API26 lane stopped at log clearing, so this evidence is attributed only to API36. Both lanes are repeated at the diagnostic-only fix before final acceptance.

### النتيجة النهائية للمحاكيات | Final emulator result

[Run 43178361895](https://github.com/mothmane977-cmd/Almishkat-Android/actions/runs/43178361895) at **`16be26b27d19b36d0cc2fe836933857a9afbc1a6`**: both lanes PASS. Logs confirm emulator boot, Gradle exit zero and exact 19-case clean JUnit totals. API26 reports Android 8.0.0; API36 reports Android 16. Both operate on synthetic labelled fixtures, not authentic user databases.

نجحت الجولتان على SHA نفسه: **19/19 لكل API** دون فشل أو خطأ أو تخطٍّ. تؤكد السجلات إقلاع المحاكي ونجاح Gradle وJUnit. هذه اختبارات Android فعلية على محاكيات، وتظل بيانات السيناريوهات معاد بنائها وليست قواعد مستخدمين أصلية.

| الجهاز / Device | النتيجة / Result | تقارير JUnit والجهاز / Reports | SHA-256 of artifact ZIP |
| --- | --- | --- | --- |
| API 36 | **PASS: 19/19**, 0 failures/errors/skips | [artifact 13925192131](https://github.com/mothmane977-cmd/Almishkat-Android/actions/runs/43178361895/artifacts/13925192131) | `sha256:54a2b42123c3f05c8b7ee1e3026b127cac46372b65c03c6903e8bfb9ee238212` |
| API 26 | **PASS: 19/19**, 0 failures/errors/skips | [artifact 13924867955](https://github.com/mothmane977-cmd/Almishkat-Android/actions/runs/43178361895/artifacts/13924867955) | `sha256:bbe769b4f498d6702e3b3230287fcd79949d02a9508544760cc406aa40c16a85` |

أزيل Workflow المحاكيات المؤقت بعد حفظ الأدلة؛ يبقى موجودًا في تاريخ الالتزام المختبَر لإعادة التشغيل. لا تغيير في `app/` أو ملفات SQL بعد SHA الاختبار أعلاه. التغيير النهائي يحدّث التوثيق ويزيل المسار المؤقت ويضيف تجميع APK الاختبار إلى فحص البناء القائم. يجب نجاح الفحص الإلزامي على رأس PR النهائي نفسه قبل الدمج.

The temporary device workflow is removed after evidence capture and remains reproducible from the tested commit. Final publication leaves all `app/` and fixture files byte-identical to the device-tested SHA; only documentation and workflows change. The existing required job retains unit execution and instrumentation-APK compilation. Required CI must pass again on the final PR head. No M1 milestone or Issue #17 closure is claimed.
