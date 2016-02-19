package com.bkp.minerva.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkp.minerva.R;
import com.bkp.minerva.realm.RBookList;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import io.realm.RealmViewHolder;

/**
 * Realm RecyclerView Adapter for book list cards.
 */
public class BookListCardAdapter extends RealmBasedRecyclerViewAdapter<RBookList, BookListCardAdapter.ViewHolder> {
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

        // Set actions button handler.
        viewHolder.btnActions.setOnClickListener(v -> {
            // TODO.
        });

        // Set list name.
        viewHolder.tvListName.setText(rBookList.getName());
    }

    /**
     * ViewHolder class.
     */
    public class ViewHolder extends RealmViewHolder {
        @Bind(R.id.list_name)
        public TextView tvListName;
        @Bind(R.id.btn_actions)
        public ImageButton btnActions;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
