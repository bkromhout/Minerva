package com.bkp.minerva.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.bkp.minerva.R;
import com.bkp.minerva.realm.RBook;
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
    public BaseBookCardAdapter.NormalCardVH onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
        return new BaseBookCardAdapter.NormalCardVH(inflater.inflate(R.layout.book_card, viewGroup, false));
    }
}
