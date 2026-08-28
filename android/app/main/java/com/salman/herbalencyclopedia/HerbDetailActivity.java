package com.salman.herbalencyclopedia;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.salman.herbalencyclopedia.data.HerbRepository;
import com.salman.herbalencyclopedia.databinding.ActivityHerbDetailBinding;
import com.salman.herbalencyclopedia.model.Herb;

/** شاشة تفاصيل عشبة واحدة - عرض أصلي كامل بلا WebView. */
public class HerbDetailActivity extends AppCompatActivity {

    public static final String EXTRA_HERB = "extra_herb";

    private ActivityHerbDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHerbDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Herb herb = (Herb) getIntent().getSerializableExtra(EXTRA_HERB);
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
