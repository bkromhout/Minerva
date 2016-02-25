package com.bkromhout.minerva.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookListItem;
import io.realm.RealmResults;

/**
 * Essentially {@link BookCardCompactAdapter}, but has to unwrap {@link RBook}s from {@link RBookListItem}s.
 */
public class BookItemCardCompactAdapter extends BaseBookCardAdapter<RBookListItem, BaseBookCardAdapter.CompactCardVH> {
    /**
     * Create a new {@link BookItemCardCompactAdapter}.
     * @param context      Context.
     * @param realmResults Results of a Realm query to display.
     */
    public BookItemCardCompactAdapter(Context context, RealmResults<RBookListItem> realmResults) {
        super(context, realmResults);
    }

    @Override
    public CompactCardVH onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new CompactCardVH(inflater.inflate(R.layout.book_card_compact, viewGroup, false));
    }
}
