package com.salman.herbalencyclopedia.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.salman.herbalencyclopedia.R;
import com.salman.herbalencyclopedia.data.BookmarkManager;
import com.salman.herbalencyclopedia.model.Herb;

import java.util.ArrayList;
import java.util.List;

public class HerbAdapter extends RecyclerView.Adapter<HerbAdapter.HerbViewHolder> {

    public interface OnHerbClickListener {
        void onHerbClick(Herb herb);
    }

    /** استدعاء عند الضغط على زر المفضلة داخل بطاقة عشبة. */
    public interface OnBookmarkClickListener {
        void onBookmarkClick(Herb herb);
    }

    private final List<Herb> items = new ArrayList<>();
    private final OnHerbClickListener listener;
    private OnBookmarkClickListener bookmarkListener;

    public HerbAdapter(OnHerbClickListener listener) {
        this.listener = listener;
    }

    public void setOnBookmarkClickListener(OnBookmarkClickListener bookmarkListener) {
        this.bookmarkListener = bookmarkListener;
    }

    /** يعيد رسم أيقونات المفضلة فقط (بعد تبديل حالة عشبة) دون إعادة تحميل كامل القائمة. */
    public void refreshBookmarkIcons() {
        notifyItemRangeChanged(0, items.size());
    }

    /** يستبدل القائمة الحالية بأخرى جديدة (نتيجة بحث/فلترة) مع حساب الفروقات فقط. */
    public void submitList(List<Herb> newItems) {
        HerbDiffCallback diffCallback = new HerbDiffCallback(items, newItems);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public HerbViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_herb, parent, false);
        return new HerbViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HerbViewHolder holder, int position) {
        holder.bind(items.get(position), listener, bookmarkListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HerbViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final TextView name;
        private final TextView preview;
        private final ImageButton bookmarkButton;

        HerbViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.herbImage);
            name = itemView.findViewById(R.id.herbName);
            preview = itemView.findViewById(R.id.herbPreview);
            bookmarkButton = itemView.findViewById(R.id.bookmarkButton);
        }

        void bind(Herb herb, OnHerbClickListener listener, OnBookmarkClickListener bookmarkListener) {
            name.setText(herb.getName());
            String previewText = herb.previewText();
            preview.setText(previewText.isEmpty() ? "—" : previewText);

            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_leaf_placeholder)
                    .error(R.drawable.ic_leaf_placeholder)
                    .transform(new RoundedCorners(24));

            Glide.with(itemView.getContext())
                    .load(herb.getImageUrl())
                    .apply(options)
                    .into(image);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onHerbClick(herb);
            });

            boolean bookmarked = BookmarkManager.getInstance(itemView.getContext()).isBookmarked(herb.getId());
            bookmarkButton.setImageResource(bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
            bookmarkButton.setOnClickListener(v -> {
                if (bookmarkListener != null) bookmarkListener.onBookmarkClick(herb);
            });
        }
    }

    private static class HerbDiffCallback extends DiffUtil.Callback {
        private final List<Herb> oldList;
        private final List<Herb> newList;

        HerbDiffCallback(List<Herb> oldList, List<Herb> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList.size(); }

        @Override
        public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getId().equals(newList.get(newPos).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            Herb a = oldList.get(oldPos);
            Herb b = newList.get(newPos);
            return a.getName().equals(b.getName())
                    && a.previewText().equals(b.previewText());
        }
    }
}
