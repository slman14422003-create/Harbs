package com.salman.herbalencyclopedia;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
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
import com.salman.herbalencyclopedia.databinding.ActivityMainBinding;
import com.salman.herbalencyclopedia.model.Category;
import com.salman.herbalencyclopedia.model.Herb;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * الشاشة الرئيسية - تطبيق أندرويد حقيقي (Java + Views أصلية)، بلا WebView
 * وبلا أي ملفات HTML أو CSS. تعرض قائمة الأعشاب مباشرة من Firestore،
 * مع بحث فوري وفلترة حسب التصنيف.
 */
public class MainActivity extends AppCompatActivity {

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
        if (item.getItemId() == R.id.action_bookmarks) {
            startActivity(new Intent(this, BookmarksActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        HerbRepository.getInstance().loadAll(new HerbRepository.Callback<List<Herb>>() {
            @Override
            public void onSuccess(List<Herb> herbs) {
                binding.swipeRefresh.setRefreshing(false);
                allHerbs.clear();
                allHerbs.addAll(herbs);
                buildCategoryChips();
                applyFilters();
                showContent();
            }

            @Override
            public void onError(String message) {
                binding.swipeRefresh.setRefreshing(false);
                showError(message);
            }
        });
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
