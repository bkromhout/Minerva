package com.bkromhout.minerva.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.enums.BookCardType;
import com.bkromhout.minerva.events.ActionEvent;
import com.bkromhout.minerva.events.PrefChangeEvent;
import com.bkromhout.minerva.events.UpdatePosEvent;
import com.bkromhout.minerva.prefs.interfaces.BCTPref;
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
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_yes_no_checkbox, null);
        final TextView content = ButterKnife.findById(view, R.id.content);
        final CheckBox checkBox = ButterKnife.findById(view, R.id.checkbox);
        content.setText(text);
        checkBox.setText(checkBoxText);

        new MaterialDialog.Builder(ctx)
                .title(title)
                .customView(view, false)
                .positiveText(R.string.yes)
                .negativeText(R.string.no)
                .onPositive((dialog, which) ->
                        EventBus.getDefault().post(new ActionEvent(actionId, checkBox.isChecked())))
                .show();
    }

    /**
     * Show a simple book card style chooser dialog.
     * @param ctx   Context to use.
     * @param prefs Some *Prefs object which implements {@link BCTPref} so that we may get/put the current/new
     *              preference.
     */
    public static void cardStyleDialog(Context ctx, BCTPref prefs) {
        // Get current card type from prefs.
        final BookCardType cardType = prefs.getCardType(BookCardType.NORMAL);
        // Show dialog.
        new MaterialDialog.Builder(ctx)
                .title(R.string.action_card_type)
                .items(BookCardType.names())
                .itemsCallbackSingleChoice(cardType.getNum(), (dialog, itemView, which, text) -> {
                    // Do nothing if it's the same.
                    if (cardType.getNum() == which) return true;

                    // Persist the new card style and fire event to let caller know.
                    prefs.putCardType(BookCardType.fromNumber(which));
                    EventBus.getDefault().post(new PrefChangeEvent(BCTPref.CARD_TYPE));
                    return true;
                })
                .show();
    }

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
                .autoDismiss(false)
                .onNeutral((dialog, which) -> dialog.dismiss())
                .onNegative((dialog, which) -> ratingBar.setRating(0F))
                .onPositive((dialog, which) -> {
                    EventBus.getDefault().post(new ActionEvent(R.id.action_rate, (int) ratingBar.getRating()));
                    dialog.dismiss();
                })
                .show();
    }

    /**
     * Shows a dialog to let user choose a list.
     * @param ctx   Context to use.
     * @param realm Realm instance to use.
     */
    public static void addToListDialogOrToast(Context ctx, Realm realm) {
        // Get list of normal lists (list-ception anyone?)
        RealmResults<RBookList> lists = realm.where(RBookList.class)
                                             .equalTo("isSmartList", false)
                                             .findAllSorted("sortName");

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
     * Shows an input dialog which will check the input against the current list names before allowing the {@link
     * ActionEvent} to be fired. The event will only be fired if the input text is non-empty and not already used as a
     * list's name. If entered text is the same as {@code preFill}, the dialog will be dismissed without doing
     * anything.
     * @param ctx         Context to use.
     * @param title       String resource to use for title.
     * @param text        String resource to use for text.
     * @param preFill     String to pre-fill the edit text with.
     * @param actionId    Action ID to send if Yes is clicked and checks all pass.
     * @param posToUpdate If not -1, the position to put into a {@link UpdatePosEvent} which will be fired if the {@link
     *                    ActionEvent} is fired.
     */
    public static void listNameDialog(Context ctx, @StringRes int title, @StringRes int text, String preFill,
                                      @IdRes int actionId, int posToUpdate) {
        new MaterialDialog.Builder(ctx)
                .title(title)
                .content(text)
                .autoDismiss(false)
                .negativeText(R.string.cancel)
                .onNegative((dialog, which) -> dialog.dismiss())
                .input(C.getStr(R.string.list_name_hint), preFill, false, (dialog, input) -> {
                    // If it's the same value, do nothing.
                    String newName = input.toString().trim();
                    if (preFill != null && preFill.equals(newName)) {
                        dialog.dismiss();
                        return;
                    }

                    // Get Realm to check if name exists.
                    try (Realm innerRealm = Realm.getDefaultInstance()) {
                        // If the name exists (other than the list's current name), set the error text on the
                        // edit text. If it doesn't, fire an event off and dismiss the dialog.
                        if (innerRealm.where(RBookList.class).equalTo("name", newName).findFirst() != null) {
                            //noinspection ConstantConditions
                            dialog.getInputEditText().setError(C.getStr(R.string.err_name_taken));
                        } else {
                            EventBus.getDefault().post(new ActionEvent(actionId, newName, posToUpdate));
                            dialog.dismiss();
                        }
                    }
                })
                .show();
    }

    /**
     * Shows a dialog which will either display {@code queryString} or a message about not having a query for a smart
     * list. Also has a button to open the query builder which will fire an {@link ActionEvent} to do just that.
     * @param ctx         Context to use.
     * @param queryString Query string to display, or null to show "no query string" message.
     * @param posToUpdate If not -1, the position to put into a {@link UpdatePosEvent} which will be fired if the {@link
     *                    ActionEvent} is fired.
     */
    public static void smartListQueryDialog(Context ctx, String queryString, int posToUpdate) {
        new MaterialDialog.Builder(ctx)
                .title(R.string.title_smart_list_query)
                .content(queryString == null || queryString.isEmpty()
                        ? C.getStr(R.string.no_query_for_smart_list) : queryString)
                .positiveText(R.string.ok)
                .neutralText(R.string.action_open_query_builder)
                .onNeutral((dialog, which) ->
                        EventBus.getDefault().post(new ActionEvent(R.id.action_open_query_builder, null, posToUpdate)))
                .show();
    }
}
