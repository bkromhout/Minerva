package com.bkromhout.minerva.adapters;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.activities.BookListActivity;
import com.bkromhout.minerva.events.BookListCardClickEvent;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.ui.RippleForegroundListener;
import com.bkromhout.minerva.ui.UiUtils;
import com.bkromhout.rrvl.RealmRecyclerViewAdapter;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;

/**
 * Realm RecyclerView Adapter for book list cards.
 * <p>
 * Unless there are no items, automatically adds an empty footer view to ensure that we'll never get into a situation
 * where a FAB is obscuring the last item and we aren't able to scroll to make it hide itself (which would otherwise
 * happen if the number/height of the items is just enough to fill the available space, but not enough to allow
 * scrolling).
 */
public class BookListCardAdapter extends RealmRecyclerViewAdapter<RBookList, RecyclerView.ViewHolder> {
    /**
     * Help our cards ripple.
     */
    private static RippleForegroundListener rippleFgListener = new RippleForegroundListener(R.id.card);
    /**
     * Activity to use for shared element transitions.
     */
    private final Activity activity;
    /**
     * Actual gesture detector implementation used to create {@link #gestureDetector}.
     */
    private final CardGestureDetector detectorImpl;
    /**
     * Gesture detector to help us with getting a legitimate X-Y point to center {@link BookListActivity}'s circular
     * reveal effect at when starting it.
     */
    private final GestureDetectorCompat gestureDetector;
    /**
     * Whether or not we're currently in selection mode. It's necessary for us to track this here so that our {@link
     * CardGestureDetector} knows what to do when it detects a single tap (click).
     */
    private boolean inSelectionMode = false;

    /**
     * Create a new {@link BookListCardAdapter}.
     * @param activity     Context.
     * @param realmResults Results of a Realm query to display.
     */
    public BookListCardAdapter(Activity activity, RealmResults<RBookList> realmResults) {
        super(activity, realmResults);
        this.activity = activity;
        this.detectorImpl = new CardGestureDetector();
        this.gestureDetector = new GestureDetectorCompat(activity, detectorImpl);
    }

    /**
     * Set whether or not the adapter should consider itself to be in selection mode. This is necessary to determine
     * what should be done when a card is tapped.
     * @param enabled Whether or not to enable selection mode.
     */
    public void setSelectionMode(boolean enabled) {
        this.inSelectionMode = enabled;
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
        return realmResults.get(position).uniqueId;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == C.FOOTER_ITEM_TYPE)
            return new RecyclerView.ViewHolder(inflater.inflate(R.layout.empty_footer, viewGroup, false)) {};
        else
            return new BookListCardVH(inflater.inflate(R.layout.book_list_card, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (position == getItemCount() || !(viewHolder instanceof BookListCardVH)) return;
        BookListCardVH vh = (BookListCardVH) viewHolder;
        final RBookList bookList = realmResults.get(position);
        if (!bookList.isValid()) return;

        // Visually distinguish selected cards during multi-select mode.
        vh.cardView.setActivated(selectedPositions.contains(position));

        vh.content.setOnTouchListener((v, event) -> {
            // Pass this into our ripple touch listener so that we still get touch feedback.
            rippleFgListener.onTouch(v, event);

            // Ignore this event if it's within the bounds of either of our buttons.
            if (UiUtils.isPointInsideView(event.getX(), event.getY(), vh.btnActions)
                    || (vh.btnSmartIcon.getVisibility() == View.VISIBLE &&
                    UiUtils.isPointInsideView(event.getX(), event.getY(), vh.btnSmartIcon))) return false;

            // If it isn't, have the gesture detector check it. This will return true if it was handled.
            detectorImpl.setValuesToSend(bookList.uniqueId, position);
            return gestureDetector.onTouchEvent(event);
        });

        // Set card long click handler.
        vh.content.setOnLongClickListener(view -> {
            EventBus.getDefault().post(
                    new BookListCardClickEvent(BookListCardClickEvent.Type.LONG, bookList.name,
                            viewHolder.getAdapterPosition()));
            return true;
        });

        // Set up btnActions so that it displays a popup menu.
        vh.btnActions.setOnClickListener(view -> {
            PopupMenu menu = new PopupMenu(view.getContext(), view);
            menu.getMenuInflater().inflate(!bookList.isSmartList ? R.menu.book_list_card_actions
                    : R.menu.book_list_smart_card_actions, menu.getMenu());
            menu.setOnMenuItemClickListener(item -> {
                EventBus.getDefault().post(new BookListCardClickEvent(BookListCardClickEvent.Type.ACTIONS,
                        bookList.name, item.getItemId(), viewHolder.getAdapterPosition()));
                return true;
            });
            menu.show();
        });

        // Set up btnSmartIcon so that it fires an event when pressed.
        vh.btnSmartIcon.setOnClickListener(view -> EventBus.getDefault().post(
                new BookListCardClickEvent(BookListCardClickEvent.Type.ACTIONS, bookList.name,
                        R.id.action_show_query, viewHolder.getAdapterPosition())));

        // Set visibility of smart list icon.
        vh.btnSmartIcon.setVisibility(bookList.isSmartList ? View.VISIBLE : View.GONE);

        // Set list name.
        vh.tvListName.setText(bookList.name);
    }

    /**
     * BookListCardVH class.
     */
    public class BookListCardVH extends RecyclerView.ViewHolder {
        @BindView(R.id.card)
        public CardView cardView;
        @BindView(R.id.content)
        public LinearLayout content;
        @BindView(R.id.list_name)
        public TextView tvListName;
        @BindView(R.id.smart_list_icon)
        public ImageButton btnSmartIcon;
        @BindView(R.id.btn_actions)
        public ImageButton btnActions;

        public BookListCardVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            // Make sure background responds to changes in "activated" state.
            cardView.getBackground().setTintMode(PorterDuff.Mode.SRC);
            cardView.getBackground().setTintList(Minerva.d().CARD_BG_COLORS);
        }
    }

    /**
     * Custom gesture detector which helps us know exactly where on a card we've tapped. By capturing this information,
     * we can provide better UX when doing the reveal transition for {@link BookListActivity} by having the animation
     * originate from the location of the tap.
     */
    private class CardGestureDetector extends GestureDetector.SimpleOnGestureListener {
        private long uniqueIdToSend;
        private int posToSend;

        void setValuesToSend(long uniqueIdToSend, int posToSend) {
            this.uniqueIdToSend = uniqueIdToSend;
            this.posToSend = posToSend;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            // Return true so that the system knows we're interested.
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // If we're in selection mode, toggle the activation state for the given position. If we aren't, start
            // the BookListActivity.
            if (inSelectionMode) toggleSelected(posToSend);
            else BookListActivity.start(activity, uniqueIdToSend, posToSend, (int) e.getRawX(), (int) e.getRawY());
            return true;
        }
    }
}
