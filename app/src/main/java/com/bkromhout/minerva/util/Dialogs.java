package com.bkromhout.minerva.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RatingBar;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.Prefs;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.enums.BookCardType;
import com.bkromhout.minerva.enums.MainFrag;
import com.bkromhout.minerva.events.ActionEvent;
import com.bkromhout.minerva.events.PrefChangeEvent;
import com.bkromhout.minerva.events.UpdatePosEvent;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.ui.SnackKiosk;
import io.realm.Realm;
import io.realm.RealmModel;
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
     * Shows a simple confirmation dialog using the given {@code title}, {@code text}, and {@code posText} strings. Upon
     * the positive button being clicked, fires an {@link ActionEvent} using the given {@code actionId}.
     * @param ctx      Context to use.
     * @param title    String resource to use for title.
     * @param text     String resource to use for text.
     * @param posText  String resource to use for positive button text.
     * @param actionId Action ID to send if Yes is clicked.
     */
    public static void simpleConfirmDialog(final Context ctx, @StringRes final int title, @StringRes final int text,
                                           @StringRes final int posText, @IdRes final int actionId) {
        simpleConfirmDialog(ctx, title, C.getStr(text), posText, actionId);
    }

    /**
     * Shows a simple confirmation dialog using the given {@code title}, {@code text}, and {@code posText} strings. Upon
     * the positive button being clicked, fires an {@link ActionEvent} using the given {@code actionId}.
     * @param ctx      Context to use.
     * @param title    String resource to use for title.
     * @param text     String to use for text.
     * @param posText  String resource to use for positive button text.
     * @param actionId Action ID to send if Yes is clicked.
     */
    public static void simpleConfirmDialog(final Context ctx, @StringRes final int title, final String text,
                                           @StringRes final int posText, @IdRes final int actionId) {
        new MaterialDialog.Builder(ctx)
                .title(title)
                .content(text)
                .positiveText(posText)
                .negativeText(R.string.cancel)
                .onPositive((dialog, which) -> EventBus.getDefault().post(new ActionEvent(actionId, null)))
                .show();
    }

    /**
     * Same as {@link #simpleConfirmDialog(Context, int, int, int, int)}, but adds a single check box which allows for
     * additional input.
     * @param ctx             Context to use.
     * @param title           String resource to use for title.
     * @param text            String resource to use for text.
     * @param checkBoxText    String resource to use for the checkbox.
     * @param checkedInfoText String resource to use for red text shown when checkbox is checked.
     * @param posText         String resource to use for positive button text.
     * @param actionId        Action ID to send if Yes is clicked.
     */
    public static void confirmCheckBoxDialog(final Context ctx, @StringRes final int title, @StringRes final int text,
                                             @StringRes final int checkBoxText, @StringRes final int checkedInfoText,
                                             @StringRes final int posText, @IdRes final int actionId) {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_yes_no_checkbox, null);
        final TextView content = ButterKnife.findById(view, R.id.content);
        final CheckBox checkBox = ButterKnife.findById(view, R.id.checkbox);
        final TextView checkedInfo = ButterKnife.findById(view, R.id.checked_info);
        content.setText(text);
        checkBox.setText(checkBoxText);
        // Show checkedInfo text when checkbox is checked.
        if (checkedInfoText != -1) checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                checkedInfo.setText(isChecked ? checkedInfoText : R.string.empty));

        new MaterialDialog.Builder(ctx)
                .title(title)
                .customView(view, false)
                .positiveText(posText)
                .negativeText(R.string.cancel)
                .onPositive((dialog, which) ->
                        EventBus.getDefault().post(new ActionEvent(actionId, checkBox.isChecked())))
                .show();
    }

    /**
     * Show a simple book card style chooser dialog.
     * @param ctx       Context to use.
     * @param whichFrag Which fragment this preference should be persisted for.
     */
    public static void cardStyleDialog(final Context ctx, final MainFrag whichFrag) {
        // Get current card type from prefs.
        final BookCardType cardType = Prefs.get().getBookCardType(BookCardType.NORMAL, whichFrag);
        // Show dialog.
        new MaterialDialog.Builder(ctx)
                .title(R.string.action_card_type)
                .negativeText(R.string.cancel)
                .items(BookCardType.names())
                .itemsCallbackSingleChoice(cardType.getNum(), (dialog, itemView, which, text) -> {
                    // Do nothing if it's the same.
                    if (cardType.getNum() == which) return true;

                    // Persist the new card style and fire event to let caller know.
                    String changedKey = Prefs.get().putBookCardType(BookCardType.fromNumber(which), whichFrag);
                    EventBus.getDefault().post(new PrefChangeEvent(changedKey));
                    return true;
                })
                .show();
    }

    /**
     * Show a rating dialog.
     * @param ctx           Context to use.
     * @param initialRating The rating that the rating bar should show initially.
     */
    public static void ratingDialog(final Context ctx, final int initialRating) {
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
     * Show the "Mark As..." dialog.
     * @param ctx Context to use.
     */
    public static void markAsDialog(final Context ctx) {
        new MaterialDialog.Builder(ctx)
                .title(R.string.title_dialog_mark_as)
                .negativeText(R.string.cancel)
                .items(R.array.mark_choices)
                .itemsCallback((dialog, itemView, which, text) ->
                        EventBus.getDefault().post(new ActionEvent(R.id.action_mark_as, which)))
                .show();
    }

    /**
     * Shows a dialog to let user choose a list.
     * @param ctx   Context to use.
     * @param realm Realm instance to use.
     */
    public static void addToListDialogOrToast(final Context ctx, final Realm realm) {
        // Get list of normal lists.
        RealmResults<RBookList> lists = realm.where(RBookList.class)
                                             .equalTo("isSmartList", false)
                                             .findAllSorted("sortName");

        if (lists.size() == 0) {
            // If we don't have any lists, just show a snackbar.
            SnackKiosk.snack(R.string.sb_no_lists, Snackbar.LENGTH_SHORT);
        } else {
            // Create a material dialog.
            new MaterialDialog.Builder(ctx)
                    .title(R.string.action_add_to_list)
                    .negativeText(R.string.cancel)
                    .items(Observable.from(lists)
                                     .map(list -> list.name)
                                     .toList()
                                     .toBlocking()
                                     .single())
                    .itemsCallback((dialog, itemView, which, text) ->
                            EventBus.getDefault().post(new ActionEvent(R.id.action_add_to_list, text)))
                    .show();
        }
    }

    /**
     * Shows an input dialog which will check the input against the current names used for the {@code modelClass} before
     * allowing the {@link ActionEvent} to be fired. The event will only be fired if the input text is non-empty and the
     * input is not a name already in use. If entered text is the same as {@code preFill}, the dialog will be dismissed
     * without doing anything.
     * @param ctx         Context to use.
     * @param modelClass  Model class whose {@code name} field will be checked.
     * @param title       String resource to use for title.
     * @param text        String resource to use for text.
     * @param hint        String resource to use for hint.
     * @param preFill     String to pre-fill the edit text with.
     * @param actionId    Action ID to send if Yes is clicked and checks all pass.
     * @param posToUpdate If not -1, the position to put into a {@link UpdatePosEvent} which will be fired if the {@link
     *                    ActionEvent} is fired.
     */
    public static void uniqueNameDialog(final Context ctx, final Class<? extends RealmModel> modelClass,
                                        @StringRes final int title, @StringRes final int text,
                                        @StringRes final int hint, final String preFill, @IdRes final int actionId,
                                        final int posToUpdate) {
        new MaterialDialog.Builder(ctx)
                .title(title)
                .content(text)
                .autoDismiss(false)
                .positiveText(R.string.save)
                .negativeText(R.string.cancel)
                .onNegative((dialog, which) -> dialog.dismiss())
                .input(C.getStr(hint), preFill, false, (dialog, input) -> {
                    // If it's the same value, do nothing.
                    String newName = input.toString().trim();
                    if (preFill != null && preFill.equals(newName)) {
                        dialog.dismiss();
                        return;
                    }

                    // Get Realm to check if name exists.
                    try (Realm innerRealm = Realm.getDefaultInstance()) {
                        // If the name exists (other than the model's current name), set the error text on the
                        // edit text. If it doesn't, fire an event off and dismiss the dialog.
                        if (innerRealm.where(modelClass).equalTo("name", newName).findFirst() != null) {
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
     * Shows a dialog which will either display {@code queryString} or a message about not having a query. Can also have
     * a button to open the query builder (which will fire an {@link ActionEvent}).
     * @param ctx            Context to use.
     * @param title          String resource to use for title.
     * @param emptyText      String resource to use for content if {@code queryString} is {@code null} or empty.
     * @param queryString    Query string to use as content (if not {@code null} or empty).
     * @param showBuilderBtn If true, show a button with the text "Open Query Builder"
     * @param posToUpdate    If not -1, the position to put into a {@link UpdatePosEvent} which will be fired if the
     *                       {@link ActionEvent} is fired.
     */
    public static void queryDialog(final Context ctx, @StringRes final int title, @StringRes final int emptyText,
                                   final String queryString, final boolean showBuilderBtn, final int posToUpdate) {
        // Build base dialog.
        MaterialDialog.Builder builder = new MaterialDialog.Builder(ctx)
                .title(title)
                .content(queryString != null && !queryString.isEmpty() ? queryString : C.getStr(emptyText))
                .positiveText(R.string.dismiss);
        // Conditionally show the "Open Query Builder" button.
        if (showBuilderBtn) builder.neutralText(R.string.action_open_query_builder)
                                   .onNeutral((dialog, which) -> EventBus.getDefault().post(
                                           new ActionEvent(R.id.action_open_query_builder, null, posToUpdate)));
        // Show the dialog.
        builder.show();
    }
}
