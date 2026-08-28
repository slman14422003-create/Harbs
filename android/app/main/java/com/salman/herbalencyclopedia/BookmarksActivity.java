package com.salman.herbalencyclopedia;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.salman.herbalencyclopedia.adapter.HerbAdapter;
import com.salman.herbalencyclopedia.data.BookmarkManager;
import com.salman.herbalencyclopedia.data.HerbRepository;
import com.salman.herbalencyclopedia.databinding.ActivityBookmarksBinding;
import com.salman.herbalencyclopedia.model.Herb;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * شاشة "المفضلة" - تعرض فقط الأعشاب التي أضافها المستخدم للمفضلة.
 * إعادة تنفيذ أصلية لميزة showBookmarksModal/clearBookmarks من نسخة الويب القديمة.
 */
public class BookmarksActivity extends AppCompatActivity {

    private ActivityBookmarksBinding binding;
    private HerbAdapter adapter;
    private BookmarkManager bookmarkManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookmarksBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bookmarkManager = BookmarkManager.getInstance(this);

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new HerbAdapter(herb -> {
            Intent intent = new Intent(this, HerbDetailActivity.class);
            intent.putExtra(HerbDetailActivity.EXTRA_HERB, herb);
            startActivity(intent);
        });
        adapter.setOnBookmarkClickListener(this::toggleBookmark);
        binding.bookmarkList.setLayoutManager(new LinearLayoutManager(this));
        binding.bookmarkList.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bookmarks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clear_bookmarks) {
            confirmClearBookmarks();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleBookmark(Herb herb) {
        bookmarkManager.toggle(herb.getId());
        refreshList();
    }

    private void confirmClearBookmarks() {
        if (bookmarkManager.count() == 0) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_bookmarks_confirm_title)
                .setMessage(R.string.clear_bookmarks_confirm_message)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                    bookmarkManager.clearAll();
                    refreshList();
                    Toast.makeText(this, R.string.bookmarks_cleared, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void refreshList() {
        Set<String> bookmarkedIds = bookmarkManager.getBookmarkedIds();
        List<Herb> bookmarkedHerbs = new ArrayList<>();
        for (Herb herb : HerbRepository.getInstance().getCachedHerbs()) {
            if (bookmarkedIds.contains(herb.getId())) {
                bookmarkedHerbs.add(herb);
            }
        }
        adapter.submitList(bookmarkedHerbs);
        binding.emptyState.setVisibility(bookmarkedHerbs.isEmpty() ? View.VISIBLE : View.GONE);
        binding.bookmarkList.setVisibility(bookmarkedHerbs.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
