package com.bkp.minerva.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.bkp.minerva.R;
import com.bkp.minerva.realm.RBook;
import com.bkp.minerva.realm.RBookListItem;
import com.bkp.minerva.util.DraggableItemTouchHelperCallback;
import com.bkp.minerva.util.RippleForegroundListener;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;

/**
 * Essentially {@link BookCardAdapter}, but has to unwrap {@link RBook}s from {@link RBookListItem}s.
 */
public class BookItemCardAdapter extends RealmBasedRecyclerViewAdapter<RBookListItem, BookCardUtil.NormalCardVH>
        implements DraggableItemTouchHelperCallback.Adapter {
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
    public BookCardUtil.NormalCardVH onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
        return new BookCardUtil.NormalCardVH(inflater.inflate(R.layout.book_card, viewGroup, false));
    }

    @Override
    public void onBindRealmViewHolder(BookCardUtil.NormalCardVH viewHolder, int position) {
        BookCardUtil.doBindViewHolder(viewHolder, position, realmResults.get(position).getBook(), rippleFgListener);
    }

    @Override
    public void onItemMove(int sourcePos, int targetPos) {
        BookCardUtil.swapItemsAtPositions(realmResults, sourcePos, targetPos);
        // TODO Call notifyMoved?? Not sure if necessary since we auto-update, but we'll see.
    }
}

