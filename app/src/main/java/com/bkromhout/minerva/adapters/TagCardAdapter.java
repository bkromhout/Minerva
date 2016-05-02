package com.bkromhout.minerva.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.TaggingActivity;
import com.bkromhout.minerva.events.TagCardClickEvent;
import com.bkromhout.minerva.realm.RTag;
import com.bkromhout.minerva.ui.RippleForegroundListener;
import com.bkromhout.minerva.ui.TriStateCheckBox;
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
    private static RippleForegroundListener rippleFgListener = new RippleForegroundListener(R.id.card);
    /**
     * List of items which are checked. The strings in this list should correspond to names of {@link RTag}s.
     */
    private List<String> checkedItems;
    /**
     * List of items which are partially checked. The strings in this list should correspond to names of {@link RTag}s.
     */
    private List<String> partiallyCheckedItems;
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
        this.partiallyCheckedItems = TaggingActivity.TaggingHelper.get().newPartiallyCheckedItems;
    }

    @Override
    public TagCardVH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TagCardVH(inflater.inflate(R.layout.tag_card, parent, false));
    }

    @Override
    public void onBindViewHolder(TagCardVH viewHolder, int position) {
        RTag tag = realmResults.get(position);

        // Set whether or not the color and action buttons are shown.
        viewHolder.textColor.setVisibility(isInActionMode ? View.GONE : View.VISIBLE);
        viewHolder.bgColor.setVisibility(isInActionMode ? View.GONE : View.VISIBLE);
        viewHolder.rename.setVisibility(isInActionMode ? View.VISIBLE : View.GONE);
        viewHolder.delete.setVisibility(isInActionMode ? View.VISIBLE : View.GONE);

        // Set card click listener.
        viewHolder.card.setOnClickListener(v -> {
            // Do nothing if in action mode.
            if (isInActionMode) return;

            CheckBox cbTag = ButterKnife.findById(v, R.id.tag_name);
            String tagName = cbTag.getText().toString();
            // Try to remove from partially checked items; if that doesn't happen, try to remove from checked items;
            // if that also doesn't happen, then add to checked items.
            if (!partiallyCheckedItems.remove(tagName) && !checkedItems.remove(tagName)) checkedItems.add(tagName);
            // Toggle the check mark state.
            cbTag.toggle();
        });

        // Set name and checked state.
        viewHolder.tag.setText(tag.name);
        if (partiallyCheckedItems.contains(tag.name)) viewHolder.tag.setPartiallyChecked(true);
        else viewHolder.tag.setChecked(checkedItems.contains(tag.name));

        // Set the color buttons colors.
        setColorButtonColor(viewHolder.textColor, tag.textColor);
        setColorButtonColor(viewHolder.bgColor, tag.bgColor);

        // Set button click handlers.
        viewHolder.rename.setOnClickListener(v -> EventBus.getDefault().post(
                new TagCardClickEvent(TagCardClickEvent.Type.RENAME, tag.name)));
        viewHolder.delete.setOnClickListener(v -> EventBus.getDefault().post(
                new TagCardClickEvent(TagCardClickEvent.Type.DELETE, tag.name)));
        viewHolder.textColor.setOnClickListener(v -> EventBus.getDefault().post(
                new TagCardClickEvent(TagCardClickEvent.Type.TEXT_COLOR, tag.name)));
        viewHolder.bgColor.setOnClickListener(v -> EventBus.getDefault().post(
                new TagCardClickEvent(TagCardClickEvent.Type.BG_COLOR, tag.name)));
    }

    /**
     * Take's {@code colorButton}'s layer drawable (which it is assumed it is), finds the {@code circle} layer, and
     * tints that drawable the given {@code color}.
     * @param colorButton {@code ImageButton} with a {@link LayerDrawable} as its drawable.
     * @param color       Color to tint the {@code circle} layer of the {@code colorButton}'s {@link LayerDrawable}.
     */
    private void setColorButtonColor(ImageButton colorButton, @ColorInt int color) {
        LayerDrawable layerDrawable = (LayerDrawable) colorButton.getDrawable();
        Drawable colorCircle = layerDrawable.findDrawableByLayerId(R.id.circle);
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            // API 21 is really quite stubborn when it comes to colors, annoyingly.
            colorCircle = DrawableCompat.wrap(colorCircle);
            DrawableCompat.setTint(colorCircle, color);
            layerDrawable.setDrawableByLayerId(R.id.circle, colorCircle);
        } else DrawableCompat.setTint(colorCircle, color);
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
        @BindView(R.id.card)
        CardView card;
        @BindView(R.id.tag_name)
        TriStateCheckBox tag;
        @BindView(R.id.tag_text_color)
        ImageButton textColor;
        @BindView(R.id.tag_bg_color)
        ImageButton bgColor;
        @BindView(R.id.rename_tag)
        ImageButton rename;
        @BindView(R.id.delete_tag)
        ImageButton delete;

        public TagCardVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            // Make the card ripple when touched.
            tag.setOnTouchListener(rippleFgListener);
        }
    }
}
