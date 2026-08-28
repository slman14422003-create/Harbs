package com.salman.herbalencyclopedia;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;

import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.salman.herbalencyclopedia.adapter.HerbAdapter;
import com.salman.herbalencyclopedia.data.BookmarkManager;
import com.salman.herbalencyclopedia.data.HerbRepository;
import com.salman.herbalencyclopedia.data.SettingsManager;
import com.salman.herbalencyclopedia.databinding.ActivityMainBinding;
import com.salman.herbalencyclopedia.model.Category;
import com.salman.herbalencyclopedia.model.Herb;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * الشاشة الرئيسية - تطبيق أندرويد حقيقي (Java + Views أصلية)، بلا WebView
 * وبلا أي ملفات HTML أو CSS. تعرض قائمة الأعشاب مباشرة من Firestore،
 * مع بحث فوري وفلترة حسب التصنيف.
 */
public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;
    private HerbAdapter adapter;

    private final List<Herb> allHerbs = new ArrayList<>();
    private String selectedCategoryId = null; // null = كل التصنيفات
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        splashScreen.setKeepOnScreenCondition(() -> binding.progressBar.getVisibility() == View.VISIBLE
                && allHerbs.isEmpty());

        setupEdgeToEdge();
        setupToolbar();
        setupList();
        setupSearch();
        setupSwipeRefresh();
        setupRetry();

        // يعرض آخر نسخة محفوظة محلياً فوراً (إن وُجدت) قبل انتظار الشبكة،
        // بديل native لِـ loadDataFromLocalCache القديمة.
        if (allHerbs.isEmpty() && HerbRepository.getInstance().loadFromCacheOnly(this)) {
            allHerbs.addAll(HerbRepository.getInstance().getCachedHerbs());
            buildCategoryChips();
            applyFilters();
            showContent();
            updateLastUpdateBadge();
        }

        registerVisit();
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null && !allHerbs.isEmpty()) {
            adapter.refreshBookmarkIcons();
        }
    }

    private void setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), systemBars.top, view.getPaddingRight(), 0);
            binding.navBarScrim.getLayoutParams().height = systemBars.bottom;
            binding.navBarScrim.requestLayout();
            return insets;
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        });
        binding.compareFab.setOnClickListener(v -> {
            Intent intent = new Intent(this, CompareActivity.class);
            startActivity(intent);
        });
    }

    private void setupList() {
        adapter = new HerbAdapter(herb -> {
            Intent intent = new Intent(this, HerbDetailActivity.class);
            intent.putExtra(HerbDetailActivity.EXTRA_HERB, herb);
            startActivity(intent);
        });
        adapter.setOnBookmarkClickListener(this::onBookmarkToggled);
        binding.herbList.setLayoutManager(new LinearLayoutManager(this));
        binding.herbList.setAdapter(adapter);
    }

    private void onBookmarkToggled(Herb herb) {
        boolean nowBookmarked = BookmarkManager.getInstance(this).toggle(herb.getId());
        adapter.refreshBookmarkIcons();
        String message = getString(nowBookmarked ? R.string.bookmark_added : R.string.bookmark_removed, herb.getName());
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_bookmarks) {
            startActivity(new Intent(this, BookmarksActivity.class));
            return true;
        } else if (id == R.id.action_advanced_search) {
            startActivity(new Intent(this, AdvancedSearchActivity.class));
            return true;
        } else if (id == R.id.action_theme_toggle) {
            SettingsManager.getInstance(this).toggleDarkMode();
            recreate();
            return true;
        } else if (id == R.id.action_font_size) {
            onFontSizeToggle();
            return true;
        } else if (id == R.id.action_stats) {
            showStatsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** مطابق لِـ fontSizeToggleBtn.click في النسخة القديمة (cycleFontSize). */
    private void onFontSizeToggle() {
        SettingsManager settings = SettingsManager.getInstance(this);
        String next = settings.cycleFontSize();
        Toast.makeText(this, getString(R.string.font_size_changed, settings.fontSizeLabel(next)), Toast.LENGTH_SHORT).show();
        recreate();
    }

    /** مطابق لِـ visitor_count في localStorage (تسجيل الزيارة عند فتح التطبيق). */
    private void registerVisit() {
        SharedPreferences prefs = getSharedPreferences("herb_stats", MODE_PRIVATE);
        int visits = prefs.getInt("visitor_count", 0) + 1;
        prefs.edit().putInt("visitor_count", visits).apply();
    }

    /** يدمج showVisitorStats() و showVisitorCategories() القديمتين بحوار واحد. */
    private void showStatsDialog() {
        SharedPreferences prefs = getSharedPreferences("herb_stats", MODE_PRIVATE);
        int visits = prefs.getInt("visitor_count", 1);
        List<Category> categories = HerbRepository.getInstance().getCachedCategories();
        int bookmarkCount = BookmarkManager.getInstance(this).count();

        StringBuilder message = new StringBuilder();
        message.append(getString(R.string.stats_visits_format, visits)).append("\n");
        message.append(getString(R.string.stats_herbs_format, allHerbs.size())).append("\n");
        message.append(getString(R.string.stats_categories_format, categories.size())).append("\n");
        message.append(getString(R.string.stats_bookmarks_format, bookmarkCount));

        if (!categories.isEmpty()) {
            message.append("\n\n").append(getString(R.string.stats_by_category_title));
            for (Category category : categories) {
                int count = 0;
                for (Herb herb : allHerbs) {
                    if (category.getId().equals(herb.getCategoryId())) count++;
                }
                message.append("\n").append(getString(R.string.stats_category_row_format, category.getName(), count));
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.stats_title)
                .setMessage(message.toString())
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
    }

    private void setupSearch() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText == null ? "" : newText.trim();
                applyFilters();
                return true;
            }
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.brand_primary);
        binding.swipeRefresh.setOnRefreshListener(this::loadData);
    }

    private void setupRetry() {
        binding.retryButton.setOnClickListener(v -> loadData());
    }

    private void loadData() {
        showLoading();
        HerbRepository.getInstance().loadAllWithCache(this, new HerbRepository.Callback<List<Herb>>() {
            @Override
            public void onSuccess(List<Herb> herbs) {
                binding.swipeRefresh.setRefreshing(false);
                allHerbs.clear();
                allHerbs.addAll(herbs);
                buildCategoryChips();
                applyFilters();
                showContent();
                updateLastUpdateBadge();
                if (HerbRepository.getInstance().wasLastLoadServedFromLocalCache()) {
                    com.google.android.material.snackbar.Snackbar
                            .make(binding.getRoot(), R.string.offline_using_cache,
                                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                            .show();
                }
            }

            @Override
            public void onError(String message) {
                binding.swipeRefresh.setRefreshing(false);
                showError(message);
            }
        });
    }

    /** مطابق لِـ updateLastUpdateBadge() القديمة، مبني على الكاش المحلي الفعلي. */
    private void updateLastUpdateBadge() {
        long millis = HerbRepository.getInstance().lastUpdateMillis(this);
        if (millis <= 0) {
            binding.lastUpdateBadge.setText(R.string.last_update_never);
            return;
        }
        String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.forLanguageTag("ar")).format(new Date(millis));
        binding.lastUpdateBadge.setText(getString(R.string.last_update_format, formatted));
    }

    private void buildCategoryChips() {
        binding.categoryChipGroup.removeAllViews();

        Chip allChip = new Chip(this);
        allChip.setText(R.string.category_all);
        allChip.setCheckable(true);
        allChip.setChecked(selectedCategoryId == null);
        allChip.setOnClickListener(v -> {
            selectedCategoryId = null;
            applyFilters();
        });
        binding.categoryChipGroup.addView(allChip);

        List<Category> categories = HerbRepository.getInstance().getCachedCategories();
        for (Category category : categories) {
            Chip chip = new Chip(this);
            chip.setText(category.getName());
            chip.setCheckable(true);
            chip.setChecked(category.getId().equals(selectedCategoryId));
            chip.setOnClickListener(v -> {
                selectedCategoryId = category.getId();
                applyFilters();
            });
            binding.categoryChipGroup.addView(chip);
        }
    }

    private void applyFilters() {
        List<Herb> filtered = new ArrayList<>();
        String query = currentQuery.toLowerCase(Locale.forLanguageTag("ar"));

        for (Herb herb : allHerbs) {
            boolean matchesCategory = selectedCategoryId == null
                    || selectedCategoryId.equals(herb.getCategoryId());
            if (!matchesCategory) continue;

            boolean matchesQuery = query.isEmpty()
                    || containsIgnoreCase(herb.getName(), query)
                    || containsIgnoreCase(herb.getBenefits(), query)
                    || containsIgnoreCase(herb.getUsage(), query);
            if (matchesQuery) {
                filtered.add(herb);
            }
        }

        adapter.submitList(filtered);
        binding.emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.herbCount.setText(getString(R.string.herb_count_format, filtered.size()));
    }

    private boolean containsIgnoreCase(String source, String query) {
        return source != null && source.toLowerCase(Locale.forLanguageTag("ar")).contains(query);
    }

    private void showLoading() {
        binding.progressBar.setVisibility(allHerbs.isEmpty() ? View.VISIBLE : View.GONE);
        binding.errorLayout.setVisibility(View.GONE);
    }

    private void showContent() {
        binding.progressBar.setVisibility(View.GONE);
        binding.errorLayout.setVisibility(View.GONE);
        binding.contentGroup.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        if (allHerbs.isEmpty()) {
            binding.contentGroup.setVisibility(View.GONE);
            binding.errorLayout.setVisibility(View.VISIBLE);
            binding.errorMessage.setText(message);
        } else {
            com.google.android.material.snackbar.Snackbar
                    .make(binding.getRoot(), message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                    .show();
        }
    }
}
