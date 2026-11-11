# M1-TST-01 — Historical SQLite fixture provenance register
**تحديث ADM-M1-036 — 2026-11-11 — سجل مصادر مُراجع، وليس قبولاً نهائياً.**

التتبع: [Issue #17](https://github.com/mothmane977-cmd/Almishkat-Android/issues/17)؛ [PR #18](https://github.com/mothmane977-cmd/Almishkat-Android/pull/18) (مفتوح، غير مدمج)؛ `docs/db/FIXTURES.md` على فرع التوثيق `test/M1-TST-01-historical-schema-audit`.

## نطاق الأدلة المؤكدة وحدودها

- الإصدار الحالي لـ `almishkat_quran.db` هو **10**؛ [شيفرة `develop`](https://github.com/mothmane977-cmd/Almishkat-Android/blob/develop/app/src/main/java/com/almishkat/studio/QuranLocalDatabase.kt) عند Git **blob SHA** `497c62c571d54cd0ed1dbe8a7ca074473c63bd20`. هذا ليس إثباتاً لشكل كل مخطط قديم.
- الملفات العشرة موجودة على **فرع اختبار مختلف غير مدمج**: [`feature/M1-TST-01-scenes-green-ADM-M1-031`](https://github.com/mothmane977-cmd/Almishkat-Android/tree/feature/M1-TST-01-scenes-green-ADM-M1-031/app/src/androidTest/assets/db)، في `app/src/androidTest/assets/db/`. **ليست بعد موجودة في فرع PR #18 نفسه أو `develop`**.
- `v1`–`v9` **RECONSTRUCTED** من شيفرة v10 ووثائق تطور المنتج الوصفية؛ **لا يوجد أصل SQLite تاريخي موثَّق ولا commit أصلي مُثبت لتعريف DDL** لهذه الإصدارات. `v10` **DERIVED FROM CURRENT onCreate**، وليس نسخة قاعدة من جهاز مستخدم.
- بصمات العمود الثالث هي **Git blob SHA-1 لملفات الاختبار الموجودة**؛ **ليست** SHA للإصدارات القديمة الأصلية، ولا SHA-256 للأرشيفات. المصادر المقتبسة مستندات ثانوية؛ بعض تفاصيل ترتيب الأعمدة والقيود والفهارس **مفترضة** وتحتاج اختباراً بمخططات أصلية عند توفرها.
- جميع الأمثلة المزروعة بيانات اصطناعية `TEST_ONLY_*`، وليست نصوصاً قرآنية موثقة أو توقيتات تلاوات أصلية. لا اختبار هنا لقواعد مستخدمين فعليين.

## جدول النسخ والمرجع

| SQLite version | Fixture على فرع الاختبارات | Git blob SHA-1 (test file) | سند إعادة البناء، لا DDL أصلي | الحالة | `PRAGMA user_version` |
| --- | --- | --- | --- | --- | --- |
| v1 | [v1.sql](https://github.com/mothmane977-cmd/Almishkat-Android/tree/feature/M1-TST-01-scenes-green-ADM-M1-031/app/src/androidTest/assets/db/v1.sql) | `93bc560cca6e091ce9263c137caf0bc5d60fbdeb` | [docs/QURAN_DATABASE_V5_AR.md](https://github.com/mothmane977-cmd/Almishkat-Android/blob/develop/docs/QURAN_DATABASE_V5_AR.md) — v1 Quran tables and source manifest | RECONSTRUCTED | 1 |
| v2 | [v2.sql](https://github.com/mothmane977-cmd/Almishkat-Android/tree/feature/M1-TST-01-scenes-green-ADM-M1-031/app/src/androidTest/assets/db/v2.sql) | `b85097208ed0d24a69b55c02afb16912f0a23427` | [docs/PROJECT_V7_LOCAL_TIMING_AR.md](https://github.com/mothmane977-cmd/Almishkat-Android/blob/develop/docs/PROJECT_V7_LOCAL_TIMING_AR.md) — v1→v2 ayah_timings | RECONSTRUCTED | 2 |
| v3 | [v3.sql](https://github.com/mothmane977-cmd/Almishkat-Android/tree/feature/M1-TST-01-scenes-green-ADM-M1-031/app/src/androidTest/assets/db/v3.sql) | `9ad8e237290da0005ce7985273fffb8867eb5dba` | [docs/PROJECT_V8_PROJECTS_AR.md](https://github.com/mothmane977-cmd/Almishkat-Android/blob/develop/docs/PROJECT_V8_PROJECTS_AR.md) — v2→v3 studio_projects | RECONSTRUCTED | 3 |
| v4 | [v4.sql](https://github.com/mothmane977-cmd/Almishkat-Android/tree/feature/M1-TST-01-scenes-green-ADM-M1-031/app/src/androidTest/assets/db/v4.sql) | `5e03abcd282b24753f878238296b7113d298e48e` | [docs/PROJECT_V12_PERSISTED_CLIPS_AR.md](https://github.com/mothmane977-cmd/Almishkat-Android/blob/develop/docs/PROJECT_V12_PERSISTED_CLIPS_AR.md) — v3→v4 audio_clip_selections | RECONSTRUCTED | 4 |
| v5 | [v5.sql](https://github.com/mothmane977-cmd/Almishkat-Android/tree/feature/M1-TST-01-scenes-green-ADM-M1-031/app/src/androidTest/assets/db/v5.sql) | `7163c3b95e169a178fee777e12d2aa86d75ddf5b` | [docs/PROJECT_V13_AYAH_LINKING_AR.md](https://github.com/mothmane977-cmd/Almishkat-Android/blob/develop/docs/PROJECT_V13_AYAH_LINKING_AR.md) — v4→v5 audio clip link columns | RECONSTRUCTED | 5 |
| v6 | [v6.sql](https://github.com/mothmane977-cmd/Almishkat-Android/tree/feature/M1-TST-01-scenes-green-ADM-M1-031/app/src/androidTest/assets/db/v6.sql) | `ff50c97a27cb52411c171acb9fedb18277a09f9a` | [docs/PROJECT_V16_SCENE_LAYERS_AR.md](https://github.com/mothmane977-cmd/Almishkat-Android/blob/develop/docs/PROJECT_V16_SCENE_LAYERS_AR.md) — v5→v6 scene_layers | RECONSTRUCTED | 6 |
| v7 | [v7.sql](https://github.com/mothmane977-cmd/Almishkat-Android/tree/feature/M1-TST-01-scenes-green-ADM-M1-031/app/src/androidTest/assets/db/v7.sql) | `bf9efcd23b3f9db5cd643ed022887e277d0509af` | [docs/PROJECT_V18_BACKGROUNDS_AR.md](https://github.com/mothmane977-cmd/Almishkat-Android/blob/develop/docs/PROJECT_V18_BACKGROUNDS_AR.md) — v6→v7 scene_backgrounds | RECONSTRUCTED | 7 |
| v8 | [v8.sql](https://github.com/mothmane977-cmd/Almishkat-Android/tree/feature/M1-TST-01-scenes-green-ADM-M1-031/app/src/androidTest/assets/db/v8.sql) | `4b1096ac5e538a84e41ae3f5d3192a27baa2ad08` | [docs/PROJECT_V19_MOTION_BACKGROUNDS_AR.md](https://github.com/mothmane977-cmd/Almishkat-Android/blob/develop/docs/PROJECT_V19_MOTION_BACKGROUNDS_AR.md) — v7→v8 video_uri | RECONSTRUCTED | 8 |
| v9 | [v9.sql](https://github.com/mothmane977-cmd/Almishkat-Android/tree/feature/M1-TST-01-scenes-green-ADM-M1-031/app/src/androidTest/assets/db/v9.sql) | `5bd2ea4898f583585d8cc1d4bee464add2560820` | [docs/PROJECT_V21_SCENE_MANAGER_AR.md](https://github.com/mothmane977-cmd/Almishkat-Android/blob/develop/docs/PROJECT_V21_SCENE_MANAGER_AR.md) — v8→v9 studio_scenes | RECONSTRUCTED | 9 |
| v10 | [v10.sql](https://github.com/mothmane977-cmd/Almishkat-Android/tree/feature/M1-TST-01-scenes-green-ADM-M1-031/app/src/androidTest/assets/db/v10.sql) | `b58dd442899c2ad1be27b2a2c65d3afb21444189` | [current SQLiteOpenHelper](https://github.com/mothmane977-cmd/Almishkat-Android/blob/develop/app/src/main/java/com/almishkat/studio/QuranLocalDatabase.kt) — current SQLiteOpenHelper v10 onCreate, not legacy dump | DERIVED FROM CURRENT | 10 |

**المصدر البنيوي الإضافي:** نسخ v1–v9 اللاحقة تستلزم ما قبلها؛ الوثائق أعلاه وصف لتاريخ المزايا، لا أرشيف `sqlite_master` أصلي لكل إصدار. قائمة [أرشيفات المصدر](https://github.com/mothmane977-cmd/Almishkat-Android/tree/develop/archive/source-snapshots) قد تُستخدم للبحث عن أصول تاريخية؛ اسم ملف ZIP أو رقم إصدار التطبيق لا يثبت رقم SQLite. لم يُثبت هنا استرجاع DDL قديم منها.

## إثبات الاختبارات المتاح (ليس قبول M1-TST-01)

- [مصفوفة `V1V10SchemaMatrixTest`](https://github.com/mothmane977-cmd/Almishkat-Android/blob/feature/M1-TST-01-scenes-green-ADM-M1-031/app/src/androidTest/java/com/almishkat/studio/V1V10SchemaMatrixTest.kt) تتحقق من `PRAGMA user_version` لكل fixture، وترقيات `v1`–`v10`، وحفظ القيم وإعادة الفتح و`integrity_check`، مع مقارنة بنيوية ولقطات فروق DDL الخام.
- [GitHub Actions Run 43019335006](https://github.com/mothmane977-cmd/Almishkat-Android/actions/runs/43019335006) حقق **12/12 PASS** لمصفوفة Android على **API 26 وAPI 36** على **فرع تكامل تجريبي يجمع إصلاح D01 وإصلاح المشاهد**. لا تنسب هذه النتائج إلى [PR #19](https://github.com/mothmane977-cmd/Almishkat-Android/pull/19) المنعزل، لأن ترقية الإصدارات القديمة فيه ما زالت تفتقد `studio_scenes`.
- الاختبارات البنيوية تتجاهل اختلاف **ترتيب الأعمدة الفيزيائي** عندما لا تتغير السمات، وتحتفظ بـDDL الخام؛ لا تثبت تكافؤ كل `CHECK` و`COLLATE` وtriggers وexpression indexes. لا يجوز توسيع ادعاء القبول دون تدقيق إضافي.
- [Historical RED Run 42994536983](https://github.com/mothmane977-cmd/Almishkat-Android/actions/runs/42994536983) يثبت فشل D01 القديم المتوقع على الشيفرة القديمة. نجاح إثبات RED ليس نجاح ترقية إنتاج.

## البنود المفتوحة قبل قبول المهمة

1. اعتماد مراجع v1–v9 بوصفها **RECONSTRUCTED** أو إيجاد الأصول التاريخية مع **commit SHA** حقيقي؛ مراجعة تاريخ المسارات السابقة وZIP المؤرشفة، مع حفظ أي نتائج أصلية دون خلطها بالتصورات.
2. قرار المراجع حول المكان المطلوب للخامات: خارطة الطريق تذكر `app/src/test/resources/db/`، والاختبارات الواقعية تستخدم `app/src/androidTest/assets/db/`. يُطلب اعتماد الموضع Android أولاً أو نقل/مزامنة موثقة مع اختبار تحميل فعلي؛ **لم يتم النقل**.
3. جعل الملفات واختبار المحمل والمقارنة البنيوية سجلاً قابلاً للمراجعة على **فرع مصدر موحد ضمن M1-TST-01** لاحقاً بعد موافقة مراجعة المسار، ثم مراجعة مستقلة للـDDL والبيانات والاختلافات.
4. M1-CI-01 `db-tests` لم تُفعّل بعد ولم تُضف إلى قواعد الحماية. لا يُدَّعى استيفاء بوابة الاعتماد، ولا تُغلق Issue #17.
5. PR #19 محتفظ بترتيبه القديم (أول commit إصلاح الإنتاج)؛ سيُعالج شرط RED-first بخطة PR بديل لاحقاً **بموافقة منفصلة، دون تعديل تاريخ PR #19 أو إنشاء PR هنا**.

**الحالة:** PROVENANCE REGISTER UPDATED / FIXTURES STILL EXPERIMENTAL / M1-TST-01 ACCEPTANCE PENDING. لا تعديل للإنتاج، ولا دمج أو تغيير لقواعد الحماية.
