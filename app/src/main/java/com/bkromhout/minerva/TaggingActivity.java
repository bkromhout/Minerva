package com.bkromhout.minerva;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.bkromhout.minerva.adapters.TagCardAdapter;
import com.bkromhout.minerva.data.ActionHelper;
import com.bkromhout.minerva.events.ActionEvent;
import com.bkromhout.minerva.events.TagCardClickEvent;
import com.bkromhout.minerva.events.UpdatePosEvent;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RTag;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.util.Dialogs;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.rrvl.RealmRecyclerView;
import com.google.common.collect.Lists;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewTextChangeEvent;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import io.realm.*;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Activity used to apply (and remove) tags to (and from) books.
 */
public class TaggingActivity extends AppCompatActivity implements SnackKiosk.Snacker, ColorChooserDialog.ColorCallback {
    // Views.
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;
    @BindView(R.id.tag_filter)
    EditText filter;
    @BindView(R.id.recycler)
    RealmRecyclerView recyclerView;

    /**
     * Instance of {@link TaggingHelper}.
     */
    private TaggingHelper taggingHelper;
    /**
     * Instance of Realm.
     */
    private Realm realm;
    /**
     * The list of {@link RTag}s being shown.
     */
    private RealmResults<RTag> items;
    /**
     * Recycler view adapter.
     */
    private TagCardAdapter adapter;
    /**
     * Filter edit text change subscription.
     */
    private Subscription filterChangeSub;
    /**
     * Temporary storage for a tag.
     */
    private RTag tempTag;

    /**
     * Convenience method to set up the {@link TaggingHelper} then start this activity from a fragment.
     * @param fragment The fragment to return a result to.
     * @param book     The selected {@link RBook}.
     */
    public static void start(Fragment fragment, RBook book) {
        start(fragment, Lists.asList(book, new RBook[] {}));
    }

    /**
     * Convenience method to set up the {@link TaggingHelper} then start this activity from a fragment.
     * @param fragment      The fragment to return a result to.
     * @param selectedBooks The selected {@link RBook}s.
     */
    public static void start(Fragment fragment, List<RBook> selectedBooks) {
        TaggingActivity.TaggingHelper.get().init(selectedBooks);
        fragment.startActivityForResult(new Intent(fragment.getContext(), TaggingActivity.class), C.RC_TAG_ACTIVITY);
    }

    /**
     * Convenience method to set up the {@link TaggingHelper} then start this activity from an activity.
     * @param activity The activity to return a result to.
     * @param book     The selected {@link RBook}.
     */
    public static void start(Activity activity, RBook book) {
        start(activity, Lists.asList(book, new RBook[] {}));
    }

