# بناء ALMISHKAT V23 إلى APK

## المتطلبات
- Android Studio حديث.
- JDK 17.
- Android SDK Platform 36 وBuild Tools 36.0.0.
- Gradle 8.11.1.

## البناء من Android Studio
1. فك ضغط مجلد `Almishkat_Android_PhoneBuild`.
2. افتح المجلد في Android Studio عبر Open.
3. انتظر Gradle Sync، ووافق على تثبيت Android SDK Platform 36 عند الطلب.
4. اختر Build > Build Bundle(s) / APK(s) > Build APK(s).
5. بعد نجاح البناء سيظهر APK في:
   `app/build/outputs/apk/debug/app-debug.apk`

## البناء السحابي
يوجد GitHub Actions workflow في `.github/workflows/android-apk.yml`.
ارفع المشروع إلى مستودع GitHub، ثم افتح Actions وشغّل:
`Build ALMISHKAT Android APK`.
بعد نجاح المهمة، نزّل artifact باسم `almishkat-debug-apk`.

## وضع البناء في هذه الجلسة
لم يُنتج APK هنا: بيئة التنفيذ لا تحتوي Gradle ولا Android SDK ولا Kotlin compiler.
إعدادات المشروع تشير إلى:
- applicationId: `com.almishkat.studio`
- minSdk: 26 (Android 8.0)
- targetSdk: 35
- compileSdk: 36
- versionName: `0.1.0`

وجود إعدادات البناء وملفات Kotlin لا يعني أن المشروع اجتاز الترجمة؛ يجب نجاح Gradle build قبل تثبيت التطبيق.
