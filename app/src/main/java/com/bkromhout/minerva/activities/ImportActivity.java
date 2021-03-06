package com.bkromhout.minerva.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.data.ImportLogger;
import com.bkromhout.minerva.data.Importer;
import com.bkromhout.minerva.events.PermGrantedEvent;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.util.Util;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import rx.Observer;

import java.io.File;
import java.util.List;

/**
 * Activity which listens to our {@link Importer} and {@link ImportLogger} and displays the information which they
 * provide.
 * <p>
 * Provides a place to manually trigger a full import and to view current and past import logs.
 */
public class ImportActivity extends PermCheckingActivity implements FolderChooserDialog.FolderCallback,
        Importer.ImportStateListener, ImportLogger.ImportLogListener, SnackKiosk.Snacker {
    /**
     * States that the header can be in.
     */
    private enum HeaderState {
        INIT, READY, IMPORTING, SAVING, CANCELLING
    }

    /**
     * States that the log view can be in.
     */
    private enum LogState {
        FULL, ERRORS
    }

    /**
     * States that the button can be in.
     */
    private enum ButtonState {
        START_IMPORT, CANCEL_IMPORT, CHOOSE_DIR
    }

    /**
     * States that the red text can be in.
     */
    private enum RedTextState {
        CANCEL_NOT_ALLOWED, MUST_CHOOSE_FOLDER, NONE
    }

    // Views
    @BindView(R.id.base)
    ViewGroup base;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.header)
    TextView tvHeader;
    @BindView(R.id.last_import_time)
    TextView tvLastImportTime;
    @BindView(R.id.import_progress)
    ProgressBar progressBar;
    @BindView(R.id.num_queued)
    TextView tvNumQueued;
    @BindView(R.id.lbl_log)
    TextView tvLogLabel;
    @BindView(R.id.import_log_cont)
    ScrollView svLogCont;
    @BindView(R.id.import_log)
    TextView tvLog;
    @BindView(R.id.import_elog_cont)
    ScrollView svELogCont;
    @BindView(R.id.import_elog)
    TextView tvELog;
    @BindView(R.id.import_log_type)
    Switch swLogType;
    @BindView(R.id.import_red_text)
    TextView tvRedText;
    @BindView(R.id.import_button)
    Button btnMain;

    /**
     * What state the button is in currently.
     */
    private ButtonState currBtnState;
    /**
     * Whether or not a library dir needs to be chosen.
     */
    private boolean needsToChooseDir = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set theme, create and bind views.
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_import);
        ButterKnife.bind(this);

        // Set up toolbar.
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initUi();

        // Handle permissions. Make sure we continue a request process if applicable.
        initAndContinuePermChecksIfNeeded();
    }

    /**
     * Initialize the UI.
     */
    private void initUi() {
        setHeaderState(HeaderState.INIT);

        // Set up the log type switch.
        swLogType.setOnCheckedChangeListener((v, isOn) -> setLogState(isOn ? LogState.ERRORS : LogState.FULL));

        // Check if the importer is already running; if it is then all we need to do is register as the listener,
        // because the fact that it's running allows us to make certain assumptions.
        Importer importer = Importer.get();
        if (!importer.isReady()) return; // Just return, we automatically register during onStart().

        // The importer isn't running, so we need to do a bit of work first, starting with checking if we have a
        // library directory set.
        File libDir = Util.tryResolveDir(Minerva.prefs().getLibDir(null));
        if (libDir == null) {
            // We don't have a library directory set, so we'll change the UI to have the user choose one.
            setButtonState(ButtonState.CHOOSE_DIR, true);
            setRedTextState(RedTextState.MUST_CHOOSE_FOLDER);
            needsToChooseDir = true;
        } else {
            // We're good! Pretend this got called with READY.
            onImportStateChanged(Importer.State.READY);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // We don't have any events, but PermCheckingActivity does.
        EventBus.getDefault().register(this);
        ImportLogger.get().startListening(this);
        Importer.get().startListening(this);
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
        Importer.get().stopListening();
        ImportLogger.get().stopListening();
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Change the state of the header text.
     * @param newState New header state.
     */
    private void setHeaderState(HeaderState newState) {
        switch (newState) {
            case INIT:
                tvHeader.setText(R.string.import_header_initializing);
                break;
            case READY:
                tvHeader.setText(R.string.import_header_ready);
                break;
            case IMPORTING:
                tvHeader.setText(R.string.import_header_importing);
                break;
            case SAVING:
                tvHeader.setText(R.string.import_header_saving);
                break;
            case CANCELLING:
                tvHeader.setText(R.string.import_header_cancelling);
                break;
        }
    }

    /**
     * Change the state of the log.
     * @param newState New log state.
     */
    private void setLogState(LogState newState) {
        switch (newState) {
            case FULL:
                svELogCont.setVisibility(View.GONE);
                svLogCont.setVisibility(View.VISIBLE);
                break;
            case ERRORS:
                svLogCont.setVisibility(View.GONE);
                svELogCont.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Change the state of the button.
     * @param newState New button state.
     * @param enabled  Whether or not to have the button enabled.
     */
    private void setButtonState(ButtonState newState, boolean enabled) {
        btnMain.setEnabled(enabled);
        switch (newState) {
            case START_IMPORT:
                btnMain.setText(R.string.start_full_import);
                break;
            case CANCEL_IMPORT:
                btnMain.setText(R.string.cancel);
                break;
            case CHOOSE_DIR:
                btnMain.setText(R.string.choose_library_dir);
                break;
        }
        currBtnState = newState;
    }

    /**
     * Change the state of the red text.
     * @param newState New red text state.
     */
    private void setRedTextState(RedTextState newState) {
        switch (newState) {
            case CANCEL_NOT_ALLOWED:
                tvRedText.setText(R.string.import_red_no_cancel);
                break;
            case MUST_CHOOSE_FOLDER:
                tvRedText.setText(R.string.import_red_choose_dir);
                break;
            case NONE:
                tvRedText.setVisibility(View.GONE);
                return;
        }
        tvRedText.setVisibility(View.VISIBLE);
    }

    /**
     * Called when a permission has been granted.
     * @param event {@link PermGrantedEvent}.
     */
    @Subscribe
    public void onPermGrantedEvent(PermGrantedEvent event) {
        switch (event.getActionId()) {
            case R.id.action_choose_lib_dir:
            case R.id.action_import:
                onButtonClick();
                break;
        }
    }

    /**
     * Show a dialog with choices for current and past logs when clicked.
     */
    @OnClick(R.id.choose_log)
    void onChooseLogClicked() {
        List<String> logs = ImportLogger.get().getLogList();
        if (logs.isEmpty()) return;
        // Show a dialog with log names as the options. When one is clicked, the ImportLogger shows it using our UI.
        new MaterialDialog.Builder(this)
                .title(R.string.lbl_choose_log)
                .negativeText(R.string.cancel)
                .items(logs)
                .itemsCallback((dialog, itemView, which, text) -> ImportLogger.get().switchLogs(which))
                .show();
    }

    /**
     * What to do when the button on the importer view is clicked.
     */
    @OnClick(R.id.import_button)
    void onButtonClick() {
        switch (currBtnState) {
            case START_IMPORT:
                if (!Util.checkForStoragePermAndFireEventIfNeeded(R.id.action_import)) return;
                Importer.get().queueFullImport();
                break;
            case CANCEL_IMPORT:
                Importer.get().cancelImportRun();
                break;
            case CHOOSE_DIR:
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
                break;
        }
    }

    @Override
    public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
        String path = folder.getAbsolutePath();
        Minerva.prefs().putLibDir(path);
        needsToChooseDir = false;
        // Pretend this got called with READY.
        onImportStateChanged(Importer.State.READY);
    }

    @Override
    public void setLatestSuccessfulRun(long time) {
        // Either "Never" or a relative time string.
        if (time == -1) tvLastImportTime.setText(R.string.last_import_time_default);
        else tvLastImportTime.setText(Util.getRelTimeString(time));
    }

    @Override
    public void setNumQueued(int numQueued) {
        // Either "# Queued" or empty.
        tvNumQueued.setText(numQueued > 0 ? Minerva.get().getString(R.string.num_queued, numQueued) : null);
    }

    /**
     * Make a log label string using the partial string given.
     * @param part Partial log label string.
     * @return Log label string.
     */
    private String makeLogLabel(String part) {
        return Minerva.get().getString(R.string.log_label, part);
    }

    @Override
    public void setCurrLogLabel(String logLabelPart) {
        tvLogLabel.setText(makeLogLabel(logLabelPart));
    }

    @Override
    public void setCurrLogs(String fullLog, String errorLog) {
        tvLog.setText(fullLog);
        tvELog.setText(errorLog != null && !errorLog.equals("null") ? errorLog : "");
    }

    @Override
    public void onProgressFlag(int maxProgress) {
        // Change indeterminate state of the progress bar.
        progressBar.setIndeterminate(maxProgress == Importer.SET_PROGRESS_INDETERMINATE);

        if (maxProgress == Importer.SET_PROGRESS_DETERMINATE_ZERO) // Set progress to 0
            progressBar.setProgress(0);
        else if (maxProgress >= 0) // Set the max progress
            progressBar.setMax(maxProgress);
    }

    @Override
    public void onImportStateChanged(Importer.State newState) {
        switch (newState) {
            case READY:
                // Don't switch do any of this if the user needs to choose a library directory.
                if (needsToChooseDir) return;
                progressBar.setIndeterminate(false);
                progressBar.setProgress(0);
                setHeaderState(HeaderState.READY);
                setButtonState(ButtonState.START_IMPORT, true);
                setRedTextState(RedTextState.NONE);
                break;
            case PREP:
            case IMPORTING:
                setHeaderState(HeaderState.IMPORTING);
                setButtonState(ButtonState.CANCEL_IMPORT, true);
                setRedTextState(RedTextState.NONE);
                break;
            case SAVING:
                setHeaderState(HeaderState.SAVING);
                setButtonState(ButtonState.CANCEL_IMPORT, false);
                setRedTextState(RedTextState.CANCEL_NOT_ALLOWED);
                break;
            case CANCELLING:
            case FINISHING:
                setHeaderState(HeaderState.CANCELLING);
                setButtonState(ButtonState.CANCEL_IMPORT, false);
                setRedTextState(RedTextState.NONE);
                break;
        }
    }

    @NonNull
    @Override
    public Observer<String> getFullLogObserver() {
        return new Observer<String>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(String s) {
                if (s == null) tvLog.setText("");
                else {
                    tvLog.append(s);
                    // Scroll log down as we append lines.
                    svLogCont.post(() -> svLogCont.fullScroll(View.FOCUS_DOWN));
                }
            }
        };
    }

    @NonNull
    @Override
    public Observer<String> getErrorLogObserver() {
        return new Observer<String>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(String s) {
                if (s == null) tvELog.setText("");
                else {
                    tvELog.append(s);
                    // Scroll log down as we append lines.
                    svELogCont.post(() -> svELogCont.fullScroll(View.FOCUS_DOWN));
                }
            }
        };
    }

    @NonNull
    @Override
    public Observer<Integer> getProgressObserver() {
        return new Observer<Integer>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Integer i) {
                progressBar.setProgress(i);
            }
        };
    }

    @NonNull
    @Override
    public View getSnackbarAnchorView() {
        return base;
    }
}
