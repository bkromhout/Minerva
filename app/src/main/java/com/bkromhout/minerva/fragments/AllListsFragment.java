package com.bkromhout.minerva.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.*;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.bkromhout.minerva.BookListActivity;
import com.bkromhout.minerva.QueryBuilderActivity;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.adapters.BookListCardAdapter;
import com.bkromhout.minerva.data.ActionHelper;
import com.bkromhout.minerva.events.ActionEvent;
import com.bkromhout.minerva.events.BookListCardClickEvent;
import com.bkromhout.minerva.events.UpdatePosEvent;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.util.Dialogs;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.rrvl.RealmRecyclerView;
import com.bkromhout.ruqus.RealmUserQuery;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

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
     * Instance of Realm.
     */
    private Realm realm;
    /**
     * {@link RBookList}s currently shown in the recycler view.
     */
    private RealmResults<RBookList> lists;
    /**
     * Adapter for the recycler view.
     */
    private RealmBasedRecyclerViewAdapter adapter;
    /**
     * Action mode.
     */
    private ActionMode actionMode;
    /**
     * Temporary storage for a list.
     */
    private RBookList tempList;

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

        // Get Realm.
        realm = Realm.getDefaultInstance();

        initUi();
    }

    /**
     * Initialize the UI.
     */
    private void initUi() {
        // Get lists, then create and bind the adapter.
        lists = realm.where(RBookList.class)
                     .findAllSorted("sortName");
        adapter = new BookListCardAdapter(getActivity(), lists, true, true);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Util.forceMenuIcons(menu, getContext(), getClass().getSimpleName());
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
        // Close adapter.
        if (adapter != null) adapter.close();
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
                BookListActivity.start(getActivity(), event.getListName(), event.getPosition());
                break;
            case LONG:
                // TODO Start multi-select.

                break;
            case ACTIONS:
                // Handle action.
                onCardMenuActionClicked(event.getActionId(), event.getListName(), event.getPosition());
                break;
        }
    }

    /**
     * Called when one of the actions in a card's popup menu is clicked.
     * @param actionId The ID of the popup menu item.
     */
    private void onCardMenuActionClicked(int actionId, String listName, int position) {
        if (actionId < 0 || listName == null) throw new IllegalArgumentException();
        tempList = lists.where().equalTo("name", listName).findFirst();

        // Do something based on the menu item ID.
        switch (actionId) {
            case R.id.action_show_query: {
                Dialogs.smartListQueryDialog(getActivity(),
                        tempList.getSmartListRuqString() == null || tempList.getSmartListRuqString().isEmpty() ? null :
                                new RealmUserQuery(tempList.getSmartListRuqString()).toString(), position);
                break;
            }
            case R.id.action_rename_list: {
                Dialogs.listNameDialog(getActivity(), R.string.title_rename_list, R.string.rename_list_prompt, listName,
                        R.id.action_rename_list, position);
                break;
            }
            case R.id.action_rename_smart_list: {
                Dialogs.listNameDialog(getActivity(), R.string.title_rename_smart_list,
                        R.string.rename_smart_list_prompt, listName, R.id.action_rename_smart_list, position);
                break;
            }
            case R.id.action_edit_smart_list: {
                String ruqString = tempList.getSmartListRuqString();
                QueryBuilderActivity.start(this, ruqString == null || ruqString.isEmpty()
                        ? null : new RealmUserQuery(ruqString));
                break;
            }
            case R.id.action_convert_to_normal_list: {
                Dialogs.simpleYesNoDialog(getActivity(), R.string.title_convert_to_normal_list,
                        R.string.convert_to_normal_list_prompt, R.id.action_convert_to_normal_list);

                break;
            }
            case R.id.action_delete_list: {
                Dialogs.simpleYesNoDialog(getActivity(), R.string.title_delete_list, R.string.delete_list_prompt,
                        R.id.action_delete_list);
                break;
            }
            case R.id.action_delete_smart_list: {
                Dialogs.simpleYesNoDialog(getActivity(), R.string.title_delete_smart_list,
                        R.string.delete_smart_list_prompt, R.id.action_delete_list);
                break;
            }
        }
    }

    /**
     * Called when we wish to take some action.
     * <p>
     * TODO handle multi-select correctly.
     * @param event {@link ActionEvent}.
     */
    @Subscribe
    public void onActionEvent(ActionEvent event) {
        switch (event.getActionId()) {
            case R.id.action_new_list: {
                ActionHelper.createNewList(realm, (String) event.getData());
                break;
            }
            case R.id.action_new_smart_list: {
                BookListActivity.start(getActivity(), (String) event.getData(), new ArrayList<>(lists).indexOf(
                        ActionHelper.createNewSmartList(realm, (String) event.getData(), null)));
                break;
            }
            case R.id.action_open_query_builder: {
                String ruqString = tempList.getSmartListRuqString();
                QueryBuilderActivity.start(this, ruqString == null || ruqString.isEmpty()
                        ? null : new RealmUserQuery(ruqString));
                break;
            }
            case R.id.action_rename_list:
            case R.id.action_rename_smart_list: {
                ActionHelper.renameList(realm, tempList, (String) event.getData());
                if (event.getPosToUpdate() != -1) adapter.notifyItemChanged(event.getPosToUpdate());
                break;
            }
            case R.id.action_convert_to_normal_list: {
                tempList.convertToNormalList();
                if (event.getPosToUpdate() != -1) adapter.notifyItemChanged(event.getPosToUpdate());
                break;
            }
            case R.id.action_delete_list:
            case R.id.action_delete_smart_list: {
                // Delete the list currently being shown, then finish the activity.
                ActionHelper.deleteList(realm, tempList);
                break;
            }
        }
        if (actionMode != null) actionMode.finish();
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
     * Show the new list dialog when the FAB is clicked.
     */
    @OnClick(R.id.fab)
    void onFabClick() {
        Dialogs.listNameDialog(getActivity(), R.string.action_new_list, R.string.new_list_prompt, null,
                R.id.action_new_list, -1);
    }
}
