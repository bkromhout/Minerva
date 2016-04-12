package com.bkromhout.minerva;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkromhout.minerva.adapters.BaseBookCardAdapter;
import com.bkromhout.minerva.adapters.BookItemCardAdapter;
import com.bkromhout.minerva.adapters.BookItemCardCompactAdapter;
import com.bkromhout.minerva.adapters.BookItemCardNoCoverAdapter;
import com.bkromhout.minerva.data.ReImporter;
import com.bkromhout.minerva.enums.BookCardType;
import com.bkromhout.minerva.events.ActionEvent;
import com.bkromhout.minerva.events.BookCardClickEvent;
import com.bkromhout.minerva.events.PrefChangeEvent;
import com.bkromhout.minerva.events.UpdatePosEvent;
import com.bkromhout.minerva.prefs.ListsPrefs;
import com.bkromhout.minerva.prefs.interfaces.BCTPref;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.realm.RBookListItem;
import com.bkromhout.minerva.util.Dialogs;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.rrvl.RealmRecyclerView;
import com.bkromhout.ruqus.RealmUserQuery;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.List;

/**
 * Activity which displays a list of books based on an {@link RBookList}.
 */
public class BookListActivity extends AppCompatActivity implements ActionMode.Callback, ReImporter.IReImportListener {
    // Key strings for the bundle passed when this activity is started.
    public static final String LIST_SEL_STR = "LIST_SEL_STR";
    private static final String KEY_IS_REORDER_MODE = "IS_REORDER_MODE";

