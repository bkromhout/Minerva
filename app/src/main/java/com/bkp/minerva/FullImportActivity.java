package com.bkp.minerva;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;

public class FullImportActivity extends AppCompatActivity {
    // Views
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.import_folder)
    TextView tvFolder;
    @Bind(R.id.last_import_time)
    TextView tvLastImportTime;
    @Bind(R.id.import_progress)
    ProgressBar progressBar;
    @Bind(R.id.import_status)
    TextView tvImportStatus;
    @Bind(R.id.import_no_cancel)
    TextView tvNoCancel;
    @Bind(R.id.import_button)
    Button button;

    /**
     * TODO!!!! Maybe just need a pref that tells us when the last full import was done instead??? Yes.
     */
    private int currState;

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
        initAndGo();
    }

    @Override
    protected void onStop() {
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
     * Get everything ready to go and start the import.
     */
    private void initAndGo() {

    }
}
