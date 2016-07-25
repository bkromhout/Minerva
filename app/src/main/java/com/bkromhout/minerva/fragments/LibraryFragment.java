package com.bkromhout.minerva.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.*;
import android.widget.RadioGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.Prefs;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.activities.ImportActivity;
import com.bkromhout.minerva.activities.TaggingActivity;
import com.bkromhout.minerva.adapters.BaseBookCardAdapter;
import com.bkromhout.minerva.adapters.BookCardAdapter;
import com.bkromhout.minerva.adapters.BookCardCompactAdapter;
import com.bkromhout.minerva.adapters.BookCardNoCoverAdapter;
import com.bkromhout.minerva.data.ActionHelper;
import com.bkromhout.minerva.enums.BookCardType;
import com.bkromhout.minerva.enums.MarkType;
import com.bkromhout.minerva.enums.SortDir;
import com.bkromhout.minerva.enums.SortType;
import com.bkromhout.minerva.events.ActionEvent;
import com.bkromhout.minerva.events.BookCardClickEvent;
import com.bkromhout.minerva.events.PermGrantedEvent;
import com.bkromhout.minerva.events.UpdatePosEvent;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.util.Dialogs;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.rrvl.BubbleTextProvider;
import com.bkromhout.rrvl.FastScrollHandleStateListener;
import com.bkromhout.rrvl.FastScrollerHandleState;
import com.bkromhout.rrvl.RealmRecyclerView;
import com.jakewharton.rxbinding.support.v7.widget.RxSearchView;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Fragment in charge of showing the user's whole library.
 */
