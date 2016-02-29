package com.bkromhout.minerva.adapters;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.realm.RTag;
import com.bkromhout.minerva.util.RippleForegroundListener;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;

import java.util.ArrayList;
import java.util.List;

/**
 * Realm RecyclerView Adapter for tag cards.
 */
public class TagCardAdapter extends RealmBasedRecyclerViewAdapter<RTag, TagCardAdapter.TagCardVH> {
    /**
     * Help our cards ripple.
     */
    private RippleForegroundListener rippleFgListener = new RippleForegroundListener(R.id.card);
    /**
     * List of items which are checked. The strings in this list should correspond to names of {@link RTag}s.
     */
    private List<String> checkedItems;

    /**
     * Create a new {@link TagCardAdapter}.
     * @param context        Context.
     * @param realmResults   Results of a Realm query to display.
     * @param initialChecked List of items which are initially checked.
     */
    public TagCardAdapter(Context context, RealmResults<RTag> realmResults, List<String> initialChecked) {
        super(context, realmResults, true, true, null);
        this.checkedItems = initialChecked != null ? initialChecked : new ArrayList<>();
    }

    @Override
    public TagCardVH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TagCardVH(inflater.inflate(R.layout.tag_card, parent, false));
    }

    @Override
    public void onBindViewHolder(TagCardVH viewHolder, int position) {
        final RTag tag = realmResults.get(position);

        // Make the card ripple when touched.
        viewHolder.tag.setOnTouchListener(rippleFgListener);

        // Set card click listener.
        viewHolder.card.setOnClickListener(this::onTagCardClick);

        // Set name and checked state
        viewHolder.tag.setText(tag.getName());
        viewHolder.tag.setChecked(checkedItems.contains(tag.getName()));
    }

    /**
     * Called when a tag card is clicked.
     * @param v The clicked view.
     */
    private void onTagCardClick(View v) {
        CheckedTextView tag = ButterKnife.findById(v, R.id.tag_name);
        String tagName = tag.getText().toString();

    }

    /**
     * TagCardVH class.
     */
    public class TagCardVH extends RecyclerView.ViewHolder {
        @Bind(R.id.card)
        CardView card;
        @Bind(R.id.tag_name)
        CheckedTextView tag;

        public TagCardVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
