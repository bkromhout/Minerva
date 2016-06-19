package com.bkromhout.minerva.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.Prefs;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.adapters.*;
import com.bkromhout.minerva.data.ActionHelper;
import com.bkromhout.minerva.data.DataUtils;
import com.bkromhout.minerva.enums.BookCardType;
import com.bkromhout.minerva.enums.MainFrag;
import com.bkromhout.minerva.enums.MarkType;
import com.bkromhout.minerva.enums.ModelType;
import com.bkromhout.minerva.events.*;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.realm.RBookListItem;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.ui.UiUtils;
import com.bkromhout.minerva.ui.transitions.CircularReveal;
import com.bkromhout.minerva.util.Dialogs;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.rrvl.RealmRecyclerView;
import com.bkromhout.ruqus.RealmUserQuery;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Collections;
import java.util.List;

/**
 * Activity which displays a list of books based on an {@link RBookList}.
 */
public class BookListActivity extends PermCheckingActivity implements ActionMode.Callback, SnackKiosk.Snacker {
    // Key strings for the bundle passed when this activity is started.
    private static final String LIST_NAME = "list_name";
    private static final String CENTER_X = "center_x";
    private static final String CENTER_Y = "center_y";
    private static final String KEY_IS_REORDER_MODE = "is_reorder_mode";

    // Views.
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;
    @BindView(R.id.recycler)
    RealmRecyclerView recyclerView;
    @BindView(R.id.smart_list_empty)
    LinearLayout emptySmartList;
    @BindView(R.id.transition_fg)
    View fg;

    /**
     * Position to use in any {@link UpdatePosEvent}s which might be sent.
     */
    private int posToUpdate;
    /**
     * Whether or not to skip the circular reveal usually done when first opening the activity. This is necessary if we
     * know we're going to immediately open the {@link QueryBuilderActivity}.
     */
    private boolean skipReveal = false;
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
    private ModelType modelType = ModelType.BOOK_LIST_ITEM;
    /**
     * The list of {@link RBookListItem}s being shown.
     */
    private RealmResults<? extends RealmObject> items;
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

    /**
     * Start the {@link BookListActivity} for the {@link RBookList} with the given {@code uniqueId}.
     * @param activity  Context to use to start the activity.
     * @param uniqueId  Unique ID which will be used to get the {@link RBookList}.
     * @param updatePos Position which should be updated when the activity closes.
     * @param centerX   X location to to as center for circular reveal. Pass {@code -1} to use center of activity.
     * @param centerY   Y location to to as center for circular reveal. Pass {@code -1} to use center of activity.
     */
    public static void start(Activity activity, long uniqueId, int updatePos, int centerX, int centerY) {
        if (uniqueId < 0) throw new IllegalArgumentException("Must supply non-negative unique ID.");

        Intent intent = new Intent(activity, BookListActivity.class).putExtra(C.UNIQUE_ID, uniqueId)
                                                                    .putExtra(C.POS_TO_UPDATE, updatePos);
        if (centerX != -1) intent.putExtra(CENTER_X, centerX);
        if (centerY != -1) intent.putExtra(CENTER_Y, centerY);

        // Yes, we do indeed pass nothing but the activity here. Why? Because the enter transition literally refuses
        // to play otherwise, that's why. Perhaps in the future we can figure out a way around that.
        //noinspection unchecked
        ActivityOptions emptyOptions = ActivityOptions.makeSceneTransitionAnimation(activity);
        activity.startActivity(intent, emptyOptions.toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);
        ButterKnife.bind(this);
        posToUpdate = getIntent().getIntExtra(C.POS_TO_UPDATE, -1);
        readPrefs();

        // Set up toolbar.
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get Realm, then get the RBookList which we will get items from.
        realm = Realm.getDefaultInstance();
        srcList = realm.where(RBookList.class)
                       .equalTo("uniqueId", getIntent().getLongExtra(C.UNIQUE_ID, -1))
                       .findFirst();

        // Set title, then check to see if we have a RUQ from the savedInstanceState.
        setTitle(srcList.name);
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

        // Set up the window enter and exit transitions.
        setupTransitions(savedInstanceState == null && !skipReveal);

        // Handle permissions. Make sure we continue a request process if applicable.
        initAndContinuePermChecksIfNeeded();
    }

