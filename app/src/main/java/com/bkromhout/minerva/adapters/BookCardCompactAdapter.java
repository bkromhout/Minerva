package com.bkromhout.minerva.adapters;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.realm.RBook;
import io.realm.RealmResults;

/**
 * Realm RecyclerView Adapter for compact book cards.
 */
public class BookCardCompactAdapter extends BaseBookCardAdapter<RBook, RecyclerView.ViewHolder> {

    public BookCardCompactAdapter(Activity activity, RealmResults<RBook> realmResults) {
        this(activity, realmResults, true);
    }

    public BookCardCompactAdapter(Activity activity, RealmResults<RBook> realmResults, boolean addFooterView) {
        super(activity, realmResults, addFooterView);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == C.FOOTER_ITEM_TYPE)
            return new RecyclerView.ViewHolder(inflater.inflate(R.layout.empty_footer, viewGroup, false)) {};
        else
            return new CompactCardVH(inflater.inflate(R.layout.book_card_compact, viewGroup, false));
    }
}