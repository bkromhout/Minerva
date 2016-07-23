package com.bkromhout.minerva.ui;

import android.Manifest;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.activities.ImportActivity;
import com.bkromhout.minerva.events.MissingPermEvent;
import com.bkromhout.minerva.util.Util;
import org.greenrobot.eventbus.EventBus;

/**
 * Allows us to show Snackbars from anywhere in the app without regard for how it gets shown.
 * <p>
 * TODO Have this use the builder pattern.
 */
public class SnackKiosk {
    /**
     * Should be implemented throughout the app so that a Snackbar can be shown at any time.
     */
    public interface Snacker {
        /**
         * Get a view appropriate to use as the anchor for a Snackbar.
         * @return View to use to anchor Snackbar.
         */
        @NonNull
        View getSnackbarAnchorView();
    }

    /**
     * Single instance.
     */
    private static SnackKiosk INSTANCE = null;

    /**
     * Current {@link Snacker} who will help us show a Snackbar.
     */
    private Snacker snacker = null;

    /**
     * Current {@link Snack} being used to show a Snackbar.
     */
    private Snack currSnack;
    /**
     * Next {@link Snack} to show a Snackbar for.
     */
    private Snack nextSnack;
    /**
     * Currently shown Snackbar.
     */
    private Snackbar snackbar;

    /**
     * Ensure we've created our instance.
     */
    private static void ensureInit() {
        if (INSTANCE == null) INSTANCE = new SnackKiosk();
    }

    /**
     * Attach a {@link Snacker} to the {@link SnackKiosk}, allowing us to show Snackbars.
     * <p>
     * This should not be called until the {@code snacker} is prepared to accept calls to {@link
     * Snacker#getSnackbarAnchorView()}, which could be called immediately if there is a queued Snackbar.
     * @param snacker {@link Snacker}.
     * @throws IllegalArgumentException if {@code snacker} is {@code null}.
     */
    public static void startSnacking(@NonNull Snacker snacker) {
        ensureInit();
        INSTANCE.attachSnacker(snacker);
    }

    /**
     * Detach the current {@link Snacker} from the {@link SnackKiosk}. Makes no attempts to hide a Snackbar if one is
     * currently showing.
     */
    public static void stopSnacking() {
        INSTANCE.detachSnacker();
    }

    /**
     * Show a Snackbar.
     * <p>
     * If a {@link Snacker} isn't currently attached, a Snackbar will be queued and shown the next time {@link
     * #attachSnacker(Snacker)} is called. However, only one Snackbar will be queued, so the last call made to any of
     * the {@code snack} methods during that time wins.
     * @param messageRes Message to show on the Snackbar.
     * @param duration   How long to show the Snackbar for.
     */
    public static void snack(@StringRes final int messageRes, final int duration) {
        snack(Minerva.get().getString(messageRes), -1, -1, duration);
    }

    /**
     * Show a Snackbar.
     * <p>
     * If a {@link Snacker} isn't currently attached, a Snackbar will be queued and shown the next time {@link
     * #attachSnacker(Snacker)} is called. However, only one Snackbar will be queued, so the last call made to any of
     * the {@code snack} methods during that time wins.
     * @param message  Message to show on the Snackbar.
     * @param duration How long to show the Snackbar for.
     */
    public static void snack(final String message, final int duration) {
        snack(message, -1, -1, duration);
    }

    /**
     * Show a Snackbar.
     * <p>
     * If a {@link Snacker} isn't currently attached, a Snackbar will be queued and shown the next time {@link
     * #attachSnacker(Snacker)} is called. However, only one Snackbar will be queued, so the last call made to any of
     * the {@code snack} methods during that time wins.
     * @param messageRes Message to show on the Snackbar.
     * @param actionRes  String resource to use for the action button's text. Can be {@code -1} to omit the action
     *                   button.
     * @param duration   How long to show the Snackbar for.
     */
    public static void snack(@StringRes final int messageRes, @StringRes final int actionRes, final int duration) {
        snack(Minerva.get().getString(messageRes), actionRes, -1, duration);
    }

