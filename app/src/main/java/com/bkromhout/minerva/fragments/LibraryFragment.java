package com.bkromhout.minerva.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.widget.RadioGroup;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkromhout.minerva.BookInfoActivity;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.FullImportActivity;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.adapters.BookCardAdapter;
import com.bkromhout.minerva.adapters.BookCardCompactAdapter;
import com.bkromhout.minerva.adapters.BookCardNoCoverAdapter;
import com.bkromhout.minerva.events.BookCardClickEvent;
import com.bkromhout.minerva.events.LibraryActionEvent;
import com.bkromhout.minerva.events.RatedEvent;
import com.bkromhout.minerva.prefs.LibraryPrefs;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.util.Dialogs;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.realmrecyclerview.RealmRecyclerView;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import rx.Observable;

import java.util.List;

/**
 * Fragment in charge of showing the user's whole library.
 */
public class LibraryFragment extends Fragment implements ActionMode.Callback {
    // Views.
    @Bind(R.id.fab)
    FloatingActionButton fabViewOpts;
    @Bind(R.id.recycler)
    RealmRecyclerView recyclerView;

    /**
     * Preferences.
     */
    private LibraryPrefs libraryPrefs;
    /**
     * The current sort type.
     */
    private String sortType;
    /**
     * The current sort direction.
     */
    private String sortDir;
    /**
     * The current card type.
     */
    private String cardType;
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
    private RealmBasedRecyclerViewAdapter adapter;
    /**
     * Action mode.
     */
    private ActionMode actionMode;

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
        libraryPrefs = LibraryPrefs.get();
        readPrefs();

        // Get Realm.
        realm = Realm.getDefaultInstance();

