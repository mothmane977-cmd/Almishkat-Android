# M0-FOUND-05 — تدقيق التبعيات وسياسة الحقوق

**الحالة:** مراجعة تقنية موثقة، وليست شهادة امتثال قانوني نهائية.  
**تاريخ التدقيق:** 2026-11-08  
**مصدر المشروع:** `mothmane977-cmd/Almishkat-Android`، فرع `develop` عند الالتزام `8624c043a07872470f33244796e2fb967cbdd070`.  
**دليل شجرة Gradle:** [تشغيل GitHub Actions 42489955397](https://github.com/mothmane977-cmd/Almishkat-Android/actions/runs/42489955397)، في [PR #7](https://github.com/mothmane977-cmd/Almishkat-Android/pull/7) المخصص للتدقيق **دون دمج**. 

## 1. قرار الترخيص

- تم اعتماد **جميع الحقوق محفوظة (proprietary / All Rights Reserved)** للشيفرة الأصلية التي يملكها صاحب المشروع قانوناً، باسم **OTMANE MOHAMED (عثمان محمد)**، وفق `LICENSE` في `develop`.
- التطبيق مجاني للمستخدمين وفق التوجه التجاري المبدئي؛ **مجانية التطبيق لا ترخّص الشيفرة للآخرين**.
- نوقش GPL-3.0 ولم يُعتمد بسبب السماح بإعادة الاستخدام والتوزيع التجاري وفق شروطه، وهو لا يحقق حصرية الاستغلال البرمجي المطلوبة. ليست هذه دعوى تعارض بين GPL-3.0 وApache-2.0.
- يظل المستودع **عاماً مؤقتاً** بقرار المالك. وتظل حقوق الاستخدام على GitHub نفسها خاضعة لشروط المنصة.
- `LICENSE` الحالي إشعار ملكية خاصة وليس رخصةً لمكتبات الأطراف الخارجية.

## 2. القائمة المباشرة المعتمدة من Gradle

| الأصل في البناء | الإصدار المعلن | الغرض |
|---|---|---|
| `com.android.application` (Android Gradle Plugin) | `8.10.1` | أداة بناء، ليست تبعية مكتبة تشغيل مستقلة |
| `org.jetbrains.kotlin.android` | `2.2.20` | أداة ترجمة وبناء |
| `org.jetbrains.kotlin.plugin.compose` | `2.2.20` | Compose Compiler Plugin |
| `androidx.compose:compose-bom` | `2025.08.00` | آلية ضبط الإصدارات، ليست حزمة APK بحد ذاتها |
| `androidx.activity:activity-compose` | `1.10.1` | تبعية تطبيق مباشرة |
| `androidx.compose.ui:ui` | تُحدَّد عبر BOM | تبعية تطبيق مباشرة |
| `androidx.compose.ui:ui-tooling-preview` | تُحدَّد عبر BOM | تبعية تطبيق مباشرة |
| `androidx.compose.material3:material3` | تُحدَّد عبر BOM | تبعية تطبيق مباشرة |
| `androidx.compose.material:material-icons-extended` | تُحدَّد عبر BOM | تبعية تطبيق مباشرة |
| `androidx.compose.ui:ui-tooling` | تُحدَّد عبر BOM | `debugImplementation` فقط |

ملفات المصدر: `app/build.gradle.kts` و`build.gradle.kts` في الالتزام المذكور.

## 3. شجرة التبعيات المحلولة فعلياً

أُجري الاختبار على GitHub Actions باستخدام JDK 17 وGradle 8.11.1 وAndroid SDK 36، مع نجاح بناء `:app:assembleDebug`، ثم:

```bash
gradle --no-daemon :app:dependencies --configuration debugRuntimeClasspath
gradle --no-daemon :app:dependencies --configuration releaseRuntimeClasspath
```

- **Debug:** 100 مدخل وحدة Maven/Gradle فريد له إصدار محلول.
- **Release:** 94 مدخل وحدة Maven/Gradle فريد له إصدار محلول.
- الفرق `debug-only` (6 مداخل):

  - `androidx.compose.material:material:1.9.0`
  - `androidx.compose.material:material-android:1.9.0`
  - `androidx.compose.ui:ui-tooling:1.9.0`
  - `androidx.compose.ui:ui-tooling-android:1.9.0`
  - `androidx.compose.ui:ui-tooling-data:1.9.0`
  - `androidx.compose.ui:ui-tooling-data-android:1.9.0`

**تنبيه دقيق:** تشمل الشجرة ملفات BOM/constraints وعناصر Maven للمنصات المتعددة وتبعيات لا تتحول بالضرورة كلها إلى مكتبات مستقلة داخل APK النهائي. لذلك هذه ليست قائمة الملفات الثنائية داخل الحزمة.

## 4. جميع مداخل Release المحلولة (94)

### `androidx.activity` (3)

- `androidx.activity:activity:1.10.1`
- `androidx.activity:activity-compose:1.10.1`
- `androidx.activity:activity-ktx:1.10.1`

### `androidx.annotation` (3)

- `androidx.annotation:annotation:1.9.1`
- `androidx.annotation:annotation-experimental:1.4.1`
- `androidx.annotation:annotation-jvm:1.9.1`

### `androidx.arch.core` (2)

- `androidx.arch.core:core-common:2.2.0`
- `androidx.arch.core:core-runtime:2.2.0`

### `androidx.autofill` (1)

- `androidx.autofill:autofill:1.0.0`

### `androidx.collection` (3)

- `androidx.collection:collection:1.5.0`
- `androidx.collection:collection-jvm:1.5.0`
- `androidx.collection:collection-ktx:1.5.0`

### `androidx.compose` (1)

- `androidx.compose:compose-bom:2025.08.00`

### `androidx.compose.animation` (4)

- `androidx.compose.animation:animation:1.9.0`
- `androidx.compose.animation:animation-android:1.9.0`
- `androidx.compose.animation:animation-core:1.9.0`
- `androidx.compose.animation:animation-core-android:1.9.0`

### `androidx.compose.foundation` (4)

- `androidx.compose.foundation:foundation:1.9.0`
- `androidx.compose.foundation:foundation-android:1.9.0`
- `androidx.compose.foundation:foundation-layout:1.9.0`
- `androidx.compose.foundation:foundation-layout-android:1.9.0`

### `androidx.compose.material` (6)

- `androidx.compose.material:material-icons-core:1.7.8`
- `androidx.compose.material:material-icons-core-android:1.7.8`
- `androidx.compose.material:material-icons-extended:1.7.8`
- `androidx.compose.material:material-icons-extended-android:1.7.8`
- `androidx.compose.material:material-ripple:1.9.0`
- `androidx.compose.material:material-ripple-android:1.9.0`

### `androidx.compose.material3` (2)

- `androidx.compose.material3:material3:1.3.2`
- `androidx.compose.material3:material3-android:1.3.2`

### `androidx.compose.runtime` (6)

- `androidx.compose.runtime:runtime:1.9.0`
- `androidx.compose.runtime:runtime-android:1.9.0`
- `androidx.compose.runtime:runtime-annotation:1.9.0`
- `androidx.compose.runtime:runtime-annotation-android:1.9.0`
- `androidx.compose.runtime:runtime-saveable:1.9.0`
- `androidx.compose.runtime:runtime-saveable-android:1.9.0`

### `androidx.compose.ui` (14)

- `androidx.compose.ui:ui:1.9.0`
- `androidx.compose.ui:ui-android:1.9.0`
- `androidx.compose.ui:ui-geometry:1.9.0`
- `androidx.compose.ui:ui-geometry-android:1.9.0`
- `androidx.compose.ui:ui-graphics:1.9.0`
- `androidx.compose.ui:ui-graphics-android:1.9.0`
- `androidx.compose.ui:ui-text:1.9.0`
- `androidx.compose.ui:ui-text-android:1.9.0`
- `androidx.compose.ui:ui-tooling-preview:1.9.0`
- `androidx.compose.ui:ui-tooling-preview-android:1.9.0`
- `androidx.compose.ui:ui-unit:1.9.0`
- `androidx.compose.ui:ui-unit-android:1.9.0`
- `androidx.compose.ui:ui-util:1.9.0`
- `androidx.compose.ui:ui-util-android:1.9.0`

### `androidx.concurrent` (1)

- `androidx.concurrent:concurrent-futures:1.1.0`

### `androidx.core` (3)

- `androidx.core:core:1.15.0`
- `androidx.core:core-ktx:1.15.0`
- `androidx.core:core-viewtree:1.0.0`

### `androidx.customview` (1)

- `androidx.customview:customview-poolingcontainer:1.0.0`

### `androidx.emoji2` (1)

- `androidx.emoji2:emoji2:1.4.0`

### `androidx.graphics` (1)

- `androidx.graphics:graphics-path:1.0.1`

### `androidx.interpolator` (1)

- `androidx.interpolator:interpolator:1.0.0`

### `androidx.lifecycle` (16)

- `androidx.lifecycle:lifecycle-common:2.9.0`
- `androidx.lifecycle:lifecycle-common-java8:2.9.0`
- `androidx.lifecycle:lifecycle-common-jvm:2.9.0`
- `androidx.lifecycle:lifecycle-livedata-core:2.9.0`
- `androidx.lifecycle:lifecycle-process:2.9.0`
- `androidx.lifecycle:lifecycle-runtime:2.9.0`
- `androidx.lifecycle:lifecycle-runtime-android:2.9.0`
- `androidx.lifecycle:lifecycle-runtime-compose:2.9.0`
- `androidx.lifecycle:lifecycle-runtime-compose-android:2.9.0`
- `androidx.lifecycle:lifecycle-runtime-ktx:2.9.0`
- `androidx.lifecycle:lifecycle-runtime-ktx-android:2.9.0`
- `androidx.lifecycle:lifecycle-viewmodel:2.9.0`
- `androidx.lifecycle:lifecycle-viewmodel-android:2.9.0`
- `androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0`
- `androidx.lifecycle:lifecycle-viewmodel-savedstate:2.9.0`
- `androidx.lifecycle:lifecycle-viewmodel-savedstate-android:2.9.0`

### `androidx.profileinstaller` (1)

- `androidx.profileinstaller:profileinstaller:1.4.0`

### `androidx.savedstate` (5)

- `androidx.savedstate:savedstate:1.3.0`
- `androidx.savedstate:savedstate-android:1.3.0`
- `androidx.savedstate:savedstate-compose:1.3.0`
- `androidx.savedstate:savedstate-compose-android:1.3.0`
- `androidx.savedstate:savedstate-ktx:1.3.0`

### `androidx.startup` (1)

- `androidx.startup:startup-runtime:1.1.1`

### `androidx.tracing` (1)

- `androidx.tracing:tracing:1.2.0`

### `androidx.versionedparcelable` (1)

- `androidx.versionedparcelable:versionedparcelable:1.1.1`

### `com.google.guava` (1)

- `com.google.guava:listenablefuture:1.0`

### `org.jetbrains` (1)

- `org.jetbrains:annotations:23.0.0`

### `org.jetbrains.kotlin` (2)

- `org.jetbrains.kotlin:kotlin-stdlib:2.2.20`
- `org.jetbrains.kotlin:kotlin-stdlib-common:2.2.20`

### `org.jetbrains.kotlinx` (7)

- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1`
- `org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.8.1`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.1`
- `org.jetbrains.kotlinx:kotlinx-serialization-bom:1.7.3`
- `org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3`
- `org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.7.3`

### `org.jspecify` (1)

- `org.jspecify:jspecify:1.0.0`

## 5. مصفوفة تراخيص العائلات

| مجموعة المكونات | الترخيص الأولي وفق المصادر المرجعية | درجة التحقق |
|---|---|---|
| `androidx.*` | غالباً Apache License 2.0 وفق مشروع AndroidX ومصادر Compose | موثق على مستوى المشروع؛ يلزم استخراج الترخيص والإشعارات لكل artifact عند النشر |
| `org.jetbrains.kotlin.*` | Apache License 2.0، مع إشعارات خاصة بمكوّنات داخلية عند انطباقها | رسمي من Kotlin؛ يلزم حفظ إشعارات الإصدار الموزع |
| `org.jetbrains.kotlinx.*` | Apache License 2.0 لكوروتينات Kotlin والتسلسل | رسمي من مستودعات المشاريع؛ تفاصيل النسخ الموزعة تُراجع قبل النشر |
| `org.jetbrains:annotations` | Apache License 2.0 | من بيانات Maven الرسمية/المشروع |
| `org.jspecify:jspecify` | Apache License 2.0 | من Maven POM للإصدار 1.0.0 |
| `com.google.guava:listenablefuture` | Apache License 2.0 | من مشروع Guava وبيانات الحزمة |

روابط المصادر:
- [AndroidX المصدر المفتوح](https://github.com/androidx/androidx) و[مصدر Material 3](https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/build.gradle)
- [Kotlin — الترخيص](https://kotlinlang.org/docs/faq.html) و[إشعارات الطرف الثالث لدى Kotlin](https://github.com/JetBrains/kotlin/blob/master/license/README.md)
- [Kotlinx Coroutines](https://github.com/Kotlin/kotlinx.coroutines) و[Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
- [JetBrains Annotations](https://github.com/JetBrains/java-annotations) و[JSpecify 1.0.0 POM](https://central.sonatype.com/artifact/org.jspecify/jspecify/1.0.0)
- [Google Guava](https://github.com/google/guava/blob/master/LICENSE)
- [Apache-2.0 النص الملزم](https://www.apache.org/licenses/LICENSE-2.0)

لا يعني انتماء الحزمة إلى عائلة يُغلب عليها Apache-2.0 أن جميع الإشعارات أو الاستثناءات القانونية جرى التحقق منها إصداراً بإصدار.

## 6. الفصل بين الشيفرة والمحتوى القرآني

- **شيفرة المشروع الأصلية:** إشعار ملكية خاصة باسم OTMANE MOHAMED، في حدود ملكيته القانونية.
- **تبعيات Android/Kotlin:** حقوق وتراخيص أصحابها منفصلة عن ترخيص الشيفرة الأصلية.
- **النصوص القرآنية والخطوط والتلاوات والخلفيات:** لا تغطيها ملكية الشيفرة؛ يجب تسجيل المصدر والإصدار وشروط الاستعمال لكل مورد قبل دمجه.
- **حفص وورش:** يلزم الفصل بين النصوص والخطوط والتوقيتات وبيانات الإصدار بحسب `docs/QURAN_EDITION_POLICY_AR.md`.
- لم تظهر ملفات مصحف معتمدة أو خطوط أو تلاوات مضمّنة في الأرشيفات السبعة التي فُحصت في المرحلة الحالية.

## 7. الأرشيفات التاريخية

فُحصت **7** ملفات ZIP، وما مجموعه **246** ملفاً نصياً مفكوكاً؛ لم تظهر أسماء ملفات `LICENSE/COPYING/NOTICE` ولا مؤشرات تراخيص متعارضة في البحث النصي. **هذا ليس دليلاً على أصالة كل سطر أو ملكيته**؛ يلزم الحفاظ على مصادر وقرارات إنشاء الشيفرة وعدم الادعاء بملكية شيفرة طرف ثالث.

## 8. قيود التدقيق والخطوات اللاحقة

- التقرير يوثق `debugRuntimeClasspath` و`releaseRuntimeClasspath` على التزام معين فقط؛ أي تحديث للتبعيات يوجب إعادة التحقق.
- لم يُجرَ تدقيق آلي شامل لمحتويات كل ملف AAR/JAR أو لملفات `META-INF/LICENSE` و`NOTICE` لكل إصدار. هذا يُستكمل **قبل أي توزيع نهائي**.
- لا يجوز وصف `docs/THIRD_PARTY_NOTICES_DRAFT.md` بأنه إشعار توزيع نهائي أو بديلاً عن النصوص الكاملة للتراخيص والحقوق اللازمة.
- أرقام GitHub Actions ودليل الإصدار محفوظة أعلاه، والتغييرات المؤقتة في PR #7 ليست معدة للدمج.
- يُستحسن فحص قانوني متخصص قبل النشر التجاري، خاصة بالنسبة للمحتوى القرآني ومساهمات الآخرين.

**قرار قبول M0-FOUND-05 (تقني، مشروط):** إشعار الملكية الخاص موجود، وسياسة رفض GPL-3.0 لصالح الملكية الخاصة مبررة، وتم الفصل بين ترخيص الشيفرة وحقوق المحتوى، مع جرد فعلي للتبعيات. لا يمثّل ذلك إصدار شهادة امتثال نهائية قبل التوزيع.
