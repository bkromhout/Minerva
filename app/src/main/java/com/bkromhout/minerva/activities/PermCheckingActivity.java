package com.bkromhout.minerva.activities;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.data.ActionHelper;
import com.bkromhout.minerva.events.MissingPermEvent;
import com.bkromhout.minerva.events.PermGrantedEvent;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.util.Util;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.EmptyPermissionListener;
import com.karumi.dexter.listener.single.PermissionListener;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Base for activities which need to check permissions.
 * <p>
 * TODO It'd be nice to handle all of this stuff by ourselves rather than relaying on Dexter.
 */
public abstract class PermCheckingActivity extends AppCompatActivity {
    private static final String ON_GRANTED_ACTION_ID = "on_granted_action_id";
    /**
     * Permission listener for the Read External Storage permission.
     */
    private PermissionListener storagePL;
    /**
     * Reference to rationale dialog so that we can dismiss it if need be.
     */
    private MaterialDialog rationaleDialog;
    /**
     * ID of an action which should be taken if the permission being requested is granted.
     */
    private int onGrantedActionId = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) onGrantedActionId = savedInstanceState.getInt(ON_GRANTED_ACTION_ID, -1);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ON_GRANTED_ACTION_ID, onGrantedActionId);
    }

    @Override
    protected void onDestroy() {
        if (rationaleDialog != null) rationaleDialog.dismiss();
        super.onDestroy();
    }

    /**
     * Called when something has indicated that we are missing a permission that we need.
     * @param event {@link MissingPermEvent}.
     */
    @Subscribe
    public final void onMissingPermEvent(MissingPermEvent event) {
        // Make sure we don't start another request if one is already happening.
        if (!Dexter.isRequestOngoing()) {
            // If the nag snackbar is shown, get rid of it first.
            SnackKiosk.dismissIfActionId(R.id.sb_action_retry_perms_check);
            // Store the ID of the action we wish to take if the permission is granted.
            onGrantedActionId = event.getActionId();
            // Currently we only have one permission we'd need to check for, read external storage, so we don't bother
            // checking the permission string in the event.
            Dexter.checkPermission(storagePL, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    /**
     * Initialize state needed to check permissions, then continues any pending requests if necessary.
     * <p>
     * This should be called at some point in the concrete class's onCreate method.
     */
    protected final void initAndContinuePermChecksIfNeeded() {
        this.storagePL = new EmptyPermissionListener() {
            @Override
            public void onPermissionDenied(PermissionDeniedResponse response) {
                super.onPermissionDenied(response);
                // Cancel any deferred action.
                onGrantedActionId = -1;
                ActionHelper.cancelDeferredAction();
                // For a regular denial, just show the snackbar. For a permanent denial, show a dialog which has a
                // link to the app info screen.
                if (!response.isPermanentlyDenied()) showPermNagSnackbar();
                else showRationaleDialog(null);
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                super.onPermissionRationaleShouldBeShown(permission, token);
                if (permission.getName().equals(Manifest.permission.READ_EXTERNAL_STORAGE))
                    showRationaleDialog(token);
            }

            @Override
            public void onPermissionGranted(PermissionGrantedResponse response) {
                super.onPermissionGranted(response);
                EventBus.getDefault().post(new PermGrantedEvent(response.getPermissionName(), onGrantedActionId));
                onGrantedActionId = -1;
            }
        };

        Dexter.continuePendingRequestIfPossible(storagePL);
    }

    /**
     * Show dialog explaining why we need permission.
     * @param token Token to continue request. If this is nonnull, then we know we're showing this dialog because the
     *              permission was already permanently denied, not simply to provide rationale before requesting it.
     */
    private void showRationaleDialog(PermissionToken token) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(this).title(R.string.storage_permission);

        // Add dependant parts.
        if (token != null) {
            // This dialog is simply to provide rationale prior to showing the system's permission request dialog.
            builder.content(R.string.storage_permission_rationale)
                   .positiveText(R.string.got_it)
                   .onPositive((dialog, which) -> dialog.cancel())
                   .cancelListener(dialog -> token.continuePermissionRequest())
                   .dismissListener(dialog -> rationaleDialog = null);
        } else {
            // This dialog needs to provide a way to open the app info screen, because the permission was already
            // permanently denied.
            builder.content(R.string.storage_permission_rationale_long)
                   .positiveText(R.string.app_info)
                   .negativeText(R.string.not_now)
                   .onPositive((dialog, which) -> Util.openAppInfo())
                   .onNegative((dialog, which) -> dialog.cancel())
                   .cancelListener(dialog -> showPermNagSnackbar())
                   .dismissListener(dialog -> rationaleDialog = null);
        }

        rationaleDialog = builder.show();
    }

    /**
     * Show a snackbar to nag user to grant permission.
     */
    private void showPermNagSnackbar() {
        // Don't queue a second snackbar.
        if (SnackKiosk.isCurrentActionId(R.id.sb_action_retry_perms_check)) return;

        // This snackbar will make Dexter try to get the permission again.
        SnackKiosk.snack(R.string.storage_permission_needed, R.string.retry, R.id.sb_action_retry_perms_check,
                Snackbar.LENGTH_LONG);
    }
}
