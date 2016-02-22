package com.bkp.minerva;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import butterknife.Bind;
import butterknife.ButterKnife;
import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import com.bkp.minerva.events.BookCardClickEvent;
import com.bkp.minerva.realm.RBook;
import com.bkp.minerva.realm.RBookList;
import com.bkp.minerva.realm.RBookListItem;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

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
    RealmRecyclerView recycler;

    /**
     * Unique string to help find the correct list to display from the DB.
     */
    private String selStr;
    /**
     * Instance of Realm.
     */
    private Realm realm;
    /**
     * The {@link RBookList} whose items are being shown.
     */
    private RBookList srcList;
    /**
     * The list of {@link RBookListItem}s being shown.
     */
    private RealmResults<RBookListItem> items;
    /**
     * Recycler view adapter.
     */
    private RealmBasedRecyclerViewAdapter adapter;

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
        if (selStr == null) throw new IllegalArgumentException("Cannot start this activity without a selector string.");

        // Get Realm, then get the RBookList which we will get items from.
        realm = Realm.getDefaultInstance();
        srcList = realm.where(RBookList.class).equalTo("name", selStr).findFirst();

        // Set title, then set up the rest of UI.
        setTitle(srcList.getName());
        initUi();
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
        items = srcList.getListItems().where().findAllSorted("pos");

        // TODO Init RV.
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
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
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
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when one of the cards is clicked.
     * @param event {@link BookCardClickEvent}.
     */
    @Subscribe
    public void onCardClicked(BookCardClickEvent event) {
        // Get the associated RBook.
        RBook book = items.where().equalTo("book.relPath", event.getRelPath()).findFirst().getBook();

        // Do something based on the click type.
        switch (event.getType()) {
            case NORMAL:
                // TODO Open the book file.
                break;
            case LONG:
                // TODO Start multi-select.
                break;
            case INFO:
                // TODO Open BookInfoActivity.
                break;
            case QUICK_TAG:
                // TODO Open quick-tag dialog??
                break;
        }
    }
}
