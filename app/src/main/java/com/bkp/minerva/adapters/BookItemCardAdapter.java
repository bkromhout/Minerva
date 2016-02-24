package com.bkp.minerva.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.bkp.minerva.R;
import com.bkp.minerva.realm.RBook;
import com.bkp.minerva.realm.RBookListItem;
import io.realm.RealmResults;

/**
 * Essentially {@link BookCardAdapter}, but has to unwrap {@link RBook}s from {@link RBookListItem}s.
 */
public class BookItemCardAdapter extends BaseBookCardAdapter<RBookListItem, BaseBookCardAdapter.NormalCardVH> {
    /**
     * Create a new {@link BookItemCardAdapter}.
     * @param context         Context.
     * @param realmResults    Results of a Realm query to display.
     * @param automaticUpdate If true, the list will update automatically.
     * @param animateResults  If true, updates will be animated.
     */
    public BookItemCardAdapter(Context context, RealmResults<RBookListItem> realmResults, boolean automaticUpdate,
                               boolean animateResults) {
        super(context, realmResults, automaticUpdate, animateResults);
    }

    @Override
    public NormalCardVH onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
        return new NormalCardVH(inflater.inflate(R.layout.book_card, viewGroup, false));
    }
}

