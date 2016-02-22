package com.bkp.minerva.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.bkp.minerva.R;
import com.bkp.minerva.realm.RBook;
import com.bkp.minerva.util.RippleForegroundListener;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;

/**
 * Realm RecyclerView Adapter for compact book cards.
 */
public class BookCardCompactAdapter extends RealmBasedRecyclerViewAdapter<RBook, BookCardUtil.CompactCardVH> {
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
    public BookCardUtil.CompactCardVH onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
        return new BookCardUtil.CompactCardVH(inflater.inflate(R.layout.book_card_compact, viewGroup, false));
    }

    @Override
    public void onBindRealmViewHolder(BookCardUtil.CompactCardVH viewHolder, int position) {
        BookCardUtil.doBindViewHolder(viewHolder, position, realmResults.get(position), rippleFgListener);
    }
}