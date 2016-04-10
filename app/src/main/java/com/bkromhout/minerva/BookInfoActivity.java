package com.bkromhout.minerva;

import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.util.Util;
import io.realm.Realm;

/**
 * Displays information for some {@link com.bkromhout.minerva.realm.RBook}.
 */
public class BookInfoActivity extends AppCompatActivity {
    // Key strings for the bundle passed when this activity is started.
    public static final String BOOK_SEL_STR = "BOOK_SEL_STR";

    // Views.
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.collapsing_toolbar)
    CollapsingToolbarLayout collapsibleToolbar;
    @Bind(R.id.cover_image)
    ImageView coverImage;
    @Bind(R.id.fab)
    FloatingActionButton fab;


    /**
     * Relative path to use to find an {@link com.bkromhout.minerva.realm.RBook}.
     */
    private String relPath;
    /**
     * Realm instance.
     */
    private Realm realm;
    /**
     * {@link RBook} whose information is being displayed.
     */
    private RBook book;

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

        // Get Realm and read extras bundle.
        realm = Realm.getDefaultInstance();
        readExtras(getIntent().getExtras());

        // Get RBook.
        book = realm.where(RBook.class).equalTo("relPath", relPath).findFirst();
        if (book == null) throw new IllegalArgumentException("Invalid relative path, no matching RBook found.");

        // Set up the rest of the UI.
        initUi();
    }

    /**
     * Fill in variables using the extras bundle.
     * @param b Extras bundle from intent used to start this activity.
     */
    private void readExtras(Bundle b) {
        if (b == null) return;
        relPath = b.getString(BOOK_SEL_STR, null);
    }

    /**
     * Init the UI.
     */
    private void initUi() {
        // Set title.
        collapsibleToolbar.setTitle(book.getTitle());

        // TODO Set cover image.

        // TODO
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
    public void onDestroy() {
        super.onDestroy();
        // Close Realm.
        if (realm != null) {
            realm.close();
            realm = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_read:
                // Open the book file.
                book.openFileUsingIntent(this);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Open the book when the FAB is clicked.
     */
    @OnClick(R.id.fab)
    void onReadFabClicked() {
        // Open the book file.
        book.openFileUsingIntent(this);
    }
}