    /**
     * Show a Snackbar.
     * <p>
     * If a {@link Snacker} isn't currently attached, a Snackbar will be queued and shown the next time {@link
     * #attachSnacker(Snacker)} is called. However, only one Snackbar will be queued, so the last call made to any of
     * the {@code snack} methods during that time wins.
     * @param message   Message to show on the Snackbar.
     * @param actionRes String resource to use for the action button's text. Can be {@code -1} to omit the action
     *                  button.
     * @param duration  How long to show the Snackbar for.
     */
    public static void snack(final String message, @StringRes final int actionRes, final int duration) {
        snack(message, actionRes, -1, duration);
    }

    /**
     * Show a Snackbar.
     * <p>
     * If a {@link Snacker} isn't currently attached, a Snackbar will be queued and shown the next time {@link
     * #attachSnacker(Snacker)} is called. However, only one Snackbar will be queued, so the last call made to any of
     * the {@code snack} methods during that time wins.
     * @param messageRes Message to show on the Snackbar.
     * @param actionRes  String resource to use for the action button's text. Can be {@code -1} to omit the action
     *                   button.
     * @param actionId   ID resource to help determine an action to take when the action button is pressed. Can be
     *                   {@code -1} to simply dismiss the Snackbar. Ignored if {@code actionRes} is {@code -1}.
     * @param duration   How long to show the Snackbar for.
     */
    public static void snack(@StringRes final int messageRes, @StringRes final int actionRes,
                             @IdRes final int actionId, final int duration) {
        snack(Minerva.get().getString(messageRes), actionRes, actionId, duration);
    }

    /**
     * Show a Snackbar.
     * <p>
     * If a {@link Snacker} isn't currently attached, a Snackbar will be queued and shown the next time {@link
     * #attachSnacker(Snacker)} is called. However, only one Snackbar will be queued, so the last call made to any of
     * the {@code snack} methods during that time wins.
     * @param message   Message to show on the Snackbar.
     * @param actionRes String resource to use for the action button's text. Can be {@code -1} to omit the action
     *                  button.
     * @param actionId  ID resource to help determine an action to take when the action button is pressed. Can be {@code
     *                  -1} to simply dismiss the Snackbar. Ignored if {@code actionRes} is {@code -1}.
     * @param duration  How long to show the Snackbar for.
     */
    public static void snack(final String message, @StringRes final int actionRes, @IdRes final int actionId,
                             final int duration) {
        ensureInit();
        INSTANCE.showOrEnqueueSnack(message, actionRes, actionId, duration);
    }

    /**
     * Checks to see if the current Snackbar has the given {@code actionId}.
     * @param actionId Action ID to check for.
     * @return True if the current Snackbar has the given {@code actionID}, false if it doesn't or a Snackbar isn't
     * being shown.
     */
    public static boolean isCurrentActionId(@IdRes final int actionId) {
        ensureInit();
        return INSTANCE.currentSnackHasActionId(actionId);
    }

    /**
     * Dismiss the currently shown Snackbar if it has the given {@code actionId}.
     * @param actionId Action ID to check for.
     */
    public static void dismissIfActionId(@IdRes final int actionId) {
        ensureInit();
        INSTANCE.dismissCurrentSnackIfActionId(actionId);
    }

    /**
     * If a Snackbar is showing, dismiss it.
     * <p>
     * If a {@link Snacker} isn't attached, removes the queued Snackbar (if there is one).
     */
    public static void dismissCurrent() {
        ensureInit();
        INSTANCE.dismissCurrentSnack();
    }

    // No public construction allowed.
    private SnackKiosk() {
    }

    /**
     * Attach a {@link Snacker} and show the {@link #nextSnack} (if it exists).
     * @param snacker {@link Snacker}.
     */
    private void attachSnacker(Snacker snacker) {
        if (snacker == null) throw new IllegalArgumentException("Snacker must not be null.");
        this.snacker = snacker;

        // Show the next snack, if it isn't null.
        if (nextSnack != null) {
            Snack snack = nextSnack;
            nextSnack = null;
            showSnackbar(snack);
        }
    }

    /**
     * Detach the current {@link #snacker}. Does not make any attempts to hide the current {@link #snackbar}.
     */
    private void detachSnacker() {
        snacker = null;
    }

