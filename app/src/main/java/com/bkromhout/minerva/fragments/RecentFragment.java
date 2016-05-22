package com.bkromhout.minerva.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.Prefs;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.activities.TaggingActivity;
import com.bkromhout.minerva.adapters.BaseBookCardAdapter;
import com.bkromhout.minerva.adapters.BookCardAdapter;
import com.bkromhout.minerva.adapters.BookCardCompactAdapter;
import com.bkromhout.minerva.adapters.BookCardNoCoverAdapter;
import com.bkromhout.minerva.data.ActionHelper;
import com.bkromhout.minerva.enums.BookCardType;
import com.bkromhout.minerva.enums.MainFrag;
import com.bkromhout.minerva.enums.MarkType;
import com.bkromhout.minerva.events.ActionEvent;
import com.bkromhout.minerva.events.BookCardClickEvent;
import com.bkromhout.minerva.events.PrefChangeEvent;
import com.bkromhout.minerva.events.UpdatePosEvent;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.util.Dialogs;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.rrvl.FastScrollHandleStateListener;
import com.bkromhout.rrvl.FastScrollerHandleState;
import com.bkromhout.rrvl.RealmRecyclerView;
import com.bkromhout.rrvl.RealmRecyclerViewAdapter;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

/**
 * Fragment in charge of showing recently opened books.
 */
