package com.bkromhout.minerva.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import com.bkromhout.minerva.activities.BookInfoActivity;
import com.bkromhout.minerva.activities.QueryBuilderActivity;
import com.bkromhout.minerva.activities.TaggingActivity;
import com.bkromhout.minerva.adapters.*;
import com.bkromhout.minerva.data.ActionHelper;
import com.bkromhout.minerva.data.DataUtils;
import com.bkromhout.minerva.enums.BookCardType;
import com.bkromhout.minerva.enums.MainFrag;
import com.bkromhout.minerva.enums.MarkType;
import com.bkromhout.minerva.enums.ModelType;
import com.bkromhout.minerva.events.ActionEvent;
import com.bkromhout.minerva.events.BookCardClickEvent;
import com.bkromhout.minerva.events.PrefChangeEvent;
import com.bkromhout.minerva.events.UpdatePosEvent;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.realm.RBookListItem;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.util.Dialogs;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.rrvl.FastScrollHandleStateListener;
import com.bkromhout.rrvl.FastScrollerHandleState;
import com.bkromhout.rrvl.RealmRecyclerView;
import com.bkromhout.ruqus.RealmUserQuery;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

/**
 * Fragment in charge of letting the user power search.
 */
public class PowerSearchFragment extends Fragment implements ActionMode.Callback, FastScrollHandleStateListener,
        SnackKiosk.Snacker {
    private static final String QUERY_TYPE = "QUERY_TYPE";

    // Views.
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;
    @BindView(R.id.fab)
    FloatingActionButton fabQuery;
    @BindView(R.id.recycler)
    RealmRecyclerView recyclerView;

    /**
     * Preferences.
     */
    Prefs prefs = Minerva.get().prefs;
    /**
     * Which type of card to use.
     */
    private BookCardType cardType;
    /**
     * Instance of Realm.
     */
    private Realm realm;
    /**
     * Current {@link RealmUserQuery} in the view.
     */
    private RealmUserQuery ruq;
    /**
     * Current query type. Changes as {@link #ruq} changes.
     */
    private ModelType queryType;
    /**
     * Current query results.
     */
    private RealmResults<? extends RealmObject> results;
    /**
     * Adapter currently being used by the recycler view.
     */
    private BaseBookCardAdapter adapter;
    /**
     * Action mode.
     */
    private static ActionMode actionMode;

    public PowerSearchFragment() {
        // Required empty public constructor
    }

    public static PowerSearchFragment newInstance() {
        PowerSearchFragment fragment = new PowerSearchFragment();
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
        View root = inflater.inflate(R.layout.fragment_power_search, container, false);
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

        // Restore RealmUserQuery if we're coming back from a configuration change.
        if (savedInstanceState != null && savedInstanceState.containsKey(C.RUQ)) {
            ruq = savedInstanceState.getParcelable(C.RUQ);
            queryType = ModelType.values()[savedInstanceState.getInt(QUERY_TYPE)];
        }

        recyclerView.setFastScrollHandleStateListener(this);
        updateUi();

        // If we were in action mode, restore the adapter's state and start action mode.
        if (savedInstanceState != null && savedInstanceState.containsKey(C.IS_IN_ACTION_MODE)) {
            adapter.restoreInstanceState(savedInstanceState);
            startActionMode();
        }

    }

    /**
     * Read preferences into variables.
     */
    private void readPrefs() {
        cardType = prefs.getPowerSearchCardType(BookCardType.NORMAL);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Util.forceMenuIcons(menu, getClass().getSimpleName());
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.power_search, menu);
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
        // Save RUQ if we have one.
        if (ruq != null) {
            outState.putParcelable(C.RUQ, ruq);
            outState.putInt(QUERY_TYPE, queryType.ordinal());
        }
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
        mode.getMenuInflater().inflate(R.menu.power_search_action_mode, menu);
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
            case R.id.action_show_query:
                Dialogs.queryDialog(getActivity(), R.string.title_curr_query, R.string.no_query,
                        ruq != null ? ruq.toString() : null, false, -1);
                return true;
            case R.id.action_save_as_smart_list:
                if (ruq == null) return true;
                Dialogs.uniqueNameDialog(getActivity(), RBookList.class, R.string.action_new_smart_list,
                        R.string.prompt_new_smart_list, R.string.list_name_hint, null, R.id.action_new_smart_list, -1);
                return true;
            case R.id.action_card_type:
                Dialogs.cardStyleDialog(getContext(), MainFrag.POWER_SEARCH);
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
            case R.id.action_add_to_list:
                Dialogs.addToListDialogOrToast(getActivity(), realm);
                return true;
            case R.id.action_tag:
                //noinspection unchecked
                TaggingActivity.start(this, getSelectedBooks());
                return true;
            case R.id.action_rate:
                int initialRating = adapter.getSelectedItemCount() == 1 ? getSelectedBooks().get(0).rating : 0;
                Dialogs.ratingDialog(getContext(), initialRating);
                return true;
            case R.id.action_mark_as:
                Dialogs.markAsDialog(getActivity());
                return true;
            case R.id.action_re_import:
                Dialogs.simpleConfirmDialog(getContext(), R.string.title_re_import_books,
                        R.string.prompt_re_import_books, R.string.action_re_import, R.id.action_re_import);
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
        switch (event.getActionId()) {
            case R.id.action_add_to_list:
                ActionHelper.addBooksToList(realm, getSelectedBooks(), (String) event.getData());
                break;
            case R.id.action_rate:
                ActionHelper.rateBooks(realm, getSelectedBooks(), (Integer) event.getData());
                break;
            case R.id.action_mark_as:
                int whichMark = (int) event.getData();
                ActionHelper.markBooks(getSelectedBooks(), whichMark < 2 ? MarkType.NEW : MarkType.UPDATED,
                        whichMark % 2 == 0);
                break;
            case R.id.action_re_import:
                ActionHelper.reImportBooks(getSelectedBooks());
                break;
            case R.id.action_delete:
                ActionHelper.deleteBooks(getSelectedBooks(), (boolean) event.getData());
                break;
            case R.id.action_new_smart_list:
                ActionHelper.createNewSmartList(realm, (String) event.getData(), ruq);
                Snackbar.make(recyclerView, R.string.smart_list_created, Snackbar.LENGTH_SHORT).show();
                break;
        }
        if (actionMode != null) actionMode.finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case C.RC_QUERY_BUILDER_ACTIVITY:
                // Came back from QueryBuilderActivity.
                if (resultCode == Activity.RESULT_OK) {
                    // We've changed our query. Get the RealmUserQuery.
                    ruq = data.getParcelableExtra(C.RUQ);
                    // Figure out the query type.
                    queryType = ModelType.fromRealmClass(ruq.getQueryClass());
                    // Update the UI.
                    updateUi();
                }
                break;
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
        RBook book;
        if (queryType == ModelType.BOOK)
            book = (RBook) results.where().equalTo("relPath", event.getRelPath()).findFirst();
        else if (queryType == ModelType.BOOK_LIST_ITEM)
            book = ((RBookListItem) results.where().equalTo("book.relPath", event.getRelPath()).findFirst()).book;
        else throw new IllegalArgumentException("Invalid queryType");

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
            case INFO:
                // Open BookInfoActivity.
                BookInfoActivity.start(getActivity(), event.getRelPath(), event.getPosition());
                break;
            case QUICK_TAG:
                TaggingActivity.start(this, book);
                break;
        }
    }

    /**
     * Show the {@link QueryBuilderActivity} when the FAB is clicked.
     */
    @OnClick(R.id.fab)
    void onFabClick() {
        // Start QueryBuilderActivity, passing it the currently held RealmUserQuery.
        QueryBuilderActivity.start(this, ruq);
    }

    /**
     * Update the UI.
     */
    private void updateUi() {
        if (ruq != null) results = ruq.execute(realm);
        adapter = makeAdapter();
        recyclerView.setAdapter(adapter);
    }

    /**
     * Get the list of {@link RBook}s which are currently selected.
     * @return List of selected books.
     */
    @SuppressWarnings("unchecked")
    private List<RBook> getSelectedBooks() {
        if (queryType == ModelType.BOOK)
            return adapter.getSelectedRealmObjects();
        else if (queryType == ModelType.BOOK_LIST_ITEM) return DataUtils.booksFromBookListItems(
                adapter.getSelectedRealmObjects());
        else throw new IllegalArgumentException("Invalid type.");
    }

    /**
     * Make adapter based on {@link RealmUserQuery#getQueryClass() ruq#getQueryClass()} and {@link #cardType}.
     * @return Adapter, or null if {@link #ruq} is null/invalid or {@link #cardType} is null/invalid.
     */
    @SuppressWarnings("unchecked")
    private BaseBookCardAdapter makeAdapter() {
        if (ruq == null || !ruq.isQueryValid()) return null;
        else if (queryType == ModelType.BOOK) {
            switch (cardType) {
                case NORMAL:
                    return new BookCardAdapter(getActivity(), (RealmResults<RBook>) results);
                case NO_COVER:
                    return new BookCardNoCoverAdapter(getActivity(), (RealmResults<RBook>) results);
                case COMPACT:
                    return new BookCardCompactAdapter(getActivity(), (RealmResults<RBook>) results);
                default:
                    return null;
            }
        } else if (queryType == ModelType.BOOK_LIST_ITEM) {
            switch (cardType) {
                case NORMAL:
                    return new BookItemCardAdapter(getActivity(), (RealmResults<RBookListItem>) results);
                case NO_COVER:
                    return new BookItemCardNoCoverAdapter(getActivity(), (RealmResults<RBookListItem>) results);
                case COMPACT:
                    return new BookItemCardCompactAdapter(getActivity(), (RealmResults<RBookListItem>) results);
                default:
                    return null;
            }
        } else return null;
    }

    /**
     * Uses the current view options to change the card layout currently in use. Preserves the position currently
     * scrolled to in the list before switching adapters.
     */
    private void changeCardType() {
        // Store the current last visible item position so that we can scroll back to it after switching adapters.
        int currLastVisPos = recyclerView.getLayoutManager().findLastCompletelyVisibleItemPosition();

        // Swap the adapter.
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
            case Prefs.POWER_SEARCH_CARD_TYPE:
                cardType = prefs.getPowerSearchCardType(cardType);
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
        if (newState == FastScrollerHandleState.PRESSED) fabQuery.hide();
    }

    @NonNull
    @Override
    public View getSnackbarAnchorView() {
        return coordinator;
    }
}
