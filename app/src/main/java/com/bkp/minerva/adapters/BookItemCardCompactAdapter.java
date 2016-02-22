package com.bkp.minerva.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.bkp.minerva.R;
import com.bkp.minerva.realm.RBook;
import com.bkp.minerva.realm.RBookListItem;
import com.bkp.minerva.util.RippleForegroundListener;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;

/**
 * Essentially {@link BookCardCompactAdapter}, but has to unwrap {@link RBook}s from {@link RBookListItem}s.
 */
public class BookItemCardCompactAdapter extends
        RealmBasedRecyclerViewAdapter<RBookListItem, BookCardVHUtil.CompactCardVH> {
    /**
     * Help our cards ripple.
     */
    private RippleForegroundListener rippleFgListener = new RippleForegroundListener(R.id.ripple_foreground_view);

    /**
     * Create a new {@link BookItemCardCompactAdapter}.
     * @param context         Context.
     * @param realmResults    Results of a Realm query to display.
     * @param automaticUpdate If true, the list will update automatically.
     * @param animateResults  If true, updates will be animated.
     */
    public BookItemCardCompactAdapter(Context context, RealmResults<RBookListItem> realmResults,
                                      boolean automaticUpdate, boolean animateResults) {
        super(context, realmResults, automaticUpdate, animateResults);
    }

    @Override
    public BookCardVHUtil.CompactCardVH onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
        return new BookCardVHUtil.CompactCardVH(inflater.inflate(R.layout.book_card_compact, viewGroup, false));
    }

    @Override
    public void onBindRealmViewHolder(BookCardVHUtil.CompactCardVH viewHolder, int position) {
        BookCardVHUtil.doBindViewHolder(viewHolder, position, realmResults.get(position).getBook(), rippleFgListener);
    }
}
