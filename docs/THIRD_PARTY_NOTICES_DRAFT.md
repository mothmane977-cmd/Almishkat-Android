# THIRD-PARTY NOTICES — DRAFT / مسودة إشعارات المكونات الخارجية

> **Audit-stage draft only — not yet suitable as a complete distribution notice.**
> 
> **هذه مسودة تدقيق وليست إشعار توزيع نهائياً.** يجب استكمال ملفات التراخيص والحقوق والإشعارات الخاصة بالإصدارات الثنائية التي ستُنشر قبل إطلاق التطبيق.

## Project copyright / حقوق المشروع

Copyright © 2026 **OTMANE MOHAMED (عثمان محمد)** — All Rights Reserved **for the original project code lawfully owned by him**.

This notice does **not** relicense or claim ownership of third-party code, content, Quranic texts, fonts, recitations, or media assets.

## Libraries observed in resolved Gradle release runtime dependency graph

| Family | Original project / المالك أو المصدر | Preliminary license | Primary licensing reference |
|---|---|---|---|
| AndroidX, Jetpack Compose, Material, Activity, Lifecycle, SavedState | Android Open Source Project contributors | Apache-2.0 | https://github.com/androidx/androidx |
| Kotlin standard library | Kotlin / JetBrains and contributors | Apache-2.0 (with applicable upstream third-party notices) | https://github.com/JetBrains/kotlin/blob/master/license/README.md |
| Kotlin Coroutines | Kotlin contributors / JetBrains | Apache-2.0 | https://github.com/Kotlin/kotlinx.coroutines |
| Kotlin Serialization | Kotlin contributors / JetBrains | Apache-2.0 | https://github.com/Kotlin/kotlinx.serialization |
| JetBrains Annotations | JetBrains and contributors | Apache-2.0 | https://github.com/JetBrains/java-annotations |
| JSpecify | JSpecify contributors | Apache-2.0 | https://central.sonatype.com/artifact/org.jspecify/jspecify/1.0.0 |
| Guava ListenableFuture | Google / Guava contributors | Apache-2.0 | https://github.com/google/guava/blob/master/LICENSE |

**Basis:** Gradle `releaseRuntimeClasspath` snapshot from [M0-FOUND-05 CI audit run](https://github.com/mothmane977-cmd/Almishkat-Android/actions/runs/42489955397). The exact version-by-version list is in [M0_FOUND_05_LICENSE_AUDIT_AR.md](M0_FOUND_05_LICENSE_AUDIT_AR.md).

**Important:** A resolved dependency graph contains BOMs, multiplatform metadata and variants, and is not identical to a definitive packaged-binary file list. Every redistributed artifact and its associated `LICENSE`/`NOTICE` terms should be checked before distribution. Do not assume every artifact has identical notices solely because its family commonly uses Apache-2.0.

## Required follow-up before publishing APK/AAB

1. Generate a fresh release dependency graph from the exact tagged commit and verify all coordinates and versions.
2. Inspect the resolved POM / AAR / JAR license metadata and embedded `META-INF` notices for each redistributed artifact; retain any source/attribution requirements.
3. Include the full text of applicable licenses, including Apache License 2.0 when required, and applicable upstream `NOTICE` attributions in an accessible distribution notice.
4. Keep the proprietary project rights notice separate; neither it nor this draft replaces third-party licensing terms.
5. Review Quranic text, fonts, recitations, images and other creator assets independently with source, edition, rights and permissions recorded.
6. Obtain a final release-oriented legal/compliance review when publishing commercially.

License text reference: https://www.apache.org/licenses/LICENSE-2.0

## عربي — توضيح الاستعمال

المكتبات أعلاه لا تصبح ملكاً للمشروع بسبب إدراجها ضمن التطبيق. يبقى لكل مكوّن ترخيصه الأصلي، ويُحتفظ بالإشعارات الملزمة عند التوزيع. لا يلزم فتح الشيفرة الأصلية للمشكاة لمجرد استخدام مكتبات Apache-2.0، لكن يجب التقيد بالتزامات هذه الرخصة.

هذه الوثيقة **لا تمنح للآخرين حق استخدام شيفرة المشكاة الأصلية**، ولا تمنح حقوقاً على النص القرآني أو الخطوط أو التلاوات. يجب التحقق من التراخيص النهائية قبل أي نشر رسمي.
