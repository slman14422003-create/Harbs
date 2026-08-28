package com.salman.herbalencyclopedia;

import android.os.Bundle;


import com.salman.herbalencyclopedia.databinding.ActivityAboutBinding;

/** شاشة "حول / الخصوصية / المساعدة" - نص أصلي عادي بدل صفحات HTML الثلاث القديمة. */
public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAboutBinding binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
}
