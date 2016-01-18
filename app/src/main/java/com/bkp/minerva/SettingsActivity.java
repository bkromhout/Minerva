package com.bkp.minerva;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.CheckedTextView;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.bkp.minerva.prefs.MainPrefs;

import java.io.File;

public class SettingsActivity extends AppCompatActivity implements FolderChooserDialog.FolderCallback {

    // Views.
    @Bind(R.id.lib_dir)
    TextView tvLibDir;
    @Bind(R.id.lib_dir_auto_import)
    CheckedTextView ctvLibDirAutoImport;

    /**
     * Preferences wrapper.
     */
    private MainPrefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get prefs.
        prefs = MainPrefs.get();

        // Create and bind views.
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        // Set up toolbar.
        setSupportActionBar(ButterKnife.findById(this, R.id.toolbar));
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        initUi();
    }

    /**
     * Initialize the UI.
     */
    private void initUi() {
        // Set up library directory textview.
        tvLibDir.setText(prefs.getLibDir(getString(R.string.lib_dir_def)));
        // Need to programmatically set the compound drawable tint.
        DrawableCompat.setTint(tvLibDir.getCompoundDrawables()[2], ContextCompat.getColor(this, R.color.grey200));

        // Set up the auto-import checked textview. TODO make this affect something!
        ctvLibDirAutoImport.setChecked(prefs.getLibAutoImport(false));
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
     * What to do when the library directory textview is clicked.
     */
    @OnClick(R.id.lib_dir)
    void onLibDirClick() {
        // Check to see if the current value is a valid folder.
        FolderChooserDialog.Builder builder = new FolderChooserDialog.Builder(this)
                .chooseButton(R.string.ok)
                .cancelButton(R.string.cancel);
        if (new File(tvLibDir.getText().toString()).exists()) builder.initialPath(tvLibDir.getText().toString());
        builder.show();
    }

    /**
     * Callback from folder chooser dialog shown when the library directory textview is clicked.
     * @param folder Chosen folder.
     */
    @Override
    public void onFolderSelection(@NonNull File folder) {
        String folderPath = folder.getAbsolutePath();
        tvLibDir.setText(folderPath);
        prefs.putLibDir(folderPath);
    }

    /**
     * What to do when the library auto import setting is toggled.
     * @param checked True if view is now checked, otherwise false.
     */
    @OnCheckedChanged(R.id.lib_dir_auto_import)
    void onLibAutoImportToggle(boolean checked) {
        prefs.putLibAutoImport(checked);
    }
}