    /**
     * Queue a new {@link Snack} to be shown as a Snackbar as soon as possible.
     * <p>
     * If {@link #snacker} isn't {@code null} when this is called, a Snackbar will be shown immediately.
     * @param message   Message to show on the Snackbar.
     * @param actionRes String resource to use for the action button's text. Can be {@code -1} to omit the action
     *                  button.
     * @param actionId  ID resource to help determine an action to take when the action button is pressed. Can be {@code
     *                  -1} to simply dismiss the Snackbar. Ignored if {@code actionRes} is {@code -1}.
     * @param duration  How long to show the Snackbar for.
     */
    private void showOrEnqueueSnack(final String message, @StringRes final int actionRes, @IdRes final int actionId,
                                    final int duration) {
        Snack newSnack = new Snack(message, actionRes, actionId, duration);
        if (snacker == null) nextSnack = newSnack;
        else showSnackbar(newSnack);
    }

    /**
     * Check to see if there is a Snackbar being shown whose {@link Snack}'s action ID matches {@code actionId}.
     * @param actionId Action ID to check for.
     * @return True if a Snackbar is being shown whose {@link Snack} has the given {@code actionId}.
     */
    private boolean currentSnackHasActionId(@IdRes final int actionId) {
        return snackbar != null && currSnack != null && currSnack.actionId == actionId;
    }

    /**
     * Dismiss the currently shown Snackbar if its action ID matches {@code actionId}.
     * @param actionId Action ID to check for.
     */
    private void dismissCurrentSnackIfActionId(@IdRes final int actionId) {
        if (currentSnackHasActionId(actionId)) snackbar.dismiss();
    }

    /**
     * Dismiss the currently shown Snackbar.
     */
    private void dismissCurrentSnack() {
        if (snackbar != null) snackbar.dismiss();
    }

    /**
     * Show a Snackbar using the given {@code snack}.
     * @param snack Information used to make and show a Snackbar.
     */
    private synchronized void showSnackbar(Snack snack) {
        // Get next snack and create basic snackbar, adding on callbacks.
        snackbar = Snackbar.make(snacker.getSnackbarAnchorView(), snack.message, snack.duration)
                           .setCallback(new Snackbar.Callback() {
                               @Override
                               public void onDismissed(Snackbar snackbar, int event) {
                                   super.onDismissed(snackbar, event);
                                   // Null our references unless we were dismissed because we made another Snackbar.
                                   if (event != DISMISS_EVENT_CONSECUTIVE) {
                                       SnackKiosk.this.currSnack = null;
                                       SnackKiosk.this.snackbar = null;
                                   }
                               }
                           });

        // Add an action button if necessary.
        if (snack.actionRes != -1) snackbar.setAction(snack.actionRes, v -> handleAction(v, snack.actionId));

        // Show the snackbar.
        snackbar.show();
        currSnack = snack;
    }

    /**
     * Called when the action button on a Snackbar is clicked.
     * @param actionId The ID of some action to take, or {@code -1} to just have the Snackbar dismiss itself.
     */
    private void handleAction(View v, @IdRes int actionId) {
        switch (actionId) {
            case R.id.sb_action_retry_perms_check:
                // Fire an event indicating we wish to retry our permissions check.
                EventBus.getDefault().post(new MissingPermEvent(Manifest.permission.WRITE_EXTERNAL_STORAGE, -1));
                break;
            case R.id.sb_action_open_import_activity:
                // Open the import activity.
                Util.startAct(v.getContext(), ImportActivity.class, null);
                break;
        }
    }

    /**
     * Holder class we use to queue Snackbars if we receive a request to show one while {@link #snacker} is {@code
     * null}.
     */
    private static class Snack {
        private final String message;
        @StringRes
        private final int actionRes;
        @IdRes
        private final int actionId;
        private final int duration;

        /**
         * Create a new {@link Snack}.
         * @param message   Message to show on the Snackbar.
         * @param actionRes String resource to use for the action button's text. Can be {@code -1} to omit the action
         *                  button.
         * @param actionId  ID resource to help determine an action to take when the action button is pressed. Can be
         *                  {@code -1} to simply dismiss the Snackbar. Ignored if {@code actionRes} is {@code -1}.
         * @param duration  How long to show the Snackbar for.
         */
        Snack(String message, @StringRes final int actionRes, @IdRes final int actionId, final int duration) {
            this.message = message;
            this.actionRes = actionRes;
            this.actionId = actionId;
            this.duration = duration;
        }
    }
}
