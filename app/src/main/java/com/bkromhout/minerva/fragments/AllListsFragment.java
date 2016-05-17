package com.bkromhout.minerva.fragments;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.activities.BookListActivity;
import com.bkromhout.minerva.activities.QueryBuilderActivity;
import com.bkromhout.minerva.adapters.BookListCardAdapter;
import com.bkromhout.minerva.data.ActionHelper;
import com.bkromhout.minerva.events.ActionEvent;
import com.bkromhout.minerva.events.BookListCardClickEvent;
import com.bkromhout.minerva.events.UpdatePosEvent;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.util.Dialogs;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.rrvl.FastScrollHandleStateListener;
import com.bkromhout.rrvl.FastScrollerHandleState;
import com.bkromhout.rrvl.RealmRecyclerView;
import com.bkromhout.rrvl.RealmRecyclerViewAdapter;
import com.bkromhout.ruqus.RealmUserQuery;
import io.realm.Realm;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

/**
 * Fragment in charge of showing all of the book lists.
 */
public class AllListsFragment extends Fragment implements ActionMode.Callback, FastScrollHandleStateListener,
        SnackKiosk.Snacker {
    // Views.
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;
    @BindView(R.id.fab)
    FloatingActionButton fabNewList;
    @BindView(R.id.fab2)
    FloatingActionButton fabNewSmartList;
    @BindView(R.id.mask_view)
    View maskView;
    @BindView(R.id.recycler)
    RealmRecyclerView recyclerView;

    /**
     * How far to translate the mini FAB in the Y direction in order to show/hide it.
     */
    private static int miniFabOffset = -1;
    /**
     * The duration of the animation to show/hide the mini FAB.
     */
    private static long miniFabAnimDuration = -1;

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
    private RealmRecyclerViewAdapter adapter;
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

        // Make sure our static vars are filled in.
        if (miniFabOffset == -1) miniFabOffset = getResources().getDimensionPixelOffset(R.dimen.mini_fab_translate);
        if (miniFabAnimDuration == -1) miniFabAnimDuration = getResources().getInteger(R.integer.fab_anim_duration);

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
     * Initialize the UI.
     */
    private void initUi() {
        // Get lists, then create and bind the adapter.
        lists = realm.where(RBookList.class)
                     .findAllSorted("sortName");
        adapter = new BookListCardAdapter(getActivity(), lists);
        recyclerView.setFastScrollHandleStateListener(this);
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
        mode.getMenuInflater().inflate(R.menu.all_lists_action_mode, menu);
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
            case R.id.action_delete_lists:
                Dialogs.simpleConfirmDialog(getContext(), R.string.title_delete_lists, R.string.prompt_delete_lists,
                        R.string.action_delete, R.id.action_delete_lists);
                return true;
            default:
                return false;
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
     * @param event {@link BookListCardClickEvent}.
     */
    @Subscribe
    public void onCardClicked(BookListCardClickEvent event) {
        if (actionMode != null) {
            if (event.getType() == BookListCardClickEvent.Type.LONG) adapter.extendSelectionTo(event.getPosition());
            else adapter.toggleSelected(event.getPosition());
            return;
        }
        // Do something based on the click type.
        switch (event.getType()) {
            case NORMAL:
                // Start BookListActivity.
                BookListActivity.start(getActivity(), event.getListName(), event.getPosition());
                break;
            case LONG:
                // Start multi-select.
                adapter.toggleSelected(event.getPosition());
                startActionMode();
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
            case R.id.action_show_query:
                Dialogs.queryDialog(getActivity(), R.string.title_smart_list_query, R.string.no_query_for_smart_list,
                        tempList.smartListRuqString != null && !tempList.smartListRuqString.isEmpty() ?
                                new RealmUserQuery(tempList.smartListRuqString).toString() : null, true, position);
                break;
            case R.id.action_rename_list:
                Dialogs.uniqueNameDialog(getActivity(), RBookList.class, R.string.title_rename_list,
                        R.string.prompt_rename_list, R.string.list_name_hint, listName, R.id.action_rename_list,
                        position);
                break;
            case R.id.action_rename_smart_list:
                Dialogs.uniqueNameDialog(getActivity(), RBookList.class, R.string.title_rename_smart_list,
                        R.string.prompt_rename_smart_list, R.string.list_name_hint, listName,
                        R.id.action_rename_smart_list, position
                );
                break;
            case R.id.action_edit_smart_list:
                String ruqString = tempList.smartListRuqString;
                QueryBuilderActivity.start(this, ruqString == null || ruqString.isEmpty()
                        ? null : new RealmUserQuery(ruqString));
                break;
            case R.id.action_convert_to_normal_list:
                Dialogs.simpleConfirmDialog(getActivity(), R.string.title_convert_to_normal_list,
                        R.string.prompt_convert_to_normal_list, R.string.action_convert,
                        R.id.action_convert_to_normal_list);

                break;
            case R.id.action_delete_list:
                Dialogs.simpleConfirmDialog(getActivity(), R.string.title_delete_list, R.string.prompt_delete_list,
                        R.string.action_delete, R.id.action_delete_list);
                break;
            case R.id.action_delete_smart_list:
                Dialogs.simpleConfirmDialog(getActivity(), R.string.title_delete_smart_list,
                        R.string.prompt_delete_smart_list, R.string.action_delete, R.id.action_delete_list);
                break;
        }
    }

    /**
     * Called when we wish to take some action.
     * @param event {@link ActionEvent}.
     */
    @Subscribe
    public void onActionEvent(ActionEvent event) {
        switch (event.getActionId()) {
            case R.id.action_new_list:
                ActionHelper.createNewList(realm, (String) event.getData());
                break;
            case R.id.action_new_smart_list:
                BookListActivity.start(getActivity(), (String) event.getData(), new ArrayList<>(lists).indexOf(
                        ActionHelper.createNewSmartList(realm, (String) event.getData(), null)));
                break;
            case R.id.action_open_query_builder:
                String ruqString = tempList.smartListRuqString;
                QueryBuilderActivity.start(this, ruqString == null || ruqString.isEmpty()
                        ? null : new RealmUserQuery(ruqString));
                break;
            case R.id.action_rename_list:
            case R.id.action_rename_smart_list:
                ActionHelper.renameList(realm, tempList, (String) event.getData());
                if (event.getPosToUpdate() != -1) adapter.notifyItemChanged(event.getPosToUpdate());
                break;
            case R.id.action_convert_to_normal_list:
                tempList.convertToNormalList();
                if (event.getPosToUpdate() != -1) adapter.notifyItemChanged(event.getPosToUpdate());
                break;
            case R.id.action_delete_list:
            case R.id.action_delete_smart_list:
                // Delete the list currently being shown, then finish the activity.
                ActionHelper.deleteList(realm, tempList);
                break;
            case R.id.action_delete_lists:
                //noinspection unchecked
                ActionHelper.deleteLists(realm, adapter.getSelectedRealmObjects());
                break;
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
        // If the event's position is ALL_POSITIONS, indicate the whole dataset changed. Otherwise, update the item
        // at the position in the event.
        if (event.getPosition() == UpdatePosEvent.ALL_POSITIONS) adapter.notifyDataSetChanged();
        else adapter.notifyItemChanged(event.getPosition());
    }

    /**
     * Show the new list dialog when the FAB is clicked.
     */
    @OnClick(R.id.fab)
    void onFabNewListClick() {
        if (fabNewList.isActivated()) Dialogs.uniqueNameDialog(getActivity(), RBookList.class, R.string.action_new_list,
                R.string.prompt_new_list, R.string.list_name_hint, null, R.id.action_new_list, -1);
        activateFabs(!fabNewList.isActivated());
    }

    /**
     * Show the new smart list dialog when the mini FAB is clicked.
     */
    @OnClick(R.id.fab2)
    void onFabNewSmartListClick() {
        // Collapse the mini FAB and change the main FAB's icon back to a plus, then show the new smart list dialog.
        Dialogs.uniqueNameDialog(getActivity(), RBookList.class, R.string.action_new_smart_list,
                R.string.prompt_new_smart_list,
                R.string.list_name_hint, null,
                R.id.action_new_smart_list, -1);
        activateFabs(false);
    }

    /**
     * Deactivate FABs when mask is clicked.
     */
    @OnClick(R.id.mask_view)
    void onMaskViewClick() {
        activateFabs(false);
    }

    private void activateFabs(boolean activate) {
        maskView.setVisibility(activate ? View.VISIBLE : View.GONE);
        ObjectAnimator.ofFloat(maskView, "alpha", activate ? 0f : 1f, activate ? 1f : 0f)
                      .setDuration(miniFabAnimDuration).start();
        fabNewList.setActivated(activate);
        // Get the animation which will be used to expand/collapse the mini FAB, then start it.
        ObjectAnimator.ofFloat(fabNewSmartList, "translationY", activate ? 0f : miniFabOffset,
                activate ? miniFabOffset : 0f).setDuration(miniFabAnimDuration).start();
    }

    @Override
    public void onHandleStateChanged(FastScrollerHandleState newState) {
        if (newState == FastScrollerHandleState.PRESSED) {
            fabNewSmartList.hide();
            fabNewList.hide();
        }
    }

    @NonNull
    @Override
    public View getSnackbarAnchorView() {
        return coordinator;
    }

    @Override
    public Activity getCtx() {
        return getActivity();
    }
}
