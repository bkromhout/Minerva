package com.bkromhout.minerva;

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
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.bkromhout.minerva.data.Importer;
import com.bkromhout.minerva.prefs.DefaultPrefs;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.util.Util;
import org.greenrobot.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.Subject;

import java.io.File;

public class ImportActivity extends PermCheckingActivity implements Importer.ImportListener, SnackKiosk.Snacker,
        FolderChooserDialog.FolderCallback {
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
        File libDir = Util.tryResolveDir(DefaultPrefs.get().getLibDir(null));
        if (libDir == null) {
            // We don't have a library directory set, so we'll change the UI to have the user choose one.
            setButtonState(ButtonState.CHOOSE_DIR, true);
            setRedTextState(RedTextState.MUST_CHOOSE_FOLDER);
            needsToChooseDir = true;
        } else {
            // We're good!
            setReady();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // We don't have any events, but PermCheckingActivity does.
        EventBus.getDefault().register(this);
        Importer.get().registerListener(this);
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
        Importer.get().unregisterListener();
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
     * Update the text view which shows the last import time.
     */
    private void updateLastImportTime() {
        long lastTime = DefaultPrefs.get().getLastImportSuccessTime(-1);

        if (lastTime == -1) tvLastImportTime.setText(R.string.last_import_time_default);
        else tvLastImportTime.setText(Util.getRelTimeString(lastTime));
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
     * Show a dialog with choices for current and past logs when clicked.
     */
    @OnClick(R.id.choose_log)
    void onChooseLogClicked() {

    }

    /**
     * What to do when the button on the importer view is clicked.
     */
    @OnClick(R.id.import_button)
    void onButtonClick(View view) {
        switch (currBtnState) {
            case START_IMPORT:
                if (!Util.checkForStoragePermAndFireEventIfNeeded()) return;
                Importer.get().doFullImport(this);
                break;
            case CANCEL_IMPORT:
                Importer.get().cancelFullImport();
                break;
            case CHOOSE_DIR:
                if (!Util.checkForStoragePermAndFireEventIfNeeded()) return;
                // Set up most of dialog. Our SettingsActivity is the only possible host for this fragment.
                FolderChooserDialog.Builder builder = new FolderChooserDialog.Builder(this)
                        .chooseButton(R.string.ok)
                        .cancelButton(R.string.cancel);

                // Check to see if the current value is a valid folder.
                String folderPath = DefaultPrefs.get().getLibDir(null);
                if (folderPath != null && new File(folderPath).exists()) builder.initialPath(folderPath);

                // Show the folder chooser dialog.
                builder.show();
                break;
        }
    }

    @Override
    public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
        String path = folder.getAbsolutePath();
        DefaultPrefs.get().putLibDir(path);
        needsToChooseDir = false;
        setReady();
    }

    @Override
    public void setMaxProgress(int maxProgress) {
        progressBar.setMax(maxProgress);
    }

    @Override
    public Subscription subscribeToLogStream(Subject<String, String> logSubject) {
        return logSubject.onBackpressureBuffer().observeOn(AndroidSchedulers.mainThread()).subscribe(
                new Action1<String>() {
                    @Override
                    public void call(String s) {
                        if (s == null) tvLog.setText("");
                        else {
                            tvLog.append(s);
                            // Scroll log down as we append lines.
                            svLogCont.post(() -> svLogCont.fullScroll(View.FOCUS_DOWN));
                        }
                    }
                });
    }

    @Override
    public Subscription subscribeToErrorStream(Subject<String, String> errorSubject) {
        return errorSubject.onBackpressureBuffer().observeOn(AndroidSchedulers.mainThread()).subscribe(s -> {
            if (s == null) tvELog.setText("");
            else {
                tvELog.append(s);
                // Scroll log down as we append lines.
                svELogCont.post(() -> svELogCont.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    @Override
    public Subscription subscribeToProgressStream(Subject<Integer, Integer> progressSubject) {
        return progressSubject.observeOn(AndroidSchedulers.mainThread()).subscribe(i -> {
            if (i < 0) progressBar.setIndeterminate(true);
            else {
                progressBar.setIndeterminate(false);
                progressBar.setProgress(i);
            }
        });
    }

    @Override
    public void setReady() {
        // Don't switch do any of this if the user needs to choose a library directory.
        if (needsToChooseDir) return;
        progressBar.setIndeterminate(false);
        progressBar.setProgress(0);
        setHeaderState(HeaderState.READY);
        setButtonState(ButtonState.START_IMPORT, true);
        setRedTextState(RedTextState.NONE);
        updateLastImportTime();
    }

    @Override
    public void setRunning() {
        setHeaderState(HeaderState.IMPORTING);
        setButtonState(ButtonState.CANCEL_IMPORT, true);
        setRedTextState(RedTextState.NONE);
    }

    @Override
    public void setSaving() {
        setHeaderState(HeaderState.SAVING);
        setButtonState(ButtonState.CANCEL_IMPORT, false);
        setRedTextState(RedTextState.CANCEL_NOT_ALLOWED);
    }

    @Override
    public void setCancelling() {
        setHeaderState(HeaderState.CANCELLING);
        setButtonState(ButtonState.CANCEL_IMPORT, false);
        setRedTextState(RedTextState.NONE);
    }

    @Override
    public void setCancelled() {
        // Does the same thing for now.
        setReady();
    }

    @NonNull
    @Override
    public View getSnackbarAnchorView() {
        return base;
    }
}
