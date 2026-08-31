package com.salman.herbalencyclopedia.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * يضغط صورة العشبة ويحوّلها لـ data URL (base64) يُخزَّن مباشرة داخل
 * مستند Firestore - راجع DataUriFetcher.kt لكيفية عرضها لاحقاً.
 *
 * تحسينات عن الآلية السابقة (كانت داخل AdminEditHerbScreen مباشرة):
 *  1) لا تُحمَّل الصورة الأصلية كاملة في الذاكرة أبداً: نقرأ أبعادها أولاً
 *     (inJustDecodeBounds) ثم نفك تشفيرها مباشرة بحجم مصغَّر مناسب
 *     (inSampleSize) - يمنع نفاد الذاكرة (OOM) مع الصور عالية الدقة من
 *     كاميرات الهواتف الحديثة (12/48 ميجابكسل وأكثر).
 *  2) تصحيح اتجاه الصورة حسب بيانات EXIF قبل الضغط - بعض الكاميرات تحفظ
 *     الصورة بشكلها الأصلي مع علم دوران فقط بالبيانات الوصفية، وبدون هذا
 *     التصحيح تُخزَّن الصورة مقلوبة على جنبها داخل قاعدة البيانات.
 *  3) الإخراج بصيغة WEBP بدل JPEG: نسبة ضغط أفضل بنفس الجودة تقريباً،
 *     أي حجم بيانات أصغر يُخزَّن في Firestore ويُنقل عبر الشبكة لكل
 *     المستخدمين عند المزامنة - توفير مساحة حقيقي دون فرق يُذكر بالجودة.
 *  4) العملية بالكامل suspend وتُنفَّذ على Dispatchers.Default (خيط
 *     خلفي) بدل تنفيذها بشكل متزامن داخل معالج اختيار الصورة، الذي كان
 *     يُجمِّد واجهة المستخدم لحظياً مع الصور الكبيرة.
 */
object ImageCompressor {

    // Firestore يرفض أي مستند يتجاوز 1 ميجابايت إجمالاً (كل الحقول
    // مجتمعة)، وترميز base64 يكبّر حجم الصورة الأصلي بحوالي 33%. نحجز
    // هامشاً كافياً لبقية حقول العشبة.
    private const val MAX_DATA_URL_BYTES = 550_000
    private const val INITIAL_MAX_DIMENSION = 900
    private const val MIN_MAX_DIMENSION = 400
    private const val INITIAL_QUALITY = 82
    private const val MIN_QUALITY = 35

    suspend fun compressToDataUrl(context: Context, uri: Uri): String? = withContext(Dispatchers.Default) {
        runCatching {
            val bounds = decodeBounds(context, uri) ?: return@runCatching null
            val sampleSize = calculateInSampleSize(bounds.first, bounds.second, INITIAL_MAX_DIMENSION)
            val sampled = decodeSampledBitmap(context, uri, sampleSize) ?: return@runCatching null
            val bitmap = correctOrientation(context, uri, sampled)

            var maxDimension = INITIAL_MAX_DIMENSION
            var quality = INITIAL_QUALITY
            var dataUrl: String

            while (true) {
                val scale = minOf(1f, maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height))
                val scaled = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt().coerceAtLeast(1),
                        (bitmap.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else bitmap

                val out = ByteArrayOutputStream()
                scaled.compress(webpFormat(), quality, out)
                if (scaled !== bitmap) scaled.recycle()
                dataUrl = "data:image/webp;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)

                if (dataUrl.length <= MAX_DATA_URL_BYTES || (quality <= MIN_QUALITY && maxDimension <= MIN_MAX_DIMENSION)) break

                quality = if (quality > MIN_QUALITY) quality - 15 else quality
                if (quality <= MIN_QUALITY) {
                    maxDimension = (maxDimension * 0.75f).toInt().coerceAtLeast(MIN_MAX_DIMENSION)
                }
            }

            bitmap.recycle()
            // إذا ظلت الصورة أكبر من الحد المسموح حتى بعد أقصى ضغط ممكن، لا
            // نرجع نصاً سيفشل حفظه لاحقاً بصمت - نرجع null ليظهر خطأ واضح.
            if (dataUrl.length > MAX_DATA_URL_BYTES) null else dataUrl
        }.getOrNull()
    }

    private fun decodeBounds(context: Context, uri: Uri): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        return options.outWidth to options.outHeight
    }

    private fun calculateInSampleSize(width: Int, height: Int, targetMax: Int): Int {
        var sampleSize = 1
        var w = width
        var h = height
        while (maxOf(w, h) / 2 >= targetMax) {
            sampleSize *= 2
            w /= 2
            h /= 2
        }
        return sampleSize
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, sampleSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    /** يدور الصورة حسب بيانات EXIF إن لزم، ويحرر البتمابات الوسيطة غير المستخدمة. */
    private fun correctOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return runCatching {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) bitmap.recycle()
            rotated
        }.getOrDefault(bitmap)
    }

    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
}
