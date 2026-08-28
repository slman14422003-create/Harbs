package com.salman.herbalencyclopedia.model;

import java.io.Serializable;

/** يمثّل تصنيفاً للأعشاب (مثل: مهدئة، هاضمة، مضادة للالتهاب...). */
public class Category implements Serializable {

    private final String id;
    private final String name;

    public Category(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
}