    /**
     * Convenience method to set up the {@link TaggingHelper} then start this activity from an activity.
     * @param activity      The activity to return a result to.
     * @param selectedBooks The selected {@link RBook}s.
     */
    public static void start(Activity activity, List<RBook> selectedBooks) {
        // Initialize the tagging helper.
        TaggingActivity.TaggingHelper.get().init(selectedBooks);
        activity.startActivityForResult(new Intent(activity, TaggingActivity.class), C.RC_TAG_ACTIVITY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set result as canceled.
        setResult(RESULT_CANCELED);

        // Create and bind views.
        setContentView(R.layout.activity_tagging);
        ButterKnife.bind(this);

        // Set up toolbar.
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setHomeAsUpIndicator(Util.getTintedDrawable(this, R.drawable.ic_close,
                R.color.textColorPrimary));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get TaggingHelper, Realm, then set up the rest of UI.
        taggingHelper = TaggingHelper.get();
        realm = Realm.getDefaultInstance();
        initUi();
    }

    /**
     * Init the UI.
     */
    private void initUi() {
        // Set up filter edit text to have a debounce before it filters the list.
        filterChangeSub = RxTextView.textChangeEvents(filter)
                                    .skip(1)
                                    .debounce(500, TimeUnit.MILLISECONDS)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(getFilterObserver());

        // Set up recycler view.
        if (taggingHelper.filter.isEmpty()) items = realm.where(RTag.class).findAllSorted("sortName", Sort.ASCENDING);
        else applyFilter(taggingHelper.filter);
        adapter = new TagCardAdapter(this, items);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Util.forceMenuIcons(menu, this, getClass().getSimpleName());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tagging, menu);
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
        // Unsubscribe from filter edit text.
        if (filterChangeSub != null && !filterChangeSub.isUnsubscribed()) filterChangeSub.unsubscribe();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_save_tags:
                saveTags();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Called when one of the action buttons is clicked on a tag card.
     * @param event {@link TagCardClickEvent}.
     */
    @Subscribe
    public void onTagCardAction(TagCardClickEvent event) {
        // Store tag.
        tempTag = items.where()
                       .equalTo("name", event.getName())
                       .findFirst();
        // Open some dialog.
        switch (event.getType()) {
            case TEXT_COLOR:
                new ColorChooserDialog.Builder(this, R.string.title_tag_text_color)
                        .preselect(tempTag.textColor)
                        .dynamicButtonColor(false)
                        .show();
                break;
            case BG_COLOR:
                new ColorChooserDialog.Builder(this, R.string.title_tag_bg_color)
                        .preselect(tempTag.bgColor)
                        .dynamicButtonColor(false)
                        .show();
                break;
            case ACTIONS:
                onCardMenuActionClicked(event.getActionId(), event.getName());
                break;
        }
    }

    /**
     * Called when one of the menu items on the tag card's action menu is clicked.
     * @param actionId ID of the clicked action.
     * @param tagName  Name of the clicked tag.
     */
    private void onCardMenuActionClicked(int actionId, String tagName) {
        switch (actionId) {
            case R.id.action_rename_tag:
                // Show rename dialog.
                Dialogs.uniqueNameDialog(this, RTag.class, R.string.title_rename_tag, R.string.prompt_rename_tag,
                        R.string.tag_name_hint, tagName, R.id.action_rename_tag, -1);
                break;
            case R.id.action_delete_tag:
                // Show delete confirm dialog.
                Dialogs.simpleConfirmDialog(this, R.string.title_delete_tag,
                        C.getStr(R.string.prompt_delete_tag, tagName), R.string.action_delete,
                        R.id.action_delete_tag);
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
            case R.id.action_new_tag:
                ActionHelper.createNewTag(realm, (String) event.getData());
                break;
            case R.id.action_rename_tag:
                ActionHelper.renameTag(realm, tempTag, (String) event.getData());
                break;
            case R.id.action_delete_tag:
                ActionHelper.deleteTag(realm, tempTag);
                break;
        }
    }

    /**
     * Set the new text or background color for {@link #tempTag}.
     * @param dialog        Dialog whose title string's resource ID will determine if we're setting the text or
     *                      background color.
     * @param selectedColor New text or background color.
     */
    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int selectedColor) {
        if (dialog.getTitle() == R.string.title_tag_text_color) // Text color dialog.
            ActionHelper.setTagTextColor(realm, tempTag, selectedColor);
        else // Background color dialog.
            ActionHelper.setTagBgColor(realm, tempTag, selectedColor);
        // Notify the adapter that it needs to re-draw the tag's card.
        adapter.notifyItemChanged(items.indexOf(tempTag));
        // Indicate that we might need an explicit update.
        taggingHelper.markForExplicitUpdateIfNecessary();
    }

    /**
     * Get the observer to use to handle filter text changes.
     * @return Filter text change observer
     */
    private Observer<TextViewTextChangeEvent> getFilterObserver() {
        return new Observer<TextViewTextChangeEvent>() {
            @Override
            public void onCompleted() {
                // Nothing.
            }

            @Override
            public void onError(Throwable e) {
                // Shouldn't happen, but we'll log it if it does.
                Timber.e("Some error occurred due to the filter observer!");
                e.printStackTrace();
            }

            @Override
            public void onNext(TextViewTextChangeEvent textViewTextChangeEvent) {
                // Apply filter to items and then update adapter's copy of items.
                applyFilter(textViewTextChangeEvent.text().toString());
                adapter.updateRealmResults(items);
            }
        };
    }

    /**
     * Show the new tag dialog when the FAB is clicked.
     */
    @OnClick(R.id.fab)
    void onFabNewTagClicked() {
        Dialogs.uniqueNameDialog(this, RTag.class, R.string.action_new_tag, R.string.prompt_new_tag,
                R.string.tag_name_hint, null, R.id.action_new_tag, -1);
    }

