package com.bkromhout.minerva;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.ui.LibsSupportFragment;

/**
 * Shows information about the Application.
 */
public class AboutActivity extends AppCompatActivity {
    /**
     * Libraries info fragment.
     */
    private LibsSupportFragment libsFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Set up toolbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        initUi();
    }

    /**
     * Initialize UI.
     */
    private void initUi() {
        // Set version text.
        try {
            PackageInfo pkgInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            //noinspection ConstantConditions
            ((TextView) findViewById(R.id.about_app_version))
                    .setText(C.getStr(R.string.version_string, pkgInfo.versionName, pkgInfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // Fill in libraries.
        libsFrag = new LibsBuilder()
                .withActivityStyle(Libs.ActivityStyle.DARK)
                .supportFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.about_libs, libsFrag).commit();
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
}
