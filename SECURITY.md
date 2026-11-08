# Security Policy | سياسة الأمان

## العربية

### حالة الدعم
المشروع قيد التطوير، ولا توجد حالياً نسخة إنتاجية مستقرة معلنة أو قائمة إصدارات مدعومة أمنياً. لا يُعد نجاح بناء APK التجريبي ضماناً للأمان.

### الإبلاغ المسؤول عن الثغرات
1. **لا تنشر تفاصيل الثغرة أو خطوات استغلالها أو الأسرار في Issue أو Pull Request عام.**
2. إن كانت ميزة **Private vulnerability reporting** مفعّلة في تبويب **Security** بالمستودع، استخدمها للإبلاغ الخاص.
3. إذا لم تكن مفعّلة، اطلب من مالك المستودع توفير قناة اتصال خاصة آمنة **دون مشاركة تفاصيل الثغرة علناً**. لم يتم التحقق من وجود بريد أمني مخصص، لذلك لا يُذكر عنوان غير مؤكد.
4. في القناة الخاصة، قدم وصفاً واضحاً للأثر، والنسخة أو الالتزام المتأثر، وخطوات إعادة الإنتاج الآمنة، وأي دليل منقّح من الأسرار أو البيانات الشخصية.

### التعامل مع البلاغات
- يُقيّم المالك نطاق المشكلة وخطورتها ويُوثّق الإصلاح والاختبارات عند التحقق منها.
- تُحفظ مفاتيح التوقيع والرموز وبيانات المستخدمين خارج المستودع.
- لا تختبر أنظمة أو حسابات الآخرين دون إذن، ولا تستخرج بيانات حقيقية.
- لا يوجد التزام منشور بمدة استجابة أو برنامج مكافآت للثغرات.
- النشر المنسق للتفاصيل الأمنية يتم بعد المعالجة وبالتوافق مع المبلّغ، عندما يكون ذلك ممكناً.

### سلامة المحتوى القرآني
أي خلل قد يؤدي إلى عرض آيات غير صحيحة أو خلط الروايات أو فساد ملفات النصوص يجب الإبلاغ عنه بوضوح؛ وقد يكون عيب سلامة محتوى حتى إن لم يكن ثغرة أمن معلومات تقليدية.

## English

### Supported versions
This repository is in active development; **no production release or supported-version security matrix is currently declared**. A passing debug APK build is not a security certification.

### Reporting a vulnerability
1. **Do not publish exploit details, credentials, private data, or proof-of-concept attacks in public issues or pull requests.**
2. Use GitHub **Security → Report a vulnerability** if private vulnerability reporting is enabled.
3. Otherwise, request a secure private contact method from the repository owner **without disclosing sensitive details publicly**. No verified security email address is currently published.
4. Privately provide impact, affected commit/version, safe reproduction steps, and sanitized supporting evidence.

### Handling and disclosure
The maintainer will assess confirmed reports and document appropriate fixes and tests. Do not probe third-party systems without permission. Do not commit secrets. No response-time SLA or bug-bounty program is promised. Coordinate disclosure after mitigation where feasible.

Quranic text corruption, incorrect verse display, or mixing of riwayat should also be reported as high-integrity content defects.
