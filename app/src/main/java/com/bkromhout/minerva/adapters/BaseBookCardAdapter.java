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
public abstract class BaseBookCardAdapter<T extends RealmObject, VH extends BaseBookCardAdapter.BaseCardVH> extends
        RealmBasedRecyclerViewAdapter<T, VH> {
    /**
     * Help our cards ripple.
     */
    RippleForegroundListener rippleFgListener = new RippleForegroundListener(R.id.card);
    /**
     * Whether or not this adapter may allow item drags to start.
     */
    boolean mayStartDrags = false;

    public BaseBookCardAdapter(Context context, RealmResults<T> realmResults) {
        super(context, realmResults, true, true, null);
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        T item = realmResults.get(position);
        if (item instanceof RBook) return ((RBook) item).getUniqueId();
        else if (item instanceof RBookListItem) return ((RBookListItem) item).getUniqueId();
        else throw new IllegalArgumentException("Unexpected item type: " + item.getClass().getName());
    }

    @Override
    public void onBindViewHolder(BaseCardVH viewHolder, int position) {
        // Get/check variables needed to help bind.
        RBookListItem bookListItem = getBookListItemFromT(realmResults.get(position));
        RBook book = bookListItem != null ? bookListItem.getBook() : getBookFromT(realmResults.get(position));
        if (book == null || rippleFgListener == null) throw new IllegalArgumentException();
        boolean selected = selectedPositions.contains(position);

        // Do common bindings. First, change the card's background color based on whether or not the item is selected.
        viewHolder.cardView.setCardBackgroundColor(selected ? R.color.selectedCard : R.color.cardview_dark_background);

        // Make the card ripple when touched.
        viewHolder.content.setOnTouchListener(rippleFgListener);

        // Set card click handler.
        viewHolder.content.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.NORMAL, book.getRelPath(), position)));

        // Set card long click handler.
        viewHolder.content.setOnLongClickListener(v -> {
            if (mayStartDrags) startDragListener.startDragging(viewHolder);
            else EventBus.getDefault().post(new BookCardClickEvent(BookCardClickEvent.Type.LONG, book.getRelPath(),
                    position));
            return true;
        });

        // Set info button handler.
        viewHolder.btnInfo.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.INFO, book.getRelPath(), position)));

        // Fill in data.
        viewHolder.tvTitle.setText(book.getTitle());
        viewHolder.tvAuthor.setText(book.getAuthor());

        // Do bindings for the rest of the view holder based on its real type.
        if (viewHolder instanceof CompactCardVH) bindCompactBookCard((CompactCardVH) viewHolder, book);
        if (viewHolder instanceof NoCoverCardVH) bindNoCoverBookCard((NoCoverCardVH) viewHolder, position, book);
        if (viewHolder instanceof NormalCardVH) bindNormalBookCard((NormalCardVH) viewHolder, book);

        // If we have an RBookListItem, store its key on the CardView.
        if (bookListItem != null) viewHolder.cardView.setTag(bookListItem.getKey());
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
        resolvedVH.htvTags.setData(Util.stringToList(book.getTags(), ";;"));
    }

    /**
     * Bind a {@link NormalCardVH}.
     * @param resolvedVH Normal book card view holder.
     * @param book       Book to populate item with.
     */
    private void bindNormalBookCard(NormalCardVH resolvedVH, RBook book) {
        // Set cover image.
        if (book.isHasCoverImage()) {
            // TODO something here
        }
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
    public boolean onMove(RecyclerView.ViewHolder dragging, RecyclerView.ViewHolder target) {
        int draggingPos = dragging.getAdapterPosition(), targetPos = target.getAdapterPosition();
        String draggingKey = (String) ((BaseCardVH) dragging).cardView.getTag();
        String targetKey = (String) ((BaseCardVH) target).cardView.getTag();
        if (draggingKey == null || targetKey == null) return false;

        //RBookListItem item1 = getBookListItemFromT(realmResults.get(draggingPos));
        //RBookListItem item2 = getBookListItemFromT(realmResults.get(targetPos));
        //if (item1 == null || item2 == null) return false;
        //RBookList.swapItemPositions(item1.getKey(), item2.getKey());

        // Sometimes we skip multiple spaces, so we'll want to move the item being dragged instead of swapping.
        if (Math.abs(draggingPos - targetPos) == 1) {
            // Moved one space.
            RBookList.swapItemPositions(draggingKey, targetKey);
            notifyItemMoved(draggingPos, targetPos);
        } else if (draggingPos > targetPos) {
            // Moved up more than one space.
            RBookList.moveItemToBefore(draggingKey, targetKey);
        } else {
            // Moved down more than one space.
            RBookList.moveItemToAfter(draggingKey, targetKey);
        }
        //notifyItemMoved(draggingPos, targetPos); //TODO here?
        return true;
    }

    /**
     * A base ViewHolder class for all book cards.
     */
    static class BaseCardVH extends RecyclerView.ViewHolder {
        @Bind(R.id.card)
        public CardView cardView;
        @Bind(R.id.content)
        public RelativeLayout content;
        @Bind(R.id.btn_info)
        public ImageButton btnInfo;
        @Bind(R.id.title)
        public TextView tvTitle;
        @Bind(R.id.author)
        public TextView tvAuthor;

        public BaseCardVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    /**
     * ViewHolder class for compact book cards. Adds a rating text view on top of {@link BaseCardVH}.
     */
    static class CompactCardVH extends BaseCardVH {
        @Bind(R.id.rating_txt)
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
        @Bind(R.id.btn_quick_tag)
        public ImageButton btnQuickTag;
        @Bind(R.id.description)
        public TextView tvDesc;
        @Bind(R.id.rating)
        public RatingBar rbRating;
        @Bind(R.id.tags)
        public HashtagView htvTags;

        public NoCoverCardVH(View itemView) {
            super(itemView);
        }
    }

    /**
     * ViewHolder class for normal book cards. Adds an ImageView for the cover on top of {@link NoCoverCardVH}
     */
    static class NormalCardVH extends NoCoverCardVH {
        @Bind(R.id.cover_image)
        public ImageView ivCoverImage;

        public NormalCardVH(View itemView) {
            super(itemView);
        }
    }
}
