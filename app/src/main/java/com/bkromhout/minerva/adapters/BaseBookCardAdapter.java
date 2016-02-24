package com.bkromhout.minerva.adapters;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.*;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.events.BookCardClickEvent;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.realm.RBookListItem;
import com.bkromhout.minerva.util.RippleForegroundListener;
import com.bkromhout.minerva.util.Util;
import com.greenfrvr.hashtagview.HashtagView;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmObject;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;

/**
 * Base adapter for book card adapters to extend.
 */
public abstract class BaseBookCardAdapter<T extends RealmObject, VH extends RecyclerView.ViewHolder> extends
        RealmBasedRecyclerViewAdapter<T, VH> {
    /**
     * Help our cards ripple.
     */
    RippleForegroundListener rippleFgListener = new RippleForegroundListener(R.id.ripple_foreground_view);
    /**
     * Whether or not this adapter may allow item drags to start.
     */
    boolean mayStartDrags = false;

    public BaseBookCardAdapter(Context context, RealmResults<T> realmResults, boolean automaticUpdate,
                               boolean animateResults) {
        super(context, realmResults, automaticUpdate, animateResults, null);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        // Get/check variables needed to help bind.
        RBook book = getBookFromT(realmResults.get(position));
        if (book == null || rippleFgListener == null) throw new IllegalArgumentException();
        boolean isSelected = selectedPositions.contains(position);

        // Fill in view holder based on its real type.
        if (viewHolder instanceof NormalCardVH) {
            bindNormalBookCard((NormalCardVH) viewHolder, position, book, isSelected);
        } else if (viewHolder instanceof CompactCardVH) {
            bindCompactBookCard((CompactCardVH) viewHolder, position, book, isSelected);
        } else if (viewHolder instanceof NoCoverCardVH) {
            bindNoCoverBookCard((NoCoverCardVH) viewHolder, position, book, isSelected);
        } else {
            throw new IllegalArgumentException("Invalid viewHolder");
        }
    }

    /**
     * Bind a {@link NormalCardVH}.
     * @param resolvedVH Normal book card view holder.
     * @param position   Item position.
     * @param book       Book to populate item with.
     * @param isSelected Whether or not the item is selected.
     */
    private void bindNormalBookCard(NormalCardVH resolvedVH, int position, RBook book, boolean isSelected) {
        ((CardView) resolvedVH.content.getParent()).setCardBackgroundColor(
                isSelected ? R.color.selectedCard : R.color.cardview_dark_background);

        // Make the card ripple when touched.
        resolvedVH.content.setOnTouchListener(rippleFgListener);

        // Set card click handler.
        resolvedVH.content.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.NORMAL, book.getRelPath(), position)));

        // Set card long click handler.
        resolvedVH.content.setOnLongClickListener(v -> {
            if (mayStartDrags) {
                startDragListener.startDragging(resolvedVH);
            } else {
                EventBus.getDefault().post(new BookCardClickEvent(BookCardClickEvent.Type.LONG, book.getRelPath(),
                        position));
            }
            return true;
        });

        // Set info button handler.
        resolvedVH.btnInfo.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.INFO, book.getRelPath(), position)));

        // Set quick tag button handler.
        resolvedVH.btnQuickTag.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.QUICK_TAG, book.getRelPath(), position)));

        // Set cover image.
        if (book.isHasCoverImage()) {
            // TODO something here
        }

        // Fill in data.
        resolvedVH.tvTitle.setText(book.getTitle());
        resolvedVH.tvAuthor.setText(book.getAuthor());
        resolvedVH.tvDesc.setText(book.getDesc());
        resolvedVH.rbRating.setRating(book.getRating());
        resolvedVH.htvTags.setData(Util.stringToList(book.getTags(), ";;"));
    }

    /**
     * Bind a {@link CompactCardVH}.
     * @param resolvedVH Compact book card view holder.
     * @param position   Item position.
     * @param book       Book to populate item with.
     * @param isSelected Whether or not the item is selected.
     */
    private void bindCompactBookCard(CompactCardVH resolvedVH, int position, RBook book, boolean isSelected) {
        ((CardView) resolvedVH.content.getParent()).setCardBackgroundColor(
                isSelected ? R.color.selectedCard : R.color.cardview_dark_background);

        // Make the card ripple when touched.
        resolvedVH.content.setOnTouchListener(rippleFgListener);

        // Set card click handler.
        resolvedVH.content.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.NORMAL, book.getRelPath(), position)));

        // Set card long click handler.
        resolvedVH.content.setOnLongClickListener(v -> {
            if (mayStartDrags) {
                startDragListener.startDragging(resolvedVH);
            } else {
                EventBus.getDefault().post(new BookCardClickEvent(BookCardClickEvent.Type.LONG, book.getRelPath(),
                        position));
            }
            return true;
        });

        // Set info button handler.
        resolvedVH.btnInfo.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.INFO, book.getRelPath(), position)));

        // Fill in data.
        resolvedVH.tvTitle.setText(book.getTitle());
        resolvedVH.tvAuthor.setText(book.getAuthor());
        resolvedVH.tvRating.setText(String.valueOf(book.getRating()));
    }

    /**
     * Bind a {@link NoCoverCardVH}.
     * @param resolvedVH No-cover book card view holder.
     * @param position   Item position.
     * @param book       Book to populate item with.
     * @param isSelected Whether or not the item is selected.
     */
    private void bindNoCoverBookCard(NoCoverCardVH resolvedVH, int position, RBook book, boolean isSelected) {
        ((CardView) resolvedVH.content.getParent()).setCardBackgroundColor(
                isSelected ? R.color.selectedCard : R.color.cardview_dark_background);

        // Make the card ripple when touched.
        resolvedVH.content.setOnTouchListener(rippleFgListener);

        // Set card click handler.
        resolvedVH.content.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.NORMAL, book.getRelPath(), position)));

        // Set card long click handler.
        resolvedVH.content.setOnLongClickListener(v -> {
            if (mayStartDrags) {
                startDragListener.startDragging(resolvedVH);
            } else {
                EventBus.getDefault().post(new BookCardClickEvent(BookCardClickEvent.Type.LONG, book.getRelPath(),
                        position));
            }
            return true;
        });

        // Set info button handler.
        resolvedVH.btnInfo.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.INFO, book.getRelPath(), position)));

        // Set quick tag button handler.
        resolvedVH.btnQuickTag.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.QUICK_TAG, book.getRelPath(), position)));

        // Fill in data.
        resolvedVH.tvTitle.setText(book.getTitle());
        resolvedVH.tvAuthor.setText(book.getAuthor());
        resolvedVH.tvDesc.setText(book.getDesc());
        resolvedVH.rbRating.setRating(book.getRating());
        resolvedVH.htvTags.setData(Util.stringToList(book.getTags(), ";;"));
    }

    /**
     * Figure out how to get the {@link RBook} from the generic T {@code item}.
     * @param item Generic T item.
     * @return The {@link RBook} from {@code item}.
     */
    private RBook getBookFromT(T item) {
        if (item instanceof RBook) return (RBook) item;
        else if (item instanceof RBookListItem) return ((RBookListItem) item).getBook();
        else throw new IllegalArgumentException("item not one of expected types, cannot get RBook.");
    }

    /**
     * If {@code item} of generic type T is an {@link RBookListItem}, return it as such, otherwise return null.
     * @param item Generic item.
     * @return {@code item} as {@link RBookListItem}, or null if it isn't.
     */
    private RBookListItem getBookListItemFromT(T item) {
        if (item instanceof RBookListItem) return (RBookListItem) item;
        else return null;
    }

    /**
     * Set whether or not the adapter may start view drags currently. Note that if the {@link
     * com.bkromhout.realmrecyclerview.RealmRecyclerView} this adapter is bound to is set to automatically begin drags
     * on long click, this will be ignored.
     * @param mayStartDrags If true, the adapter can start item drags.
     */
    public void setDragMode(boolean mayStartDrags) {
        this.mayStartDrags = mayStartDrags;
    }

    @Override
    public void onMove(int draggingPos, int targetPos) {
        RBookListItem item1 = getBookListItemFromT(realmResults.get(draggingPos));
        RBookListItem item2 = getBookListItemFromT(realmResults.get(targetPos));
        if (item1 == null || item2 == null) return;

        RBookList.swapItemPositions(item1, item2);
    }

    /**
     * ViewHolder class for normal book cards.
     */
    static class NormalCardVH extends RecyclerView.ViewHolder {
        @Bind(R.id.content)
        public RelativeLayout content;
        @Bind(R.id.cover_image)
        public ImageView ivCoverImage;
        @Bind(R.id.btn_info)
        public ImageButton btnInfo;
        @Bind(R.id.btn_quick_tag)
        public ImageButton btnQuickTag;
        @Bind(R.id.title)
        public TextView tvTitle;
        @Bind(R.id.author)
        public TextView tvAuthor;
        @Bind(R.id.description)
        public TextView tvDesc;
        @Bind(R.id.rating)
        public RatingBar rbRating;
        @Bind(R.id.tags)
        public HashtagView htvTags;

        public NormalCardVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    /**
     * ViewHolder class for compact book cards.
     */
    static class CompactCardVH extends RecyclerView.ViewHolder {
        @Bind(R.id.content)
        public RelativeLayout content;
        @Bind(R.id.btn_info)
        public ImageButton btnInfo;
        @Bind(R.id.title)
        public TextView tvTitle;
        @Bind(R.id.author)
        public TextView tvAuthor;
        @Bind(R.id.rating_txt)
        public TextView tvRating;

        public CompactCardVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    /**
     * ViewHolder class for book cards without covers.
     */
    static class NoCoverCardVH extends RecyclerView.ViewHolder {
        @Bind(R.id.content)
        public RelativeLayout content;
        @Bind(R.id.btn_info)
        public ImageButton btnInfo;
        @Bind(R.id.btn_quick_tag)
        public ImageButton btnQuickTag;
        @Bind(R.id.title)
        public TextView tvTitle;
        @Bind(R.id.author)
        public TextView tvAuthor;
        @Bind(R.id.description)
        public TextView tvDesc;
        @Bind(R.id.rating)
        public RatingBar rbRating;
        @Bind(R.id.tags)
        public HashtagView htvTags;

        public NoCoverCardVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