    // Views.
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.recycler)
    RealmRecyclerView recyclerView;

    /**
     * Preferences.
     */
    private ListsPrefs listsPrefs;
    /**
     * Unique string to help find the correct list to display from the DB.
     */
    private String selStr;
    /**
     * Which type of card to use.
     */
    private BookCardType cardType;
    /**
     * Instance of Realm.
     */
    private Realm realm;
    /**
     * The {@link RBookList} whose items are being shown.
     */
    private RBookList srcList;
    /**
     * If {@link #srcList} is a smart list, this will be the {@link RealmUserQuery} we use to show it. Otherwise, this
     * will be null.
     */
    private RealmUserQuery smartListRuq = null;
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
    private static ActionMode actionMode;
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
        listsPrefs = ListsPrefs.get();
        readPrefs();

        // Get Realm, then get the RBookList which we will get items from.
        realm = Realm.getDefaultInstance();
        srcList = realm.where(RBookList.class).equalTo("name", selStr).findFirst();

        // Set title, then set up the rest of UI.
        setTitle(srcList.getName());
        // TODO Check savedInstanceState for a RUQ.
        initUi();

        // If we have a saved instance state, check to see if we were in action mode.
        if (savedInstanceState != null && savedInstanceState.getBoolean(C.IS_IN_ACTION_MODE)) {
            // If we were in action mode, restore the adapter's state and start action mode.
            isReorderMode = savedInstanceState.getBoolean(KEY_IS_REORDER_MODE);
            adapter.restoreInstanceState(savedInstanceState);
            startActionMode();
        }
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
        cardType = listsPrefs.getCardType(BookCardType.NORMAL);
    }

    /**
     * Init the UI.
     */
    private void initUi() {
        // If we already have a RealmUserQuery, just use that right away.
        if (smartListRuq != null) {
            // TODO Use RUQ to set up UI.

            return;
        }
        // Check first to see if list is a smart list.
        if (srcList.isSmartList()) {
            String ruqString = srcList.getSmartListRuqString();
            // Smart list; check to see if we need to set it up.
            if (ruqString != null && !ruqString.isEmpty()) {
                // TODO Smart list already has a non-empty RUQ string, create a RUQ and use it.

            } else if (ruqString == null) {
                // TODO We need to set up the smart list first. Open the query builder.

            } else { // ruqString.isEmpty() = true;
                //TODO User has already been to the query builder once, it's up to them now, show a message saying that.

            }
        } else {
            // Normal list.
            items = srcList.getListItems().where().findAllSorted("pos");
            adapter = makeAdapter();
            recyclerView.setAdapter(adapter);
        }
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
        // Reattach to the ReImporter if it's currently running so that it can draw its dialog.
        ReImporter.reAttachIfExists(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save adapter state if we're in action mode.
        if (actionMode != null) {
            // TODO Save RUQ if this is a smart list.
            adapter.saveInstanceState(outState);
            outState.putBoolean(C.IS_IN_ACTION_MODE, true);
            outState.putBoolean(KEY_IS_REORDER_MODE, isReorderMode);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        // Detach from the ReImporter if it's currently running.
        ReImporter.detachListener();
        // Finish action mode so that it doesn't leak.
        if (actionMode != null) actionMode.finish();
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
        if (actionMode != null) actionMode.finish();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (smartListRuq != null) {
            // Smart list; can't reorder list items so it has less action items.
            mode.getMenuInflater().inflate(R.menu.book_list_smart_action_mode, menu);
        } else if (!isReorderMode) {
            // Normal list; can reorder list items (though this isn't the re-order mode).
            mode.getMenuInflater().inflate(R.menu.book_list_action_mode, menu);
        } else {
            // Normal list; reorder mode.
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
                startActionMode();
                return true;
            case R.id.action_show_query:
                // TODO
                return true;
            case R.id.action_card_type:
                Dialogs.cardStyleDialog(this, listsPrefs);
                return true;
            case R.id.action_rename_list:
                // TODO
                return true;
            case R.id.action_rename_smart_list:
                // TODO
                return true;
            case R.id.action_edit_smart_list:
                // TODO
                return true;
            case R.id.action_clear:
                Dialogs.simpleYesNoDialog(this, R.string.title_clear_list, R.string.clear_list_prompt,
                        R.id.action_clear);
                return true;
            case R.id.action_convert_to_normal_list:
                // TODO
                return true;
            case R.id.action_delete_list:
                Dialogs.simpleYesNoDialog(this, R.string.title_delete_list, R.string.delete_list_prompt,
                        R.id.action_delete_list);
                return true;
            case R.id.action_delete_smart_list:
                // TODO
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        // Handle select all/none first, and if it isn't those then don't do anything if we haven't selected any items.
        if (item.getItemId() == R.id.action_select_all) {
            adapter.selectAll();
            return true;
        } else if (item.getItemId() == R.id.action_select_none) {
            adapter.clearSelections();
            return true;
        } else if (adapter.getSelectedItemCount() == 0) return true;

        // Handle actions.
        switch (item.getItemId()) {
            case R.id.action_tag:
                //noinspection unchecked
                TaggingActivity.start(this, RBookListItem.booksFromBookListItems(adapter.getSelectedRealmObjects()));
                return true;
            case R.id.action_rate:
                int initialRating = adapter.getSelectedItemCount() == 1
                        ? ((RBook) adapter.getSelectedRealmObjects().get(0)).getRating() : 0;
                Dialogs.ratingDialog(this, initialRating);
                return true;
            case R.id.action_move_to_top:
                //noinspection unchecked
                srcList.moveItemsToStart(adapter.getSelectedRealmObjects());
                actionMode.finish();
                return true;
            case R.id.action_move_to_bottom:
                //noinspection unchecked
                srcList.moveItemsToEnd(adapter.getSelectedRealmObjects());
                actionMode.finish();
                return true;
            case R.id.action_re_import:
                Dialogs.simpleYesNoDialog(this, R.string.title_dialog_re_import, R.string.re_import_prompt,
                        R.id.action_re_import);
                return true;
            case R.id.action_remove:
                Dialogs.simpleYesNoDialog(this, R.string.title_remove_books, R.string.remove_from_list_prompt,
                        R.id.action_remove);
                return true;
            case R.id.action_delete:
                Dialogs.yesNoCheckBoxDialog(this, R.string.title_delete_books, R.string.delete_books_prompt,
                        R.string.delete_from_device_too, R.id.action_delete);
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
            case R.id.action_clear: {
                realm.executeTransaction(tRealm -> srcList.getListItems().clear());
                break;
            }
            case R.id.action_rename_list:
            case R.id.action_rename_smart_list: {
                // TODO
                break;
            }
            case R.id.action_convert_to_normal_list: {
                // TODO

                // TODO Update UI, Refresh menu options, etc
                break;
            }
            case R.id.action_delete_list:
            case R.id.action_delete_smart_list: {
                // Delete the list currently being shown, then finish the activity.
                srcList.deleteList();
                finish();
                break;
            }
            case R.id.action_rate: {
                realm.executeTransaction(tRealm -> {
                    for (RBook item : selectedItems) item.setRating((Integer) event.getData());
                });
                break;
            }
            case R.id.action_add_to_list: {
                // TODO actually implement a move/copy to other lists feature???
                //RBookList list = realm.where(RBookList.class).equalTo("name", (String) event.getData()).findFirst();
                //RBookList.addBooks(list, selectedItems);
                break;
            }
            case R.id.action_re_import: {
                ReImporter.reImportBooks(selectedItems, this);
                // Don't dismiss action mode yet.
                return;
            }
            case R.id.action_remove: {
                srcList.removeBooks(selectedItems);
                break;
            }
            case R.id.action_delete: {
                // Delete the RBooks from Realm.
                List<String> relPaths = RBook.deleteBooks(selectedItems);
                // If the user wants us to, also try to delete the corresponding files from the device.
                if ((boolean) event.getData()) {
                    for (String relPath : relPaths) {
                        File file = Util.getFileFromRelPath(relPath);
                        if (file != null) file.delete();
                    }
                }
                break;
            }
        }
        if (actionMode != null) actionMode.finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case C.RC_TAG_ACTIVITY: {
                // Came back from TaggingActivity.
                if (resultCode == Activity.RESULT_OK) {
                    // We've changed the tags on some books.
                    if (actionMode != null) actionMode.finish();
                }
                break;
            }
            case C.RC_QUERY_BUILDER_ACTIVITY: {
                // Came back from QueryBuilderActivity.
                if (resultCode == RESULT_OK) {
                    // TODO There's a valid RUQ in the extras.
                } else if (smartListRuq == null) {
                    // No valid RUQ returned, and we don't have one already, meaning that srcList's RUQ string is null.
                    // Make it empty string instead so we don't keep forcing the user back into the query builder.
                    srcList.setSmartListRuqString("");
                }
                // Update the UI.
                initUi();
                break;
            }
        }
    }

    @Override
    public void onReImportFinished(boolean wasSuccess) {
        // Notify the adapter that it should refresh the layouts of the selected items.
        adapter.notifySelectedItemsChanged();
        // If we finished successfully, finish the action mode.
        if (wasSuccess) actionMode.finish();
    }

    /**
     * Starts action mode (if it hasn't been already).
     */
    private void startActionMode() {
        if (actionMode == null) actionMode = startSupportActionMode(this);
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
            case NORMAL:
                return new BookItemCardAdapter(this, items);
            case NO_COVER:
                return new BookItemCardNoCoverAdapter(this, items);
            case COMPACT:
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
                book.openFileUsingIntent(this);
                break;
            case LONG:
                // Start multi-select.
                adapter.toggleSelected(event.getPosition());
                startActionMode();
                break;
            case INFO:
                // Open BookInfoActivity.
                BookInfoActivity.start(this, event.getRelPath(), event.getPosition());
                break;
            case QUICK_TAG:
                TaggingActivity.start(this, book);
                break;
        }
    }

    /**
     * React to a changed preference.
     * @param event {@link PrefChangeEvent}.
     */
    @Subscribe
    public void onPrefChangeEvent(PrefChangeEvent event) {
        // Do something different based on name of changed preference.
        switch (event.getPrefName()) {
            case BCTPref.CARD_TYPE:
                cardType = listsPrefs.getCardType(cardType);
                changeCardType();
                break;
        }
    }

    /**
     * When called, update the item at the position carried in the event.
     * @param event {@link UpdatePosEvent}.
     */
    @Subscribe(sticky = true)
    public void onUpdatePosEvent(UpdatePosEvent event) {
        // Remove the sticky event.
        EventBus.getDefault().removeStickyEvent(event);
        // Update the item at the position in the event.
        adapter.notifyItemChanged(event.getPosition());
    }

    @Override
    public Activity getCtx() {
        // Provide our activity context to the ReImporter so that it can draw its progress dialog.
        return this;
    }
}
