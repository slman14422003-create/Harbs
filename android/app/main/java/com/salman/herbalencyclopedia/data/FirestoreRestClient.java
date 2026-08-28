package com.salman.herbalencyclopedia.data;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * عميل بسيط لقراءة مجموعات Cloud Firestore عبر REST API مباشرة،
 * بدون الاعتماد على مكتبة Firebase Android SDK (التي تتطلب ملف
 * google-services.json غير متوفر لدينا) وبدون WebView أو JavaScript.
 *
 * يعتمد فقط على HttpURLConnection و org.json المدمجتين في Android،
 * وهو نفس المشروع ونفس القاعدة (Firestore) التي كان يقرأ منها تطبيق الويب.
 */
public final class FirestoreRestClient {

    private static final String TAG = "FirestoreRestClient";
    private static final String PROJECT_ID = "semoharbs";
    // نفس مفتاح الويب العام المستخدم أصلاً داخل index.html (مفتاح عام مخصص
    // للعميل، وليس سرياً - نفس ما كان مكشوفاً بالفعل في كود الواجهة).
    private static final String API_KEY = "AIzaSyAkVYaspguYs6gXAOaV7xoiesa38nqgm10";
    private static final String BASE_URL =
            "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents/";
    private static final int PAGE_SIZE = 300;
    private static final int TIMEOUT_MS = 15000;

    private FirestoreRestClient() {}

    /**
     * يجلب كل مستندات مجموعة معيّنة (مع دعم الصفحات) ويعيدها كقائمة من
     * JSONObject خام (كل عنصر = مستند واحد بصيغة Firestore REST).
     * يجب استدعاؤها من خيط عمل (worker thread) وليس من الخيط الرئيسي.
     */
    public static List<JSONObject> fetchCollection(String collectionName) throws IOException {
        List<JSONObject> documents = new ArrayList<>();
        String pageToken = null;

        do {
            StringBuilder urlBuilder = new StringBuilder(BASE_URL)
                    .append(collectionName)
                    .append("?pageSize=").append(PAGE_SIZE)
                    .append("&key=").append(API_KEY);
            if (pageToken != null) {
                urlBuilder.append("&pageToken=").append(URLEncoder.encode(pageToken, "UTF-8"));
            }

            JSONObject page = get(urlBuilder.toString());
            JSONArray docsArray = page.optJSONArray("documents");
            if (docsArray != null) {
                try {
                    for (int i = 0; i < docsArray.length(); i++) {
                        documents.add(docsArray.getJSONObject(i));
                    }
                } catch (org.json.JSONException e) {
                    throw new IOException("تعذّر قراءة مستندات الاستجابة", e);
                }
            }
            pageToken = page.optString("nextPageToken", null);
        } while (pageToken != null && !pageToken.isEmpty());

        return documents;
    }

    private static JSONObject get(String urlString) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");

            int code = connection.getResponseCode();
            InputStream stream = (code >= 200 && code < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String body = readStream(stream);

            if (code < 200 || code >= 300) {
                Log.e(TAG, "HTTP " + code + " -> " + body);
                throw new IOException("فشل الاتصال بالخادم (كود " + code + ")");
            }
            return new JSONObject(body);
        } catch (org.json.JSONException e) {
            throw new IOException("تعذّر قراءة استجابة الخادم", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /** يستخرج معرّف المستند (آخر جزء من الحقل "name" الكامل). */
    public static String extractDocumentId(JSONObject document) {
        String fullName = document.optString("name", "");
        int lastSlash = fullName.lastIndexOf('/');
        return lastSlash >= 0 ? fullName.substring(lastSlash + 1) : fullName;
    }

    /** يقرأ قيمة نصية (stringValue) من حقول مستند Firestore بأمان. */
    public static String getString(JSONObject document, String fieldName) {
        JSONObject fields = document.optJSONObject("fields");
        if (fields == null) return "";
        JSONObject field = fields.optJSONObject(fieldName);
        if (field == null) return "";
        String value = field.optString("stringValue", "");
        return value != null ? value : "";
    }
}
