# إصلاحات قاعدة البيانات M1 | M1 database repairs

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
| `v10.sql` | `b58dd442899c2ad1be27b2a2c65d3afb21444189` | DERIVED FROM CURRENT onCreate |

هذه بصمات ملفات اختبار، وليست بصمات قواعد مستخدمين أصلية. لم يُسترجع DDL أصلي لـv1–v9. سيناريو v10 المفقود منه جدول المشاهد يُحاكي عيب الترقية المؤكد، وليس قاعدة مسترجعة من جهاز. جميع القيم المزروعة اصطناعية `TEST_ONLY`؛ لا تتضمن نصوصًا قرآنية معتمدة.

These unchanged test fixtures retain PR #18 provenance labels. v1–v4 are reconstructed scenarios; v10 derives from the baseline source, not an authenticated device snapshot. Removing the scene table simulates the observed faulty upgrade state. Test hashes are not historical database hashes. No religious-content validation is claimed.

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
- لم تُختبر قواعد مستخدمين أصلية أو الترقيات v5–v9 في هذه الحزمة. يلزم استرجاع مخططاتها والتحقق منها قبل اعتماد توافق تاريخي شامل.
- API 26 و35 اختبارات Robolectric مع SQLite الأصلي؛ ليست اختبارات جهاز أو محاكي Android، ولم يُختبر API 36 هنا.
- لا يوجد قيد UNIQUE جديد للخلفيات العامة؛ تبقى المكررات القديمة، وأي SQL مباشر خارج دالة الحفظ يمكنه إنشاء مكررات. تحتاج معالجة هذه السجلات وخطة القيد إلى مراجعة تحفظ جميع البيانات.
- أي دمج يحتاج مراجعة التغييرات والفحوص الإلزامية وموافقة المالك وفق سياسة المستودع. PR #18 و#19 و#22 لم تُدمج ولم تُعدَّل بهذا العمل.

This bounded repair does not complete M1, Issue #17, or the full DB-001…DB-015 matrix. Authentic historical databases and v5–v9 upgrades remain unverified. Native-SQLite Robolectric coverage at API 26/35 is not device/emulator coverage, and API 36 was not tested here. No new uniqueness constraint or destructive cleanup was introduced; direct SQL can still bypass the application save path. Existing PRs remain untouched, and merging requires the repository's normal review and approval gates.
