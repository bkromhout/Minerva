package com.bkromhout.minerva.adapters;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
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
     * Create a new {@link BookListCardAdapter}.
     * @param activity     Context.
     * @param realmResults Results of a Realm query to display.
     */
    public BookListCardAdapter(Activity activity, RealmResults<RBookList> realmResults) {
        super(activity, realmResults);
        this.activity = activity;
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

        // Set the transition name to use for the card view.
        vh.cardView.setTransitionName(Minerva.get().getString(R.string.trans_book_list) + bookList.uniqueId);

        // Set card click handler.
        vh.content.setOnClickListener(view -> {
            BookListActivity.startWithTransition(activity, bookList.uniqueId, position,
                    Pair.create(vh.cardView, vh.cardView.getTransitionName()));
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

            // Make the card ripple when touched.
            content.setOnTouchListener(rippleFgListener);
        }
    }
}
