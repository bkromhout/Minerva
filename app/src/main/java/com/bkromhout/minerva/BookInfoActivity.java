package com.bkromhout.minerva;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkromhout.minerva.util.Util;

/**
 * TODO
 */
public class BookInfoActivity extends AppCompatActivity {
    // Key strings for the bundle passed when this activity is started.
    public static final String BOOK_SEL_STR = "BOOK_SEL_STR";

    // Views.
    @Bind(R.id.toolbar)
    Toolbar toolbar;

    /**
     * Unique string to help find the correct book to display from the DB.
     */
    private String selStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create and bind views.
        setContentView(R.layout.activity_book_info);
        ButterKnife.bind(this);

        // Set up toolbar.
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Read extras bundle.
        readExtras(getIntent().getExtras());

        // Set up the rest of the UI.
        initUi();

        // TODO set the title in here somewhere.
    }

    /**
     * Fill in variables using the extras bundle.
     * @param b Extras bundle from intent used to start this activity.
     */
    private void readExtras(Bundle b) {
        if (b == null) return;
        selStr = b.getString(BOOK_SEL_STR, null);
    }

    /**
     * Init the UI.
     */
    private void initUi() {
        if (selStr == null) {
            // TODO something here.
            return;
        }

        // TODO use sel string to somehow get the book from the database.
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Util.forceMenuIcons(menu, this, getClass().getSimpleName());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.book_info, menu);
        return super.onCreateOptionsMenu(menu);
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

}
