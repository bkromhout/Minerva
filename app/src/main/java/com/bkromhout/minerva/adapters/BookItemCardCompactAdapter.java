package com.bkromhout.minerva.adapters;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookListItem;
import io.realm.RealmResults;

/**
 * Essentially {@link BookCardCompactAdapter}, but has to unwrap {@link RBook}s from {@link RBookListItem}s.
 */
public class BookItemCardCompactAdapter extends BaseBookCardAdapter<RBookListItem, RecyclerView.ViewHolder> {
    /**
     * Create a new {@link BookItemCardCompactAdapter}.
     * @param activity     Activity.
     * @param realmResults Results of a Realm query to display.
     */
    public BookItemCardCompactAdapter(Activity activity, RealmResults<RBookListItem> realmResults) {
        super(activity, realmResults);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == C.FOOTER_ITEM_TYPE)
            return new RecyclerView.ViewHolder(inflater.inflate(R.layout.empty_footer, viewGroup, false)) {};
        else
            return new CompactCardVH(inflater.inflate(R.layout.book_card_compact, viewGroup, false));
    }
}
