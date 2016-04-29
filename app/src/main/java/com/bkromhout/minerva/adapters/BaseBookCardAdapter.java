package com.bkromhout.minerva.adapters;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.data.CoverHelper;
import com.bkromhout.minerva.events.BookCardClickEvent;
import com.bkromhout.minerva.realm.ListItemPositionHelper;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookListItem;
import com.bkromhout.minerva.util.RippleForegroundListener;
import com.bkromhout.minerva.util.TagBackgroundSpan;
import com.bumptech.glide.Glide;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmObject;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;

/**
 * Base adapter for book card adapters to extend.
 * <p>
 * Unless there are no items, automatically adds an empty footer view to ensure that we'll never get into a situation
 * where a FAB is obscuring the last item and we aren't able to scroll to make it hide itself (which would otherwise
 * happen if the number/height of the items is just enough to fill the available space, but not enough to allow
 * scrolling).
 */
public abstract class BaseBookCardAdapter<T extends RealmObject, VH extends RecyclerView.ViewHolder> extends
        RealmBasedRecyclerViewAdapter<T, VH> {
    private static TagBackgroundSpan.TagBGDrawingInfo tagBGDrawingInfo = new TagBackgroundSpan.TagBGDrawingInfo();
    /**
     * Help our cards ripple.
     */
    private static RippleForegroundListener rippleFgListener = new RippleForegroundListener(R.id.card);
    /**
     * Activity to pass to Glide so that it will automatically get lifecycle events.
     */
    private Activity activity;
    /**
     * Whether or not this adapter may allow item drags to start.
     */
    boolean mayStartDrags = false;

    public BaseBookCardAdapter(Activity activity, RealmResults<T> realmResults) {
        super(activity, realmResults, true, true, null);
        this.activity = activity;
        setHasStableIds(true);
    }

    @Override
    public int getItemCount() {
        int superCount = super.getItemCount();
        return superCount == 0 ? 0 : superCount + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (super.getItemCount() != 0 && position == super.getItemCount()) return C.FOOTER_ITEM_TYPE;
        else return super.getItemViewType(position);
    }

    @Override
    public long getItemId(int position) {
        if (position == super.getItemCount()) return Long.MIN_VALUE;
        T item = realmResults.get(position);
        if (item instanceof RBook) return ((RBook) item).getUniqueId();
        else if (item instanceof RBookListItem) return ((RBookListItem) item).getUniqueId();
        else throw new IllegalArgumentException("Unexpected item type: " + item.getClass().getName());
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (position == getItemCount() || !(viewHolder instanceof BaseCardVH)) return;
        BaseCardVH vh = (BaseCardVH) viewHolder;
        // Get/check variables needed to help bind.
        RBookListItem bookListItem = getBookListItemFromT(realmResults.get(position));
        RBook book = bookListItem != null ? bookListItem.getBook() : getBookFromT(realmResults.get(position));
        if (book == null || rippleFgListener == null) throw new IllegalArgumentException();

        // Visually distinguish selected cards during multi-select mode.
        vh.cardView.setActivated(selectedPositions.contains(position));

        // Set card click handler.
        vh.content.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.NORMAL, book.getRelPath(), position)));

        // Set card long click handler.
        vh.content.setOnLongClickListener(v -> {
            if (mayStartDrags) startDragging(vh);
            else EventBus.getDefault().post(new BookCardClickEvent(BookCardClickEvent.Type.LONG, book.getRelPath(),
                    position));
            return true;
        });

        // Set info button handler.
        vh.btnInfo.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.INFO, book.getRelPath(), position)));

        // Fill in data.
        vh.tvTitle.setText(book.getTitle());
        vh.tvAuthor.setText(book.getAuthor());

        // Do bindings for the rest of the view holder based on its real type.
        if (vh instanceof CompactCardVH) bindCompactBookCard((CompactCardVH) vh, book);
        if (vh instanceof NoCoverCardVH) bindNoCoverBookCard((NoCoverCardVH) vh, position, book);
        if (vh instanceof NormalCardVH) bindNormalBookCard((NormalCardVH) vh, book);

        // If we have an RBookListItem, store its key on the CardView.
        if (bookListItem != null) vh.cardView.setTag(bookListItem.getUniqueId());
    }

    /**
     * Bind a {@link CompactCardVH}.
     * @param resolvedVH Compact book card view holder.
     * @param book       Book to populate item with.
     */
    private void bindCompactBookCard(CompactCardVH resolvedVH, RBook book) {
        // Fill in data.
        resolvedVH.tvRating.setText(String.valueOf(book.getRating()));
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
                BookCardClickEvent.Type.QUICK_TAG, book.getRelPath(), position)));

        // Fill in data.
        resolvedVH.tvDesc.setText(book.getDesc());
        resolvedVH.rbRating.setRating(book.getRating());
        // Create a spannable string for the tag textview.
        resolvedVH.tvTags.setText(TagBackgroundSpan.getSpannedTagString(book, tagBGDrawingInfo,
                resolvedVH.tvTags.getMaxLines()), TextView.BufferType.SPANNABLE);
    }

    /**
     * Bind a {@link NormalCardVH}.
     * @param resolvedVH Normal book card view holder.
     * @param book       Book to populate item with.
     */
    private void bindNormalBookCard(NormalCardVH resolvedVH, RBook book) {
        // Set cover image.
        if (book.hasCoverImage()) Glide.with(activity)
                                       .load(CoverHelper.get().getCoverImageFile(book.getRelPath()))
                                       .centerCrop()
                                       .into(resolvedVH.ivCoverImage);
        else resolvedVH.ivCoverImage.setImageDrawable(ContextCompat.getDrawable(Minerva.getAppCtx(),
                R.drawable.epub_logo_color));
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

    /**
     * A base ViewHolder class for all book cards.
     */
    static class BaseCardVH extends RecyclerView.ViewHolder {
        @BindView(R.id.card)
        public CardView cardView;
        @BindView(R.id.content)
        public RelativeLayout content;
        @BindView(R.id.btn_info)
        public ImageButton btnInfo;
        @BindView(R.id.title)
        public TextView tvTitle;
        @BindView(R.id.author)
        public TextView tvAuthor;

        public BaseCardVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            // Make sure background responds to changes in "activated" state.
            cardView.getBackground().setTintMode(PorterDuff.Mode.SRC);
            cardView.getBackground().setTintList(C.CARD_BG_COLORS);

            // Make the card ripple when touched.
            content.setOnTouchListener(rippleFgListener);
        }
    }

    /**
     * ViewHolder class for compact book cards. Adds a rating text view on top of {@link BaseCardVH}.
     */
    static class CompactCardVH extends BaseCardVH {
        @BindView(R.id.rating_txt)
        public TextView tvRating;

        public CompactCardVH(View itemView) {
            super(itemView);
        }
    }

    /**
     * ViewHolder class for book cards without covers. Adds a quick-tag button, a description text view, a rating bar,
     * and a custom view for tags on top of {@link BaseCardVH}.
     */
    static class NoCoverCardVH extends BaseCardVH {
        @BindView(R.id.btn_quick_tag)
        public ImageButton btnQuickTag;
        @BindView(R.id.description)
        public TextView tvDesc;
        @BindView(R.id.rating)
        public RatingBar rbRating;
        @BindView(R.id.tags)
        public TextView tvTags;

        public NoCoverCardVH(View itemView) {
            super(itemView);
        }
    }

    /**
     * ViewHolder class for normal book cards. Adds an ImageView for the cover on top of {@link NoCoverCardVH}
     */
    static class NormalCardVH extends NoCoverCardVH {
        @BindView(R.id.cover_image)
        public ImageView ivCoverImage;

        public NormalCardVH(View itemView) {
            super(itemView);
        }
    }
}
