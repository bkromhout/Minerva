package com.bkromhout.minerva.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.realm.RBook;
import io.realm.RealmResults;

/**
 * Realm RecyclerView Adapter for book cards with no covers.
 */
public class BookCardNoCoverAdapter extends BaseBookCardAdapter<RBook, BaseBookCardAdapter.NoCoverCardVH> {
    /**
     * Create a new {@link BookCardNoCoverAdapter}.
     * @param context      Context.
     * @param realmResults Results of a Realm query to display.
     */
    public BookCardNoCoverAdapter(Context context, RealmResults<RBook> realmResults) {
        super(context, realmResults);
    }

    @Override
    public NoCoverCardVH onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new NoCoverCardVH(inflater.inflate(R.layout.book_card_no_cover, viewGroup, false));
    }
}
