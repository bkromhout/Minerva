package com.bkromhout.minerva.adapters;

import android.app.Activity;
import android.view.ViewGroup;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookListItem;
import io.realm.RealmResults;

/**
 * Essentially {@link BookCardNoCoverAdapter}, but has to unwrap {@link RBook}s from {@link RBookListItem}s.
 */
public class BookItemCardNoCoverAdapter extends BaseBookCardAdapter<RBookListItem, BaseBookCardAdapter.NoCoverCardVH> {
    /**
     * Create a new {@link BookItemCardNoCoverAdapter}.
     * @param activity     Activity.
     * @param realmResults Results of a Realm query to display.
     */
    public BookItemCardNoCoverAdapter(Activity activity, RealmResults<RBookListItem> realmResults) {
        super(activity, realmResults);
    }

    @Override
    public NoCoverCardVH onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new NoCoverCardVH(inflater.inflate(R.layout.book_card_no_cover, viewGroup, false));
    }
}