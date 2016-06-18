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
import android.widget.PopupMenu;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.activities.TaggingActivity;
import com.bkromhout.minerva.events.TagCardClickEvent;
import com.bkromhout.minerva.realm.RTag;
import com.bkromhout.minerva.ui.RippleForegroundListener;
import com.bkromhout.minerva.ui.TriStateCheckBox;
import com.bkromhout.rrvl.RealmRecyclerViewAdapter;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * Realm RecyclerView Adapter for tag cards.
 */
public class TagCardAdapter extends RealmRecyclerViewAdapter<RTag, RecyclerView.ViewHolder> {
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
     * Create a new {@link TagCardAdapter}.
     * @param context      Context.
     * @param realmResults Results of a Realm query to display.
     */
    public TagCardAdapter(Context context, RealmResults<RTag> realmResults) {
        super(context, realmResults);
        this.checkedItems = TaggingActivity.TaggingHelper.get().newCheckedItems;
        this.partiallyCheckedItems = TaggingActivity.TaggingHelper.get().newPartiallyCheckedItems;
    }

    @Override
    public int getItemCount() {
        int superCount = super.getItemCount();
        return superCount == 0 ? 0 : superCount + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (super.getItemCount() != 0 && position == super.getItemCount()) return C.FOOTER_ITEM_TYPE;
        else return super.getItemViewType(position);
    }

    @Override
    public long getItemId(int position) {
        if (position == super.getItemCount()) return Long.MIN_VALUE;
        else return super.getItemId(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == C.FOOTER_ITEM_TYPE)
            return new RecyclerView.ViewHolder(inflater.inflate(R.layout.empty_footer, parent, false)) {};
        else
            return new TagCardVH(inflater.inflate(R.layout.tag_card, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (position == getItemCount() || !(viewHolder instanceof TagCardVH)) return;
        TagCardVH vh = (TagCardVH) viewHolder;
        RTag tag = realmResults.get(position);

        // Set card click listener.
        vh.card.setOnClickListener(v -> {
            CheckBox cbTag = ButterKnife.findById(v, R.id.tag_name);
            String tagName = cbTag.getText().toString();
            // Try to remove from partially checked items; if that doesn't happen, try to remove from checked items;
            // if that also doesn't happen, then add to checked items.
            if (!partiallyCheckedItems.remove(tagName) && !checkedItems.remove(tagName)) checkedItems.add(tagName);
            // Toggle the check mark state.
            cbTag.toggle();
        });

        // Set name and checked state.
        vh.tag.setText(tag.name);
        if (partiallyCheckedItems.contains(tag.name)) vh.tag.setPartiallyChecked(true);
        else vh.tag.setChecked(checkedItems.contains(tag.name));

        // Set the color buttons colors.
        setColorButtonColor(vh.textColor, tag.textColor);
        setColorButtonColor(vh.bgColor, tag.bgColor);

        // Set color button click handlers.
        vh.textColor.setOnClickListener(v -> EventBus.getDefault().post(
                new TagCardClickEvent(TagCardClickEvent.Type.TEXT_COLOR, tag.name)));
        vh.bgColor.setOnClickListener(v -> EventBus.getDefault().post(
                new TagCardClickEvent(TagCardClickEvent.Type.BG_COLOR, tag.name)));

        // Set up btnActions so that it displays a popup menu.
        vh.btnActions.setOnClickListener(view -> {
            PopupMenu menu = new PopupMenu(view.getContext(), view);
            menu.getMenuInflater().inflate(R.menu.tag_card_actions, menu.getMenu());
            menu.setOnMenuItemClickListener(item -> {
                EventBus.getDefault().post(new TagCardClickEvent(TagCardClickEvent.Type.ACTIONS, tag.name,
                        item.getItemId()));
                return true;
            });
            menu.show();
        });
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
     * TagCardVH class.
     */
    class TagCardVH extends RecyclerView.ViewHolder {
        @BindView(R.id.card)
        CardView card;
        @BindView(R.id.tag_name)
        TriStateCheckBox tag;
        @BindView(R.id.tag_text_color)
        ImageButton textColor;
        @BindView(R.id.tag_bg_color)
        ImageButton bgColor;
        @BindView(R.id.btn_actions)
        ImageButton btnActions;

        TagCardVH(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            // Make the card ripple when touched.
            tag.setOnTouchListener(rippleFgListener);
        }
    }
}
