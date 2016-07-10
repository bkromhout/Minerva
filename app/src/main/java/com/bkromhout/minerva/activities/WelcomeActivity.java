package com.bkromhout.minerva.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.data.BackupUtils;
import com.bkromhout.minerva.data.DataUtils;
import com.bkromhout.minerva.events.PermGrantedEvent;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.util.Util;
import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.app.NavigationPolicy;
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;
import com.heinrichreimersoftware.materialintro.slide.Slide;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import rx.Observable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Super fancy material-design-compliant welcome activity.
 */
public class WelcomeActivity extends IntroActivity implements SnackKiosk.Snacker, FolderChooserDialog.FolderCallback {
    private static final String KEY_DEFERRED_ACTION = "deferred_action";
    private static final String KEY_RESTORE_PATH_INDEX = "restore_path_index";

    private static final int WELCOME = 0;
    private static final int PERMISSIONS = 1;
    private static final int CHOOSE_FOLDER = 2;
    private static final int RESTORE_DB = 3;
    private static final int FINISHED = 4;

    /**
     * Due to the way that our permissions handling works, we have to make sure that we wait to retry things until
     * onResume is called. These are the possible options currently.
     */
    private enum DeferredAction {
        OPEN_F_CHOOSER, SHOW_DB_BACKUPS_LIST
    }

