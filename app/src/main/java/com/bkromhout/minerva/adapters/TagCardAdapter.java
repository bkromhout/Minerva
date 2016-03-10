package com.bkromhout.minerva.adapters;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.TaggingActivity;
import com.bkromhout.minerva.events.TagCardClickEvent;
import com.bkromhout.minerva.realm.RTag;
import com.bkromhout.minerva.util.RippleForegroundListener;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;

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
     * Whether or not we're currently in action mode.
     */
    private boolean isInActionMode = false;

    /**
     * Create a new {@link TagCardAdapter}.
     * @param context      Context.
     * @param realmResults Results of a Realm query to display.
     */
    public TagCardAdapter(Context context, RealmResults<RTag> realmResults) {
        super(context, realmResults, true, true, null);
        this.checkedItems = TaggingActivity.TaggingHelper.get().newCheckedItems;
    }

    @Override
    public TagCardVH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TagCardVH(inflater.inflate(R.layout.tag_card, parent, false));
    }

    @Override
    public void onBindViewHolder(TagCardVH viewHolder, int position) {
        RTag tag = realmResults.get(position);

        // Make the card ripple when touched.
        viewHolder.tag.setOnTouchListener(rippleFgListener);

        // Set card click listener.
        viewHolder.card.setOnClickListener(v -> {
            // Do nothing if in action mode.
            if (isInActionMode) return;

            CheckedTextView ctvTag = ButterKnife.findById(v, R.id.tag_name);
            String tagName = ctvTag.getText().toString();
            if (!checkedItems.remove(tagName)) {
                checkedItems.add(tagName);
                ctvTag.setChecked(true);
            } else {
                ctvTag.setChecked(false);
            }
        });

        // Set name and checked state
        viewHolder.tag.setText(tag.getName());
        viewHolder.tag.setChecked(checkedItems.contains(tag.getName()));

        // Set whether or not the action buttons are shown.
        viewHolder.rename.setVisibility(isInActionMode ? View.VISIBLE : View.GONE);
        viewHolder.delete.setVisibility(isInActionMode ? View.VISIBLE : View.GONE);

        // Set action button click handlers.
        viewHolder.rename.setOnClickListener(v -> EventBus.getDefault().post(
                new TagCardClickEvent(TagCardClickEvent.Type.RENAME, tag.getName())));
        viewHolder.delete.setOnClickListener(v -> EventBus.getDefault().post(
                new TagCardClickEvent(TagCardClickEvent.Type.DELETE, tag.getName())));
    }

    /**
     * Set whether or not we're currently in action mode.
     * @param isInActionMode True or false :)
     */
    public void setInActionMode(boolean isInActionMode) {
        if (this.isInActionMode == isInActionMode) return;
        this.isInActionMode = isInActionMode;
        notifyDataSetChanged();
    }

    /**
     * TagCardVH class.
     */
    public class TagCardVH extends RecyclerView.ViewHolder {
        @Bind(R.id.card)
        CardView card;
        @Bind(R.id.tag_name)
        CheckedTextView tag;
        @Bind(R.id.rename_tag)
        ImageButton rename;
        @Bind(R.id.delete_tag)
        ImageButton delete;

        public TagCardVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
