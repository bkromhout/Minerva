package com.bkromhout.minerva.adapters;

import android.app.Activity;
import android.view.ViewGroup;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.realm.RBook;
import io.realm.RealmResults;

/**
 * Realm RecyclerView Adapter for normal book cards.
 */
public class BookCardAdapter extends BaseBookCardAdapter<RBook, BaseBookCardAdapter.NormalCardVH> {
    /**
     * Create a new {@link BookCardAdapter}.
     * @param activity     Activity.
     * @param realmResults Results of a Realm query to display.
     */
    public BookCardAdapter(Activity activity, RealmResults<RBook> realmResults) {
        super(activity, realmResults);
    }

    @Override
    public NormalCardVH onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new NormalCardVH(inflater.inflate(R.layout.book_card, viewGroup, false));
    }
}
