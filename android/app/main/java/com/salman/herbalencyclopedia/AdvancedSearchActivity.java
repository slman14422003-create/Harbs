package com.salman.herbalencyclopedia;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.salman.herbalencyclopedia.adapter.HerbAdapter;
import com.salman.herbalencyclopedia.data.BookmarkManager;
import com.salman.herbalencyclopedia.data.HerbRepository;
import com.salman.herbalencyclopedia.databinding.ActivityAdvancedSearchBinding;
import com.salman.herbalencyclopedia.model.Category;
import com.salman.herbalencyclopedia.model.Herb;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * بحث متقدم - بديل native كامل لِ SearchSystem (showAdvanced/perform) من
 * نسخة الويب القديمة: بحث بحقل محدد (الاسم/الفوائد/التحذيرات/جميع الحقول)،
 * فلترة حسب التصنيف، حساسية الأحرف، تطابق تام، وسجل بحث محفوظ محلياً
 * (بديل localStorage['search_history']).
 */
public class AdvancedSearchActivity extends BaseActivity {

    private static final String PREFS_NAME = "herb_search_history";
    private static final String KEY_HISTORY = "search_history";
    private static final int MAX_HISTORY = 10;

    // نفس ترتيب/قيم <select id="advSearchField"> في النسخة القديمة
    private static final String[] FIELD_VALUES = {"name", "benefits", "warnings", "all"};

    private ActivityAdvancedSearchBinding binding;
    private List<Category> categories;
    private HerbAdapter resultsAdapter;
    private SharedPreferences historyPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdvancedSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        historyPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        categories = HerbRepository.getInstance().getCachedCategories();
        setupCategorySpinner();
        setupFieldSpinner();
        setupResultsList();
        renderHistoryChips();

        binding.searchButton.setOnClickListener(v -> performSearch());
    }

    private void setupCategorySpinner() {
        List<String> names = new ArrayList<>();
        names.add(getString(R.string.category_all));
        for (Category category : categories) {
            names.add(category.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categorySpinner.setAdapter(adapter);
    }

    private void setupFieldSpinner() {
        String[] labels = new String[]{
                getString(R.string.adv_search_field_name),
                getString(R.string.adv_search_field_benefits),
                getString(R.string.adv_search_field_warnings),
                getString(R.string.adv_search_field_all)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.fieldSpinner.setAdapter(adapter);
    }

    private void setupResultsList() {
        resultsAdapter = new HerbAdapter(herb -> {
            Intent intent = new Intent(this, HerbDetailActivity.class);
            intent.putExtra(HerbDetailActivity.EXTRA_HERB, herb);
            startActivity(intent);
        });
        resultsAdapter.setOnBookmarkClickListener(herb -> {
            BookmarkManager.getInstance(this).toggle(herb.getId());
            resultsAdapter.refreshBookmarkIcons();
        });
        binding.resultsList.setLayoutManager(new LinearLayoutManager(this));
        binding.resultsList.setAdapter(resultsAdapter);
    }

    private void performSearch() {
        TextInputEditText queryField = binding.queryInput;
        String rawQuery = queryField.getText() == null ? "" : queryField.getText().toString().trim();

        if (rawQuery.isEmpty()) {
            Toast.makeText(this, R.string.adv_search_empty_query, Toast.LENGTH_SHORT).show();
            return;
        }

        addToHistory(rawQuery);
        renderHistoryChips();

        boolean caseSensitive = binding.caseSensitiveCheck.isChecked();
        boolean exactMatch = binding.exactMatchCheck.isChecked();
        String searchField = FIELD_VALUES[binding.fieldSpinner.getSelectedItemPosition()];

        int categoryIndex = binding.categorySpinner.getSelectedItemPosition();
        String categoryId = categoryIndex <= 0 ? null : categories.get(categoryIndex - 1).getId();

        String query = caseSensitive ? rawQuery : lower(rawQuery);

        List<Herb> results = new ArrayList<>();
        for (Herb herb : HerbRepository.getInstance().getCachedHerbs()) {
            if (categoryId != null && !categoryId.equals(herb.getCategoryId())) continue;
            if (matchesField(herb, searchField, query, caseSensitive, exactMatch)) {
                results.add(herb);
            }
        }

        resultsAdapter.submitList(results);
        binding.resultsHeader.setVisibility(View.VISIBLE);
        binding.resultsHeader.setText(results.isEmpty()
                ? getString(R.string.adv_search_no_results)
                : getString(R.string.adv_search_results_format, results.size()));
    }

    private boolean matchesField(Herb herb, String field, String query, boolean caseSensitive, boolean exactMatch) {
        switch (field) {
            case "name":
                return matches(herb.getName(), query, caseSensitive, exactMatch);
            case "benefits":
                return matches(herb.getBenefits(), query, caseSensitive, exactMatch);
            case "warnings":
                return matches(herb.getWarnings(), query, caseSensitive, exactMatch);
            default: // "all" - جميع الحقول
                return matches(herb.getName(), query, caseSensitive, exactMatch)
                        || matches(herb.getBenefits(), query, caseSensitive, exactMatch)
                        || matches(herb.getWarnings(), query, caseSensitive, exactMatch)
                        || matches(herb.getHarms(), query, caseSensitive, exactMatch)
                        || matches(herb.getUsage(), query, caseSensitive, exactMatch)
                        || matches(herb.getNotes(), query, caseSensitive, exactMatch);
        }
    }

    private boolean matches(String value, String query, boolean caseSensitive, boolean exactMatch) {
        String v = value == null ? "" : value;
        v = caseSensitive ? v : lower(v);
        return exactMatch ? v.equals(query) : v.contains(query);
    }

    private String lower(String s) {
        return s.toLowerCase(Locale.forLanguageTag("ar"));
    }

    // ---------------------------------------------------------------
    // سجل البحث (بديل SearchSystem.loadHistory/saveHistory/addToHistory)
    // ---------------------------------------------------------------

    private List<String> loadHistory() {
        List<String> history = new ArrayList<>();
        String raw = historyPrefs.getString(KEY_HISTORY, null);
        if (raw == null) return history;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                history.add(array.getString(i));
            }
        } catch (JSONException ignored) {
        }
        return history;
    }

    private void addToHistory(String query) {
        List<String> history = loadHistory();
        history.remove(query);
        history.add(0, query);
        if (history.size() > MAX_HISTORY) {
            history = history.subList(0, MAX_HISTORY);
        }
        JSONArray array = new JSONArray();
        for (String q : history) array.put(q);
        historyPrefs.edit().putString(KEY_HISTORY, array.toString()).apply();
    }

    private void renderHistoryChips() {
        binding.historyChipGroup.removeAllViews();
        List<String> history = loadHistory();
        for (String query : history) {
            Chip chip = new Chip(this);
            chip.setText(query);
            chip.setOnClickListener(v -> {
                binding.queryInput.setText(query);
                performSearch();
            });
            binding.historyChipGroup.addView(chip);
        }
    }
}