    /**
     * Set up transitions for this activity.
     * @param doEnterTrans If false, we'll skip the content enter transition. True allows it to happen.
     */
    private void setupTransitions(boolean doEnterTrans) {
        // We want to set a specific center point for our circular reveal transition.
        int centerX = getIntent().getIntExtra(CENTER_X, coordinator.getWidth() / 2);
        int centerY = getIntent().getIntExtra(CENTER_Y, coordinator.getHeight() / 2);

        if (doEnterTrans) {
            // If we're going to do the enter transition, set it up a bit.
            CircularReveal enterTrans = (CircularReveal) getWindow().getEnterTransition();
            enterTrans.setCenter(centerX, centerY);

            // Ensure that we're fading out the foreground overlay view as we do the reveal when entering.
            enterTrans.addListener(new UiUtils.TransitionListenerAdapter() {
                @Override
                public void onTransitionStart(Transition transition) {
                    super.onTransitionStart(transition);
                    fg.animate()
                      .alpha(0f)
                      .setDuration(300)
                      .setInterpolator(AnimationUtils.loadInterpolator(
                              BookListActivity.this, android.R.interpolator.accelerate_cubic))
                      .setListener(new AnimatorListenerAdapter() {
                          @Override
                          public void onAnimationEnd(Animator animation) {
                              super.onAnimationEnd(animation);
                              fg.setAlpha(0f);
                          }
                      })
                      .start();
                }
            });
        } else {
            // If we're going to skip the enter transition, tell the system that, then hide the foreground overlay.
            getWindow().setEnterTransition(null);
            fg.setAlpha(0f);
        }

        // Set up the return transition.
        CircularReveal returnTrans = (CircularReveal) getWindow().getReturnTransition();
        returnTrans.setCenter(centerX, centerY);

        // Ensure that we fading in the foreground overlay view as we do the "reversed reveal" when returning.
        returnTrans.addListener(new UiUtils.TransitionListenerAdapter() {
            @Override
            public void onTransitionStart(Transition transition) {
                super.onTransitionStart(transition);
                fg.animate()
                  .alpha(1f)
                  .setDuration(200)
                  .setInterpolator(AnimationUtils.loadInterpolator(
                          BookListActivity.this, android.R.interpolator.decelerate_cubic))
                  .setListener(new AnimatorListenerAdapter() {
                      @Override
                      public void onAnimationEnd(Animator animation) {
                          super.onAnimationEnd(animation);
                          fg.setAlpha(1f);
                      }
                  })
                  .start();
            }
        });
    }

    /**
     * Read preferences into variables.
     */
    private void readPrefs() {
        cardType = Minerva.prefs().getListCardType(BookCardType.NORMAL);
    }

