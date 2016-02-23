package com.bkp.minerva.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.bkp.minerva.R;
import com.bkp.minerva.realm.RBook;
import com.bkp.minerva.realm.RBookList;
import com.bkp.minerva.realm.RBookListItem;
import io.realm.RealmResults;

/**
 * Essentially {@link BookCardNoCoverAdapter}, but has to unwrap {@link RBook}s from {@link RBookListItem}s.
 */
public class BookItemCardNoCoverAdapter extends BaseBookCardAdapter<RBookListItem, BaseBookCardAdapter.NoCoverCardVH> {
    /**
     * Create a new {@link BookItemCardNoCoverAdapter}.
     * @param context         Context.
     * @param realmResults    Results of a Realm query to display.
     * @param automaticUpdate If true, the list will update automatically.
     * @param animateResults  If true, updates will be animated.
     */
    public BookItemCardNoCoverAdapter(Context context, RealmResults<RBookListItem> realmResults,
                                      boolean automaticUpdate, boolean animateResults) {
        super(context, realmResults, automaticUpdate, animateResults);
    }

    @Override
    public BaseBookCardAdapter.NoCoverCardVH onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
        return new BaseBookCardAdapter.NoCoverCardVH(inflater.inflate(R.layout.book_card_no_cover, viewGroup, false));
    }

    @Override
    public void onMoveDo(RBookListItem draggingObj, RBookListItem targetObj) {
        RBookList.swapItemPositions(draggingObj, targetObj);
        // TODO Call notifyMoved?? Not sure if necessary since we auto-update, but we'll see.
    }
}