public class LibraryFragment extends Fragment implements ActionMode.Callback, BubbleTextProvider,
        FastScrollHandleStateListener, SnackKiosk.Snacker {
    // Views.
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;
    @BindView(R.id.fab)
    FloatingActionButton fabViewOpts;
    @BindView(R.id.recycler)
    RealmRecyclerView recyclerView;
    @BindView(R.id.empty_library)
    PercentRelativeLayout emptyLibraryView;

    /**
     * The current sort type.
     */
    private SortType sortType;
    /**
     * The current sort direction.
     */
    private SortDir sortDir;
    /**
     * The current card type.
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
     * Current filter string.
     */
    private String filter = null;
    /**
     * SearchView text change subscription.
     */
    private Subscription searchViewChangeSub;
    /**
     * Action mode.
     */
    private static ActionMode actionMode;
    /**
     * Realm change listener which takes care of toggling view visibility when {@link #books} changes from empty to
     * non-empty and vice versa.
     */
    private final RealmChangeListener<RealmResults<RBook>> emptyListener = results -> toggleEmptyState(results.isEmpty());

    public LibraryFragment() {
        // Required empty public constructor
    }

    public static LibraryFragment newInstance() {
        LibraryFragment fragment = new LibraryFragment();
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
        // Inflate the layout for this fragment, then bind and set up views.
        View root = inflater.inflate(R.layout.fragment_library, container, false);
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

        // Check saved state.
        if (savedInstanceState != null) {
            // Try to restore filter text.
            filter = savedInstanceState.getString(C.SEARCH_STRING);
            // If we were in action mode, restore the adapter's state and start action mode.
            if (savedInstanceState.getBoolean(C.IS_IN_ACTION_MODE)) {
                adapter.restoreInstanceState(savedInstanceState);
                startActionMode();
            }
        }
    }

    /**
     * Read preferences into variables.
     */
    private void readPrefs() {
        Prefs prefs = Minerva.prefs();
        sortType = prefs.getLibrarySortType(SortType.TITLE);
        sortDir = prefs.getLibrarySortDir(SortDir.ASC);
        cardType = prefs.getLibraryCardType(BookCardType.NORMAL);
    }

    /**
     * Initialize the UI.
     */
    private void initUi() {
        // Get results, sort them, then create and bind the adapter.
        books = realm.where(RBook.class)
                     .findAll();
        books.addChangeListener(emptyListener);
        toggleEmptyState(books.isEmpty());
        sortRealmResults();
        adapter = makeAdapter();
        recyclerView.setFastScrollHandleStateListener(this);
        recyclerView.setBubbleTextProvider(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Util.forceMenuIcons(menu, getClass().getSimpleName());
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.library, menu);
        // Get SearchView.
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView != null) {
            // Restore search view text.
            if (filter != null && !filter.isEmpty()) {
                searchItem.expandActionView();
                searchView.setQuery(filter, false);
            }

            // Create searchViewChangeSub.
            createSearchViewChangeSub(searchView);
        }
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
        // Save the current filter string.
        outState.putString(C.SEARCH_STRING, filter);
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
        // Remove listener.
        books.removeChangeListener(emptyListener);
        // Close Realm.
        if (realm != null) {
            realm.close();
            realm = null;
        }
        if (actionMode != null) actionMode.finish();
        // Unsubscribe from SearchView text changes.
        if (searchViewChangeSub != null && !searchViewChangeSub.isUnsubscribed()) searchViewChangeSub.unsubscribe();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.library_action_mode, menu);
        adapter.setSelectionMode(true);
        // Change status bar color to be dark to correspond to dark toolbar color.
        getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(getActivity(), R.color.grey900));
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Util.forceMenuIcons(menu, getClass().getSimpleName());
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        adapter.setSelectionMode(false);
        // Set status bar color back to normal.
        getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark));
        adapter.clearSelections();
        actionMode = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_import:
                // Open the import activity.
                Util.startAct(getActivity(), ImportActivity.class, null);
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
                Dialogs.addToListDialogOrSnack(getActivity(), realm);
                return true;
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
        //noinspection unchecked
        List<RBook> selectedItems = adapter.getSelectedRealmObjects();

        switch (event.getActionId()) {
            case R.id.action_add_to_list:
                ActionHelper.addBooksToList(realm, selectedItems, (String) event.getData());
                break;
            case R.id.action_rate:
                ActionHelper.rateBooks(realm, selectedItems, (Integer) event.getData());
                break;
            case R.id.action_mark_as:
                int whichMark = (int) event.getData();
                ActionHelper.markBooks(selectedItems, whichMark < 2 ? MarkType.NEW : MarkType.UPDATED,
                        whichMark % 2 == 0);
                break;
            case R.id.action_re_import:
                ActionHelper.reImportBooks(selectedItems);
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
     * Called when a permission has been granted.
     * @param event {@link PermGrantedEvent}.
     */
    @Subscribe
    public void onPermGrantedEvent(PermGrantedEvent event) {
        if (event.getActionId() == R.id.action_execute_deferred) ActionHelper.doDeferredAction();
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
            if (event.getType() == BookCardClickEvent.Type.LONG) adapter.extendSelectionTo(event.getAdapterPosition());
            else adapter.toggleSelected(event.getAdapterPosition());
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
                adapter.toggleSelected(event.getAdapterPosition());
                startActionMode();
                break;
            case QUICK_TAG:
                TaggingActivity.start(this, book);
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

    @OnClick(R.id.open_full_importer)
    void onOpenImporterClick() {
        // Open the full import activity.
        Util.startAct(getActivity(), ImportActivity.class, null);
    }

    /**
     * Show the view options dialog when the FAB is clicked.
     */
    @OnClick(R.id.fab)
    void onFabClick() {
        // Inflate dialog view and get views.
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(getContext()).inflate(R.layout.library_view_opts, null);
        final RadioGroup rgSortType = ButterKnife.findById(view, R.id.rg_sort_type);
        final RadioGroup rgSortDir = ButterKnife.findById(view, R.id.rg_sort_dir);
        final RadioGroup rgCardType = ButterKnife.findById(view, R.id.rg_card_type);

        // Set up views.
        rgSortType.check(sortType.getResId());
        rgSortDir.check(sortDir.getResId());
        rgCardType.check(cardType.getResId());

        // Construct material dialog.
        new MaterialDialog.Builder(getContext())
                .title(R.string.action_view_opts)
                .titleGravity(GravityEnum.CENTER)
                .customView(view, true)
                .contentGravity(GravityEnum.CENTER)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive((dialog, which) -> {
                    // Figure out which options are different.
                    boolean sortTypeChanged = rgSortType.getCheckedRadioButtonId() != sortType.getResId();
                    boolean sortDirChanged = rgSortDir.getCheckedRadioButtonId() != sortDir.getResId();
                    boolean cardTypeChanged = rgCardType.getCheckedRadioButtonId() != cardType.getResId();

                    // Save new options locally if different, then persist them all to preferences.
                    if (sortTypeChanged) sortType = SortType.fromResId(rgSortType.getCheckedRadioButtonId());
                    if (sortDirChanged) sortDir = SortDir.fromResId(rgSortDir.getCheckedRadioButtonId());
                    if (cardTypeChanged) cardType = BookCardType.fromResId(rgCardType.getCheckedRadioButtonId());
                    Minerva.prefs().putLibraryViewOpts(sortType, sortDir, cardType);

                    // Re-sort data if necessary.
                    if (sortTypeChanged || sortDirChanged) sortRealmResults();

                    // Switching the card type means switching the recycler view adapter, we certainly don't want to
                    // do that if we haven't changed it.
                    if (cardTypeChanged) changeCardType();

                    // We only need to explicitly tell the recycler view to redraw its items if we changed our sort
                    // options and didn't change our card type (swapping adapters to change card types would force a
                    // redraw anyway).
                    if ((sortTypeChanged || sortDirChanged) && !cardTypeChanged) //noinspection unchecked
                        adapter.updateRealmResults(books);
                })
                .show();
    }

    /**
     * Create a subscriber which observes changes to the SearchView's text and updates filters {@link #books}
     * accordingly.
     * @param searchView SearchView to observe changes of.
     */
    private void createSearchViewChangeSub(SearchView searchView) {
        // Unsubscribe from an existing subscriber (if it exists) first.
        if (searchViewChangeSub != null && !searchViewChangeSub.isUnsubscribed()) searchViewChangeSub.unsubscribe();
        // Create new subscriber.
        searchViewChangeSub = RxSearchView.queryTextChanges(searchView)
                                          .skip(1)
                                          .debounce(500, TimeUnit.MILLISECONDS)
                                          .observeOn(AndroidSchedulers.mainThread())
                                          .subscribe(cs -> {
                                              // Apply filter to items and then update adapter's copy of items.
                                              getBooksList(cs.toString());
                                              //noinspection unchecked
                                              adapter.updateRealmResults(books);
                                          }, t -> {
                                              // Shouldn't happen, but we'll log it if it does.
                                              Timber.e(t, "Some error occurred due to the SearchView observer!");
                                          });
    }

    /**
     * Queries Realm to get the {@link #books} list. Uses the given {@code filter} and keeps the current sort method and
     * direction. This method does NOT update the adapter.
     * @param filter String to use to filter the list. Can be {@code null}/empty.
     */
    private void getBooksList(String filter) {
        this.filter = filter;
        if (filter == null || filter.isEmpty())
            books = realm.where(RBook.class)
                         .findAllSorted(sortType.getRealmFields(), sortDir.getRealmSort(sortType.getNumRealmFields()));
        else
            books = realm.where(RBook.class)
                         .contains("title", filter, Case.INSENSITIVE)
                         .or()
                         .contains("author", filter, Case.INSENSITIVE)
                         .findAllSorted(sortType.getRealmFields(), sortDir.getRealmSort(sortType.getNumRealmFields()));

        books.addChangeListener(emptyListener);
        // TODO Might need to manually call toggleEmptyState() here.
    }

    /**
     * Uses the current view options to resort the current {@link RealmResults} in {@link #books}. This method makes no
     * attempts to force a redraw on the actual recycler view.
     * <p>
     * If {@link #realm} or {@link #books} are {@code null}, or otherwise not available/ready, this method does
     * nothing.
     */
    private void sortRealmResults() {
        if (realm == null || realm.isClosed() || books == null || !books.isValid()) return;
        recyclerView.setUseFastScrollBubble(sortType != SortType.TIME_ADDED);
        books = books.sort(sortType.getRealmFields(), sortDir.getRealmSort(sortType.getNumRealmFields()));
        books.addChangeListener(emptyListener);
    }

    /**
     * Create a {@link BaseBookCardAdapter} based on the current view options and return it.
     * @return New {@link BaseBookCardAdapter}. Will return null if we cannot get the activity context, if {@link
     * #books} is null or invalid, or if the current value of {@link #cardType} is not valid.
     */
    private BaseBookCardAdapter makeAdapter() {
        Context ctx = getActivity();
        if (ctx == null || books == null || !books.isValid()) return null;

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
        //noinspection ConstantConditions
        recyclerView.setAdapter(adapter);

        // Scroll back to the same position.
        if (currLastVisPos != RecyclerView.NO_POSITION) recyclerView.getRecyclerView().scrollToPosition(currLastVisPos);
    }

    /**
     * Change visibility of UI elements so that the empty view is shown if {@code showEmptyView} is true, or the
     * RecyclerView and FAB are shown if {@code showEmptyView} is false.
     * @param showEmptyView Whether to show the empty view or not.
     */
    private void toggleEmptyState(boolean showEmptyView) {
        boolean isFiltering = filter != null && !filter.isEmpty();
        // TODO Add a specific view to show when there are no results due to the filter.
        recyclerView.setVisibility(!isFiltering && showEmptyView ? View.GONE : View.VISIBLE);
        fabViewOpts.setVisibility(!isFiltering && showEmptyView ? View.GONE : View.VISIBLE);
        emptyLibraryView.setVisibility(!isFiltering && showEmptyView ? View.VISIBLE : View.GONE);
    }

    @Override
    public String getFastScrollBubbleText(int position) {
        if (position < 0 || position >= books.size()) return null;
        switch (sortType) {
            case TITLE:
                return String.valueOf(Character.toUpperCase(books.get(position).title.charAt(0)));
            case AUTHOR:
                return String.valueOf(Character.toUpperCase(books.get(position).author.charAt(0)));
            case RATING:
                return String.valueOf(books.get(position).rating);
            default:
                return null;
        }
    }

    @Override
    public void onHandleStateChanged(FastScrollerHandleState newState) {
        if (newState == FastScrollerHandleState.PRESSED) fabViewOpts.hide();
    }

    @NonNull
    @Override
    public View getSnackbarAnchorView() {
        return coordinator;
    }
}
