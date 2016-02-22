package com.bkp.minerva.fragments;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.menu.MenuBuilder;
import android.util.Log;
import android.view.*;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkp.minerva.BookInfoActivity;
import com.bkp.minerva.BookListActivity;
import com.bkp.minerva.C;
import com.bkp.minerva.R;
import com.bkp.minerva.adapters.BookListCardAdapter;
import com.bkp.minerva.events.BookListCardClickEvent;
import com.bkp.minerva.prefs.AllListsPrefs;
import com.bkp.minerva.realm.RBookList;
import com.bkp.minerva.util.Util;
import io.realm.Realm;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.reflect.Method;

/**
 * Fragment in charge of showing all of the book lists.
 */
public class AllListsFragment extends Fragment {
    // Views.
    @Bind(R.id.fab)
    FloatingActionButton fabViewOpts;
    @Bind(R.id.recycler)
    RealmRecyclerView recyclerView;

    /**
     * Preferences.
     */
    private AllListsPrefs allListsPrefs;
    /**
     * Instance of Realm.
     */
    private Realm realm;
    /**
     * {@link RBookList}s currently shown in the recycler view.
     */
    private RealmResults<RBookList> lists;

    public AllListsFragment() {
        // Required empty public constructor
    }

    public static AllListsFragment newInstance() {
        AllListsFragment fragment = new AllListsFragment();
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
        View root = inflater.inflate(R.layout.fragment_all_lists, container, false);
        ButterKnife.bind(this, root);
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Read prefs to fill in vars.
        allListsPrefs = AllListsPrefs.get();
        readPrefs();

        // Get Realm.
        realm = Realm.getDefaultInstance();

        initUi();
    }

    /**
     * Read preferences into variables.
     */
    private void readPrefs() {

    }

    /**
     * Initialize the UI.
     */
    private void initUi() {
        // Get lists, then create and bind the adapter.
        lists = realm.where(RBookList.class)
                     .findAllSorted("sortName");
        recyclerView.setAdapter(new BookListCardAdapter(getActivity(), lists, true, true));
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (menu != null) {
            // Make sure all icons are tinted the correct color, including those in the overflow menu.
            for (int i = 0; i < menu.size(); i++)
                menu.getItem(i).getIcon()
                    .setColorFilter(ContextCompat.getColor(getContext(), R.color.textColorPrimary),
                            PorterDuff.Mode.SRC_IN);
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
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.all_lists, menu);
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
        // Close Realm.
        if (realm != null) {
            realm.close();
            realm = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Called when one of the cards is clicked.
     * @param event {@link BookListCardClickEvent}.
     */
    @Subscribe
    public void onCardClicked(BookListCardClickEvent event) {
        // Do something based on the click type.
        switch (event.getType()) {
            case NORMAL:
                Bundle b = new Bundle();
                b.putString(BookListActivity.LIST_SEL_STR, event.getName());
                Util.startAct(getActivity(), BookInfoActivity.class, b);
                break;
            case LONG:
                // TODO Start multi-select.
                break;
            case ACTIONS:
                // Handle action.
                onCardMenuActionClicked(event.getActionId(), event.getName());
                break;
        }
    }

    /**
     * Called when one of the actions in a card's popup menu is clicked.
     * @param actionId The ID of the popup menu item.
     */
    private void onCardMenuActionClicked(int actionId, String listName) {
        if (actionId < 0 || listName == null) throw new IllegalArgumentException();

        // Do something based on the menu item ID.
        switch (actionId) {
            case R.id.action_rename_list: {
                // Show a dialog to rename the list.
                new MaterialDialog.Builder(getActivity())
                        .title(R.string.title_rename_list)
                        .content(R.string.rename_list_prompt)
                        .autoDismiss(false)
                        .negativeText(R.string.cancel)
                        .onNegative((dialog, which) -> dialog.dismiss())
                        .input(C.getStr(R.string.list_name_hint), listName, false, ((dialog, input) -> {
                            // Get Realm instance, then check to see if the entered name has already been taken.
                            Realm innerRealm = Realm.getDefaultInstance();
                            String newName = input.toString().trim();
                            boolean nameExists = innerRealm.where(RBookList.class)
                                                           .equalTo("name", newName)
                                                           .findFirst() != null;

                            // If the name exists (other than the list's current name), set the error text on the
                            // edit text. If it doesn't, rename the RBookList.
                            if (listName.equals(newName)) {
                                // If it's the same name, just close Realm and dismiss the dialog.
                                innerRealm.close();
                                dialog.dismiss();
                            } else if (nameExists) {
                                //noinspection ConstantConditions
                                dialog.getInputEditText().setError(C.getStr(R.string.list_name_exists));
                                innerRealm.close();
                            } else {
                                innerRealm.executeTransaction(tRealm -> {
                                    RBookList list = tRealm.where(RBookList.class)
                                                           .equalTo("name", listName)
                                                           .findFirst();
                                    list.setName(newName);
                                    list.setSortName(newName.toLowerCase()); // TODO Work-around
                                });
                                innerRealm.close();
                                dialog.dismiss();
                            }
                        }))
                        .show();
                break;
            }
            case R.id.action_delete_list: {
                // Show a confirmation dialog for deleting the list.
                // TODO have this work with an undo snackbar...needs to be more complex that simply waiting??
                // TODO maybe a boolean on the model called "isDeleting", and have the query not include those?
                new MaterialDialog.Builder(getActivity())
                        .title(R.string.title_delete_list)
                        .content(C.getStr(R.string.delete_list_prompt, listName))
                        .positiveText(R.string.yes)
                        .negativeText(R.string.no)
                        .onPositive((dialog, which) -> {
                            // Get Realm instance, then delete the list.
                            Realm.getDefaultInstance()
                                 .executeTransaction(tRealm -> tRealm
                                         .where(RBookList.class)
                                         .equalTo("name", listName)
                                         .findFirst()
                                         .removeFromRealm());
                        })
                        .show();
                break;
            }
        }
    }

    /**
     * Show the new list dialog when the FAB is clicked.
     */
    @OnClick(R.id.fab)
    void onFabClick() {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.action_new_list)
                .content(R.string.new_list_prompt)
                .autoDismiss(false)
                .negativeText(R.string.cancel)
                .onNegative((dialog, which) -> dialog.dismiss())
                .input(R.string.list_name_hint, 0, false, (dialog, input) -> {
                    // Get Realm instance, then check to see if the entered name has already been taken.
                    Realm innerRealm = Realm.getDefaultInstance();
                    String newName = input.toString().trim();
                    boolean nameExists = innerRealm.where(RBookList.class)
                                                   .equalTo("name", newName)
                                                   .findFirst() != null;

                    // If the name exists, set the error text on the edit text. If it doesn't, create the new RBookList.
                    if (nameExists) {
                        //noinspection ConstantConditions
                        dialog.getInputEditText().setError(C.getStr(R.string.list_name_exists));
                        innerRealm.close();
                    } else {
                        innerRealm.executeTransaction(tRealm -> tRealm.copyToRealm(new RBookList(newName)));
                        innerRealm.close();
                        dialog.dismiss();
                    }
                })
                .show();
    }
}
