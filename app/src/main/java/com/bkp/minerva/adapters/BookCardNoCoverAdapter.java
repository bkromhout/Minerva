package com.bkp.minerva.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkp.minerva.R;
import com.bkp.minerva.events.BookCardClickEvent;
import com.bkp.minerva.realm.RBook;
import com.bkp.minerva.util.Util;
import com.greenfrvr.hashtagview.HashtagView;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import io.realm.RealmViewHolder;
import org.greenrobot.eventbus.EventBus;

/**
 * Realm RecyclerView Adapter for book cards with no covers.
 */
public class BookCardNoCoverAdapter extends RealmBasedRecyclerViewAdapter<RBook, BookCardNoCoverAdapter.ViewHolder> {
    /**
     * Create a new {@link BookCardNoCoverAdapter}.
     * @param context         Context.
     * @param realmResults    Results of a Realm query to display.
     * @param automaticUpdate If true, the list will update automatically.
     * @param animateResults  If true, updates will be animated.
     */
    public BookCardNoCoverAdapter(Context context, RealmResults<RBook> realmResults, boolean automaticUpdate,
                                  boolean animateResults) {
        super(context, realmResults, automaticUpdate, animateResults);
    }

    @Override
    public ViewHolder onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.book_card_no_cover, viewGroup, false));
    }

    @Override
    public void onBindRealmViewHolder(ViewHolder viewHolder, int position) {
        final RBook rBook = realmResults.get(position);

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

        // Set quick tag button handler.
        viewHolder.btnQuickTag.setOnClickListener(view -> EventBus.getDefault().post(new BookCardClickEvent(
                BookCardClickEvent.Type.QUICK_TAG, rBook.getRelPath())));

        // Fill in data.
        viewHolder.tvTitle.setText(rBook.getTitle());
        viewHolder.tvAuthor.setText(rBook.getAuthor());
        viewHolder.tvDesc.setText(rBook.getDesc());
        viewHolder.rbRating.setRating(rBook.getRating());
        viewHolder.htvTags.setData(Util.stringToList(rBook.getTags(), ";;"));
    }

    /**
     * ViewHolder class.
     */
    public class ViewHolder extends RealmViewHolder {
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

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
