package com.salman.herbalencyclopedia.model;

import java.io.Serializable;

/**
 * يمثّل عشبة واحدة في الموسوعة.
 * الحقول مطابقة تماماً لأسماء الحقول المخزّنة في مجموعة "herbs" على Firestore
 * حتى تبقى البيانات القديمة صالحة بدون أي تعديل على الخادم.
 */
public class Herb implements Serializable {

    private String id;
    private String name;
    private String categoryId;
    private String benefits;
    private String warnings;
    private String harms;
    private String usage;
    private String notes;
    private String imageUrl;

    public Herb() {
        // مطلوب لبعض أدوات الانعكاس، غير مستخدم مباشرة
    }

    public Herb(String id, String name, String categoryId, String benefits,
                String warnings, String harms, String usage, String notes, String imageUrl) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.benefits = benefits;
        this.warnings = warnings;
        this.harms = harms;
        this.usage = usage;
        this.notes = notes;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategoryId() { return categoryId; }
    public String getBenefits() { return benefits; }
    public String getWarnings() { return warnings; }
    public String getHarms() { return harms; }
    public String getUsage() { return usage; }
    public String getNotes() { return notes; }
    public String getImageUrl() { return imageUrl; }

    /**
     * نص قصير يُستخدم كمعاينة أسفل الاسم في قائمة البطاقات.
     */
    public String previewText() {
        if (benefits != null && !benefits.trim().isEmpty()) {
            return benefits.trim();
        }
        if (usage != null && !usage.trim().isEmpty()) {
            return usage.trim();
        }
        return "";
    }
}
