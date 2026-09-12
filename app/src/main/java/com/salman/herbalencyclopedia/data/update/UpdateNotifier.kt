package com.salman.herbalencyclopedia.data.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.salman.herbalencyclopedia.MainActivity
import com.salman.herbalencyclopedia.R
import com.salman.herbalencyclopedia.data.model.AppUpdateInfo

/**
 * قدرة جديدة: إشعار نظام تلقائي عند اكتشاف تحديث متوفر للتطبيق.
 *
 * حتى الآن كان التحقق من التحديث يحدث فقط عند ضغط المستخدم يدوياً على "تحقق
 * من التحديثات" بشاشة الإعدادات — لا يوجد أي تنبيه تلقائي إن لم يفتح
 * المستخدم تلك الشاشة بنفسه، حتى لو مرّت أشهر على وجود إصدار أحدث فعلياً.
 * [com.salman.herbalencyclopedia.ui.AppViewModel.checkForUpdateSilently] يستدعي
 * هذا الكائن تلقائياً في كل مرة يُفتح فيها التطبيق (init{} الخاص بـ
 * AppViewModel، الذي يُنشأ مرة واحدة فعلياً لكل فتحة للتطبيق)، فيصل التنبيه
 * للمستخدم بلا حاجة لأي إجراء منه.
 *
 * يستخدم عمداً *نفس* معرّف القناة (CHANNEL_ID) المستخدم أصلاً في
 * [UpdateDownloadService] لإشعارات تقدّم/جاهزية التحميل — كلاهما يخص نفس
 * الموضوع العملي من منظور المستخدم ("تحديثات التطبيق")، فتظهر كل إشعارات
 * التحديث تحت مفتاح تفعيل/تعطيل واحد في إعدادات النظام بدل قناتين منفصلتين
 * لغرض واحد. [ensureChannel] هنا مستقلة تماماً عن تلك الموجودة في الخدمة
 * (private هناك، ولا داعي لمشاركتها) لأن هذا الإشعار قد يظهر أولاً — قبل أن
 * يبدأ المستخدم أي تحميل فعلي يُنشئ القناة من جانب الخدمة.
 */
object UpdateNotifier {
    private const val CHANNEL_ID = "update_downloads"
    private const val NOTIFICATION_ID = 4103

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تحديثات التطبيق",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات تحميل وتثبيت تحديثات التطبيق"
            }
            manager?.createNotificationChannel(channel)
        }
    }

    /** أندرويد 13+ يتطلّب إذناً صريحاً؛ إن لم يُمنح بعد، لا يُظهَر شيء (نفس منطق UpdateDownloadService — التحقق نفسه يستمر يعمل، فقط الإشعار المرئي لا يظهر). */
    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * يُظهر إشعاراً واحداً بتوفّر [info]. الضغط عليه يفتح التطبيق مباشرة (نفس
     * نقطة الدخول العادية) حيث يستطيع المستخدم التوجّه لشاشة الإعدادات
     * لتحميل التحديث — بلا حاجة لإعداد رابط عميق (Deep Link) مخصّص لشاشة
     * بعينها لتحقيق هذه الميزة بأبسط شكل ممكن.
     */
    fun notifyUpdateAvailable(context: Context, info: AppUpdateInfo) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = "النسخة ${info.versionName} من التطبيق متوفرة الآن — اضغط للتحميل من شاشة الإعدادات"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("تحديث جديد متوفر 🌿")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