    /**
     * Init the UI.
     */
    private void updateUi() {
        emptySmartList.setVisibility(View.GONE);
        // If we already have a RealmUserQuery, just use that right away.
        if (smartListRuq != null) {
            // Use RUQ to set up UI.
            modelType = ModelType.fromRealmClass(smartListRuq.getQueryClass());
            items = smartListRuq.execute(realm);
            adapter = makeAdapter();
            recyclerView.setAdapter(adapter);
            return;
        }
        // Check first to see if list is a smart list.
        if (srcList.isSmartList) {
            String ruqString = srcList.smartListRuqString;
            // Smart list; check to see if we need to set it up.
            if (ruqString != null && !ruqString.isEmpty()) {
                // Smart list already has a non-empty RUQ string, create a RUQ and then set up the UI using it.
                smartListRuq = new RealmUserQuery(srcList.smartListRuqString);
                updateUi();
            } else if (ruqString == null) {
                // We need to set up the smart list first. Open the query builder, and make sure we don't animate in
                // this activity.
                skipReveal = true;
                QueryBuilderActivity.start(this, null);
            } else { // ruqString.isEmpty() == true here.
                //User has already been to the query builder once, it's up to them now, show a message saying that.
                emptySmartList.setVisibility(View.VISIBLE);
            }
        } else {
            // Normal list.
            items = srcList.listItems.where().findAllSorted("pos");
            modelType = ModelType.BOOK_LIST_ITEM;
            adapter = makeAdapter();
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Util.forceMenuIcons(menu, getClass().getSimpleName());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(srcList.isSmartList ? R.menu.book_list_smart : R.menu.book_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SnackKiosk.startSnacking(this);
    }

    @Override
    protected void onPause() {
        SnackKiosk.stopSnacking();
        super.onPause();
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
        if (needsPosUpdate) EventBus.getDefault().postSticky(new UpdatePosEvent(posToUpdate));
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
        // Change status bar color to be dark to correspond to dark toolbar color.
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.grey900));
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Util.forceMenuIcons(menu, getClass().getSimpleName());
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // Set status bar color back to normal.
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
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
                Dialogs.queryDialog(this, R.string.title_smart_list_query, R.string.no_query_for_smart_list,
                        smartListRuq != null ? smartListRuq.toString() : null, true, -1);
                return true;
            case R.id.action_card_type:
                Dialogs.cardStyleDialog(this, MainFrag.ALL_LISTS);
                return true;
            case R.id.action_rename_list:
                Dialogs.uniqueNameDialog(this, RBookList.class, R.string.title_rename_list, R.string.prompt_rename_list,
                        R.string.list_name_hint, srcList.name, R.id.action_rename_list, posToUpdate);
                return true;
            case R.id.action_rename_smart_list:
                Dialogs.uniqueNameDialog(this, RBookList.class, R.string.title_rename_smart_list,
                        R.string.prompt_rename_smart_list, R.string.list_name_hint, srcList.name,
                        R.id.action_rename_smart_list, posToUpdate);
                return true;
            case R.id.action_edit_smart_list:
                QueryBuilderActivity.start(this, smartListRuq);
                return true;
            case R.id.action_clear:
                Dialogs.simpleConfirmDialog(this, R.string.action_clear_list, R.string.prompt_clear_list,
                        R.string.action_clear, R.id.action_clear);
                return true;
            case R.id.action_convert_to_normal_list:
                Dialogs.simpleConfirmDialog(this, R.string.title_convert_to_normal_list,
                        R.string.prompt_convert_to_normal_list, R.string.action_convert,
                        R.id.action_convert_to_normal_list);
                return true;
            case R.id.action_delete_list:
                Dialogs.simpleConfirmDialog(this, R.string.title_delete_list, R.string.prompt_delete_list,
                        R.string.action_delete, R.id.action_delete_list);
                return true;
            case R.id.action_delete_smart_list:
                Dialogs.simpleConfirmDialog(this, R.string.title_delete_smart_list, R.string.prompt_delete_smart_list,
                        R.string.action_delete, R.id.action_delete_smart_list);
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
                int initialRating = adapter.getSelectedItemCount() == 1 ? getSelectedBooks().get(0).rating : 0;
                Dialogs.ratingDialog(this, initialRating);
                return true;
            case R.id.action_mark_as:
                Dialogs.markAsDialog(this);
                return true;
            case R.id.action_move_to_top:
                //noinspection unchecked
                ActionHelper.moveItemsToStart(srcList, adapter.getSelectedRealmObjects());
                actionMode.finish();
                return true;
            case R.id.action_move_to_bottom:
                //noinspection unchecked
                ActionHelper.moveItemsToEnd(srcList, adapter.getSelectedRealmObjects());
                actionMode.finish();
                return true;
            case R.id.action_re_import:
                Dialogs.simpleConfirmDialog(this, R.string.title_re_import_books, R.string.prompt_re_import_books,
                        R.string.action_re_import, R.id.action_re_import);
                return true;
            case R.id.action_remove:
                Dialogs.simpleConfirmDialog(this, R.string.title_remove_books, R.string.prompt_remove_from_list,
                        R.string.action_remove, R.id.action_remove);
                return true;
            case R.id.action_delete:
                Dialogs.confirmCheckBoxDialog(this, R.string.title_delete_books, R.string.prompt_delete_books,
                        R.string.prompt_delete_from_device_too, R.string.info_delete_from_device_permanent,
                        R.string.action_delete, R.id.action_delete);
            default:
                return false;
        }
    }

    /**
     * Called when we wish to take some action.
     * @param event {@link ActionEvent}.
     */
    @Subscribe(priority = 1)
    public void onActionEvent(ActionEvent event) {
        switch (event.getActionId()) {
            case R.id.action_clear:
                realm.executeTransaction(tRealm -> {
                    // Remove all items from the list, then reset its nextPos value back to 0.
                    srcList.listItems.deleteAllFromRealm();
                    srcList.nextPos = 0L;
                });
                break;
            case R.id.action_open_query_builder:
                QueryBuilderActivity.start(this, smartListRuq);
                break;
            case R.id.action_rename_list:
            case R.id.action_rename_smart_list:
                ActionHelper.renameList(realm, srcList, (String) event.getData());
                setTitle((String) event.getData());
                // Update intent used to start activity so that we don't crash if we rotate or something.
                getIntent().putExtra(LIST_NAME, (String) event.getData());
                needsPosUpdate = true;
                break;
            case R.id.action_convert_to_normal_list:
                if (smartListRuq != null) {
                    srcList.convertToNormalListUsingRuq(realm, smartListRuq);
                    smartListRuq = null;
                    // Refresh options menu, then update the UI.
                    invalidateOptionsMenu();
                    updateUi();
                    needsPosUpdate = true;
                }
                break;
            case R.id.action_delete_list:
            case R.id.action_delete_smart_list:
                // Delete the list currently being shown, then finish the activity.
                ActionHelper.deleteLists(realm, Collections.singletonList(srcList));
                finishAfterTransition();
                break;
            case R.id.action_rate:
                ActionHelper.rateBooks(realm, getSelectedBooks(), (Integer) event.getData());
                break;
            case R.id.action_mark_as:
                int whichMark = (int) event.getData();
                ActionHelper.markBooks(getSelectedBooks(), whichMark < 2 ? MarkType.NEW : MarkType.UPDATED,
                        whichMark % 2 == 0);
                break;
            case R.id.action_add_to_list:
                // TODO actually implement a move/copy to other lists feature???
                //RBookList list = realm.where(RBookList.class).equalTo("name", (String) event.getData()).findFirst();
                //RBookList.addBooks(list, selectedItems);
                break;
            case R.id.action_re_import:
                ActionHelper.reImportBooks(getSelectedBooks());
                break;
            case R.id.action_remove:
                srcList.removeBooks(realm, getSelectedBooks());
                break;
            case R.id.action_delete:
                ActionHelper.deleteBooks(getSelectedBooks(), (boolean) event.getData());
                break;
        }
        // Ensure that AllListsFragment doesn't see the event, which is otherwise would since we're a "transparent"
        // activity, meaning that AllListsFragment is still running behind us and would otherwise also receive the
        // event. This is also why we subscribe with a higher priority.
        EventBus.getDefault().cancelEventDelivery(event);
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
            case C.RC_QUERY_BUILDER_ACTIVITY:
                // Came back from QueryBuilderActivity.
                if (resultCode == RESULT_OK) {
                    // There's a valid RUQ in the extras.
                    updateRuq(data.getParcelableExtra(C.RUQ));
                    // If it's different than what we started with, we'll want to update the card in AllListsFragment.
                    if (data.getBooleanExtra(C.HAS_CHANGED, false)) needsPosUpdate = true;
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

    /**
     * Called when a permission has been granted.
     * @param event {@link PermGrantedEvent}.
     */
    @Subscribe
    public void onPermGrantedEvent(PermGrantedEvent event) {
        if (event.getActionId() == R.id.action_execute_deferred) ActionHelper.doDeferredAction();
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
        if (modelType == ModelType.BOOK)
            return adapter.getSelectedRealmObjects();
        else if (modelType == ModelType.BOOK_LIST_ITEM)
            return DataUtils.booksFromBookListItems(adapter.getSelectedRealmObjects());
        else throw new IllegalArgumentException("Invalid type.");
    }

    /**
     * Make adapter based on {@link RealmUserQuery#getQueryClass() ruq#getQueryClass()} and {@link #cardType}.
     * @return Adapter.
     */
    @SuppressWarnings("unchecked")
    private BaseBookCardAdapter makeAdapter() {
        if (modelType == ModelType.BOOK) {
            switch (cardType) {
                case NORMAL:
                    return new BookCardAdapter(this, (RealmResults<RBook>) items, false);
                case NO_COVER:
                    return new BookCardNoCoverAdapter(this, (RealmResults<RBook>) items, false);
                case COMPACT:
                    return new BookCardCompactAdapter(this, (RealmResults<RBook>) items, false);
                default:
                    throw new IllegalStateException("Invalid card type.");
            }
        } else if (modelType == ModelType.BOOK_LIST_ITEM) {
            switch (cardType) {
                case NORMAL:
                    return new BookItemCardAdapter(this, (RealmResults<RBookListItem>) items, false);
                case NO_COVER:
                    return new BookItemCardNoCoverAdapter(this, (RealmResults<RBookListItem>) items, false);
                case COMPACT:
                    return new BookItemCardCompactAdapter(this, (RealmResults<RBookListItem>) items, false);
                default:
                    throw new IllegalStateException("Invalid card type.");
            }
        } else throw new IllegalStateException("Invalid adapter type.");
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
     * Called when one of the cards is clicked.
     * @param event {@link BookCardClickEvent}.
     */
    @Subscribe
    public void onCardClicked(BookCardClickEvent event) {
        if (isReorderMode) return;
        // Get the associated RBook.
        RBook book;
        if (modelType == ModelType.BOOK)
            book = realm.where(RBook.class).equalTo("relPath", event.getRelPath()).findFirst();
        else if (modelType == ModelType.BOOK_LIST_ITEM)
            book = realm.where(RBookListItem.class).equalTo("book.relPath", event.getRelPath()).findFirst().book;
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
     * React to a changed preference.
     * @param event {@link PrefChangeEvent}.
     */
    @Subscribe
    public void onPrefChangeEvent(PrefChangeEvent event) {
        // Do something different based on name of changed preference.
        switch (event.getPrefName()) {
            case Prefs.LIST_CARD_TYPE:
                cardType = Minerva.prefs().getListCardType(cardType);
                changeCardType();
                break;
        }
    }

    /**
     * When called, update the item at the position carried in the event.
     * @param event {@link UpdatePosEvent}.
     */
    @Subscribe(sticky = true, priority = 1)
    public void onUpdatePosEvent(UpdatePosEvent event) {
        // Remove the sticky event, which also ensures that AllListsFragment won't see it.
        EventBus.getDefault().removeStickyEvent(event);
        // If the event's position is ALL_POSITIONS, indicate the whole dataset changed. Otherwise, update the item
        // at the position in the event.
        if (event.getPosition() == UpdatePosEvent.ALL_POSITIONS) adapter.notifyDataSetChanged();
        else adapter.notifyItemChanged(event.getPosition());
    }

    /**
     * Open the query builder when the button shown for an empty smart list is clicked.
     */
    @OnClick(R.id.open_query_builder)
    void onOpenQueryBuilderClicked() {
        QueryBuilderActivity.start(this, null);
    }

    @NonNull
    @Override
    public View getSnackbarAnchorView() {
        return coordinator;
    }
}
