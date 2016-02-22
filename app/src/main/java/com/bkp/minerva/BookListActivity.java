package com.bkp.minerva;

import android.annotation.SuppressLint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import butterknife.Bind;
import butterknife.ButterKnife;
import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkp.minerva.adapters.BookItemCardAdapter;
import com.bkp.minerva.adapters.BookItemCardCompactAdapter;
import com.bkp.minerva.adapters.BookItemCardNoCoverAdapter;
import com.bkp.minerva.events.BookCardClickEvent;
import com.bkp.minerva.prefs.AllListsPrefs;
import com.bkp.minerva.realm.RBook;
import com.bkp.minerva.realm.RBookList;
import com.bkp.minerva.realm.RBookListItem;
import com.bkp.minerva.util.DraggableItemTouchHelperCallback;
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
    RealmRecyclerView recyclerView;

    /**
     * Preferences.
     */
    private AllListsPrefs listsPrefs;
    /**
     * Unique string to help find the correct list to display from the DB.
     */
    private String selStr;
    /**
     * Which type of card to use.
     */
    private String cardType;
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

        // Get and read preferences.
        listsPrefs = AllListsPrefs.get();
        readPrefs();

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
     * Read preferences into variables.
     */
    private void readPrefs() {
        cardType = listsPrefs.getCardType(C.BOOK_CARD_NORMAL);
    }

    /**
     * Init the UI.
     */
    private void initUi() {
        items = srcList.getListItems().where().findAllSorted("pos");
        adapter = makeAdapter();
        recyclerView.setAdapter(adapter);

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
            case R.id.action_card_type:
                showCardStyleDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Create a {@link RealmBasedRecyclerViewAdapter} based on the current view options and return it.
     * @return New {@link RealmBasedRecyclerViewAdapter}. Will return null if we cannot get the activity context, if
     * {@link #items} is null or invalid, or if the current value of {@link #cardType} is not valid.
     */
    private RealmBasedRecyclerViewAdapter makeAdapter() {
        if (items == null || !items.isValid()) return null;

        // Create a new adapter based on the card type.
        switch (cardType) {
            case C.BOOK_CARD_NORMAL:
                return new BookItemCardAdapter(this, items, true, true);
            case C.BOOK_CARD_NO_COVER:
                return new BookItemCardNoCoverAdapter(this, items, true, true);
            case C.BOOK_CARD_COMPACT:
                return new BookItemCardCompactAdapter(this, items, true, true);
            default:
                return null;
        }
    }

    /**
     * Adds drag and drop functionality to the recyclerview.
     */
    private void addDragDrop() {
        ItemTouchHelper touchHelper = new ItemTouchHelper(new DraggableItemTouchHelperCallback(
                (DraggableItemTouchHelperCallback.Adapter) adapter));
        touchHelper.attachToRecyclerView(recyclerView);
        // TODO sadly this will be impossible if we don't create our own version of the library :(
    }

    /**
     * Uses the current view options to change the card layout currently in use. Preserves the position currently
     * scrolled to in the list before switching adapters.
     */
    private void changeCardType() {
        // Store the current first visible item position so that we can scroll back to it after switching adapters.
        int currFirstVisPos = recyclerView.findFirstVisibleItemPosition();

        // Swap the adapter
        if (adapter != null) adapter.close();
        adapter = makeAdapter();
        recyclerView.setAdapter(adapter);

        // Scroll back to the same position.
        // TODO the smooth scroll can take a while... I'd much rather it was instant.
        if (currFirstVisPos != RecyclerView.NO_POSITION) recyclerView.smoothScrollToPosition(currFirstVisPos);
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

    /**
     * Shows a dialog which allows the user to pick the card style.
     */
    private void showCardStyleDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.action_card_type)
                .items(C.getStr(R.string.card_normal),
                        C.getStr(R.string.card_no_cover),
                        C.getStr(R.string.card_compact))
                .itemsCallbackSingleChoice(idxFromStrConst(cardType), (dialog, itemView, which, text) -> {
                    // Do nothing if it's the same.
                    if (idxFromStrConst(cardType) == which) return true;

                    // Persist the new card style.
                    cardType = strConstFromIdx(which);
                    listsPrefs.putCardType(cardType);

                    // Change the adapter.
                    changeCardType();
                    return true;
                })
                .show();
    }

    /**
     * Convert an index to the string constant that that index represents.
     * @param idx An index.
     * @return A string constant.
     */
    @SuppressLint("DefaultLocale")
    private static String strConstFromIdx(int idx) {
        switch (idx) {
            // Card types.
            case 0:
                return C.BOOK_CARD_NORMAL;
            case 1:
                return C.BOOK_CARD_NO_COVER;
            case 2:
                return C.BOOK_CARD_COMPACT;
            default:
                throw new IllegalArgumentException(String.format("Invalid resource ID: %d", idx));
        }
    }

    /**
     * Convert a string constant to the index that represents it.
     * @param str String constant.
     * @return An index.
     */
    private static Integer idxFromStrConst(String str) {
        switch (str) {
            // Card types.
            case C.BOOK_CARD_NORMAL:
                return 0;
            case C.BOOK_CARD_NO_COVER:
                return 1;
            case C.BOOK_CARD_COMPACT:
                return 2;
            default:
                throw new IllegalArgumentException(String.format("Invalid string constant: %s", str));
        }
    }
}
