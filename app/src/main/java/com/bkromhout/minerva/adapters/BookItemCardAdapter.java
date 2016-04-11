package com.bkromhout.minerva.adapters;

import android.app.Activity;
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
     * @param activity     Activity.
     * @param realmResults Results of a Realm query to display.
     */
    public BookItemCardAdapter(Activity activity, RealmResults<RBookListItem> realmResults) {
        super(activity, realmResults);
    }

    @Override
    public NormalCardVH onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new NormalCardVH(inflater.inflate(R.layout.book_card, viewGroup, false));
    }
}

