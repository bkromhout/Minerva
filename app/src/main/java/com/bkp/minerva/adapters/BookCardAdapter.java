package com.bkp.minerva.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkp.minerva.R;
import com.bkp.minerva.realm.RBook;
import com.greenfrvr.hashtagview.HashtagView;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import io.realm.RealmViewHolder;

import java.util.Arrays;

/**
 * RecyclerView Adapter for the normal book cards.
 */
public class BookCardAdapter extends RealmBasedRecyclerViewAdapter<RBook, BookCardAdapter.ViewHolder> {
    /**
     * Create a new {@link BookCardAdapter}.
     * @param context         Context.
     * @param realmResults    Results of a Realm query to display.
     * @param automaticUpdate If true, the list will update automatically.
     * @param animateResults  If true, updates will be animated.
     */
    public BookCardAdapter(Context context, RealmResults<RBook> realmResults, boolean automaticUpdate,
                           boolean animateResults) {
        super(context, realmResults, automaticUpdate, animateResults);
    }

    @Override
    public ViewHolder onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.book_card, viewGroup, false));
    }

    @Override
    public void onBindRealmViewHolder(ViewHolder viewHolder, int position) {
        final RBook rBook = realmResults.get(position);

        // Set cover image.
        if (rBook.isHasCoverImage()) {
            // TODO something here
            // TODO also not sure if I need to explicitly set the epub logo again?
        }

        // TODO Set info button handler.

        // TODO Set quick tag button handler.

        // Fill in data.
        viewHolder.tvTitle.setText(rBook.getTitle());
        viewHolder.tvAuthor.setText(rBook.getAuthor());
        viewHolder.tvDesc.setText(rBook.getDesc());
        viewHolder.rbRating.setNumStars(rBook.getRating());
        viewHolder.htvTags.setData(Arrays.asList(rBook.getTags().split(";;")));
    }

    /**
     * ViewHolder class.
     */
    public class ViewHolder extends RealmViewHolder {
        @Bind(R.id.cover_image)
        public ImageView coverImage;
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