    @Override
    public void onBackPressed() {
        // Reset the tagging helper when we back out of the activity.
        taggingHelper = null;
        TaggingHelper.reset();
        super.onBackPressed();
    }

    /**
     * Called when the save button is clicked.
     */
    private void saveTags() {
        // Use the two checked lists to get deltas, then use the deltas to figure out which tags were added and which
        // were removed.
        Patch<String> patch = DiffUtils.diff(taggingHelper.oldCheckedItems, taggingHelper.newCheckedItems);
        List<String> removedTagNames = getDeltaLines(Delta.TYPE.DELETE, patch.getDeltas());
        List<String> addedTagNames = getDeltaLines(Delta.TYPE.INSERT, patch.getDeltas());

        // Now diff the partially checked items.
        patch = DiffUtils.diff(taggingHelper.oldPartiallyCheckedItems, taggingHelper.newPartiallyCheckedItems);
        // Add any removed partial checks to removedTagNames, but then remove any which are in addedTagNames (since
        // we may have moved from partial->unchecked->checked).
        removedTagNames.addAll(getDeltaLines(Delta.TYPE.DELETE, patch.getDeltas()));
        removedTagNames.removeAll(addedTagNames);

        boolean removedAny = !removedTagNames.isEmpty(), addedAny = !addedTagNames.isEmpty();
        // Remove and add the applicable tags.
        if (removedAny)
            RTag.removeTagsFromBooks(taggingHelper.selectedBooks, RTag.stringListToTagList(removedTagNames, false));
        if (addedAny)
            RTag.addTagsToBooks(taggingHelper.selectedBooks, RTag.stringListToTagList(addedTagNames, true));
        // If we actually removed and/or added any tags, indicate that we may need an explicit update.
        if (removedAny || addedAny) taggingHelper.markForExplicitUpdateIfNecessary();

        // Reset the TaggingHelper and finish this activity.
        taggingHelper = null;
        TaggingHelper.reset();
        setResult(RESULT_OK);
        finish();
    }

    /**
     * Take a list of Deltas and return a list of lines which are within deltas of the specified {@code type}.
     * @param deltaType Type of Delta to pull lines from.
     * @param deltas    List of deltas.
     * @return List of strings.
     */
    private List<String> getDeltaLines(Delta.TYPE deltaType, List<Delta<String>> deltas) {
        ArrayList<String> lines = new ArrayList<>();
        // Loop through all of the deltas.
        for (Delta delta : deltas) {
            // For all deltas which are of the specified type or are CHANGE deltas, add their lines to the list.
            if (delta.getType().equals(deltaType) || delta.getType() == Delta.TYPE.CHANGE) {
                // Have to look in different places based on the type of delta.
                if (deltaType == Delta.TYPE.DELETE) //noinspection unchecked
                    lines.addAll(delta.getOriginal().getLines());
                else if (deltaType == Delta.TYPE.INSERT) //noinspection unchecked
                    lines.addAll(delta.getRevised().getLines());
            }
        }
        return lines;
    }

    /**
     * Applies the current filter string by re-querying Realm for a new {@link #items} list. Does NOT update the
     * adapter.
     */
    private void applyFilter(String filter) {
        taggingHelper.filter = filter;
        items = realm.where(RTag.class)
                     .contains("name", filter, Case.INSENSITIVE)
                     .findAllSorted("sortName");
    }

    @NonNull
    @Override
    public View getSnackbarAnchorView() {
        return coordinator;
    }

    @Override
    public Activity getCtx() {
        return this;
    }

    /**
     * Override this method so that we remove focus from our filter EditText when we click outside of its bounds.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    // Consume this touch event, we don't want to accidentally toggle one of the tag cards.
                    return true;
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * Class to help us hold complex objects necessary for keeping track of tagging state until not needed any longer.
     */
    public static class TaggingHelper {
        /**
         * Instance of tagging helper.
         */
        private static TaggingHelper INSTANCE = null;

