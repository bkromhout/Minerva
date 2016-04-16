package com.bkromhout.minerva;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.bkromhout.minerva.adapters.*;
import com.bkromhout.minerva.data.ActionHelper;
import com.bkromhout.minerva.data.ReImporter;
import com.bkromhout.minerva.enums.AdapterType;
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
import io.realm.RealmObject;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

/**
 * Activity which displays a list of books based on an {@link RBookList}.
 */
public class BookListActivity extends AppCompatActivity implements ActionMode.Callback, ReImporter.IReImportListener {
    // Key strings for the bundle passed when this activity is started.
    public static final String LIST_NAME = "LIST_NAME";
    private static final String KEY_IS_REORDER_MODE = "IS_REORDER_MODE";

    // Views.
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.recycler)
    RealmRecyclerView recyclerView;
    @Bind(R.id.smart_list_empty)
    LinearLayout emptySmartList;

    /**
     * Preferences.
     */
    private ListsPrefs listsPrefs;
    /**
     * Unique string to help find the correct list to display from the DB.
     */
    private String selStr;
    /**
     * Position to use in any {@link UpdatePosEvent}s which might be sent.
     */
    private int posToUpdate;
    /**
     * If true, send a {@link UpdatePosEvent} to the {@link com.bkromhout.minerva.fragments.AllListsFragment} when we
     * exit this activity.
     */
    private boolean needsPosUpdate = false;
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
     * Type of objects in the adapter.
     */
    private AdapterType adapterType = AdapterType.BOOK;
    /**
     * The list of {@link RBookListItem}s being shown.
     */
    private RealmResults<? extends RealmObject> items;
    /**
     * Recycler view adapter.
     */
    private RealmBasedRecyclerViewAdapter adapter;
    /**
     * Action mode.
     */
    private static ActionMode actionMode;
    /**
     * Whether or not the current action mode is the normal or reorder mode.
     */
    private boolean isReorderMode;

    public static void start(Context context, String listName, int posToUpdate) {
        if (listName == null) throw new IllegalArgumentException("Cannot start this activity without a list name.");
        context.startActivity(new Intent(context, BookListActivity.class)
                .putExtra(LIST_NAME, listName)
                .putExtra(C.POS_TO_UPDATE, posToUpdate));
    }

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

        // Get and read preferences.
        listsPrefs = ListsPrefs.get();
        readPrefs();

        // Get Realm, then get the RBookList which we will get items from.
        realm = Realm.getDefaultInstance();
        srcList = realm.where(RBookList.class).equalTo("name", selStr).findFirst();

        // Set title, then check to see if we have a RUQ from the savedInstanceState.
        setTitle(srcList.getName());
        if (savedInstanceState != null && savedInstanceState.containsKey(C.RUQ))
            smartListRuq = savedInstanceState.getParcelable(C.RUQ);

        // Set up the UI.
        updateUi();

        // If we have a saved instance state...
        if (savedInstanceState != null) {
            // ...check to see if we were in action mode.
            if (savedInstanceState.getBoolean(C.IS_IN_ACTION_MODE)) {
                // If we were in action mode, restore the adapter's state and start action mode.
                isReorderMode = savedInstanceState.getBoolean(KEY_IS_REORDER_MODE);
                adapter.restoreInstanceState(savedInstanceState);
                startActionMode();
            }
            // ...And whether we will still need to send a position update upon finishing.
            if (savedInstanceState.getBoolean(C.NEEDS_POS_UPDATE)) needsPosUpdate = true;
        }
    }

    /**
     * Fill in variables using the extras bundle.
     * @param b Extras bundle from intent used to start this activity.
     */
    private void readExtras(Bundle b) {
        if (b == null) return;
        selStr = b.getString(LIST_NAME, null);
        posToUpdate = b.getInt(C.POS_TO_UPDATE, -1);
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
    private void updateUi() {
        emptySmartList.setVisibility(View.GONE);
        // If we already have a RealmUserQuery, just use that right away.
        if (smartListRuq != null) {
            // Use RUQ to set up UI.
            adapterType = AdapterType.fromRealmClass(smartListRuq.getQueryClass());
            items = smartListRuq.execute(realm);
            adapter = makeAdapter();
            recyclerView.setAdapter(adapter);
            return;
        }
        // Check first to see if list is a smart list.
        if (srcList.isSmartList()) {
            String ruqString = srcList.getSmartListRuqString();
            // Smart list; check to see if we need to set it up.
            if (ruqString != null && !ruqString.isEmpty()) {
                // Smart list already has a non-empty RUQ string, create a RUQ and then set up the UI using it.
                smartListRuq = new RealmUserQuery(srcList.getSmartListRuqString());
                updateUi();
            } else if (ruqString == null) {
                // We need to set up the smart list first. Open the query builder.
                QueryBuilderActivity.start(this, null);
            } else { // ruqString.isEmpty() == true here.
                //User has already been to the query builder once, it's up to them now, show a message saying that.
                emptySmartList.setVisibility(View.VISIBLE);
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
        getMenuInflater().inflate(smartListRuq == null ? R.menu.book_list : R.menu.book_list_smart, menu);
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
        if (smartListRuq != null) outState.putParcelable(C.RUQ, smartListRuq);
        outState.putBoolean(C.NEEDS_POS_UPDATE, needsPosUpdate);
        // Save adapter state if we're in action mode.
        if (actionMode != null) {
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
        // If we need to update the list's card in AllListsFragment, send the sticky event now.
        if (needsPosUpdate) EventBus.getDefault().post(new UpdatePosEvent(posToUpdate));
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
            ((BaseBookCardAdapter) adapter).setDragMode(true);
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
            ((BaseBookCardAdapter) adapter).setDragMode(false);
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
                Dialogs.smartListQueryDialog(this, smartListRuq == null ? null : smartListRuq.toString(), -1);
                return true;
            case R.id.action_card_type:
                Dialogs.cardStyleDialog(this, listsPrefs);
                return true;
            case R.id.action_rename_list:
                Dialogs.listNameDialog(this, R.string.title_rename_list, R.string.prompt_rename_list, srcList.getName(),
                        R.id.action_rename_list, posToUpdate);
                return true;
            case R.id.action_rename_smart_list:
                Dialogs.listNameDialog(this, R.string.title_rename_smart_list, R.string.prompt_rename_smart_list,
                        srcList.getName(), R.id.action_rename_smart_list, posToUpdate);
                return true;
            case R.id.action_edit_smart_list:
                QueryBuilderActivity.start(this, smartListRuq);
                return true;
            case R.id.action_clear:
                Dialogs.simpleYesNoDialog(this, R.string.action_clear_list, R.string.prompt_clear_list,
                        R.id.action_clear);
                return true;
            case R.id.action_convert_to_normal_list:
                Dialogs.simpleYesNoDialog(this, R.string.title_convert_to_normal_list,
                        R.string.prompt_convert_to_normal_list, R.id.action_convert_to_normal_list);
                return true;
            case R.id.action_delete_list:
                Dialogs.simpleYesNoDialog(this, R.string.title_delete_list, R.string.prompt_delete_list,
                        R.id.action_delete_list);
                return true;
            case R.id.action_delete_smart_list:
                Dialogs.simpleYesNoDialog(this, R.string.title_delete_smart_list, R.string.prompt_delete_smart_list,
                        R.id.action_delete_smart_list);
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
                TaggingActivity.start(this, getSelectedBooks());
                return true;
            case R.id.action_rate:
                int initialRating = adapter.getSelectedItemCount() == 1 ? getSelectedBooks().get(0).getRating() : 0;
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
                Dialogs.simpleYesNoDialog(this, R.string.title_re_import_books, R.string.prompt_re_import_books,
                        R.id.action_re_import);
                return true;
            case R.id.action_remove:
                Dialogs.simpleYesNoDialog(this, R.string.title_remove_books, R.string.prompt_remove_from_list,
                        R.id.action_remove);
                return true;
            case R.id.action_delete:
                Dialogs.yesNoCheckBoxDialog(this, R.string.title_delete_books, R.string.prompt_delete_books,
                        R.string.prompt_delete_from_device_too, R.id.action_delete);
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
        switch (event.getActionId()) {
            case R.id.action_clear: {
                realm.executeTransaction(tRealm -> srcList.getListItems().deleteAllFromRealm());
                break;
            }
            case R.id.action_open_query_builder: {
                QueryBuilderActivity.start(this, smartListRuq);
                break;
            }
            case R.id.action_rename_list:
            case R.id.action_rename_smart_list: {
                ActionHelper.renameList(realm, srcList, (String) event.getData());
                setTitle((String) event.getData());
                // Update intent used to start activity so that we don't crash if we rotate or something.
                getIntent().putExtra(LIST_NAME, (String) event.getData());
                break;
            }
            case R.id.action_convert_to_normal_list: {
                if (smartListRuq != null) {
                    srcList.convertToNormalListUsingRuq(realm, smartListRuq);
                    smartListRuq = null;
                    // Refresh options menu, then update the UI.
                    invalidateOptionsMenu();
                    updateUi();
                    needsPosUpdate = true;
                }
                break;
            }
            case R.id.action_delete_list:
            case R.id.action_delete_smart_list: {
                // Delete the list currently being shown, then finish the activity.
                ActionHelper.deleteList(realm, srcList);
                finish();
                break;
            }
            case R.id.action_rate: {
                ActionHelper.rateBooks(realm, getSelectedBooks(), (Integer) event.getData());
                break;
            }
            case R.id.action_add_to_list: {
                // TODO actually implement a move/copy to other lists feature???
                //RBookList list = realm.where(RBookList.class).equalTo("name", (String) event.getData()).findFirst();
                //RBookList.addBooks(list, selectedItems);
                break;
            }
            case R.id.action_re_import: {
                ActionHelper.reImportBooks(getSelectedBooks(), this);
                // Don't dismiss action mode yet.
                return;
            }
            case R.id.action_remove: {
                srcList.removeBooks(getSelectedBooks());
                break;
            }
            case R.id.action_delete: {
                ActionHelper.deleteBooks(getSelectedBooks(), (boolean) event.getData());
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
                    // There's a valid RUQ in the extras.
                    updateRuq(data.getParcelableExtra(C.RUQ));
                } else if (smartListRuq == null) {
                    // No valid RUQ returned, and we don't have one already, meaning that srcList's RUQ string is null.
                    // Make it empty string instead so we don't keep forcing the user back into the query builder.
                    ActionHelper.updateSmartList(realm, srcList, "");
                }
                // Update the UI.
                updateUi();
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
     * Set {@link #smartListRuq} to {@code ruq}, then updates {@link #srcList}'s RUQ string.
     * @param ruq {@link RealmUserQuery}.
     */
    private void updateRuq(RealmUserQuery ruq) {
        if (ruq == null) throw new IllegalArgumentException("ruq must not be null.");
        smartListRuq = ruq;
        ActionHelper.updateSmartList(realm, srcList, ruq.toRuqString());
    }

    /**
     * Starts action mode (if it hasn't been already).
     */
    private void startActionMode() {
        if (actionMode == null) actionMode = startSupportActionMode(this);
    }

    /**
     * Get the list of {@link RBook}s which are currently selected.
     * @return List of selected books.
     */
    @SuppressWarnings("unchecked")
    private List<RBook> getSelectedBooks() {
        if (adapterType == AdapterType.BOOK)
            return adapter.getSelectedRealmObjects();
        else if (adapterType == AdapterType.BOOK_LIST_ITEM) return RBookListItem.booksFromBookListItems(
                adapter.getSelectedRealmObjects());
        else throw new IllegalArgumentException("Invalid type.");
    }

    /**
     * Make adapter based on {@link RealmUserQuery#getQueryClass() ruq#getQueryClass()} and {@link #cardType}.
     * @return Adapter.
     */
    @SuppressWarnings("unchecked")
    private RealmBasedRecyclerViewAdapter makeAdapter() {
        if (adapterType == AdapterType.BOOK) {
            switch (cardType) {
                case NORMAL:
                    return new BookCardAdapter(this, (RealmResults<RBook>) items);
                case NO_COVER:
                    return new BookCardNoCoverAdapter(this, (RealmResults<RBook>) items);
                case COMPACT:
                    return new BookCardCompactAdapter(this, (RealmResults<RBook>) items);
                default:
                    return null;
            }
        } else if (adapterType == AdapterType.BOOK_LIST_ITEM) {
            switch (cardType) {
                case NORMAL:
                    return new BookItemCardAdapter(this, (RealmResults<RBookListItem>) items);
                case NO_COVER:
                    return new BookItemCardNoCoverAdapter(this, (RealmResults<RBookListItem>) items);
                case COMPACT:
                    return new BookItemCardCompactAdapter(this, (RealmResults<RBookListItem>) items);
                default:
                    return null;
            }
        } else throw new IllegalArgumentException("Invalid adapter type.");
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
        if (currLastVisPos != RecyclerView.NO_POSITION) recyclerView.getRecyclerView().scrollToPosition(currLastVisPos);
    }

    /**
     * Called when one of the cards is clicked.
     * @param event {@link BookCardClickEvent}.
     */
    @Subscribe
    public void onCardClicked(BookCardClickEvent event) {
        // Get the associated RBook.
        RBook book;
        if (adapterType == AdapterType.BOOK)
            book = realm.where(RBook.class).equalTo("relPath", event.getRelPath()).findFirst();
        else if (adapterType == AdapterType.BOOK_LIST_ITEM)
            book = realm.where(RBookListItem.class).equalTo("book.relPath", event.getRelPath()).findFirst().getBook();
        else throw new IllegalArgumentException("Invalid adapter type.");

        if (actionMode != null) {
            if (event.getType() == BookCardClickEvent.Type.LONG) adapter.extendSelectionTo(event.getPosition());
            else adapter.toggleSelected(event.getPosition());
            return;
        }
        // Do something based on the click type.
        switch (event.getType()) {
            case NORMAL:
                // Open the book file.
                ActionHelper.openBookUsingIntent(book, this);
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

    /**
     * Open the query builder when the button shown for an empty smart list is clicked.
     */
    @OnClick(R.id.open_query_builder)
    void onOpenQueryBuilderClicked() {
        QueryBuilderActivity.start(this, null);
    }

    @Override
    public Activity getCtx() {
        // Provide our activity context to the ReImporter so that it can draw its progress dialog.
        return this;
    }
}
