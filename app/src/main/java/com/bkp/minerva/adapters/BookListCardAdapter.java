package com.bkp.minerva.adapters;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkp.minerva.R;
import com.bkp.minerva.events.BookListCardClickEvent;
import com.bkp.minerva.realm.RBookList;
import com.bkp.minerva.util.RippleForegroundListener;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import io.realm.RealmViewHolder;
import org.greenrobot.eventbus.EventBus;

/**
 * Realm RecyclerView Adapter for book list cards.
 */
public class BookListCardAdapter extends RealmBasedRecyclerViewAdapter<RBookList, BookListCardAdapter.ViewHolder> {
    /**
     * Help our cards ripple.
     */
    private RippleForegroundListener rippleFgListener = new RippleForegroundListener(R.id.ripple_foreground_view);

    /**
     * Create a new {@link BookListCardAdapter}.
     * @param context         Context.
     * @param realmResults    Results of a Realm query to display.
     * @param automaticUpdate If true, the list will update automatically.
     * @param animateResults  If true, updates will be animated.
     */
    public BookListCardAdapter(Context context, RealmResults<RBookList> realmResults, boolean automaticUpdate,
                               boolean animateResults) {
        super(context, realmResults, automaticUpdate, animateResults);
    }

    @Override
    public ViewHolder onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.book_list_card, viewGroup, false));
    }

    @Override
    public void onBindRealmViewHolder(ViewHolder viewHolder, int position) {
        final RBookList rBookList = realmResults.get(position);

        // Make the card ripple when touched.
        viewHolder.content.setOnTouchListener(rippleFgListener);

        // Set card click handler.
        viewHolder.content.setOnClickListener(view ->
                EventBus.getDefault().post(new BookListCardClickEvent(BookListCardClickEvent.Type.NORMAL,
                        rBookList.getName())));

        // Set card long click handler.
        viewHolder.content.setOnLongClickListener(view -> {
            EventBus.getDefault().post(
                    new BookListCardClickEvent(BookListCardClickEvent.Type.LONG, rBookList.getName()));
            return true;
        });

        // Set list name.
        viewHolder.tvListName.setText(rBookList.getName());
    }

    /**
     * ViewHolder class.
     */
    public class ViewHolder extends RealmViewHolder implements PopupMenu.OnMenuItemClickListener {
        @Bind(R.id.content)
        public RelativeLayout content;
        @Bind(R.id.list_name)
        public TextView tvListName;
        @Bind(R.id.btn_actions)
        public ImageButton btnActions;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            // Set up btnActions so that it displays a popup menu.
            btnActions.setOnClickListener(view -> {
                PopupMenu menu = new PopupMenu(view.getContext(), view);
                menu.getMenuInflater().inflate(R.menu.book_list_card_actions, menu.getMenu());
                menu.setOnMenuItemClickListener(ViewHolder.this);
                menu.show();
            });
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            EventBus.getDefault().post(new BookListCardClickEvent(BookListCardClickEvent.Type.ACTIONS,
                    tvListName.getText().toString(), item.getItemId()));
            return true;
        }
    }
}
