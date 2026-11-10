# Almishkat Studio | المشكاة ستوديو

**العربية** · [English](#english)

## العربية

### ما هو المشروع؟
المشكاة ستوديو مشروع تطبيق Android أصلي لإنشاء مقاطع فيديو قرآنية عمودية، باستخدام **Kotlin** و**Jetpack Compose**. المستودع حالياً في مرحلة التأسيس والتطوير، وليس إصداراً إنتاجياً جاهزاً للنشر.

### الميزات والنطاق
- **الهدف المخطط:** تحرير مشاهد ومقاطع التلاوة والخلفيات والتوقيتات وإخراج فيديوهات قرآنية، مع دعم العمل دون اتصال وبالاتصال عند اكتمال التنفيذ.
- **الوضع الحالي المؤكد:** توجد شيفرة وواجهات تأسيسية وملفات توثيق لإصدارات وتجارب سابقة؛ نجاح بناء APK تجريبي لا يثبت جاهزية كل الوظائف أو صحة مخرجاتها.
- **الروايات المستهدفة:** حفص عن عاصم وورش عن نافع، مع فصل مصادر النصوص والتوقيتات وبيانات كل رواية. **لم تُدمج النصوص القرآنية الرسمية المعتمدة بعد**؛ لا يجوز اعتبار التطبيق مرجعاً قرآنياً معتمداً في حالته الحالية.

### المتطلبات التقنية
- Android Studio مع أدوات Android SDK.
- **JDK 17** و**Gradle 8.11.1**.
- Android SDK Platform **36** وBuild Tools **36.0.0**.
- إعدادات التطبيق الحالية: `applicationId=com.almishkat.studio`، `minSdk=26`، `targetSdk=35`، `compileSdk=36`، الإصدار `0.1.0`.

### بناء التطبيق محلياً
1. استنسخ المستودع: `git clone https://github.com/mothmane977-cmd/Almishkat-Android.git`.
2. افتح مجلد المستودع الجذري في Android Studio، واضبط JDK 17 وAndroid SDK 36.
3. انتظر مزامنة Gradle، ثم نفّذ `gradle :app:assembleDebug` من جذر المشروع، أو استخدم **Build APK(s)** في Android Studio.
4. بعد نجاح البناء، ستجد الملف في `app/build/outputs/apk/debug/app-debug.apk`.

**ملاحظة:** يستخدم مسار CI الحالي Gradle مثبتاً في بيئة GitHub Actions؛ لا يُفترض وجود Gradle Wrapper في هذا المستودع.

### البناء والتثبيت من GitHub (مناسب للهاتف)
1. افتح تبويب **Actions** في المستودع، واختر **Almishkat Current Source - Clean Clone Build**.
2. شغّل `workflow_dispatch` على فرع يدعم التشغيل، أو راقب بناء Pull Request.
3. بعد نجاح التشغيل، نزّل artifact باسم `Almishkat-Clean-Clone-Debug-APK`، ثم استخرج `app-debug.apk`.
4. ثبّت ملف APK التجريبي على جهاز Android متوافق بعد التحقق من مصدره والسماح بالتثبيت من المصدر المختار عند الحاجة.

لا تخلط بين APK تجريبي ونسخة إصدار موقعة أو تطبيق صالح للنشر.

### بنية المستودع
| المسار | الغرض |
| --- | --- |
| `app/` | تطبيق Android الحالي |
| `core/`, `data/`, `domain/`, `ui/` | مجلدات تنظيمية/تمهيدية؛ ليست دليلاً على اكتمال Clean Architecture |
| `scripts/` | أدوات مساعدة |
| `docs/` | الوثائق التقنية وسياسات النص القرآني |
| `archive/` | لقطات المصادر والإصدارات السابقة |
| `.github/workflows/` | فحوص GitHub Actions |

المشروع الحالي يضم وحدة Gradle `:app`؛ المجلدات التمهيدية ليست بالضرورة وحدات Gradle مستقلة.

### القيود والحوكمة
- إصلاحات قاعدة البيانات، واختبارات الترحيل والانحدار، والتحقق من النصوص القرآنية الرسمية ما زالت خاضعة لخارطة الطريق المعتمدة.
- لا تُستخدم النصوص أو الخطوط أو التلاوات دون التحقق من المصدر والإصدار والترخيص.
- لا تُدمج التغييرات في `main` أو `develop` مباشرة؛ راجع [دليل المساهمة](CONTRIBUTING.md) و[سياسة Git Flow](docs/GIT_FLOW_POLICY.md).
- راجع [سياسة الروايات](docs/QURAN_EDITION_POLICY_AR.md) و[وثائق المشروع](docs/). ترخيص الشيفرة الأصلية المعتمد: **ملكية خاصة — جميع الحقوق محفوظة** وفق [LICENSE](LICENSE). تبقى تراخيص المكتبات والنصوص القرآنية والخطوط والتلاوات والمواد الخارجية مستقلة، ويجب التحقق من حقوقها قبل دمجها أو توزيعها.

---

## English

### Overview
**Almishkat Studio** is a native Android project for creating vertical Quranic videos, built with **Kotlin** and **Jetpack Compose**. It is an **in-development foundation**, not a production-ready release.

### Features and scope
- **Planned:** scene/timeline editing, recitation clips, visual backgrounds, synchronization, video export, and offline/online workflows.
- **Verified current scope:** foundational application code, early interfaces, historical project documentation, and a GitHub Actions debug APK build. A successful APK build **does not establish functional completeness**.
- **Target recitations/readings:** Hafs 'an Asim and Warsh 'an Nafi', with distinct edition data and timing sources. **Official verified Quran text packages have not yet been integrated**.

### Requirements
- Android Studio and Android SDK.
- **JDK 17**, **Gradle 8.11.1**, **Android SDK Platform 36**, and **Build Tools 36.0.0**.
- Current configuration: `com.almishkat.studio`, `minSdk 26`, `targetSdk 35`, `compileSdk 36`, version `0.1.0`.

### Build and install
1. Clone: `git clone https://github.com/mothmane977-cmd/Almishkat-Android.git`.
2. Open the repository root in Android Studio and configure JDK 17 / SDK 36.
3. Run `gradle :app:assembleDebug` from the repository root, or use Android Studio's **Build APK(s)**.
4. The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk` after a successful build.
5. Alternatively, use **Actions → Almishkat Current Source - Clean Clone Build → Run workflow**, then download and extract the `Almishkat-Clean-Clone-Debug-APK` artifact and install the debug APK on a compatible Android device.

The CI workflow supplies Gradle on its runner; a Gradle Wrapper should not be assumed to be present. A debug APK is not a signed production release.

### Repository architecture
- `app/`: current Android application module (`:app`).
- `core/`, `data/`, `domain/`, `ui/`: preparatory directories, **not evidence of completed Clean Architecture**.
- `scripts/`: utilities.
- `docs/`: documentation and Quran edition policy.
- `archive/`: historical source snapshots.
- `.github/workflows/`: CI workflows.

### Limitations and governance
Database migrations, regression tests, Quran text verification, and production readiness remain subject to the approved roadmap. Do not assume the app currently contains authenticated Quran text, complete video-export behavior, or fully cleared third-party content rights.

See [Contributing](CONTRIBUTING.md), [Git Flow](docs/GIT_FLOW_POLICY.md), [Quran Edition Policy](docs/QURAN_EDITION_POLICY_AR.md), and [technical documentation](docs/). The original project source code is **proprietary — All Rights Reserved**, as documented in [LICENSE](LICENSE). Third-party libraries, Quranic texts, fonts, recitations, and other assets have separate licenses and rights that must be verified before integration or distribution.
