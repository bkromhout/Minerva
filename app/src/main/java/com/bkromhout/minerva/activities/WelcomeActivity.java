package com.bkromhout.minerva.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.data.BackupUtils;
import com.bkromhout.minerva.data.Importer;
import com.bkromhout.minerva.events.PermGrantedEvent;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.util.Util;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import rx.Observable;

import java.io.File;

public class WelcomeActivity extends PermCheckingActivity implements SnackKiosk.Snacker,
        FolderChooserDialog.FolderCallback {
    /**
     * Due to the way that our permissions handling works, we have to make sure that we wait to retry things until
     * onResume is called. These are the possible options currently.
     */
    private enum DeferredAction {
        OPEN_F_CHOOSER, SHOW_DB_BACKUPS_LIST, START_IMPORT
    }

    // Views
    @BindView(R.id.base)
    ViewGroup base;
    @BindView(R.id.lbl_folder)
    TextView tvFolderLbl;
    @BindView(R.id.folder)
    TextView tvFolder;
    @BindView(R.id.first_import_prompt)
    TextView tvFirstImportPrompt;
    @BindView(R.id.start_full_import)
    Button btnStartFullImport;
    @BindView(R.id.restore_db_backup)
    Button btnRestoreDb;

    /**
     * What deferred action to take when {@link #onResume()} is called.
     */
    private DeferredAction deferredAction = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        setContentView(R.layout.activity_welcome);
        ButterKnife.bind(this);

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

        // Resume a deferred action now.
        if (deferredAction != null) {
            switch (deferredAction) {
                case OPEN_F_CHOOSER:
                    onChooseFolderClick();
                    break;
                case SHOW_DB_BACKUPS_LIST:
                    // TODO
                    break;
                case START_IMPORT:
                    onStartFullImportClicked();
                    break;
            }
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
            case R.id.action_import:
                deferredAction = DeferredAction.START_IMPORT;
                break;
        }
    }

    @Override
    public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
        String path = folder.getAbsolutePath();
        Minerva.prefs().putLibDir(path);

        // Set the path of the folder to the textview.
        tvFolder.setText(path);

        // Show the rest of the views.
        tvFolderLbl.setVisibility(View.VISIBLE);
        tvFolder.setVisibility(View.VISIBLE);
        tvFirstImportPrompt.setVisibility(View.VISIBLE);
        btnStartFullImport.setVisibility(View.VISIBLE);
        btnRestoreDb.setVisibility(View.VISIBLE);
    }

    /**
     * Show the folder chooser when that button is clicked.
     */
    @OnClick(R.id.choose_folder)
    void onChooseFolderClick() {
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
     * Trigger a full import, then finish and show the {@link ImportActivity} when clicked.
     */
    @OnClick(R.id.start_full_import)
    void onStartFullImportClicked() {
        if (!Util.checkForStoragePermAndFireEventIfNeeded(R.id.action_import)) return;
        Importer.get().queueFullImport();
        Minerva.prefs().setFirstImportTriggered();
        setResult(RESULT_OK);
        finish();
    }

    /**
     * Click handler for the restore DB item.
     */
    @OnClick(R.id.restore_db_backup)
    void onRestoreDbClick() {
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
                .content(R.string.prompt_choose_backed_up_db)
                // Transform files to file names and use those as the items in the dialog.
                .items(Observable.from(backedUpRealmFiles)
                                 .map(File::getName)
                                 // Be sure we strip the extension and make the time part prettier.
                                 .map(s -> s.replace(BackupUtils.DB_BACKUP_EXT, "").replace("_", ":"))
                                 .toList()
                                 .toBlocking().single())
                .positiveText(R.string.action_restore)
                .negativeText(R.string.cancel)
                .itemsCallbackSingleChoice(-1, (dialog, itemView, which, text) -> {
                    BackupUtils.prepareToRestoreRealmFile(this, backedUpRealmFiles[which]);
                    return true;
                })
                .show();
    }

    @NonNull
    @Override
    public View getSnackbarAnchorView() {
        return base;
    }
}
