package com.bkromhout.minerva;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkromhout.minerva.adapters.BaseBookCardAdapter;
import com.bkromhout.minerva.adapters.BookItemCardAdapter;
import com.bkromhout.minerva.adapters.BookItemCardCompactAdapter;
import com.bkromhout.minerva.adapters.BookItemCardNoCoverAdapter;
import com.bkromhout.minerva.events.ActionEvent;
import com.bkromhout.minerva.events.BookCardClickEvent;
import com.bkromhout.minerva.prefs.AllListsPrefs;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.realm.RBookListItem;
import com.bkromhout.minerva.util.Dialogs;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.realmrecyclerview.RealmRecyclerView;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

/**
 * Activity which displays a list of books based on an {@link RBookList}.
 */
public class BookListActivity extends AppCompatActivity implements ActionMode.Callback {
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
    private BaseBookCardAdapter adapter;
    /**
     * Action mode.
     */
    private ActionMode actionMode;
    /**
     * Whether or not the current action mode is the normal or reorder mode.
     */
    private boolean isReorderMode;

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
        Util.forceMenuIcons(menu, this, getClass().getSimpleName());
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
        // Close adapter.
        if (adapter != null) adapter.close();
        // Close Realm.
        if (realm != null) {
            realm.close();
            realm = null;
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (!isReorderMode) {
            mode.getMenuInflater().inflate(R.menu.book_list_action_mode, menu);
        } else {
            mode.setTitle(R.string.title_reorder_mode);
            adapter.setDragMode(true);
        }
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Util.forceMenuIcons(menu, this, getClass().getSimpleName());
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (!isReorderMode) {
            adapter.clearSelections();
        } else {
            adapter.setDragMode(false);
            isReorderMode = false;
        }
        actionMode = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_reorder:
                isReorderMode = true;
                startSupportActionMode(this);
                return true;
            case R.id.action_card_type:
                showCardStyleDialog();
                return true;
            case R.id.action_clear:
                new MaterialDialog.Builder(this)
                        .title(R.string.title_clear_list)
                        .content(R.string.clear_list_prompt)
                        .positiveText(R.string.yes)
                        .negativeText(R.string.no)
                        .onPositive((dialog, which) ->
                                EventBus.getDefault().post(new ActionEvent(R.id.action_clear, null)))
                        .show();
                return true;
            case R.id.action_delete_list:
                // TODO
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_tag:
                //noinspection unchecked
                TaggingActivity.start(this, RBookListItem.booksFromBookListItems(adapter.getSelectedRealmObjects()));
                actionMode.finish();
                return true;
            case R.id.action_rate:
                int initialRating = adapter.getItemCount() == 1
                        ? ((RBook) adapter.getSelectedRealmObjects().get(0)).getRating() : 0;
                Dialogs.showRatingDialog(this, initialRating);
                return true;
            case R.id.action_select_all:
                adapter.selectAll();
                return true;
            case R.id.action_select_none:
                adapter.clearSelections();
                return true;
            case R.id.action_move_to_top:
                //noinspection unchecked
                RBookList.moveItemsToStart(adapter.getSelectedRealmObjects());
                return true;
            case R.id.action_move_to_bottom:
                //noinspection unchecked
                RBookList.moveItemsToEnd(adapter.getSelectedRealmObjects());
                return true;
            case R.id.action_re_import:
                // TODO Re-import the selected items.
                return true;
            case R.id.action_remove:
                // Show dialog to confirm removal from list. TODO move to Dialogs.
                new MaterialDialog.Builder(this)
                        .title(R.string.title_remove_from_list)
                        .content(R.string.remove_from_list_prompt)
                        .positiveText(R.string.yes)
                        .negativeText(R.string.no)
                        .onPositive((dialog, which) ->
                                EventBus.getDefault().post(new ActionEvent(R.id.action_remove, null)))
                        .show();
                return true;
            default:
                return false;
        }
    }

    /**
     * Called when we wish to take some action.
     * @param event {@link ActionEvent}.
     */
    @Subscribe
    public void onActionEvent(ActionEvent event) {
        //noinspection unchecked
        List<RBook> selectedItems = adapter.getSelectedRealmObjects();

        switch (event.getActionId()) {
            case R.id.action_add_to_list: {
                // TODO actually implement a move/copy to other lists feature???
                //RBookList list = realm.where(RBookList.class).equalTo("name", (String) event.getData()).findFirst();
                //RBookList.addBooks(list, selectedItems);
                break;
            }
            case R.id.action_rate: {
                realm.executeTransaction(tRealm -> {
                    for (RBook item : selectedItems) item.setRating((Integer) event.getData());
                });
                if (actionMode != null) actionMode.finish();
                break;
            }
            case R.id.action_clear: {
                realm.executeTransaction(tRealm -> srcList.getListItems().clear());
                break;
            }
            case R.id.action_remove: {
                RBookList.removeBooks(srcList, selectedItems);
                break;
            }
        }
        if (actionMode != null) actionMode.finish();
    }

    /**
     * Create a {@link RealmBasedRecyclerViewAdapter} based on the current view options and return it.
     * @return New {@link RealmBasedRecyclerViewAdapter}. Will return null if we cannot get the activity context, if
     * {@link #items} is null or invalid, or if the current value of {@link #cardType} is not valid.
     */
    private BaseBookCardAdapter makeAdapter() {
        if (items == null || !items.isValid()) return null;

        // Create a new adapter based on the card type.
        switch (cardType) {
            case C.BOOK_CARD_NORMAL:
                return new BookItemCardAdapter(this, items);
            case C.BOOK_CARD_NO_COVER:
                return new BookItemCardNoCoverAdapter(this, items);
            case C.BOOK_CARD_COMPACT:
                return new BookItemCardCompactAdapter(this, items);
            default:
                return null;
        }
    }

    /**
     * Uses the current view options to change the card layout currently in use. Preserves the position currently
     * scrolled to in the list before switching adapters.
     */
    private void changeCardType() {
        // Store the current last visible item position so that we can scroll back to it after switching adapters.
        int currLastVisPos = recyclerView.getLayoutManger().findLastCompletelyVisibleItemPosition();

        // Swap the adapter
        if (adapter != null) adapter.close();
        adapter = makeAdapter();
        recyclerView.setAdapter(adapter);

        // Scroll back to the same position.
        if (currLastVisPos != RecyclerView.NO_POSITION) recyclerView.scrollToPosition(currLastVisPos);
    }

    /**
     * Called when one of the cards is clicked.
     * @param event {@link BookCardClickEvent}.
     */
    @Subscribe
    public void onCardClicked(BookCardClickEvent event) {
        // Get the associated RBook.
        RBook book = items.where().equalTo("book.relPath", event.getRelPath()).findFirst().getBook();

        if (actionMode != null) {
            if (event.getType() == BookCardClickEvent.Type.LONG) adapter.extendSelectionTo(event.getPosition());
            else adapter.toggleSelected(event.getPosition());
            return;
        }
        // Do something based on the click type.
        switch (event.getType()) {
            case NORMAL:
                // Open the book file.
                Util.openFileUsingIntent(this, Util.getFileFromRelPath(book.getRelPath()));
                break;
            case LONG:
                // Start multi-select.
                adapter.toggleSelected(event.getPosition());
                if (actionMode == null) actionMode = startSupportActionMode(this);
                break;
            case INFO:
                // Open BookInfoActivity.
                Bundle b = new Bundle();
                b.putString(BookInfoActivity.BOOK_SEL_STR, event.getRelPath());
                Util.startAct(this, BookInfoActivity.class, b);
                break;
            case QUICK_TAG:
                //noinspection unchecked
                TaggingActivity.start(this, book);
                break;
        }
    }

    /**
     * TODO move to Dialogs.
     * <p>
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