public class RecentFragment extends Fragment implements ActionMode.Callback, FastScrollHandleStateListener,
        SnackKiosk.Snacker {
    // Views.
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;
    @BindView(R.id.fab)
    FloatingActionButton fabOpenRecent;
    @BindView(R.id.recycler)
    RealmRecyclerView recyclerView;

    /**
     * Which type of card to use.
     */
    private BookCardType cardType;
    /**
     * Instance of Realm.
     */
    private Realm realm;
    /**
     * {@link RBook}s currently shown in the recycler view.
     */
    private RealmResults<RBook> books;
    /**
     * Adapter currently being used by the recycler view.
     */
    private BaseBookCardAdapter adapter;
    /**
     * Action mode.
     */
    private static ActionMode actionMode;

    public RecentFragment() {
        // Required empty public constructor
    }

    public static RecentFragment newInstance() {
        RecentFragment fragment = new RecentFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We have menu items we'd like to add.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment, then bind views.
        View root = inflater.inflate(R.layout.fragment_recent, container, false);
        ButterKnife.bind(this, root);
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Read prefs to fill in vars.
        readPrefs();

        // Get Realm.
        realm = Realm.getDefaultInstance();

        initUi();

        // If we have a saved instance state, check to see if we were in action mode.
        if (savedInstanceState != null && savedInstanceState.getBoolean(C.IS_IN_ACTION_MODE)) {
            // If we were in action mode, restore the adapter's state and start action mode.
            adapter.restoreInstanceState(savedInstanceState);
            startActionMode();
        }
    }

    /**
     * Read preferences into variables.
     */
    private void readPrefs() {
        cardType = Minerva.prefs().getRecentsCardType(BookCardType.NORMAL);
    }

    /**
     * Initialize the UI.
     */
    private void initUi() {
        // Get results, then create and bind the adapter.
        books = realm.where(RBook.class)
                     .equalTo("isInRecents", true)
                     .findAllSorted("lastReadDate", Sort.DESCENDING);
        adapter = makeAdapter();
        recyclerView.setFastScrollHandleStateListener(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Util.forceMenuIcons(menu, getClass().getSimpleName());
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.recent, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        SnackKiosk.startSnacking(this);
    }

    @Override
    public void onPause() {
        SnackKiosk.stopSnacking();
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save adapter state if we're in action mode.
        if (actionMode != null) {
            adapter.saveInstanceState(outState);
            outState.putBoolean(C.IS_IN_ACTION_MODE, true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
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
        mode.getMenuInflater().inflate(R.menu.recent_action_mode, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Util.forceMenuIcons(menu, getClass().getSimpleName());
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        adapter.clearSelections();
        actionMode = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_card_type:
                Dialogs.cardStyleDialog(getContext(), MainFrag.RECENT);
                return true;
            case R.id.action_clear:
                Dialogs.simpleConfirmDialog(getContext(), R.string.action_clear_recents, R.string.prompt_clear_recents,
                        R.string.action_clear, R.id.action_clear);
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
                TaggingActivity.start(this, adapter.getSelectedRealmObjects());
                return true;
            case R.id.action_rate:
                int initialRating = adapter.getSelectedItemCount() == 1
                        ? ((RBook) adapter.getSelectedRealmObjects().get(0)).rating : 0;
                Dialogs.ratingDialog(getContext(), initialRating);
                return true;
            case R.id.action_mark_as:
                Dialogs.markAsDialog(getActivity());
                return true;
            case R.id.action_add_to_list:
                Dialogs.addToListDialogOrToast(getActivity(), realm);
                return true;
            case R.id.action_re_import:
                Dialogs.simpleConfirmDialog(getContext(), R.string.title_re_import_books,
                        R.string.prompt_re_import_books, R.string.action_re_import, R.id.action_re_import);
                return true;
            case R.id.action_remove:
                Dialogs.simpleConfirmDialog(getContext(), R.string.title_remove_books,
                        R.string.prompt_remove_from_recents, R.string.action_remove, R.id.action_remove);
                return true;
            case R.id.action_delete:
                Dialogs.confirmCheckBoxDialog(getContext(), R.string.title_delete_books, R.string.prompt_delete_books,
                        R.string.prompt_delete_from_device_too, R.string.info_delete_from_device_permanent,
                        R.string.action_delete, R.id.action_delete);
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
            case R.id.action_clear:
                realm.executeTransaction(tRealm -> {
                    // Set isInRecents to false for all RBooks which currently have it set to true.
                    RealmResults<RBook> recentBooks = tRealm.where(RBook.class).equalTo("isInRecents", true).findAll();
                    for (int i = recentBooks.size() - 1; i >= 0; i--) recentBooks.get(i).isInRecents = false;
                });
                break;
            case R.id.action_rate:
                ActionHelper.rateBooks(realm, selectedItems, (Integer) event.getData());
                break;
            case R.id.action_mark_as:
                int whichMark = (int) event.getData();
                ActionHelper.markBooks(selectedItems, whichMark < 2 ? MarkType.NEW : MarkType.UPDATED,
                        whichMark % 2 == 0);
                break;
            case R.id.action_add_to_list:
                ActionHelper.addBooksToList(realm, selectedItems, (String) event.getData());
                break;
            case R.id.action_re_import:
                ActionHelper.reImportBooks(selectedItems);
                break;
            case R.id.action_remove:
                realm.executeTransaction(tRealm -> {
                    // Set isInRecents to false for all selected RBooks.
                    for (RBook book : selectedItems) book.isInRecents = false;
                });
                break;
            case R.id.action_delete:
                ActionHelper.deleteBooks(selectedItems, (boolean) event.getData());
                break;
        }
        if (actionMode != null) actionMode.finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case C.RC_TAG_ACTIVITY:
                // Came back from TaggingActivity.
                if (resultCode == Activity.RESULT_OK) {
                    // We've changed the tags on some books.
                    if (actionMode != null) actionMode.finish();
                }
                break;
        }
    }

    /**
     * Starts action mode (if it hasn't been already).
     */
    private void startActionMode() {
        if (actionMode == null) actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(this);
    }

    /**
     * Called when one of the cards is clicked.
     * @param event {@link BookCardClickEvent}.
     */
    @Subscribe
    public void onCardClicked(BookCardClickEvent event) {
        // Get the associated RBook.
        RBook book = books.where().equalTo("relPath", event.getRelPath()).findFirst();

        if (actionMode != null) {
            if (event.getType() == BookCardClickEvent.Type.LONG) adapter.extendSelectionTo(event.getPosition());
            else adapter.toggleSelected(event.getPosition());
            return;
        }
        // Do something based on the click type.
        switch (event.getType()) {
            case NORMAL:
                // Open the book file.
                ActionHelper.openBookUsingIntent(book);
                break;
            case LONG:
                // Start multi-select.
                adapter.toggleSelected(event.getPosition());
                startActionMode();
                break;
            case QUICK_TAG:
                TaggingActivity.start(this, book);
                break;
        }
    }

    /**
     * Open the most recently read book when the FAB is clicked.
     */
    @OnClick(R.id.fab)
    void onFabClick() {
        if (books != null && books.isValid() && !books.isEmpty())
            ActionHelper.openBookUsingIntent(books.first());
    }

    /**
     * Create a {@link RealmRecyclerViewAdapter} based on the current view options and return it.
     * @return New {@link RealmRecyclerViewAdapter}. Will return null if we cannot get the activity context, if {@link
     * #books} is null or invalid, or if the current value of {@link #cardType} is not valid.
     */
    private BaseBookCardAdapter makeAdapter() {
        if (books == null || !books.isValid()) return null;

        // Create a new adapter based on the card type.
        switch (cardType) {
            case NORMAL:
                return new BookCardAdapter(getActivity(), books);
            case NO_COVER:
                return new BookCardNoCoverAdapter(getActivity(), books);
            case COMPACT:
                return new BookCardCompactAdapter(getActivity(), books);
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
        int currLastVisPos = recyclerView.getLayoutManager().findLastCompletelyVisibleItemPosition();

        // Swap the adapter
        if (adapter != null) adapter.close();
        adapter = makeAdapter();
        recyclerView.setAdapter(adapter);

        // Scroll back to the same position.
        if (currLastVisPos != RecyclerView.NO_POSITION) recyclerView.getRecyclerView().scrollToPosition(currLastVisPos);
    }

    /**
     * React to a changed preference.
     * @param event {@link PrefChangeEvent}.
     */
    @Subscribe
    public void onPrefChangeEvent(PrefChangeEvent event) {
        // Do something different based on name of changed preference.
        switch (event.getPrefName()) {
            case Prefs.RECENTS_CARD_TYPE:
                cardType = Minerva.prefs().getRecentsCardType(cardType);
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
        // If the event's position is ALL_POSITIONS, indicate the whole dataset changed. Otherwise, update the item
        // at the position in the event.
        if (event.getPosition() == UpdatePosEvent.ALL_POSITIONS) adapter.notifyDataSetChanged();
        else adapter.notifyItemChanged(event.getPosition());
    }

    @Override
    public void onHandleStateChanged(FastScrollerHandleState newState) {
        if (newState == FastScrollerHandleState.PRESSED) fabOpenRecent.hide();
    }

    @NonNull
    @Override
    public View getSnackbarAnchorView() {
        return coordinator;
    }
}
