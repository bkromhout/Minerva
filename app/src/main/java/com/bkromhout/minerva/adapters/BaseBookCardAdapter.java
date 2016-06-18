package com.bkromhout.minerva.adapters;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Pair;
import android.view.View;
import android.widget.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.activities.BookInfoActivity;
import com.bkromhout.minerva.data.DataUtils;
import com.bkromhout.minerva.data.ListItemPositionHelper;
import com.bkromhout.minerva.events.BookCardClickEvent;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookListItem;
import com.bkromhout.minerva.ui.RippleForegroundListener;
import com.bkromhout.minerva.ui.TagBackgroundSpan;
import com.bkromhout.rrvl.RealmRecyclerViewAdapter;
import com.bkromhout.rrvl.UIDModel;
import com.bumptech.glide.Glide;
import io.realm.RealmObject;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;

/**
 * Base adapter for book card adapters to extend.
 * <p>
 * Unless there are no items, can optionally automatically add an empty footer view to ensure that we'll never get into
 * a situation where a FAB is obscuring the last item and we aren't able to scroll to make it hide itself (which would
 * otherwise happen if the number/height of the items is <i>just</i> enough to fill the viewport, but not enough to
 * allow scrolling).
 */
public abstract class BaseBookCardAdapter<T extends RealmObject & UIDModel, VH extends RecyclerView.ViewHolder> extends
        RealmRecyclerViewAdapter<T, VH> {
    /**
     * Help our cards ripple.
     */
    private static RippleForegroundListener rippleFgListener = new RippleForegroundListener(R.id.card);
    /**
     * Activity to use for Glide and shared element transitions.
     */
    private final Activity activity;
    /**
     * If true, add a footer view which will prevent the last item from being obscured.
     */
    private boolean addFooterView;
    /**
     * Whether or not this adapter may allow item dragging to start.
     */
    private boolean mayStartDrags = false;

    public BaseBookCardAdapter(Activity activity, RealmResults<T> realmResults) {
        this(activity, realmResults, true);
    }

    public BaseBookCardAdapter(Activity activity, RealmResults<T> realmResults, boolean addFooterView) {
        super(activity, realmResults);
        this.activity = activity;
        this.addFooterView = addFooterView;
        setHasStableIds(true);
    }

    @Override
    public int getItemCount() {
        // Just call super if we know we don't want to add a footer view.
        if (!addFooterView) return super.getItemCount();
        int superCount = super.getItemCount();
        return superCount == 0 ? 0 : superCount + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (addFooterView && super.getItemCount() != 0 && position == super.getItemCount()) return C.FOOTER_ITEM_TYPE;
        else return super.getItemViewType(position);
    }

    @Override
    public long getItemId(int position) {
        if (position == super.getItemCount()) return Long.MIN_VALUE;
        T item = realmResults.get(position);
        if (!item.isValid()) return -1;
        else if (item instanceof RBook) return ((RBook) item).uniqueId;
        else if (item instanceof RBookListItem) return ((RBookListItem) item).uniqueId;
        else throw new IllegalArgumentException("Unexpected item type: " + item.getClass().getName());
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (position == getItemCount() || !(viewHolder instanceof BaseCardVH)) return;
        BaseCardVH vh = (BaseCardVH) viewHolder;
        // Get/check variables needed to help bind.
        T item = realmResults.get(position);
        if (!item.isValid()) return; // Stop now if the object is no longer managed by Realm.
        RBookListItem bookListItem = getBookListItemFromT(item);
        RBook book = bookListItem != null ? bookListItem.book : getBookFromT(item);
        if (book == null || rippleFgListener == null) throw new IllegalArgumentException();

        // Visually distinguish selected cards during multi-select mode.
        vh.cardView.setActivated(selectedPositions.contains(position));

        // Set transition names for certain elements. These depend on whether our card has a cover view and the
        // unique ID of the book we're binding for currently.
        if (vh instanceof NormalCardVH) {
            // Normal cards have cover image views which are transitioned to the corresponding image view in the
            // BookInfoActivity, and the cardview itself transitions to BookInfoActivity's dummy background view.
            ((NormalCardVH) vh).ivCoverImage.setTransitionName(
                    activity.getString(R.string.trans_cover_image) + book.uniqueId);
            vh.cardView.setTransitionName(activity.getString(R.string.trans_book_info_bg) + book.uniqueId);
        } else {
            // Other cards don't have a cover image view, so the card transitions to the whole BookInfoActivity.
            vh.cardView.setTransitionName(activity.getString(R.string.trans_book_info_content) + book.uniqueId);
        }

        // Set card click handler.
        vh.content.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.NORMAL, book.relPath, position)));

        // Set card long click handler.
        vh.content.setOnLongClickListener(v -> {
            if (mayStartDrags) startDragging(vh);
            else EventBus.getDefault().post(new BookCardClickEvent(BookCardClickEvent.Type.LONG, book.relPath,
                    position));
            return true;
        });

        // Set info button click handler.
        vh.btnInfo.setOnClickListener(view -> {
            if (vh instanceof NormalCardVH) {
                ImageView coverImage = ((NormalCardVH) vh).ivCoverImage;
                //noinspection unchecked
                BookInfoActivity.startWithTransition(activity, book.uniqueId, position, true,
                        Pair.create(coverImage, coverImage.getTransitionName()),
                        Pair.create(vh.cardView, vh.cardView.getTransitionName()));
            } else {
                //noinspection unchecked
                BookInfoActivity.startWithTransition(activity, book.uniqueId, position, false,
                        Pair.create(vh.cardView, vh.cardView.getTransitionName()));
            }
        });

        // Fill in data and set transition names.
        vh.tvTitle.setText(book.title);
        vh.tvAuthor.setText(book.author);

        // Do bindings for the rest of the view holder based on its real type.
        if (vh instanceof CompactCardVH) bindCompactBookCard((CompactCardVH) vh, book);
        else if (vh instanceof NoCoverCardVH) bindNoCoverBookCard((NoCoverCardVH) vh, position, book);
        if (vh instanceof NormalCardVH) bindNormalBookCard((NormalCardVH) vh, book);

        // If we have an RBookListItem, store its key on the CardView.
        if (bookListItem != null) vh.cardView.setTag(bookListItem.uniqueId);
    }

    /**
     * Bind a {@link CompactCardVH}.
     * @param resolvedVH Compact book card view holder.
     * @param book       Book to populate item with.
     */
    private void bindCompactBookCard(CompactCardVH resolvedVH, RBook book) {
        // Fill in data.
        resolvedVH.tvRating.setText(String.valueOf(book.rating));
    }

    /**
     * Bind a {@link NoCoverCardVH}. (This would be called for normal book cards as well.)
     * @param resolvedVH No-cover book card view holder.
     * @param position   Item position.
     * @param book       Book to populate item with.
     */
    private void bindNoCoverBookCard(NoCoverCardVH resolvedVH, int position, RBook book) {
        // Set quick tag button handler.
        resolvedVH.btnQuickTag.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.QUICK_TAG, book.relPath, position)));

        // Fill in data.
        resolvedVH.tvDesc.setText(DataUtils.stripHtmlTags(book.desc)); // Strip HTML tags since we have limited space.
        resolvedVH.rbRating.setRating(book.rating);
        // Create a spannable string for the tag textview.
        resolvedVH.tvTags.setText(TagBackgroundSpan.getSpannedTagString(book, resolvedVH.tvTags.getMaxLines()),
                TextView.BufferType.SPANNABLE);
    }

    /**
     * Bind a {@link NormalCardVH}.
     * @param resolvedVH Normal book card view holder.
     * @param book       Book to populate item with.
     */
    private void bindNormalBookCard(NormalCardVH resolvedVH, RBook book) {
        // Set cover image.
        if (book.hasCoverImage) Glide.with(activity)
                                     .load(DataUtils.getCoverImageFile(book.relPath))
                                     .dontTransform()
                                     .into(resolvedVH.ivCoverImage);
        else resolvedVH.ivCoverImage.setImageDrawable(
                ContextCompat.getDrawable(Minerva.get(), R.drawable.default_cover));
    }

    /**
     * Figure out how to get the {@link RBook} from the generic T {@code item}.
     * @param item Generic T item.
     * @return The {@link RBook} from {@code item}.
     */
    private RBook getBookFromT(T item) {
        if (item instanceof RBook) return (RBook) item;
        else if (item instanceof RBookListItem) return ((RBookListItem) item).book;
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
     * com.bkromhout.rrvl.RealmRecyclerView} this adapter is bound to is set to automatically begin drags on long click,
     * this will be ignored.
     * @param mayStartDrags If true, the adapter can start item drags.
     */
    public void setDragMode(boolean mayStartDrags) {
        this.mayStartDrags = mayStartDrags;
    }

    @Override
    public boolean onMove(RecyclerView.ViewHolder dragging, RecyclerView.ViewHolder target) {
        int draggingPos = dragging.getAdapterPosition(), targetPos = target.getAdapterPosition();
        long draggingId = (long) ((BaseCardVH) dragging).cardView.getTag();
        long targetId = (long) ((BaseCardVH) target).cardView.getTag();

        // Determine which way to move the item being dragged.
        if (draggingPos > targetPos) // Moved up multiple.
            ListItemPositionHelper.moveItemToBefore(draggingId, targetId);
        else if (draggingPos < targetPos) // Moved down multiple.
            ListItemPositionHelper.moveItemToAfter(draggingId, targetId);

        return true;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null && viewHolder instanceof BaseCardVH)
            ((BaseCardVH) viewHolder).cardView.setSelected(true);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if (viewHolder != null && viewHolder instanceof BaseCardVH)
            ((BaseCardVH) viewHolder).cardView.setSelected(false);
    }

    /**
     * A base ViewHolder class for all book cards.
     */
    static class BaseCardVH extends RecyclerView.ViewHolder {
        @BindView(R.id.card)
        CardView cardView;
        @BindView(R.id.content)
        public RelativeLayout content;
        @BindView(R.id.btn_info)
        ImageButton btnInfo;
        @BindView(R.id.title)
        TextView tvTitle;
        @BindView(R.id.author)
        TextView tvAuthor;

        BaseCardVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            // Make sure background responds to changes in "activated" state.
            cardView.getBackground().setTintMode(PorterDuff.Mode.SRC);
            cardView.getBackground().setTintList(Minerva.d().CARD_BG_COLORS);

            // Make the card ripple when touched.
            content.setOnTouchListener(rippleFgListener);
        }
    }

    /**
     * ViewHolder class for compact book cards. Adds a rating text view on top of {@link BaseCardVH}.
     */
    static class CompactCardVH extends BaseCardVH {
        @BindView(R.id.rating_txt)
        TextView tvRating;

        CompactCardVH(View itemView) {
            super(itemView);
        }
    }

    /**
     * ViewHolder class for book cards without covers. Adds a quick-tag button, a description text view, a rating bar,
     * and a custom view for tags on top of {@link BaseCardVH}.
     */
    static class NoCoverCardVH extends BaseCardVH {
        @BindView(R.id.btn_quick_tag)
        ImageButton btnQuickTag;
        @BindView(R.id.description)
        TextView tvDesc;
        @BindView(R.id.rating)
        RatingBar rbRating;
        @BindView(R.id.tags)
        TextView tvTags;

        NoCoverCardVH(View itemView) {
            super(itemView);
        }
    }

    /**
     * ViewHolder class for normal book cards. Adds an ImageView for the cover on top of {@link NoCoverCardVH}
     */
    static class NormalCardVH extends NoCoverCardVH {
        @BindView(R.id.cover_image)
        ImageView ivCoverImage;

        NormalCardVH(View itemView) {
            super(itemView);
        }
    }
}
