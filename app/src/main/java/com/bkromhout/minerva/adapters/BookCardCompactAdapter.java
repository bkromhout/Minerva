package com.bkromhout.minerva.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.realm.RBook;
import io.realm.RealmResults;

/**
 * Realm RecyclerView Adapter for compact book cards.
 */
public class BookCardCompactAdapter extends BaseBookCardAdapter<RBook, BaseBookCardAdapter.CompactCardVH> {
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
    public CompactCardVH onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new BaseBookCardAdapter.CompactCardVH(inflater.inflate(R.layout.book_card_compact, viewGroup, false));
    }
}