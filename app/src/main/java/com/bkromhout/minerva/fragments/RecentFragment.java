package com.bkromhout.minerva.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkromhout.minerva.BookInfoActivity;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.TaggingActivity;
import com.bkromhout.minerva.adapters.BaseBookCardAdapter;
import com.bkromhout.minerva.adapters.BookCardAdapter;
import com.bkromhout.minerva.adapters.BookCardCompactAdapter;
import com.bkromhout.minerva.adapters.BookCardNoCoverAdapter;
import com.bkromhout.minerva.events.ActionEvent;
import com.bkromhout.minerva.events.BookCardClickEvent;
import com.bkromhout.minerva.prefs.RecentPrefs;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.util.Dialogs;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.realmrecyclerview.RealmRecyclerView;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import io.realm.Sort;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.List;

/**
 * Fragment in charge of showing recently opened books.
 */
public class RecentFragment extends Fragment implements ActionMode.Callback {
    // Views.
    @Bind(R.id.fab)
    FloatingActionButton fabViewOpts;
    @Bind(R.id.recycler)
    RealmRecyclerView recyclerView;

    /**
     * Preferences.
     */
    private RecentPrefs recentPrefs;
    /**
     * Which type of card to use.
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
        recentPrefs = RecentPrefs.get();
        readPrefs();

        // Get Realm.
        realm = Realm.getDefaultInstance();

        initUi();
    }

    /**
     * Read preferences into variables.
     */
    private void readPrefs() {
        cardType = recentPrefs.getCardType(C.BOOK_CARD_NORMAL);
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
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Util.forceMenuIcons(menu, getContext(), getClass().getSimpleName());
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
        mode.getMenuInflater().inflate(R.menu.recent_action_mode, menu);
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
            case R.id.action_card_type:
                showCardStyleDialog();
                return true;
            case R.id.action_clear:
                Dialogs.simpleYesNoDialog(getContext(), R.string.title_clear_list, R.string.clear_list_prompt,
                        R.id.action_clear);
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
                        ? ((RBook) adapter.getSelectedRealmObjects().get(0)).getRating() : 0;
                Dialogs.ratingDialog(getContext(), initialRating);
                return true;
            case R.id.action_add_to_list:
                Dialogs.addToListDialogOrToast(getActivity(), realm);
                return true;
            case R.id.action_re_import:
                Dialogs.simpleYesNoDialog(getContext(), R.string.title_dialog_reimport, R.string.reimport_prompt,
                        R.id.action_re_import);
                return true;
            case R.id.action_remove:
                Dialogs.simpleYesNoDialog(getContext(), R.string.title_remove_books,
                        R.string.remove_from_recents_prompt,
                        R.id.action_remove);
                return true;
            case R.id.action_delete:
                Dialogs.yesNoCheckBoxDialog(getContext(), R.string.title_delete_books, R.string.delete_books_prompt,
                        R.string.delete_from_device_too, R.id.action_delete);
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
            case R.id.action_clear: {
                realm.executeTransaction(tRealm -> {
                    // Set isInRecents to false for all RBooks which currently have it set to true.
                    RealmResults<RBook> recentBooks = tRealm.where(RBook.class).equalTo("isInRecents", true).findAll();
                    for (int i = recentBooks.size() - 1; i >= 0; i--) recentBooks.get(i).setInRecents(false);
                });
                break;
            }
            case R.id.action_rate: {
                realm.executeTransaction(tRealm -> {
                    for (RBook item : selectedItems) item.setRating((Integer) event.getData());
                });
                break;
            }
            case R.id.action_add_to_list: {
                // Add books to the list selected list.
                realm.where(RBookList.class)
                     .equalTo("name", (String) event.getData())
                     .findFirst()
                     .addBooks(selectedItems);
                break;
            }
            case R.id.action_re_import: {
                // TODO Re-import the selected items.
                break;
            }
            case R.id.action_remove: {
                realm.executeTransaction(tRealm -> {
                    // Set isInRecents to false for all selected RBooks.
                    for (RBook book : selectedItems) book.setInRecents(false);
                });
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
        }
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
                book.openFileUsingIntent(getContext());
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
                TaggingActivity.start(this, book);
                break;
        }
    }

    /**
     * Open the most recently read book when the FAB is clicked.
     */
    @OnClick(R.id.fab)
    void onFabClick() {
        if (books != null && books.isValid() && !books.isEmpty()) books.first().openFileUsingIntent(getContext());
    }

    /**
     * Create a {@link RealmBasedRecyclerViewAdapter} based on the current view options and return it.
     * @return New {@link RealmBasedRecyclerViewAdapter}. Will return null if we cannot get the activity context, if
     * {@link #books} is null or invalid, or if the current value of {@link #cardType} is not valid.
     */
    private BaseBookCardAdapter makeAdapter() {
        if (books == null || !books.isValid()) return null;

        // Create a new adapter based on the card type.
        switch (cardType) {
            case C.BOOK_CARD_NORMAL:
                return new BookCardAdapter(getActivity(), books);
            case C.BOOK_CARD_NO_COVER:
                return new BookCardNoCoverAdapter(getActivity(), books);
            case C.BOOK_CARD_COMPACT:
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
        int currLastVisPos = recyclerView.getLayoutManger().findLastCompletelyVisibleItemPosition();

        // Swap the adapter
        if (adapter != null) adapter.close();
        adapter = makeAdapter();
        recyclerView.setAdapter(adapter);

        // Scroll back to the same position.
        if (currLastVisPos != RecyclerView.NO_POSITION) recyclerView.scrollToPosition(currLastVisPos);
    }

    /**
     * Shows a dialog which allows the user to pick the card style.
     * <p>
     * TODO move to Dialogs after making card type into an enum class.
     */
    private void showCardStyleDialog() {
        new MaterialDialog.Builder(getContext())
                .title(R.string.action_card_type)
                .items(C.getStr(R.string.card_normal),
                        C.getStr(R.string.card_no_cover),
                        C.getStr(R.string.card_compact))
                .itemsCallbackSingleChoice(idxFromStrConst(cardType), (dialog, itemView, which, text) -> {
                    // Do nothing if it's the same.
                    if (idxFromStrConst(cardType) == which) return true;

                    // Persist the new card style.
                    cardType = strConstFromIdx(which);
                    recentPrefs.putCardType(cardType);

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
