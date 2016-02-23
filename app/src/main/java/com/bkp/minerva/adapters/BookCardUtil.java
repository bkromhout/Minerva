package com.bkp.minerva.adapters;

import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.*;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkp.minerva.R;
import com.bkp.minerva.events.BookCardClickEvent;
import com.bkp.minerva.realm.RBook;
import com.bkp.minerva.realm.RBookList;
import com.bkp.minerva.realm.RBookListItem;
import com.bkp.minerva.util.RippleForegroundListener;
import com.bkp.minerva.util.Util;
import com.greenfrvr.hashtagview.HashtagView;
import io.realm.RealmResults;
import io.realm.RealmViewHolder;
import org.greenrobot.eventbus.EventBus;

/**
 * Utility class to help reduce duplicate code in the adapters for this package.
 */
public class BookCardUtil {

    /**
     * Swaps the items at the given positions. (Note that the positions are the positions of the items in the {@code
     * items} list, not the pos fields of the {@link RBookListItem}s.)
     * @param items List of {@link RBookListItem}s which both items belong to.
     * @param p1    Position in {@code items} of an item.
     * @param p2    Position in {@code items} of another item.
     */
    static void swapItemsAtPositions(RealmResults<RBookListItem> items, int p1, int p2) {
        // Get items.
        RBookListItem item1 = items.get(p1);
        RBookListItem item2 = items.get(p2);
        // Swap items' positions.
        RBookList.swapItemPositions(item1, item2);
    }

    /**
     * Do all of the main logic for binding the view holders for the various book card adapters here so as to prevent
     * bugs by reducing duplicated code.
     * @param viewHolder       The view holder to fill in.
     * @param position         The position of the item.
     * @param book             The book to use to populate views.
     * @param rippleFgListener The listener to use to help cards ripple when clicked.
     * @param isSelected       Whether or not this item is in a selected state.
     */
    static void doBindViewHolder(RealmViewHolder viewHolder, int position, RBook book,
                                 RippleForegroundListener rippleFgListener, boolean isSelected) {
        if (book == null || rippleFgListener == null) throw new IllegalArgumentException();

        // Fill in view holder based on its real type.
        if (viewHolder instanceof NormalCardVH) {
            // Normal cards.
            NormalCardVH resolvedVH = (NormalCardVH) viewHolder;

            ((CardView) resolvedVH.content.getParent()).setCardBackgroundColor(
                    isSelected ? R.color.selectedCard : R.color.cardview_dark_background);

            // Make the card ripple when touched.
            resolvedVH.content.setOnTouchListener(rippleFgListener);

            // Set card click handler.
            resolvedVH.content.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                    BookCardClickEvent.Type.NORMAL, book.getRelPath(), position)));

            // Set card long click handler.
            resolvedVH.content.setOnLongClickListener(v -> {
                EventBus.getDefault().post(new BookCardClickEvent(BookCardClickEvent.Type.LONG, book.getRelPath(),
                        position));
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
        } else if (viewHolder instanceof CompactCardVH) {
            // Compact cards.
            CompactCardVH resolvedVH = (CompactCardVH) viewHolder;

            ((CardView) resolvedVH.content.getParent()).setCardBackgroundColor(
                    isSelected ? R.color.selectedCard : R.color.cardview_dark_background);

            // Make the card ripple when touched.
            resolvedVH.content.setOnTouchListener(rippleFgListener);

            // Set card click handler.
            resolvedVH.content.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                    BookCardClickEvent.Type.NORMAL, book.getRelPath(), position)));

            // Set card long click handler.
            resolvedVH.content.setOnLongClickListener(v -> {
                EventBus.getDefault().post(new BookCardClickEvent(BookCardClickEvent.Type.LONG, book.getRelPath(),
                        position));
                return true;
            });

            // Set info button handler.
            resolvedVH.btnInfo.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                    BookCardClickEvent.Type.INFO, book.getRelPath(), position)));

            // Fill in data.
            resolvedVH.tvTitle.setText(book.getTitle());
            resolvedVH.tvAuthor.setText(book.getAuthor());
            resolvedVH.tvRating.setText(String.valueOf(book.getRating()));
        } else if (viewHolder instanceof NoCoverCardVH) {
            // No-cover cards.
            NoCoverCardVH resolvedVH = (NoCoverCardVH) viewHolder;

            ((CardView) resolvedVH.content.getParent()).setCardBackgroundColor(
                    isSelected ? R.color.selectedCard : R.color.cardview_dark_background);

            // Make the card ripple when touched.
            resolvedVH.content.setOnTouchListener(rippleFgListener);

            // Set card click handler.
            resolvedVH.content.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                    BookCardClickEvent.Type.NORMAL, book.getRelPath(), position)));

            // Set card long click handler.
            resolvedVH.content.setOnLongClickListener(v -> {
                EventBus.getDefault().post(new BookCardClickEvent(BookCardClickEvent.Type.LONG, book.getRelPath(),
                        position));
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
        } else {
            throw new IllegalArgumentException("Invalid viewHolder");
        }
    }

    /**
     * ViewHolder class for normal book cards.
     */
    static class NormalCardVH extends RealmViewHolder {
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
    static class CompactCardVH extends RealmViewHolder {
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
    static class NoCoverCardVH extends RealmViewHolder {
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
