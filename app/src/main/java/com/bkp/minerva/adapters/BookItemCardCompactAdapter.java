package com.bkp.minerva.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.bkp.minerva.R;
import com.bkp.minerva.realm.RBook;
import com.bkp.minerva.realm.RBookListItem;
import io.realm.RealmResults;

/**
 * Essentially {@link BookCardCompactAdapter}, but has to unwrap {@link RBook}s from {@link RBookListItem}s.
 */
public class BookItemCardCompactAdapter extends BaseBookCardAdapter<RBookListItem, BaseBookCardAdapter.CompactCardVH> {
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
    public BaseBookCardAdapter.CompactCardVH onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
        return new BaseBookCardAdapter.CompactCardVH(inflater.inflate(R.layout.book_card_compact, viewGroup, false));
    }

    @Override
    public void onMove(int draggingPos, int targetPos) {
        super.onMove(draggingPos, targetPos);
        notifyItemMoved(draggingPos, targetPos);
    }

    @Override
    public void onMoveDo(RBookListItem draggingObj, RBookListItem targetObj) {
        //RBookList.swapItemPositions(draggingObj, targetObj);

        // TODO Call notifyMoved?? Not sure if necessary since we auto-update, but we'll see.
    }
}
