package com.bkp.minerva.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.ViewGroup;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.EmptyPermissionListener;

/**
 * This is really just a copy of the code in SnackbarOnDeniedPermissionListener, plus an overridden
 * onPermissionRationaleShouldBeShown method. I'd extend the actual class, but its constructor is private, so that's not
 * an option.
 */
public class SnackbarPermListener extends EmptyPermissionListener {

    private final ViewGroup rootView;
    private final String text;
    private final String buttonText;
    private final View.OnClickListener onButtonClickListener;

    /**
     * @param rootView              Parent view to show the snackbar
     * @param text                  Message displayed in the snackbar
     * @param buttonText            Message displayed in the snackbar button
     * @param onButtonClickListener Action performed when the user clicks the snackbar button
     */
    private SnackbarPermListener(ViewGroup rootView, String text, String buttonText,
                                 View.OnClickListener onButtonClickListener) {
        this.rootView = rootView;
        this.text = text;
        this.buttonText = buttonText;
        this.onButtonClickListener = onButtonClickListener;
    }

    @Override
    public void onPermissionDenied(PermissionDeniedResponse response) {
        super.onPermissionDenied(response);

        Snackbar snackbar = Snackbar.make(rootView, text, Snackbar.LENGTH_LONG);
        if (buttonText != null && onButtonClickListener != null) {
            snackbar.setAction(buttonText, onButtonClickListener);
        }
        snackbar.show();
    }

    @Override
    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
        super.onPermissionRationaleShouldBeShown(permission, token);


    }

    /**
     * Builder class to configure the displayed snackbar Non set fields will not be shown
     */
    public static class Builder {
        private final ViewGroup rootView;
        private final String text;
        private String buttonText;
        private View.OnClickListener onClickListener;

        private Builder(ViewGroup rootView, String text) {
            this.rootView = rootView;
            this.text = text;
        }

        public static Builder with(ViewGroup rootView, String text) {
            return new Builder(rootView, text);
        }

        public static Builder with(ViewGroup rootView, @StringRes int textResourceId) {
            return Builder.with(rootView, rootView.getContext().getString(textResourceId));
        }

        /**
         * Adds a text button with the provided click listener
         */
        public Builder withButton(String buttonText, View.OnClickListener onClickListener) {
            this.buttonText = buttonText;
            this.onClickListener = onClickListener;
            return this;
        }

        /**
         * Adds a text button with the provided click listener
         */
        public Builder withButton(@StringRes int buttonTextResourceId, View.OnClickListener onClickListener) {
            return withButton(rootView.getContext().getString(buttonTextResourceId), onClickListener);
        }

        /**
         * Adds a button that opens the application settings when clicked
         */
        public Builder withOpenSettingsButton(String buttonText) {
            this.buttonText = buttonText;
            this.onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = rootView.getContext();
                    Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + context.getPackageName()));
                    myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                    myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(myAppSettings);
                }
            };
            return this;
        }

        /**
         * Adds a button that opens the application settings when clicked
         */
        public Builder withOpenSettingsButton(@StringRes int buttonTextResourceId) {
            return withOpenSettingsButton(rootView.getContext().getString(buttonTextResourceId));
        }

        /**
         * Builds a new instance of {@link SnackbarPermListener}
         */
        public SnackbarPermListener build() {
            return new SnackbarPermListener(rootView, text, buttonText, onClickListener);
        }
    }
}
