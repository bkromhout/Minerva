package com.bkromhout.minerva;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.ButtonBarLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkromhout.minerva.adapters.TagCardAdapter;
import com.bkromhout.minerva.events.TagCardClickEvent;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RTag;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.realmrecyclerview.RealmRecyclerView;
import com.google.common.collect.Lists;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewTextChangeEvent;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Activity used to apply (and remove) tags to (and from) books.
 */
public class TaggingActivity extends AppCompatActivity implements ActionMode.Callback {
    // Views.
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.recycler)
    RealmRecyclerView recyclerView;
    @Bind(R.id.tag_filter)
    EditText filter;
    @Bind(R.id.buttons)
    ButtonBarLayout buttons;
    @Bind(R.id.cancel)
    Button btnCancel;
    @Bind(R.id.save)
    Button btnSave;

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
        TaggingActivity.TaggingHelper.get().init(selectedBooks,
                RTag.tagListToStringList(RTag.listOfSharedTags(selectedBooks)));
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
        TaggingActivity.TaggingHelper.get().init(selectedBooks,
                RTag.tagListToStringList(RTag.listOfSharedTags(selectedBooks)));
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

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
        if (taggingHelper.filter.isEmpty()) items = realm.allObjectsSorted(RTag.class, "sortName", Sort.ASCENDING);
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
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(R.string.title_tag_edit_mode);
        buttons.setVisibility(View.GONE);
        adapter.setInActionMode(true);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Util.forceMenuIcons(menu, this, getClass().getSimpleName());
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        adapter.setInActionMode(false);
        buttons.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_new_tag:
                new MaterialDialog.Builder(this)
                        .title(R.string.action_new_tag)
                        .content(R.string.new_tag_prompt)
                        .autoDismiss(false)
                        .negativeText(R.string.cancel)
                        .input(R.string.tag_name_hint, 0, false, (dialog, input) -> {
                            String newName = input.toString().trim();
                            try (Realm iRealm = Realm.getDefaultInstance()) {
                                if (iRealm.where(RTag.class).equalTo("name", newName).findFirst() == null) {
                                    // Name is available, create the RTag then dismiss the dialog.
                                    iRealm.executeTransaction(tRealm -> tRealm.copyToRealm(new RTag(newName)));
                                    dialog.dismiss();
                                } else {
                                    //noinspection ConstantConditions
                                    dialog.getInputEditText().setError(C.getStr(R.string.err_name_taken));
                                }
                            }
                        })
                        .show();
                return true;
            case R.id.action_edit_tags:
                startSupportActionMode(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            default:
                return false;
        }
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
                Log.e("TaggingActivity", "Some error occurred due to the filter observer!");
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
     * Called when the cancel button is clicked.
     */
    @OnClick(R.id.cancel)
    void onCancelButtonClicked() {
        finish();
    }

    /**
     * Called when the save button is clicked.
     * <p>
     * TODO find a way to make sure that other items are updated in the RV if needed. Example, rename a tag but not...
     * all of the books which have that tag are selected, so some items may be wrong if they don't get refreshed by RV
     * before the user can see them (i.e., they are not selected but are within the set of already-drawn RV items).
     */
    @OnClick(R.id.save)
    void onSaveButtonClicked() {
        // Use the two lists to get deltas, then use them to figure out which tags were added and which were removed.
        Patch<String> patch = DiffUtils.diff(taggingHelper.oldCheckedItems, taggingHelper.newCheckedItems);
        List<String> removedTagNames = getDeltaLines(Delta.TYPE.DELETE, patch.getDeltas());
        List<String> addedTagNames = getDeltaLines(Delta.TYPE.INSERT, patch.getDeltas());

        // Remove and add the applicable tags.
        RTag.removeTagsFromBooks(taggingHelper.selectedBooks, RTag.stringListToTagList(removedTagNames, false));
        RTag.addTagsToBooks(taggingHelper.selectedBooks, RTag.stringListToTagList(addedTagNames, true));

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
            // For all of the specified type, add their lines to the list.
            if (delta.getType().equals(deltaType)) {
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

    /**
     * Called when one of the action buttons is clicked on a tag card.
     * @param event {@link TagCardClickEvent}.
     */
    @Subscribe
    public void onTagCardAction(TagCardClickEvent event) {
        switch (event.getType()) {
            case RENAME:
                // Show rename dialog.
                new MaterialDialog.Builder(this)
                        .title(R.string.title_rename_tag)
                        .content(R.string.rename_tag_prompt)
                        .autoDismiss(false)
                        .negativeText(R.string.cancel)
                        .input(C.getStr(R.string.tag_name_hint), event.getName(), false, (dialog, input) -> {
                            String newName = input.toString().trim();

                            // If it's the same name, do nothing, just dismiss.
                            if (event.getName().equals(newName)) {
                                dialog.dismiss();
                                return;
                            }

                            // Get Realm and check if name already exists.
                            try (Realm iRealm = Realm.getDefaultInstance()) {
                                if (iRealm.where(RTag.class).equalTo("name", newName).findFirst() == null) {
                                    // Name is available; rename the RTag.
                                    iRealm.executeTransaction(tRealm -> {
                                        RTag rTag = tRealm.where(RTag.class)
                                                          .equalTo("name", event.getName())
                                                          .findFirst();
                                        rTag.setName(newName);
                                        rTag.setSortName(newName.toLowerCase());
                                    });

                                    // Now make sure that we swap the old name for the new one in the old/new lists.
                                    TaggingHelper th = TaggingHelper.get();
                                    if (th.oldCheckedItems.remove(event.getName())) th.oldCheckedItems.add(newName);
                                    if (th.newCheckedItems.remove(event.getName())) th.newCheckedItems.add(newName);

                                    dialog.dismiss();
                                } else {
                                    // Name already exists, set the error text and _don't_ dismiss the dialog.
                                    //noinspection ConstantConditions
                                    dialog.getInputEditText().setError(C.getStr(R.string.err_name_taken));
                                }
                            }
                        })
                        .show();
                break;
            case DELETE:
                // Show delete confirm dialog.
                new MaterialDialog.Builder(this)
                        .title(R.string.title_delete_tag)
                        .content(C.getStr(R.string.delete_tag_prompt, event.getName()))
                        .positiveText(R.string.yes)
                        .negativeText(R.string.no)
                        .onPositive((dialog, which) -> {
                            // Get Realm instance, then delete the list.
                            // TODO do we need to manually loop through the tagged books and remove the tag first???
                            Realm.getDefaultInstance()
                                 .executeTransaction(tRealm -> tRealm
                                         .where(RTag.class)
                                         .equalTo("name", event.getName())
                                         .findFirst()
                                         .removeFromRealm());

                            // Remove tag name from the lists (if present).
                            TaggingHelper th = TaggingHelper.get();
                            th.oldCheckedItems.remove(event.getName());
                            th.newCheckedItems.remove(event.getName());
                        })
                        .show();
                break;
        }
    }

    /**
     * Class to help us hold complex objects until not needed any longer.
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
         * List of originally checked tags.
         */
        public List<String> oldCheckedItems;
        /**
         * List of finally checked tags.
         */
        public List<String> newCheckedItems;
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
         */
        public static void reset() {
            INSTANCE = new TaggingHelper();
        }

        private TaggingHelper() {
            selectedBooks = null;
            oldCheckedItems = new ArrayList<>();
            newCheckedItems = new ArrayList<>();
        }

        /**
         * Convenience method for initializing the {@link TaggingHelper}.
         * @param books            List of books.
         * @param currCheckedItems List of tags to have checked.
         */
        public void init(List<RBook> books, List<String> currCheckedItems) {
            this.selectedBooks = books;
            this.oldCheckedItems = currCheckedItems;
            this.newCheckedItems = new ArrayList<>(currCheckedItems);
        }
    }
}
