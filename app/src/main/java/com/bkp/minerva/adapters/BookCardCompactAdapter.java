package com.bkp.minerva.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkp.minerva.R;
import com.bkp.minerva.events.BookCardClickEvent;
import com.bkp.minerva.realm.RBook;
import com.bkp.minerva.util.RippleForegroundListener;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import io.realm.RealmViewHolder;
import org.greenrobot.eventbus.EventBus;

/**
 * Realm RecyclerView Adapter for compact book cards.
 */
public class BookCardCompactAdapter extends RealmBasedRecyclerViewAdapter<RBook, BookCardCompactAdapter.ViewHolder> {
    /**
     * Help our cards ripple.
     */
    private RippleForegroundListener rippleFgListener = new RippleForegroundListener(R.id.ripple_foreground_view);

    /**
     * Create a new {@link BookCardCompactAdapter}.
     * @param context         Context.
     * @param realmResults    Results of a Realm query to display.
     * @param automaticUpdate If true, the list will update automatically.
     * @param animateResults  If true, updates will be animated.
     */
    public BookCardCompactAdapter(Context context, RealmResults<RBook> realmResults, boolean automaticUpdate,
                                  boolean animateResults) {
        super(context, realmResults, automaticUpdate, animateResults);
    }

    @Override
    public ViewHolder onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.book_card_compact, viewGroup, false));
    }

    @Override
    public void onBindRealmViewHolder(ViewHolder viewHolder, int position) {
        final RBook rBook = realmResults.get(position);

        // Make the card ripple when touched.
        viewHolder.content.setOnTouchListener(rippleFgListener);

        // Set card click handler.
        viewHolder.content.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.NORMAL, rBook.getRelPath())));

        // Set card long click handler.
        viewHolder.content.setOnLongClickListener(v -> {
            EventBus.getDefault().post(new BookCardClickEvent(BookCardClickEvent.Type.LONG, rBook.getRelPath()));
            return true;
        });

        // Set info button handler.
        viewHolder.btnInfo.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.INFO, rBook.getRelPath())));

        // Fill in data.
        viewHolder.tvTitle.setText(rBook.getTitle());
        viewHolder.tvAuthor.setText(rBook.getAuthor());
        viewHolder.tvRating.setText(String.valueOf(rBook.getRating()));
    }

    /**
     * ViewHolder class.
     */
    public class ViewHolder extends RealmViewHolder {
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

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}