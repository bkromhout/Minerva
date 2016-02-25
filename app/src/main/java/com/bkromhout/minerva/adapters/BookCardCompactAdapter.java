package com.bkromhout.minerva.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.realm.RBook;
import io.realm.RealmResults;

/**
 * Realm RecyclerView Adapter for compact book cards.
 */
public class BookCardCompactAdapter extends BaseBookCardAdapter<RBook, BaseBookCardAdapter.CompactCardVH> {
    /**
     * Create a new {@link BookCardCompactAdapter}.
     * @param context      Context.
     * @param realmResults Results of a Realm query to display.
     */
    public BookCardCompactAdapter(Context context, RealmResults<RBook> realmResults) {
        super(context, realmResults);
    }

    @Override
    public CompactCardVH onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new CompactCardVH(inflater.inflate(R.layout.book_card_compact, viewGroup, false));
    }
}