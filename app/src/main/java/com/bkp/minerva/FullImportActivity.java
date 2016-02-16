package com.bkp.minerva;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.bkp.minerva.prefs.DefaultPrefs;
import com.bkp.minerva.util.FullImporter;
import com.bkp.minerva.util.Util;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.Subject;

import java.io.File;

public class FullImportActivity extends AppCompatActivity implements FullImporter.IFullImportListener,
        FolderChooserDialog.FolderCallback {
    /**
     * States that the header can be in.
     */
    private enum HeaderState {
        INIT, READY, IMPORTING, SAVING, CANCELLING
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
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.header)
    TextView tvHeader;
    @Bind(R.id.import_folder)
    TextView tvFolder;
    @Bind(R.id.last_import_time)
    TextView tvLastImportTime;
    @Bind(R.id.import_progress)
    ProgressBar progressBar;
    @Bind(R.id.import_log)
    TextView tvImportLog;
    @Bind(R.id.import_red_text)
    TextView tvRedText;
    @Bind(R.id.import_button)
    Button button;

    /**
     * What state the button is in currently.
     */
    private ButtonState currBtnState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set theme, create and bind views.
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_full_import);
        ButterKnife.bind(this);

        // Set up toolbar.
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        initUi();
    }

    /**
     * Initialize the UI.
     */
    private void initUi() {
        setHeaderState(HeaderState.INIT);

        // Check if the importer is already running; if it is then all we need to do is register as the listener,
        // because the fact that it's running allows us to make certain assumptions.
        FullImporter importer = FullImporter.get();
        if (!importer.isReady()) return; // Just return, we automatically register during onStart().

        // The importer isn't running, so we need to do a bit of work first, starting with checking if we have a
        // library directory set.
        File libDir = Util.tryResolveDir(DefaultPrefs.get().getLibDir(null));
        if (libDir == null) {
            // We don't have a library directory set, so we'll change the UI to have the user choose one.
            setButtonState(ButtonState.CHOOSE_DIR, true);
            setRedTextState(RedTextState.MUST_CHOOSE_FOLDER);
            return;
        } else {
            // We have a valid library directory.
            tvFolder.setText(libDir.getAbsolutePath());
        }

        // We're good!
        setReady();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.full_import, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FullImporter.get().registerListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FullImporter.get().unregisterListener();
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
        long lastTime = DefaultPrefs.get().getLastFullImportTime(-1);

        if (lastTime == -1) tvLastImportTime.setText(R.string.def_last_full_import_time);
        else tvLastImportTime.setText(DateUtils.getRelativeDateTimeString(this, lastTime, DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME));
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
     * Change the state of the button.
     * @param newState New button state.
     * @param enabled  Whether or not to have the button enabled.
     */
    private void setButtonState(ButtonState newState, boolean enabled) {
        button.setEnabled(enabled);
        switch (newState) {
            case START_IMPORT:
                button.setText(R.string.start_full_import);
                break;
            case CANCEL_IMPORT:
                button.setText(R.string.cancel);
                break;
            case CHOOSE_DIR:
                button.setText(R.string.choose_library_dir);
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
     * What to do when the button on the importer view is clicked.
     */
    @OnClick(R.id.import_button)
    void onButtonClick(View view) {
        switch (currBtnState) {
            case START_IMPORT:
                // TODO check permissions.

                FullImporter.get().doFullImport(this);
                break;
            case CANCEL_IMPORT:
                FullImporter.get().cancelFullImport();
                break;
            case CHOOSE_DIR:
                // TODO check permissions.

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
    public void onFolderSelection(@NonNull File folder) {
        String path = folder.getAbsolutePath();
        DefaultPrefs.get().putLibDir(path);
        tvFolder.setText(path);
        setReady();
    }

    @Override
    public void setMaxProgress(int maxProgress) {
        progressBar.setMax(maxProgress);
    }

    @Override
    public void setCurrImportDir(String importDir) {
        if (importDir == null) return;
        tvFolder.setText(importDir);
    }

    @Override
    public Subscription subscribeToLogStream(Subject<String, String> logSubject) {
        return logSubject.observeOn(AndroidSchedulers.mainThread()).subscribe(s -> {
            if (s == null) tvImportLog.setText("");
            else tvImportLog.append(s);
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
        tvImportLog.append(C.getStr(R.string.fil_done)); // We aren't subscribed anymore, so we append this here.
        setReady();
    }
}
