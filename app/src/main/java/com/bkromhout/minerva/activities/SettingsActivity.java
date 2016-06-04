package com.bkromhout.minerva.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.Prefs;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.data.ActionHelper;
import com.bkromhout.minerva.data.BackupUtils;
import com.bkromhout.minerva.enums.MarkType;
import com.bkromhout.minerva.events.PermGrantedEvent;
import com.bkromhout.minerva.realm.RTag;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.util.Util;
import io.realm.Realm;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import rx.Observable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        Minerva.prefs().putLibDir(folder.getAbsolutePath());
    }

    @NonNull
    @Override
    public View getSnackbarAnchorView() {
        return coordinator;
    }

    /**
     * Custom PreferenceFragment.
     */
    public static class SettingsFragment extends PreferenceFragment implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        /**
         * Preferences.
         */
        private Prefs prefs = Minerva.prefs();

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
            Preference libDir = getPreferenceScreen().findPreference(Prefs.LIB_DIR);
            libDir.setOnPreferenceClickListener(this::onLibDirPrefClick);
            libDir.setSummary(prefs.getLibDir(""));

            // Set up the new book tag preference.
            Preference newBookTag = getPreferenceScreen().findPreference(Prefs.NEW_BOOK_TAG);
            newBookTag.setOnPreferenceClickListener(this::onNewBookTagPrefClick);
            String newBookTagVal = prefs.getNewBookTag(null);
            newBookTag.setSummary(
                    newBookTagVal != null ? Minerva.get().getString(R.string.summary_tag_as, newBookTagVal) : "");

            // Set up the updated book tag preference.
            Preference updatedBookTag = getPreferenceScreen().findPreference(Prefs.UPDATED_BOOK_TAG);
            updatedBookTag.setOnPreferenceClickListener(this::onUpdatedBookTagPrefClick);
            String updatedBookTagVal = prefs.getUpdatedBookTag(null);
            updatedBookTag.setSummary(updatedBookTagVal != null
                    ? Minerva.get().getString(R.string.summary_tag_as, updatedBookTagVal) : "");

            // Set up the backup DB item.
            Preference backupDb = getPreferenceScreen().findPreference(getString(R.string.key_backup_db));
            backupDb.setOnPreferenceClickListener(this::onBackupDbClick);

            // Set up the restore DB item.
            Preference restoreDb = getPreferenceScreen().findPreference(getString(R.string.key_restore_db));
            restoreDb.setOnPreferenceClickListener(this::onRestoreDbClick);
        }

        @Override
        public void onStart() {
            super.onStart();
            EventBus.getDefault().register(this);
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

        @Override
        public void onStop() {
            super.onStop();
            EventBus.getDefault().unregister(this);
        }

        /**
         * Called when a permission has been granted.
         * @param event {@link PermGrantedEvent}.
         */
        @Subscribe
        public void onPermGrantedEvent(PermGrantedEvent event) {
            if (event.getActionId() == R.id.action_choose_lib_dir) onLibDirPrefClick(null);
        }

        /**
         * Listen for preference changes.
         * @param sharedPreferences The default shared preferences.
         * @param key               The key string for the preference that changed.
         */
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(Prefs.LIB_DIR)) {
                // Update the summary for the library directory.
                getPreferenceScreen().findPreference(Prefs.LIB_DIR)
                                     .setSummary(sharedPreferences.getString(key, ""));
            } else if (key.equals(Prefs.NEW_BOOK_TAG)) {
                String value = sharedPreferences.getString(key, null);
                // Update the summary for the new book tag.
                getPreferenceScreen().findPreference(Prefs.NEW_BOOK_TAG)
                                     .setSummary(
                                             value != null ? Minerva.get().getString(R.string.summary_tag_as, value) :
                                                     "");
            } else if (key.equals(Prefs.UPDATED_BOOK_TAG)) {
                String value = sharedPreferences.getString(key, null);
                // Updated the summary for the updated book tag.
                getPreferenceScreen().findPreference(Prefs.UPDATED_BOOK_TAG)
                                     .setSummary(
                                             value != null ? Minerva.get().getString(R.string.summary_tag_as, value) :
                                                     "");
            }
        }

        /**
         * Click handler for the library directory preference.
         * @param preference The actual preference.
         * @return Always {@code true}, since we always handle the click.
         */
        private boolean onLibDirPrefClick(Preference preference) {
            if (!Util.checkForStoragePermAndFireEventIfNeeded(R.id.action_choose_lib_dir)) return true;

            // Set up most of dialog. Our SettingsActivity is the only possible host for this fragment.
            FolderChooserDialog.Builder builder = new FolderChooserDialog.Builder((SettingsActivity) getActivity())
                    .chooseButton(R.string.ok)
                    .cancelButton(R.string.cancel);

            // Check to see if the current value is a valid folder.
            String folderPath = prefs.getLibDir(null);
            if (folderPath != null && new File(folderPath).exists()) builder.initialPath(folderPath);

            // Show the folder chooser dialog.
            builder.show();
            return true;
        }

        /**
         * Click handler for the new book tag preference.
         * @param preference The actual preference.
         * @return Always {@code true}, since we always handle the click.
         */
        private boolean onNewBookTagPrefClick(Preference preference) {
            showTagChooserOrSnackbar(MarkType.NEW);
            return true;
        }

        /**
         * Click handler for the updated book tag preference.
         * @param preference The actual preference.
         * @return Always {@code true}, since we always handle the click.
         */
        private boolean onUpdatedBookTagPrefClick(Preference preference) {
            showTagChooserOrSnackbar(MarkType.UPDATED);
            return true;
        }

        /**
         * Show a dialog with a list of tags, or a snackbar stating that no tags exist.
         * @param markType Which mark to choose a tag for.
         */
        private void showTagChooserOrSnackbar(MarkType markType) {
            // Get list of tag names.
            List<String> tagNames = new ArrayList<>();
            try (Realm realm = Realm.getDefaultInstance()) {
                RealmResults<RTag> tags = realm.where(RTag.class).findAllSorted("sortName");
                for (RTag tag : tags) tagNames.add(tag.name);
            }

            // Remove the name of the tag which is in use as the updated tag (if we're picking the new tag), or as the
            // new tag (if we're picking the updated tag).
            String remove = markType == MarkType.NEW ? prefs.getUpdatedBookTag(null) :
                    prefs.getNewBookTag(null);
            if (remove != null) tagNames.remove(remove);

            // If we don't have any names, show a snackbar saying that, then return.
            if (tagNames.isEmpty()) {
                SnackKiosk.snack(R.string.sb_no_available_tags, Snackbar.LENGTH_SHORT);
                return;
            }

            // Create dialog to let user pick a new tag to use.
            new MaterialDialog.Builder(getActivity())
                    .title(R.string.title_dialog_choose_tag)
                    .negativeText(R.string.cancel)
                    .neutralText(R.string.clear)
                    .items(tagNames)
                    .onNeutral((dialog, which) -> {
                        String oldTagName = null;
                        // Change preference.
                        if (markType == MarkType.NEW) {
                            oldTagName = prefs.getNewBookTag(null);
                            prefs.putNewBookTag(null);
                        } else if (markType == MarkType.UPDATED) {
                            oldTagName = prefs.getUpdatedBookTag(null);
                            prefs.putUpdatedBookTag(null);
                        }
                        // Remove tag from books.
                        ActionHelper.replaceMarkTagOnBooks(markType, oldTagName, null);
                    })
                    .itemsCallback((dialog, itemView, which, text) -> {
                        String oldTagName = null;
                        // Change preference.
                        if (markType == MarkType.NEW) {
                            oldTagName = prefs.getNewBookTag(null);
                            prefs.putNewBookTag(text.toString());
                        } else if (markType == MarkType.UPDATED) {
                            oldTagName = prefs.getUpdatedBookTag(null);
                            prefs.putUpdatedBookTag(text.toString());
                        }
                        // Replace tag on books.
                        ActionHelper.replaceMarkTagOnBooks(markType, oldTagName, text.toString());
                    })
                    .show();
        }

        /**
         * Click handler for the backup DB item.
         * @param preference The actual preference.
         * @return Always {@code true}, since we always handle the click.
         */
        private boolean onBackupDbClick(Preference preference) {
            BackupUtils.backupRealmFile();
            return true;
        }

        /**
         * Click handler for the restore DB item.
         * @param preference The actual preference.
         * @return Always {@code true}, since we always handle the click.
         */
        private boolean onRestoreDbClick(Preference preference) {
            // Get a list of backed up realm files. If there aren't any, tell the user that and we're done.
            List<File> backedUpRealmFiles = BackupUtils.getRestorableRealmFiles();
            if (backedUpRealmFiles.isEmpty()) {
                SnackKiosk.snack(R.string.sb_no_db_backups, Snackbar.LENGTH_SHORT);
                return true;
            }

            // Show a dialog to let the user choose a file to restore.
            new MaterialDialog.Builder(getActivity())
                    .title(R.string.title_restore_db)
                    .content(R.string.prompt_choose_backed_up_db)
                    // Transform files to file names and use those as the items in the dialog.
                    .items(Observable.from(backedUpRealmFiles)
                                     .map(File::getName)
                                     .toList().toBlocking().single())
                    .positiveText(R.string.action_restore)
                    .negativeText(R.string.cancel)
                    .itemsCallbackSingleChoice(-1, (dialog, itemView, which, text) -> {
                        BackupUtils.restoreRealmFile(which);
                        return true;
                    })
                    .show();
            return true;
        }
    }
}
