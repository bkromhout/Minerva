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
 * Essentially {@link BookCardAdapter}, but has to unwrap {@link RBook}s from {@link RBookListItem}s.
 */
public class BookItemCardAdapter extends RealmBasedRecyclerViewAdapter<RBookListItem, BookCardVHUtil.NormalCardVH> {
    /**
     * Help our cards ripple.
     */
    private RippleForegroundListener rippleFgListener = new RippleForegroundListener(R.id.ripple_foreground_view);

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
    public BookCardVHUtil.NormalCardVH onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
        return new BookCardVHUtil.NormalCardVH(inflater.inflate(R.layout.book_card, viewGroup, false));
    }

    @Override
    public void onBindRealmViewHolder(BookCardVHUtil.NormalCardVH viewHolder, int position) {
        BookCardVHUtil.doBindViewHolder(viewHolder, position, realmResults.get(position).getBook(), rippleFgListener);
    }
}