        /**
         * List of {@link RBook}s whose will be modified.
         */
        public List<RBook> selectedBooks;
        /**
         * Whether or not we'll require an explicit update if we make changes or tag(s) are deleted. This is set to true
         * if {@link #selectedBooks} is of size 1.
         */
        private boolean willRequireExplicitUpdate;
        /**
         * Whether or not we should actually <i>do</i> an explicit update before our next reset.
         */
        private boolean doExplicitUpdate;
        /**
         * List of originally checked tags (those which all selected books share).
         */
        public List<String> oldCheckedItems;
        /**
         * List of originally partially checked tags (those which some, but not all, selected books have).
         */
        public List<String> oldPartiallyCheckedItems;
        /**
         * List of finally checked tags (those which all selected books share).
         */
        public List<String> newCheckedItems;
        /**
         * List of finally partially checked tags (those which some, but not all, selected books have).
         */
        public List<String> newPartiallyCheckedItems;
        /**
         * Current filter string.
         */
        public String filter = "";

        /**
         * Get the current instance of {@link TaggingHelper}.
         * @return {@link TaggingHelper} instance.
         */
        public static TaggingHelper get() {
            if (INSTANCE == null) reset();
            return INSTANCE;
        }

        /**
         * Resets the current {@link TaggingHelper} instance.
         * <p>
         * If an old instance is present and has indicated it requires an explicit update, will sent an {@link
         * UpdatePosEvent} with the value {@link UpdatePosEvent#ALL_POSITIONS} before resetting.
         */
        private static void reset() {
            // If our instance has indicated it needs an explicit update, send one before resetting.
            if (INSTANCE != null && INSTANCE.doExplicitUpdate)
                EventBus.getDefault().postSticky(new UpdatePosEvent(UpdatePosEvent.ALL_POSITIONS));
            // Recreate instance.
            INSTANCE = new TaggingHelper();
        }

        private TaggingHelper() {
            selectedBooks = null;
            willRequireExplicitUpdate = false;
            doExplicitUpdate = false;
            oldCheckedItems = new ArrayList<>();
            oldPartiallyCheckedItems = new ArrayList<>();
            newCheckedItems = new ArrayList<>();
            newPartiallyCheckedItems = new ArrayList<>();
        }

        /**
         * Convenience method for initializing the {@link TaggingHelper}.
         * @param books List of books.
         */
        private void init(List<RBook> books) {
            this.selectedBooks = books;
            willRequireExplicitUpdate = books.size() == 1;
            calculateSharedTags(books);
            this.newCheckedItems = new ArrayList<>(this.oldCheckedItems);
            this.newPartiallyCheckedItems = new ArrayList<>(this.oldPartiallyCheckedItems);
        }

        /**
         * Indicate that we'll want to send an explicit update at some point if we think it would be necessary.
         */
        public void markForExplicitUpdateIfNecessary() {
            if (willRequireExplicitUpdate) doExplicitUpdate = true;
        }

        /**
         * Processes the given {@code books} and populates {@link #oldCheckedItems} and {@link
         * #oldPartiallyCheckedItems}.
         * @param books A list of {@link RBook}s.
         */
        private void calculateSharedTags(List<RBook> books) {
            if (books == null) throw new IllegalArgumentException("books must not be null.");
            if (books.isEmpty()) return;

            ArrayList<RTag> tagsOnAll = null;
            HashSet<RTag> tagsOnSome = new HashSet<>();

            // Loop through books.
            for (RBook book : books) {
                RealmList<RTag> bookTags = book.tags;

                // Add all of this book's tags to the tagsOnSome HashSet (we won't have duplicates).
                tagsOnSome.addAll(bookTags);

                if (tagsOnAll != null)// Only keep tags which are also in this book's tag list.
                    tagsOnAll.retainAll(book.tags);
                else // If we haven't created the tag list, do that now.
                    tagsOnAll = new ArrayList<>(book.tags);
            }

            if (tagsOnAll != null) {
                // Remove anything from tagsOnSome which is in tagsOnAll.
                tagsOnSome.removeAll(tagsOnAll);
                // Assign names strings from tagsOnAll to oldCheckedItems.
                for (RTag tag : tagsOnAll) oldCheckedItems.add(tag.name);
            }

            // Assign name strings from tagsOnSome to oldPartiallyCheckedItems.
            for (RTag tag : tagsOnSome) oldPartiallyCheckedItems.add(tag.name);
        }
    }
}
