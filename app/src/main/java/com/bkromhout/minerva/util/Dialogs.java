package com.bkromhout.minerva.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.events.RatedEvent;
import org.greenrobot.eventbus.EventBus;

/**
 * Utility class for constructing and showing dialogs which we use in multiple places in the app.
 */
public class Dialogs {
    /**
     * Show a rating dialog. A {@link com.bkromhout.minerva.events.RatedEvent} will be fired when the user saves a
     * rating.
     * @param ctx           Context to use.
     * @param initialRating The rating that the rating bar should show initially.
     */
    public static void showRatingDialog(Context ctx, int initialRating) {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_rating, null);
        final RatingBar ratingBar = ButterKnife.findById(view, R.id.rating_bar);
        ratingBar.setRating(initialRating);

        new MaterialDialog.Builder(ctx)
                .title(R.string.title_dialog_rating)
                .customView(view, false)
                .positiveText(R.string.save)
                .negativeText(R.string.clear)
                .neutralText(R.string.cancel)
                .onNegative((dialog, which) -> ratingBar.setRating(0F))
                .onPositive((dialog, which) -> EventBus.getDefault().post(new RatedEvent((int) ratingBar.getRating())))
                .show();
    }
}