    /**
     * What deferred action to take when {@link #onResume()} is called.
     */
    private DeferredAction deferredAction = null;
    /**
     * Path to the DB backup file the user wishes to restore when setup is finished.
     */
    private String restorePath = null;
    /**
     * Index of the restore path in the list of paths returned by {@link BackupUtils#getRestorableRealmFiles()}. This is
     * only used to make sure the same option is checked if the user opens the dialog again.
     */
    private int restorePathIdx = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setFullscreen(true);
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_DEFERRED_ACTION))
                deferredAction = DeferredAction.values()[savedInstanceState.getInt(KEY_DEFERRED_ACTION)];
            if (savedInstanceState.containsKey(C.RESTORE_PATH)) {
                restorePath = savedInstanceState.getString(C.RESTORE_PATH);
                restorePathIdx = savedInstanceState.getInt(KEY_RESTORE_PATH_INDEX);
            }
        }

        setButtonBackFunction(BUTTON_BACK_FUNCTION_BACK);

        setNavigationPolicy(new NavigationPolicy() {
            @Override
            public boolean canGoForward(int whichScreen) {
                switch (whichScreen) {
                    case PERMISSIONS:
                        return ContextCompat.checkSelfPermission(WelcomeActivity.this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                    case CHOOSE_FOLDER:
                        return Util.tryResolveDir(Minerva.prefs().getLibDir(null)) != null;
                    case WELCOME:
                    case RESTORE_DB:
                    case FINISHED:
                    default:
                        return true;
                }
            }

            @Override
            public boolean canGoBackward(int whichScreen) {
                return whichScreen != WELCOME;
            }
        });

        addSlides(makeSlides());
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

        // Resume a deferred action now.
        if (deferredAction != null) {
            switch (deferredAction) {
                case OPEN_F_CHOOSER:
                    onOpenFolderChooserClick();
                    break;
                case SHOW_DB_BACKUPS_LIST:
                    onRestoreDbClick();
                    break;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (deferredAction != null) outState.putInt(KEY_DEFERRED_ACTION, deferredAction.ordinal());
        if (restorePath != null) {
            outState.putString(C.RESTORE_PATH, restorePath);
            outState.putInt(KEY_RESTORE_PATH_INDEX, restorePathIdx);
        }
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

    /**
     * Called when a permission has been granted.
     * @param event {@link PermGrantedEvent}.
     */
    @Subscribe
    public void onPermGrantedEvent(PermGrantedEvent event) {
        switch (event.getActionId()) {
            case R.id.action_choose_lib_dir:
                deferredAction = DeferredAction.OPEN_F_CHOOSER;
                break;
            case R.id.action_restore_db:
                deferredAction = DeferredAction.SHOW_DB_BACKUPS_LIST;
                break;
        }
    }

    /**
     * Create Slides for the intro pager.
     * @return List of intro slides.
     */
    private List<Slide> makeSlides() {
        ArrayList<Slide> slides = new ArrayList<>();

        slides.add(new FragmentSlide.Builder()
                .fragment(R.layout.welcome_logo_slide)
                .background(R.color.brown500)
                .backgroundDark(R.color.brown700)
                .build());

        slides.add(new SimpleSlide.Builder()
                .title(R.string.storage_permission_needed)
                .description(R.string.storage_permission_rationale)
                .image(R.drawable.welcome_micro_sd_card)
                .background(R.color.red500)
                .backgroundDark(R.color.red700)
                .permission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .build());

        slides.add(new SimpleSlide.Builder()
                .layout(R.layout.welcome_extra_text_slide)
                .title(R.string.choose_library_dir)
                .description(R.string.choose_library_dir_desc)
                .image(R.drawable.welcome_book)
                .background(R.color.orange500)
                .backgroundDark(R.color.orange700)
                .buttonCtaLabel(R.string.open_folder_chooser)
                .buttonCtaClickListener(view -> onOpenFolderChooserClick())
                .build());

        slides.add(new SimpleSlide.Builder()
                .title(R.string.title_restore_db)
                .description(R.string.restore_database_desc)
                .image(R.drawable.welcome_restore_database)
                .background(R.color.blueGrey500)
                .backgroundDark(R.color.blueGrey700)
                .buttonCtaLabel(R.string.choose_database_backup)
                .buttonCtaClickListener(view -> onRestoreDbClick())
                .build());

        slides.add(new SimpleSlide.Builder()
                .title(R.string.ready_to_go)
                .description(R.string.ready_to_go_desc)
                .image(R.drawable.welcome_ready_to_go)
                .background(R.color.green500)
                .backgroundDark(R.color.green700)
                .build());

        return slides;
    }

    /**
     * Sets the text of the folder text view.
     * @param folderPath Path to the chosen library folder.
     */
    private void setFolderText(String folderPath) {
        View root = getSlide(CHOOSE_FOLDER).getFragment().getView();
        TextView desc = root != null ? (TextView) root.findViewById(R.id.mi_description) : null;
        TextView extra = root != null ? (TextView) root.findViewById(R.id.extra_slide_text) : null;
        boolean notNullOrEmpty = folderPath != null && !folderPath.isEmpty();
        if (extra != null && notNullOrEmpty) {
            if (desc != null) extra.setTextColor(desc.getTextColors());
            extra.setVisibility(View.VISIBLE);
            extra.setText(DataUtils.toSpannedHtml(String.format(getString(R.string.lbl_folder), folderPath)));
        } else if (extra != null) extra.setVisibility(View.GONE);
    }

    /**
     * Show the folder chooser when that button is clicked.
     */
    private void onOpenFolderChooserClick() {
        if (!Util.checkForStoragePermAndFireEventIfNeeded(R.id.action_choose_lib_dir)) return;
        // Set up most of dialog.
        FolderChooserDialog.Builder builder = new FolderChooserDialog.Builder(this)
                .chooseButton(R.string.ok)
                .cancelButton(R.string.cancel);

        // Check to see if the current value is a valid folder.
        String folderPath = Minerva.prefs().getLibDir(null);
        if (folderPath != null && new File(folderPath).exists()) builder.initialPath(folderPath);

        // Show the folder chooser dialog.
        builder.show();
    }

    /**
     * Click handler for the restore DB item.
     */
    private void onRestoreDbClick() {
        if (!Util.checkForStoragePermAndFireEventIfNeeded(R.id.action_restore_db)) return;
        // Get a list of backed up realm files. If there aren't any, tell the user that and we're done.
        final File[] backedUpRealmFiles = BackupUtils.getRestorableRealmFiles();
        if (backedUpRealmFiles.length == 0) {
            SnackKiosk.snack(R.string.sb_no_db_backups, Snackbar.LENGTH_SHORT);
            return;
        }

        // Show a dialog to let the user choose a file to restore.
        new MaterialDialog.Builder(this)
                .title(R.string.title_restore_db)
                .content(R.string.welcome_restore_backed_up_db_prompt)
                // Transform files to file names and use those as the items in the dialog.
                .items(Observable.from(backedUpRealmFiles)
                                 .map(File::getName)
                                 // Be sure we strip the extension and make the time part prettier.
                                 .map(s -> s.replace(BackupUtils.DB_BACKUP_EXT, "").replace("_", ":"))
                                 .toList()
                                 .toBlocking().single())
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .itemsCallbackSingleChoice(restorePathIdx, (dialog, itemView, which, text) -> {
                    // Save path to backup file for later.
                    restorePathIdx = which;
                    restorePath = backedUpRealmFiles[which].getAbsolutePath();
                    return true;
                })
                .show();
    }

    @Override
    public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
        String path = folder.getAbsolutePath();
        Minerva.prefs().putLibDir(path);
        setFolderText(path);
    }

    @Override
    public Intent makeReturnIntent() {
        // If the user wants to restore a DB backup, we should pass its path back to MainActivity.
        return restorePath != null ? new Intent().putExtra(C.RESTORE_PATH, restorePath) : null;
    }

    @NonNull
    @Override
    public View getSnackbarAnchorView() {
        return findViewById(R.id.mi_frame);
    }
}
