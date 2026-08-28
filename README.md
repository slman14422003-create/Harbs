# موسوعة الأعشاب الطبية — نسخة أندرويد أصلية (Kotlin + Jetpack Compose)

هذا المشروع تحويل كامل لتطبيقكم من غلاف WebView إلى تطبيق أندرويد **أصلي 100%**
مبني بأحدث تقنيات Google الرسمية:

- **Kotlin** + **Jetpack Compose** (بدل XML/Views القديمة)
- **Material 3** مع ألوان ديناميكية (Material You) ودعم الوضع الداكن
- **Navigation-Compose** للتنقل بين الشاشات
- **Coroutines + StateFlow** لإدارة الحالة (بدل استدعاءات JS المباشرة)
- **Firebase Auth + Firestore** — متصل بنفس مشروعكم الحالي (`semoharbs`)
  بنفس مجموعتَي البيانات `herbs` و`categories`، فلا حاجة لنقل أو تكرار أي بيانات
- **Coil** لتحميل صور الأعشاب
- **DataStore** لحفظ المفضلة وتفضيلات المظهر محليًا على الجهاز

## الميزات المتوفرة
- تصفح التصنيفات (Grid عصري) وعدد الأعشاب في كل تصنيف
- قائمة أعشاب كل تصنيف + صفحة تفاصيل منظمة (الفوائد، الاستخدام، التحذيرات، الأضرار، ملاحظات)
- بحث فوري بالاسم أو الفائدة
- المفضلة (محفوظة محليًا على الجهاز)
- تسجيل الدخول / إنشاء حساب عبر Firebase Auth
- لوحة تحكم أدمن كاملة: إضافة / تعديل / حذف الأعشاب (بنفس منطق UID المسؤول الأصلي)
- إعدادات: وضع داكن / اتّباع النظام / ألوان ديناميكية

## كيف تفتحه وتُشغّله
1. ثبّت **Android Studio** (أحدث إصدار — Ladybug أو أحدث).
2. افتح المجلد `HerbalEncyclopedia` كمشروع (Open an existing project).
3. اتركه يُنزّل الاعتماديات (Gradle Sync) — يحتاج اتصال إنترنت في أول مرة فقط.
4. اضغط Run ▶ على جهاز حقيقي أو محاكي.

**ملاحظة مهمة:** Firebase مُهيّأ يدويًا داخل `HerbalApp.kt` بنفس مفاتيح مشروعكم
الحالية (نفس المفاتيح المستخدمة في نسخة الويب) — لذلك **لا تحتاجون** لملف
`google-services.json` كي يعمل المشروع فورًا.

## للنشر النهائي على المتجر (موصى به لاحقًا)
- سجّلوا تطبيق أندرويد بنفس اسم الحزمة `com.salman.herbalencyclopedia` في
  Firebase Console، وأضيفوا بصمة SHA-1 لمفتاح التوقيع — هذا يقوّي الحماية
  (App Check) ويفتح ميزات مستقبلية مثل تسجيل الدخول عبر Google أو الإشعارات.
- بعد التسجيل، نزّلوا `google-services.json` الحقيقي وضعوه في `app/`، ثم يمكن
  لاحقًا استبدال التهيئة اليدوية في `HerbalApp.kt` بمكوّن Firebase Gradle
  الرسمي إن رغبتم.
- استخدموا نفس `release.keystore.jks` الذي أنشأتموه سابقًا (المذكور في
  `ANDROID_APK_GUIDE.md` الأصلي) لتوقيع الإصدارات القادمة بنفس المفتاح.

## الرفع على GitHub وبناء APK تلقائيًا
المشروع يتضمن `.github/workflows/android-release.yml` يبني APK ويصدر Release
تلقائيًا عند كل `push` إلى main/master (أو يدويًا عبر "Run workflow").

1. ارفعوا محتوى هذا المجلد بالكامل (بما فيه `.github/` و`app/` وباقي الملفات)
   إلى مستودع GitHub، بنفس البنية الحالية.
2. من تبويب **Settings → Actions → General → Workflow permissions**، اختاروا
   **"Read and write permissions"** حتى يستطيع الووركفلو إنشاء Release.
3. عند أول `push`، سيبدأ البناء تلقائيًا، وستجدون الـ APK جاهزًا تحت تبويب
   **Releases**.

### توقيع دائم (مهم لتحديثات لاحقة)
بدون هذه الخطوة، كل إصدار جديد يُوقَّع بمفتاح مختلف تلقائيًا، فيضطر المستخدمون
لحذف النسخة القديمة قبل تثبيت الجديدة:

```bash
keytool -genkeypair -v -keystore release.keystore.jks -alias herbal \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.keystore.jks > keystore_base64.txt
```

ثم أضيفوا في **Settings → Secrets and variables → Actions** أربعة أسرار:

| الاسم | القيمة |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | محتوى ملف `keystore_base64.txt` |
| `ANDROID_KEYSTORE_PASSWORD` | كلمة سر الـ keystore |
| `ANDROID_KEY_ALIAS` | `herbal` (أو الاسم الذي اخترتموه) |
| `ANDROID_KEY_PASSWORD` | كلمة سر المفتاح |

احتفظوا بملف `release.keystore.jks` في مكان آمن؛ فقدانه يعني عدم القدرة على
نشر تحديثات لنفس التطبيق مستقبلًا على المتجر.

## بنية المشروع
```
app/src/main/java/com/salman/herbalencyclopedia/
├── HerbalApp.kt              # تهيئة Firebase + حاوية الاعتماديات
├── MainActivity.kt           # نقطة الدخول + شاشة البدء (Splash)
├── data/
│   ├── model/                # Herb.kt, Category.kt
│   └── repository/           # Firestore, Auth, DataStore
└── ui/
    ├── theme/                 # ألوان Material 3 + الخطوط
    ├── navigation/            # مسارات التنقل
    ├── components/            # عناصر واجهة قابلة لإعادة الاستخدام
    └── screens/               # كل شاشة في مجلدها الخاص
```

## ما لم يُنقل من النسخة القديمة (يمكن إضافته لاحقًا عند الطلب)
- صفحة "مقارنة الأعشاب" (`compare.html`) وصفحة المساعدة (`help.html`) —
  يمكن بناؤهما كشاشتين إضافيتين بنفس الأسلوب.
- العمل بدون إنترنت بالكامل (offline-first) — البنية التحتية لذلك (DataStore،
  Firestore persistent cache) جاهزة، ويحتاج فقط طبقة مزامنة إضافية.
