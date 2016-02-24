package com.bkromhout.minerva.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.realm.RBook;
import io.realm.RealmResults;

/**
 * Realm RecyclerView Adapter for normal book cards.
 */
public class BookCardAdapter extends BaseBookCardAdapter<RBook, BaseBookCardAdapter.NormalCardVH> {
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
    public NormalCardVH onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new BaseBookCardAdapter.NormalCardVH(inflater.inflate(R.layout.book_card, viewGroup, false));
    }
}
