package com.bkromhout.minerva;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.bkromhout.minerva.prefs.DefaultPrefs;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.util.Util;
import org.greenrobot.eventbus.EventBus;

import java.io.File;

/**
 * Settings activity, just loads a custom PreferenceFragment.
 */
public class SettingsActivity extends PermCheckingActivity implements FolderChooserDialog.FolderCallback,
        SnackKiosk.Snacker {
    // Views
    private CoordinatorLayout coordinator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        coordinator = ButterKnife.findById(this, R.id.coordinator);

        // Set up toolbar.
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Show preference fragment.
        getFragmentManager().beginTransaction().replace(R.id.content, new SettingsFragment()).commit();

        // Handle permissions. Make sure we continue a request process if applicable.
        initAndContinuePermChecksIfNeeded();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // We don't have any events, but PermCheckingActivity does.
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SnackKiosk.startSnacking(this);
    }

    @Override
    protected void onPause() {
        SnackKiosk.stopSnacking();
        super.onPause();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This isn't really kosher, but since the about activity isn't something which needs proper Up
                // navigation, we'd rather treat it like the back button.
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback from folder chooser dialog shown when the library directory textview is clicked.
     * @param folder Chosen folder.
     */
    @Override
    public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
        // Updating this preference cause the fragment to notice and update the summary for the library directory
        // preference, since the fragment implements OnSharedPreferenceChangeListener.
        DefaultPrefs.get().putLibDir(folder.getAbsolutePath());
    }

    @NonNull
    @Override
    public View getSnackbarAnchorView() {
        return coordinator;
    }

    @Override
    public Activity getCtx() {
        return this;
    }

    /**
     * Custom PreferenceFragment.
     */
    public static class SettingsFragment extends PreferenceFragment implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            // Init the UI.
            initUi();
        }

        /**
         * Init the UI.
         */
        private void initUi() {
            // Set up the library directory preference.
            Preference libDir = getPreferenceScreen().findPreference(DefaultPrefs.LIB_DIR);
            libDir.setOnPreferenceClickListener(this::onLibDirPrefClick);
            libDir.setSummary(DefaultPrefs.get().getLibDir(""));
        }

        @Override
        public void onResume() {
            super.onResume();
            // Register SharedPreferences change listener.
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            // Unregister SharedPreferences change listener.
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        /**
         * Listen for preference changes.
         * @param sharedPreferences The default shared
         * @param key               The key string for the preference that changed.
         */
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(DefaultPrefs.LIB_DIR)) {
                // Update the summary for the library directory fragment to be the path.
                getPreferenceScreen().findPreference(DefaultPrefs.LIB_DIR)
                                     .setSummary(sharedPreferences.getString(key, ""));
            }
        }

        /**
         * Click handler for the library directory preference.
         * @param preference The actual preference.
         * @return Always true, since we always handle the click.
         */
        private boolean onLibDirPrefClick(Preference preference) {
            if (!Util.checkForStoragePermAndFireEventIfNeeded()) return true;

            // Set up most of dialog. Our SettingsActivity is the only possible host for this fragment.
            FolderChooserDialog.Builder builder = new FolderChooserDialog.Builder((SettingsActivity) getActivity())
                    .chooseButton(R.string.ok)
                    .cancelButton(R.string.cancel);

            // Check to see if the current value is a valid folder.
            String folderPath = DefaultPrefs.get().getLibDir(null);
            if (folderPath != null && new File(folderPath).exists()) builder.initialPath(folderPath);

            // Show the folder chooser dialog.
            builder.show();
            return true;
        }
    }
}
