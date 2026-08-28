package com.salman.herbalencyclopedia;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;


import com.salman.herbalencyclopedia.data.HerbRepository;
import com.salman.herbalencyclopedia.databinding.ActivityCompareBinding;
import com.salman.herbalencyclopedia.model.Herb;

import java.util.ArrayList;
import java.util.List;

/** مقارنة عشبتين جنباً إلى جنب - بديل أصلي لصفحة compare.html القديمة. */
public class CompareActivity extends BaseActivity {

    private ActivityCompareBinding binding;
    private final List<Herb> herbs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCompareBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        herbs.addAll(HerbRepository.getInstance().getCachedHerbs());

        if (herbs.size() < 2) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.compareContent.setVisibility(View.GONE);
            return;
        }

        List<String> names = new ArrayList<>();
        for (Herb herb : herbs) names.add(herb.getName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.firstHerbSpinner.setAdapter(adapter);
        binding.secondHerbSpinner.setAdapter(adapter);
        binding.secondHerbSpinner.setSelection(Math.min(1, names.size() - 1));

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                renderComparison();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        binding.firstHerbSpinner.setOnItemSelectedListener(listener);
        binding.secondHerbSpinner.setOnItemSelectedListener(listener);

        renderComparison();
    }

    private void renderComparison() {
        int firstIndex = binding.firstHerbSpinner.getSelectedItemPosition();
        int secondIndex = binding.secondHerbSpinner.getSelectedItemPosition();
        if (firstIndex < 0 || secondIndex < 0 || firstIndex >= herbs.size() || secondIndex >= herbs.size()) return;

        Herb first = herbs.get(firstIndex);
        Herb second = herbs.get(secondIndex);

        binding.firstHerbName.setText(first.getName());
        binding.secondHerbName.setText(second.getName());

        setRow(binding.benefitsFirst, binding.benefitsSecond, first.getBenefits(), second.getBenefits());
        setRow(binding.warningsFirst, binding.warningsSecond, first.getWarnings(), second.getWarnings());
        setRow(binding.harmsFirst, binding.harmsSecond, first.getHarms(), second.getHarms());
        setRow(binding.usageFirst, binding.usageSecond, first.getUsage(), second.getUsage());
    }

    private void setRow(android.widget.TextView firstView, android.widget.TextView secondView, String first, String second) {
        firstView.setText(valueOrDash(first));
        secondView.setText(valueOrDash(second));
    }

    private String valueOrDash(String value) {
        return (value == null || value.trim().isEmpty()) ? "—" : value.trim();
    }
}
