package com.bkromhout.minerva.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.data.Importer;
import com.bkromhout.minerva.events.PermGrantedEvent;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.util.Util;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

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

    @NonNull
    @Override
    public View getSnackbarAnchorView() {
        return base;
    }
}
