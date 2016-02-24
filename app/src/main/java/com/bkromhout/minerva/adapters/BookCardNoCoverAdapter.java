package com.bkromhout.minerva.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.realm.RBook;
import io.realm.RealmResults;

/**
 * Realm RecyclerView Adapter for book cards with no covers.
 */
public class BookCardNoCoverAdapter extends BaseBookCardAdapter<RBook, BaseBookCardAdapter.NoCoverCardVH> {
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
    public BaseBookCardAdapter.NoCoverCardVH onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
        return new BaseBookCardAdapter.NoCoverCardVH(inflater.inflate(R.layout.book_card_no_cover, viewGroup, false));
    }
}
