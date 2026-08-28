package com.salman.herbalencyclopedia;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.salman.herbalencyclopedia.data.BookmarkManager;
import com.salman.herbalencyclopedia.data.HerbRepository;
import com.salman.herbalencyclopedia.databinding.ActivityHerbDetailBinding;
import com.salman.herbalencyclopedia.model.Herb;

/** شاشة تفاصيل عشبة واحدة - عرض أصلي كامل بلا WebView. */
public class HerbDetailActivity extends AppCompatActivity {

    public static final String EXTRA_HERB = "extra_herb";

    private ActivityHerbDetailBinding binding;
    private BookmarkManager bookmarkManager;
    private Herb herb;
    private Menu optionsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHerbDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bookmarkManager = BookmarkManager.getInstance(this);

        herb = (Herb) getIntent().getSerializableExtra(EXTRA_HERB);
        if (herb == null) {
            finish();
            return;
        }

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        bind(herb);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_herb_detail, menu);
        optionsMenu = menu;
        updateBookmarkIcon();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_bookmark) {
            toggleBookmark();
            return true;
        } else if (id == R.id.action_share) {
            shareHerb();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleBookmark() {
        boolean nowBookmarked = bookmarkManager.toggle(herb.getId());
        updateBookmarkIcon();
        String message = getString(nowBookmarked ? R.string.bookmark_added : R.string.bookmark_removed, herb.getName());
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateBookmarkIcon() {
        if (optionsMenu == null) return;
        MenuItem bookmarkItem = optionsMenu.findItem(R.id.action_bookmark);
        if (bookmarkItem == null) return;
        boolean bookmarked = bookmarkManager.isBookmarked(herb.getId());
        bookmarkItem.setIcon(bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
        bookmarkItem.setTitle(bookmarked ? R.string.action_bookmark_remove : R.string.action_bookmark);
    }

    /** مشاركة نص العشبة عبر أي تطبيق مثبت (واتساب، رسائل، إلخ) - بديل أصلي لميزة shareAppBtn/printHerbDetail. */
    private void shareHerb() {
        String benefits = valueOrDash(herb.getBenefits());
        String warnings = valueOrDash(herb.getWarnings());
        String usage = valueOrDash(herb.getUsage());
        String text = getString(R.string.share_herb_format, herb.getName(), benefits, warnings, usage);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, herb.getName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
    }

    private String valueOrDash(String value) {
        return (value == null || value.trim().isEmpty()) ? getString(R.string.value_not_available) : value.trim();
    }

    private void bind(Herb herb) {
        binding.collapsingToolbar.setTitle(herb.getName());

        String categoryName = HerbRepository.getInstance().categoryNameFor(herb.getCategoryId());
        binding.categoryLabel.setText(categoryName.isEmpty() ? getString(R.string.category_unknown) : categoryName);

        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.ic_leaf_placeholder)
                .error(R.drawable.ic_leaf_placeholder)
                .transform(new RoundedCorners(32));
        Glide.with(this).load(herb.getImageUrl()).apply(options).into(binding.herbImage);

        bindSection(binding.benefitsLabel, binding.benefitsText, R.string.section_benefits, herb.getBenefits());
        bindSection(binding.warningsLabel, binding.warningsText, R.string.section_warnings, herb.getWarnings());
        bindSection(binding.harmsLabel, binding.harmsText, R.string.section_harms, herb.getHarms());
        bindSection(binding.usageLabel, binding.usageText, R.string.section_usage, herb.getUsage());
        bindSection(binding.notesLabel, binding.notesText, R.string.section_notes, herb.getNotes());
    }

    private void bindSection(android.widget.TextView label, android.widget.TextView content, int labelRes, String value) {
        label.setText(labelRes);
        boolean hasValue = value != null && !value.trim().isEmpty();
        content.setText(hasValue ? value.trim() : getString(R.string.value_not_available));
    }
}
