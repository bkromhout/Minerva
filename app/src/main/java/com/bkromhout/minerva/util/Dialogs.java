package com.bkromhout.minerva.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RatingBar;
import android.widget.Toast;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.events.ActionEvent;
import com.bkromhout.minerva.realm.RBookList;
import io.realm.Realm;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import rx.Observable;

/**
 * Utility class for constructing and showing dialogs which we use in multiple places in the app.
 * <p>
 * The dialogs here will usually fire some sort of event when dismissed in certain ways.
 */
public class Dialogs {
    /**
     * Show a rating dialog.
     * @param ctx           Context to use.
     * @param initialRating The rating that the rating bar should show initially.
     */
    public static void ratingDialog(Context ctx, int initialRating) {
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
                .onPositive((dialog, which) ->
                        EventBus.getDefault().post(new ActionEvent(R.id.action_rate, (int) ratingBar.getRating())))
                .show();
    }

    /**
     * Shows a dialog to let user choose a list.
     * @param ctx   Context to use.
     * @param realm Realm instance to use.
     */
    public static void addToListDialogOrToast(Context ctx, Realm realm) {
        // Get list of lists (list-ception?)
        RealmResults<RBookList> lists = realm.where(RBookList.class).findAllSorted("sortName");

        if (lists.size() == 0) {
            // If we don't have any lists, just show a toast.
            Toast.makeText(ctx, R.string.toast_no_lists, Toast.LENGTH_SHORT).show();
        } else {
            // Create a material dialog.
            new MaterialDialog.Builder(ctx)
                    .title(R.string.action_add_to_list)
                    .items(Observable.from(lists)
                                     .map(RBookList::getName)
                                     .toList()
                                     .toBlocking()
                                     .single())
                    .itemsCallback((dialog, itemView, which, text) ->
                            EventBus.getDefault().post(new ActionEvent(R.id.action_add_to_list, text)))
                    .show();
        }
    }

    /**
     * Shows a simple Yes/No dialog using the given {@code title} and {@code text} string resources. Upon Yes being
     * clicked, fires an {@link ActionEvent} using the given {@code actionId}.
     * @param ctx      Context to use.
     * @param title    String resource to use for title.
     * @param text     String resource to use for text.
     * @param actionId Action ID to send if Yes is clicked.
     */
    public static void simpleYesNoDialog(Context ctx, @StringRes int title, @StringRes int text, @IdRes int actionId) {
        new MaterialDialog.Builder(ctx)
                .title(title)
                .content(text)
                .positiveText(R.string.yes)
                .negativeText(R.string.no)
                .onPositive((dialog, which) -> EventBus.getDefault().post(new ActionEvent(actionId, null)))
                .show();
    }

    /**
     * Same as {@link #simpleYesNoDialog(Context, int, int, int)}, but adds a single check box which allows for
     * additional input.
     * @param ctx          Context to use.
     * @param title        String resource to use for title.
     * @param text         String resource to use for text.
     * @param checkBoxText String resource to use for the checkbox.
     * @param actionId     Action ID to send if Yes is clicked.
     */
    public static void yesNoCheckBoxDialog(Context ctx, @StringRes int title, @StringRes int text,
                                         @StringRes int checkBoxText, @IdRes int actionId) {
        // Create the checkbox.
        final CheckBox checkbox = new CheckBox(ctx);
        checkbox.setText(checkBoxText);

        new MaterialDialog.Builder(ctx)
                .title(title)
                .content(text)
                .customView(checkbox, false)
                .positiveText(R.string.yes)
                .negativeText(R.string.no)
                .onPositive((dialog, which) ->
                        EventBus.getDefault().post(new ActionEvent(actionId, checkbox.isChecked())))
                .show();
    }
}
