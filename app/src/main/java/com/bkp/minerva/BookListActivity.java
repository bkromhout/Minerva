package com.bkp.minerva;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import butterknife.Bind;
import butterknife.ButterKnife;

import java.lang.reflect.Method;

/**
 * TODO
 */
public class BookListActivity extends AppCompatActivity {
    // Key strings for the bundle passed when this activity is started.
    public static final String LIST_SEL_STR = "LIST_SEL_STR";

    // Views.
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.recycler)
    RecyclerView recycler;

    /**
     * Unique string to help find the correct list to display from the DB.
     */
    private String selStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create and bind views.
        setContentView(R.layout.activity_book_list);
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
        selStr = b.getString(LIST_SEL_STR, null);
    }

    /**
     * Init the UI.
     */
    private void initUi() {
        if (selStr == null) {
            // TODO something here.
            return;
        }

        // TODO use sel string to somehow get the list from the database.
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu != null) {
            // Make sure all icons are tinted the correct color, including those in the overflow menu.
            for (int i = 0; i < menu.size(); i++)
                menu.getItem(i).getIcon()
                    .setColorFilter(ContextCompat.getColor(this, R.color.textColorPrimary), PorterDuff.Mode.SRC_IN);
            // And use a bit of reflection to ensure we show icons even in the overflow menu.
            if (menu.getClass().equals(MenuBuilder.class)) {
                try {
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "onMenuOpened...unable to set icons for overflow menu", e);
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.book_list, menu);
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
