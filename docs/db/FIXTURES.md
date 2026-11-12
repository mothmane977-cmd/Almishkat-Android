# M1-TST-01 — سجل مصادر مخططات الاختبار | Fixture provenance register

المهمة: [Issue #17](https://github.com/mothmane977-cmd/Almishkat-Android/issues/17). هذا السجل يستكمل توثيق الملفات الموجودة بعد دمج [PR #23](https://github.com/mothmane977-cmd/Almishkat-Android/pull/23)، دون تغيير SQL أو كود الإنتاج أو الاختبارات.

This register documents the existing fixture files after PR #23. It does not change SQL, production code or test behavior, close Issue #17, or certify M1 completion.

## المصدر الثابت | Pinned source

- أساس هذا الجرد / Inventory baseline: `develop` at `6fc5cbd8f3406db28b120b8821b664212920d60d`.
- مسار الملفات الحالي / Current path: `app/src/test/resources/db/vN.sql`.
- مصدر ملفات الاختبار المنسوخة / Copied test-fixture source: [PR #18 commit `5ffb077c02d66dcf7388218f8902a890856cd390`](https://github.com/mothmane977-cmd/Almishkat-Android/tree/5ffb077c02d66dcf7388218f8902a890856cd390/app/src/androidTest/assets/db). All ten files are byte-identical to that source; PR #18 itself remains unmerged.
- مرجع مخطط v10 / v10 structural reference: baseline `QuranLocalDatabase.kt` Git blob `497c62c571d54cd0ed1dbe8a7ca074473c63bd20` at commit `22dac68190250f093bcca044509fbfa1e32a8327`.

هذه معرفات مصدر ملفات الاختبار، وليست معرفات إصدارات SQLite التاريخية الأصلية. لم يُسترجع commit أصلي موثَّق أو نسخة جهاز أصلية لـv1–v9. v10 مشتق من `onCreate` في الكود السابق؛ الإصدار الحالي للتطبيق بعد الإصلاح هو SQLite **11**، وتبقى ملفات الاختبار مجمدة عند إصدارات الإدخال v1–v10.

These identifiers establish fixture-file provenance, not authentic historical database provenance. No original historical DDL commit or device database for v1–v9 has been recovered. v10 derives from the former `onCreate`, not a device dump. The current helper targets SQLite **11**; fixtures intentionally retain their v1–v10 input versions.

## الملفات والبصمات | Files and hashes

البصمات أدناه **Git blob SHA-1 لملفات الاختبار**، وليست SHA-256 لقاعدة مستخدم أو لأرشيف قديم. `user_version` هو إصدار الإدخال المكتوب داخل كل ملف.

The following values are **Git blob SHA-1 hashes of test fixtures**, not historical database or archive SHA-256 hashes. Each `user_version` is declared by the fixture itself.

| الملف / File | `user_version` | Git blob SHA-1 | مصدر المخطط / Schema status |
| --- | ---: | --- | --- |
| [v1.sql](../../app/src/test/resources/db/v1.sql) | 1 | `93bc560cca6e091ce9263c137caf0bc5d60fbdeb` | RECONSTRUCTED |
| [v2.sql](../../app/src/test/resources/db/v2.sql) | 2 | `b85097208ed0d24a69b55c02afb16912f0a23427` | RECONSTRUCTED |
| [v3.sql](../../app/src/test/resources/db/v3.sql) | 3 | `9ad8e237290da0005ce7985273fffb8867eb5dba` | RECONSTRUCTED |
| [v4.sql](../../app/src/test/resources/db/v4.sql) | 4 | `5e03abcd282b24753f878238296b7113d298e48e` | RECONSTRUCTED |
| [v5.sql](../../app/src/test/resources/db/v5.sql) | 5 | `7163c3b95e169a178fee777e12d2aa86d75ddf5b` | RECONSTRUCTED |
| [v6.sql](../../app/src/test/resources/db/v6.sql) | 6 | `ff50c97a27cb52411c171acb9fedb18277a09f9a` | RECONSTRUCTED |
| [v7.sql](../../app/src/test/resources/db/v7.sql) | 7 | `bf9efcd23b3f9db5cd643ed022887e277d0509af` | RECONSTRUCTED |
| [v8.sql](../../app/src/test/resources/db/v8.sql) | 8 | `4b1096ac5e538a84e41ae3f5d3192a27baa2ad08` | RECONSTRUCTED |
| [v9.sql](../../app/src/test/resources/db/v9.sql) | 9 | `5bd2ea4898f583585d8cc1d4bee464add2560820` | RECONSTRUCTED |
| [v10.sql](../../app/src/test/resources/db/v10.sql) | 10 | `b58dd442899c2ad1be27b2a2c65d3afb21444189` | DERIVED FROM CURRENT onCreate |

تفاصيل المصادر الثانوية والافتراضات محفوظة داخل ملفات SQL؛ تشمل وثائق تطور جداول القرآن والتوقيتات والمشاريع والمقاطع والطبقات والخلفيات والمشاهد. الأسماء والإصدارات الوصفية لا تثبت DDL تاريخيًا أصليًا. كل القيم المزروعة في الاختبارات اصطناعية `TEST_ONLY`، وليست نصوصًا قرآنية أو توقيتات موثقة.

Secondary-document references and reconstruction assumptions remain in the unchanged SQL headers. Product-version names and descriptive documents do not authenticate historical DDL. Seeded records are synthetic `TEST_ONLY` values, not authenticated Quran text or recitation timings.

## طريقة التحقق | Verification method

- يحمّل [العقد المشترك](../../app/src/sharedTest/java/com/almishkat/studio/DatabaseRegressionContract.kt) كل إصدار ويفحص `db.version` قبل الترقية، ثم حفظ القيم والبنية وإعادة الفتح وسلامة SQLite. تتشارك اختبارات JVM وAndroid ملفات SQL نفسها.
- أُثبتت نتائج الاختبارات السابقة في [سجل الأدلة](M1_REPAIR_EVIDENCE.md): 38/38 لكل من Debug وRelease محليًا؛ و19/19 على كل من محاكيَي API 26 و36 عند SHA `16be26b27d19b36d0cc2fe836933857a9afbc1a6`.
- هذه أدلة موجودة قبل إضافة السجل، وليست تشغيلًا جديدًا منسوبًا إلى تغيير التوثيق. فحص البناء الإلزامي لطلب هذا التغيير يُراجع منفصلًا على رأسه النهائي.

The shared test contract loads every input version and checks `db.version`, preservation, column/index metadata, repeat opens and SQLite integrity. JVM and Android runners consume the same SQL files. The linked evidence records prior 38/38 Debug and Release results and 19/19 per API26/API36 emulator at the pinned SHA. Those are existing results, not new executions claimed for this documentation change. Required CI for this PR must be verified on its own final head.

لإعادة التحقق بعد تثبيت متطلبات البناء الموثقة / Reproduce with the documented build prerequisites:

```sh
git hash-object app/src/test/resources/db/v1.sql
gradle --no-daemon --stacktrace :app:testDebugUnitTest
gradle --no-daemon --stacktrace :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.almishkat.studio.DatabaseInstrumentedRegressionTest
```

الأمر الأخير يحتاج جهازًا أو محاكيًا جاهزًا، وهو ليس ادعاء تشغيله في هذه الجولة التوثيقية. / The last command requires a ready device or emulator; listing it does not claim a new device execution in this documentation round.

## حدود قبول المهمة | Acceptance limits

- يسمح نص Issue #17 بالمخططات المعاد بناؤها عند تعذر استرجاع الأصل بشرط وسمها وتسجيل مصدرها؛ يبقى توافق قواعد المستخدمين التاريخية غير مثبت بهذا البديل وحده.
- يملأ هذا الملف مسار سجل المصادر المطلوب فقط. وجوده لا يغني عن بقية شروط #17، ومنها بوابة `db-tests` الإلزامية وقبول أدلة الترحيل.
- لا يفترض هذا السجل تطابق معرّفات D1–D15 مع DB-001–DB-015، ولا يُنشئ جدول اختبارات بديلًا. مطابقة نطاق M1 الكامل مع `ROADMAP.md` مستقلة عن توثيق مصادر ملفات #17.

Issue #17 permits explicitly labelled reconstructions when originals cannot be recovered; this does not establish compatibility with authentic historical user databases. This file fills the required provenance-register path only. The remaining issue criteria, including the required `db-tests` gate and acceptance of migration evidence, remain separate. D1–D15 are not assumed equivalent to DB-001–DB-015, and no alternative test table is introduced. Full M1 conformance to the owner's `ROADMAP.md` remains separate from this fixture register.
