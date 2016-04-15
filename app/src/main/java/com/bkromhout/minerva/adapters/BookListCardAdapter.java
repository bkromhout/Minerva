package com.bkromhout.minerva.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.events.BookListCardClickEvent;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.util.RippleForegroundListener;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;

/**
 * Realm RecyclerView Adapter for book list cards.
 */
public class BookListCardAdapter extends RealmBasedRecyclerViewAdapter<RBookList, BookListCardAdapter.BookListCardVH> {
    /**
     * Help our cards ripple.
     */
    private RippleForegroundListener rippleFgListener = new RippleForegroundListener(R.id.card);

    /**
     * Create a new {@link BookListCardAdapter}.
     * @param context         Context.
     * @param realmResults    Results of a Realm query to display.
     * @param automaticUpdate If true, the list will update automatically.
     * @param animateResults  If true, updates will be animated.
     */
    public BookListCardAdapter(Context context, RealmResults<RBookList> realmResults, boolean automaticUpdate,
                               boolean animateResults) {
        super(context, realmResults, automaticUpdate, animateResults, null);
    }

    @Override
    public BookListCardVH onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new BookListCardVH(inflater.inflate(R.layout.book_list_card, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(BookListCardVH viewHolder, int position) {
        final RBookList rBookList = realmResults.get(position);

        // Make the card ripple when touched.
        viewHolder.content.setOnTouchListener(rippleFgListener);

        // Set card click handler.
        viewHolder.content.setOnClickListener(view ->
                EventBus.getDefault().post(new BookListCardClickEvent(BookListCardClickEvent.Type.NORMAL,
                        rBookList.getName(), position)));

        // Set card long click handler.
        viewHolder.content.setOnLongClickListener(view -> {
            EventBus.getDefault().post(
                    new BookListCardClickEvent(BookListCardClickEvent.Type.LONG, rBookList.getName(), position));
            return true;
        });

        // Set up btnActions so that it displays a popup menu.
        viewHolder.btnActions.setOnClickListener(view -> {
            PopupMenu menu = new PopupMenu(view.getContext(), view);
            menu.getMenuInflater().inflate(!rBookList.isSmartList() ? R.menu.book_list_card_actions
                    : R.menu.book_list_smart_card_actions, menu.getMenu());
            menu.setOnMenuItemClickListener(item -> {
                EventBus.getDefault().post(new BookListCardClickEvent(BookListCardClickEvent.Type.ACTIONS,
                        rBookList.getName(), item.getItemId(), position));
                return true;
            });
            menu.show();
        });

        // Set up btnSmartIcon so that it fires an event when pressed.
        viewHolder.btnSmartIcon.setOnClickListener(view -> EventBus.getDefault().post(
                new BookListCardClickEvent(BookListCardClickEvent.Type.ACTIONS, rBookList.getName(),
                        R.id.action_show_query, position)));

        // Set visibility of smart list icon.
        viewHolder.btnSmartIcon.setVisibility(rBookList.isSmartList() ? View.VISIBLE : View.GONE);

        // Set list name.
        viewHolder.tvListName.setText(rBookList.getName());
    }

    /**
     * BookListCardVH class.
     */
    public class BookListCardVH extends RecyclerView.ViewHolder {
        @Bind(R.id.content)
        public RelativeLayout content;
        @Bind(R.id.list_name)
        public TextView tvListName;
        @Bind(R.id.smart_list_icon)
        public ImageButton btnSmartIcon;
        @Bind(R.id.btn_actions)
        public ImageButton btnActions;

        public BookListCardVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
