package com.bkromhout.minerva.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookListItem;
import io.realm.RealmResults;

/**
 * Essentially {@link BookCardAdapter}, but has to unwrap {@link RBook}s from {@link RBookListItem}s.
 */
public class BookItemCardAdapter extends BaseBookCardAdapter<RBookListItem, BaseBookCardAdapter.NormalCardVH> {
    /**
     * Create a new {@link BookItemCardAdapter}.
     * @param context      Context.
     * @param realmResults Results of a Realm query to display.
     */
    public BookItemCardAdapter(Context context, RealmResults<RBookListItem> realmResults) {
        super(context, realmResults);
    }

    @Override
    public NormalCardVH onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new NormalCardVH(inflater.inflate(R.layout.book_card, viewGroup, false));
    }
}