        initUi();
    }

    /**
     * Read preferences into variables.
     */
    private void readPrefs() {
        sortType = libraryPrefs.getSortType(C.SORT_TITLE);
        sortDir = libraryPrefs.getSortDir(C.SORT_ASC);
        cardType = libraryPrefs.getCardType(C.BOOK_CARD_NORMAL);
    }

    /**
     * Initialize the UI.
     */
    private void initUi() {
        // Get results, sort them, then create and bind the adapter.
        books = realm.where(RBook.class)
                     .findAll();
        sortRealmResults();
        adapter = makeAdapter();
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Util.forceMenuIcons(menu, getContext(), getClass().getSimpleName());
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.library, menu);
        super.onCreateOptionsMenu(menu, inflater);
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
        mode.getMenuInflater().inflate(R.menu.library_action_mode, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Util.forceMenuIcons(menu, getContext(), getClass().getSimpleName());
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
            case R.id.action_import:
                // Open the full import activity.
                Util.startAct(getActivity(), FullImportActivity.class, null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_to_list:
                showAddToListDialogOrToast();
                return true;
            case R.id.action_tag:
                // TODO
                // Open the tagging dialog to tag the selected items. Any tags which all items share will be pre-filled.
                return true;
            case R.id.action_rate:
                int initialRating = adapter.getItemCount() == 1
                        ? ((RBook) adapter.getSelectedRealmObjects().get(0)).getRating() : 0;
                Dialogs.showRatingDialog(getContext(), initialRating);
                return true;
            case R.id.action_select_all:
                adapter.selectAll();
                return true;
            case R.id.action_select_none:
                adapter.clearSelections();
                return true;
            case R.id.action_re_import:
                // TODO Re-import the selected items.
                return true;
            case R.id.action_delete:
                // TODO Delete the selected items from our DB (and optionally the device).
                return true;
            default:
                return false;
        }
    }

    /**
     * Called when the user wishes to add some items to a list.
     */
    private void showAddToListDialogOrToast() {
        // Get list of lists (list-ception?)
        RealmResults<RBookList> lists = realm.where(RBookList.class).findAllSorted("sortName");

        if (lists.size() == 0) {
            // If we don't have any lists, just show a toast.
            Toast.makeText(getActivity(), R.string.toast_no_lists, Toast.LENGTH_SHORT).show();
        } else {
            // Create a material dialog.
            new MaterialDialog.Builder(getActivity())
                    .title(R.string.action_add_to_list)
                    .items(Observable.from(lists)
                                     .map(RBookList::getName)
                                     .toList()
                                     .toBlocking()
                                     .single())
                    .itemsCallback((dialog, itemView, which, text) ->
                            EventBus.getDefault().post(new LibraryActionEvent(R.id.action_add_to_list, text)))
                    .show();
        }
    }

    /**
     * Called when we wish to take some action.
     * @param event {@link LibraryActionEvent}.
     */
    @Subscribe
    public void onLibraryActionEvent(LibraryActionEvent event) {
        //noinspection unchecked
        List<RBook> selectedItems = adapter.getSelectedRealmObjects();

        switch (event.getActionId()) {
            case R.id.action_add_to_list: {
                RBookList list = realm.where(RBookList.class).equalTo("name", (String) event.getData()).findFirst();
                RBookList.addBooks(list, selectedItems);
                break;
            }
            case R.id.action_tag: {
                // TODO
                break;
            }
            case R.id.action_delete: {
                // TODO
                break;
            }
        }
        if (actionMode != null) actionMode.finish();
    }

    /**
     * Called when we saved a rating from the rating dialog. Updates the ratings of the selected items.
     * @param event {@link RatedEvent}.
     */
    @Subscribe
    public void onRatedEvent(RatedEvent event) {
        //noinspection unchecked
        List<RBook> selectedItems = adapter.getSelectedRealmObjects();
        realm.executeTransaction(tRealm -> {
            for (RBook item : selectedItems) item.setRating(event.getRating());
        });
        if (actionMode != null) actionMode.finish();
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
        books.sort(C.getRealmSortField(sortType), C.getRealmSortDir(sortDir));
    }

    /**
     * Create a {@link RealmBasedRecyclerViewAdapter} based on the current view options and return it.
     * @return New {@link RealmBasedRecyclerViewAdapter}. Will return null if we cannot get the activity context, if
     * {@link #books} is null or invalid, or if the current value of {@link #cardType} is not valid.
     */
    private RealmBasedRecyclerViewAdapter makeAdapter() {
        Context ctx = getActivity();
        if (ctx == null || books == null || !books.isValid()) return null;

        // Create a new adapter based on the card type.
        switch (cardType) {
            case C.BOOK_CARD_NORMAL:
                return new BookCardAdapter(ctx, books);
            case C.BOOK_CARD_NO_COVER:
                return new BookCardNoCoverAdapter(ctx, books);
            case C.BOOK_CARD_COMPACT:
                return new BookCardCompactAdapter(ctx, books);
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
        // TODO this probably won't show the expected item it both the card and sort type/dir are changed, because while
        // TODO the position will be correct, the item at that position will be different... we'll figure it out.
        if (currLastVisPos != RecyclerView.NO_POSITION) recyclerView.scrollToPosition(currLastVisPos);
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
                // TODO Open the book file.

                break;
            case LONG:
                // Start multi-select.
                adapter.toggleSelected(event.getPosition());
                if (actionMode == null) actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(this);
                break;
            case INFO:
                // Open BookInfoActivity.
                Bundle b = new Bundle();
                b.putString(BookInfoActivity.BOOK_SEL_STR, event.getRelPath());
                Util.startAct(getActivity(), BookInfoActivity.class, b);
                break;
            case QUICK_TAG:
                // TODO Open quick-tag dialog??

                break;
        }
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
        rgSortType.check(idFromStrConst(sortType));
        rgSortDir.check(idFromStrConst(sortDir));
        rgCardType.check(idFromStrConst(cardType));

        // Construct material dialog.
        new MaterialDialog.Builder(getContext())
                .title(R.string.action_view_opts)
                .titleGravity(GravityEnum.CENTER)
                .customView(view, false)
                .contentGravity(GravityEnum.CENTER)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive((dialog, which) -> {
                    // Figure out which options are different.
                    boolean sortTypeChanged = !strConstFromId(rgSortType.getCheckedRadioButtonId()).equals(sortType);
                    boolean sortDirChanged = !strConstFromId(rgSortDir.getCheckedRadioButtonId()).equals(sortDir);
                    boolean cardTypeChanged = !strConstFromId(rgCardType.getCheckedRadioButtonId()).equals(cardType);

                    // Save new options locally if different, then persist them all to preferences.
                    if (sortTypeChanged) sortType = strConstFromId(rgSortType.getCheckedRadioButtonId());
                    if (sortDirChanged) sortDir = strConstFromId(rgSortDir.getCheckedRadioButtonId());
                    if (cardTypeChanged) cardType = strConstFromId(rgCardType.getCheckedRadioButtonId());
                    libraryPrefs.putLibraryViewOpts(sortType, sortDir, cardType);

                    // Re-sort data if necessary.
                    if (sortTypeChanged || sortDirChanged) sortRealmResults();

                    // Switching the card type means switching the recycler view adapter, we certainly don't want to
                    // do that if we haven't changed it.
                    if (cardTypeChanged) changeCardType();

                    // We only need to explicitly tell the recycler view to redraw its items if we changed our sort
                    // options and didn't change our card type (swapping adapters to change card types would force a
                    // redraw anyway).
                    if ((sortTypeChanged || sortDirChanged) && !cardTypeChanged) adapter.notifyDataSetChanged();
                })
                .show();
    }

    /**
     * Convert a view ID to the string constant that that view represents.
     * @param id A view ID.
     * @return A string constant.
     */
    @SuppressLint("DefaultLocale")
    private static String strConstFromId(@IdRes int id) {
        switch (id) {
            // Sort types.
            case R.id.sort_title:
                return C.SORT_TITLE;
            case R.id.sort_author:
                return C.SORT_AUTHOR;
            case R.id.sort_time_added:
                return C.SORT_TIME_ADDED;
            case R.id.sort_rating:
                return C.SORT_RATING;
            // Sort directions.
            case R.id.sort_asc:
                return C.SORT_ASC;
            case R.id.sort_desc:
                return C.SORT_DESC;
            // Card types.
            case R.id.card_normal:
                return C.BOOK_CARD_NORMAL;
            case R.id.card_no_cover:
                return C.BOOK_CARD_NO_COVER;
            case R.id.card_compact:
                return C.BOOK_CARD_COMPACT;
            default:
                throw new IllegalArgumentException(String.format("Invalid resource ID: %d", id));
        }
    }

    /**
     * Convert a string constant to the id of a view that represents it.
     * @param str String constant.
     * @return A view ID.
     */
    private static Integer idFromStrConst(String str) {
        switch (str) {
            // Sort types.
            case C.SORT_TITLE:
                return R.id.sort_title;
            case C.SORT_AUTHOR:
                return R.id.sort_author;
            case C.SORT_TIME_ADDED:
                return R.id.sort_time_added;
            case C.SORT_RATING:
                return R.id.sort_rating;
            // Sort directions.
            case C.SORT_ASC:
                return R.id.sort_asc;
            case C.SORT_DESC:
                return R.id.sort_desc;
            // Card types.
            case C.BOOK_CARD_NORMAL:
                return R.id.card_normal;
            case C.BOOK_CARD_NO_COVER:
                return R.id.card_no_cover;
            case C.BOOK_CARD_COMPACT:
                return R.id.card_compact;
            default:
                throw new IllegalArgumentException(String.format("Invalid string constant: %s", str));
        }
    }
}
