package com.bkp.minerva.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.bkp.minerva.R;
import com.bkp.minerva.realm.RBook;
import com.bkp.minerva.util.RippleForegroundListener;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;

/**
 * Realm RecyclerView Adapter for book cards with no covers.
 */
public class BookCardNoCoverAdapter extends RealmBasedRecyclerViewAdapter<RBook, BookCardUtil.NoCoverCardVH> {
    /**
     * Help our cards ripple.
     */
    private RippleForegroundListener rippleFgListener = new RippleForegroundListener(R.id.ripple_foreground_view);

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
    public BookCardUtil.NoCoverCardVH onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
        return new BookCardUtil.NoCoverCardVH(inflater.inflate(R.layout.book_card_no_cover, viewGroup, false));
    }

    @Override
    public void onBindRealmViewHolder(BookCardUtil.NoCoverCardVH viewHolder, int position) {
        BookCardUtil.doBindViewHolder(viewHolder, position, realmResults.get(position), rippleFgListener,
                selectedPositions.contains(position));
    }
}
