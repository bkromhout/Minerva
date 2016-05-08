package com.bkromhout.minerva;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.bkromhout.minerva.data.ActionHelper;
import com.bkromhout.minerva.data.CoverHelper;
import com.bkromhout.minerva.data.ReImporter;
import com.bkromhout.minerva.events.ActionEvent;
import com.bkromhout.minerva.events.UpdatePosEvent;
import com.bkromhout.minerva.prefs.DefaultPrefs;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.ui.TagBackgroundSpan;
import com.bkromhout.minerva.util.Dialogs;
import com.bkromhout.minerva.util.Util;
import com.bumptech.glide.Glide;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

/**
 * Displays information for some {@link com.bkromhout.minerva.realm.RBook}.
 */
public class BookInfoActivity extends PermCheckingActivity implements ReImporter.IReImportListener, SnackKiosk.Snacker {
    /**
     * Part of the information for which views are conditionally shown.
     * @see #togglePart(Part, boolean)
     */
    private enum Part {
        LISTS, SUBJECTS, TYPES, FORMAT, LANGUAGE, PUBLISHER, PUBLISH_DATE, MOD_DATE
    }

    // AppBarLayout views.
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;
    @BindView(R.id.collapsing_toolbar)
    CollapsingToolbarLayout collapsibleToolbar;
    @BindView(R.id.cover_image)
    ImageView coverImage;
    @BindView(R.id.fab)
    FloatingActionButton fab;

    // Views which always are shown.
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.author)
    TextView author;
    @BindView(R.id.desc)
    TextView desc;
    @BindView(R.id.chap_count)
    TextView chapCount;
    @BindView(R.id.rating)
    RatingBar rating;
    @BindView(R.id.edit_rating)
    ImageButton editRating;
    @BindView(R.id.edit_tags)
    ImageButton editTags;
    @BindView(R.id.tags)
    TextView tags;
    @BindView(R.id.path)
    TextView path;
    @BindView(R.id.last_read_date)
    TextView lastReadDate;
    @BindView(R.id.last_import_date)
    TextView lastImportDate;

    // Views which are conditionally shown.
    @BindView(R.id.lists)
    TextView lists;
    @BindView(R.id.no_lists)
    TextView noLists;
    @BindView(R.id.lbl_subjects)
    TextView lblSubjects;
    @BindView(R.id.subjects)
    TextView subjects;
    @BindView(R.id.lbl_types)
    TextView lblTypes;
    @BindView(R.id.types)
    TextView types;
    @BindView(R.id.lbl_format)
    TextView lblFormat;
    @BindView(R.id.format)
    TextView format;
    @BindView(R.id.lbl_language)
    TextView lblLanguage;
    @BindView(R.id.language)
    TextView language;
    @BindView(R.id.lbl_publisher)
    TextView lblPublisher;
    @BindView(R.id.publisher)
    TextView publisher;
    @BindView(R.id.lbl_publish_date)
    TextView lblPublishDate;
    @BindView(R.id.publish_date)
    TextView publishDate;
    @BindView(R.id.lbl_mod_date)
    TextView lblModDate;
    @BindView(R.id.mod_date)
    TextView modDate;

    /**
     * Position to use in any {@link UpdatePosEvent}s which might be sent.
     */
    private int posToUpdate;
    /**
     * If true, send a {@link UpdatePosEvent} to the {@link com.bkromhout.minerva.fragments.AllListsFragment} when we
     * exit this activity.
     */
    private boolean needsPosUpdate = false;
    /**
     * Realm instance.
     */
    private Realm realm;
    /**
     * {@link RBook} whose information is being displayed.
     */
    private RBook book;
    /**
     * Listen for changes to {@link #book}. Call {@link #updateUi()} when they occur.
     */
    private RealmChangeListener<RBook> bookListener = newBook -> updateUi();

    /**
     * Start the {@link BookInfoActivity} for the {@link RBook} with the given {@code relPath}.
     * @param context   Context to use to start the activity.
     * @param relPath   Relative path which will be used to get the {@link RBook}.
     * @param updatePos Position which should be updated when the activity closes.
     */
    public static void start(Context context, String relPath, int updatePos) {
        if (relPath == null || relPath.isEmpty())
            throw new IllegalArgumentException("Must supply non-null, non-empty relative path.");
        if (updatePos < 0)
            throw new IllegalArgumentException("Must supply a position >= 0.");
        context.startActivity(new Intent(context, BookInfoActivity.class).putExtra(C.REL_PATH, relPath)
                                                                         .putExtra(C.POS_TO_UPDATE, updatePos));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create and bind views.
        setContentView(R.layout.activity_book_info);
        ButterKnife.bind(this);

        // Set up toolbar.
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get Realm and read extras bundle.
        String relPath = getIntent().getStringExtra(C.REL_PATH);
        posToUpdate = getIntent().getIntExtra(C.POS_TO_UPDATE, -1);
        realm = Realm.getDefaultInstance();

        // Get RBook.
        book = realm.where(RBook.class).equalTo("relPath", relPath).findFirst();
        if (book == null) throw new IllegalArgumentException("Invalid relative path, no matching RBook found.");

        // Set cover image (we do this separately since we don't want flickering if the change listener fires).
        if (book.hasCoverImage) {
            Glide.with(this)
                 .load(CoverHelper.get().getCoverImageFile(book.relPath))
                 .centerCrop()
                 .into(coverImage);
        } else coverImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.epub_logo_color));
        // Set up the rest of the UI.
        updateUi();

        // Add the change listener to the RBook.
        book.addChangeListener(bookListener);

        // If we have a saved instance state, check whether we will still need to send a position update upon finishing.
        if (savedInstanceState != null && savedInstanceState.getBoolean(C.NEEDS_POS_UPDATE)) needsPosUpdate = true;

        // Handle permissions. Make sure we continue a request process if applicable.
        initAndContinuePermChecksIfNeeded();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Util.forceMenuIcons(menu, this, getClass().getSimpleName());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.book_info, menu);
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
        outState.putBoolean(C.NEEDS_POS_UPDATE, needsPosUpdate);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().postSticky(new UpdatePosEvent(posToUpdate));
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Remove listener.
        book.removeChangeListener(bookListener);
        // Close Realm.
        if (realm != null) {
            realm.close();
            realm = null;
        }
        // If we need to update the book's card, send the sticky event now (unless there's already a sticky event.)
        if (needsPosUpdate && EventBus.getDefault().getStickyEvent(UpdatePosEvent.class) == null)
            EventBus.getDefault().postSticky(new UpdatePosEvent(posToUpdate));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_read:
                // Open the book file.
                ActionHelper.openBookUsingIntent(book, this);
                return true;
            case R.id.action_add_to_list:
                Dialogs.addToListDialogOrToast(this, realm);
                return true;
            case R.id.action_tag:
                //noinspection unchecked
                TaggingActivity.start(this, book);
                return true;
            case R.id.action_rate:
                Dialogs.ratingDialog(this, book.rating);
                return true;
            case R.id.action_re_import:
                Dialogs.simpleConfirmDialog(this, R.string.title_re_import_book, R.string.prompt_re_import_book,
                        R.string.action_re_import, R.id.action_re_import);
                return true;
            case R.id.action_delete:
                Dialogs.confirmCheckBoxDialog(this, R.string.title_delete_book, R.string.prompt_delete_book,
                        R.string.prompt_delete_from_device_too, R.string.info_delete_from_device_permanent,
                        R.string.action_delete, R.id.action_delete);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Called when we wish to take some action.
     * @param event {@link ActionEvent}.
     */
    @Subscribe
    public void onActionEvent(ActionEvent event) {
        switch (event.getActionId()) {
            case R.id.action_rate: {
                ActionHelper.rateBook(realm, book, (Integer) event.getData());
                // The rating changed, so we'll need to update the book's card.
                needsPosUpdate = true;
                break;
            }
            case R.id.action_add_to_list: {
                ActionHelper.addBookToList(realm, book, (String) event.getData());
                break;
            }
            case R.id.action_re_import: {
                ActionHelper.reImportBook(book, this);
                // We may have changed things, so we'll need to update the book's card.
                needsPosUpdate = true;
                return;
            }
            case R.id.action_delete: {
                ActionHelper.deleteBook(book, (boolean) event.getData());
                finish();
                break;
            }
        }
    }

    @OnClick(R.id.cover_image)
    void onHeaderImageClicked(View v) {
        // If this book actually has a cover, start the CoverActivity.
        if (book.hasCoverImage) CoverActivity.start(this, book.relPath);
    }

    /**
     * Open the book when the FAB is clicked.
     */
    @OnClick(R.id.fab)
    void onReadFabClicked() {
        // Open the book file.
        ActionHelper.openBookUsingIntent(book, this);
    }

    /**
     * Open the rating dialog when the edit rating button is clicked.
     */
    @OnClick(R.id.edit_rating)
    void onEditRatingClicked() {
        // Open the rating dialog.
        Dialogs.ratingDialog(this, book.rating);
    }

    /**
     * Open the tagging dialog when the edit tags button is clicked.
     */
    @OnClick(R.id.edit_tags)
    void onEditTagsClicked() {
        // Open the tagging activity.
        TaggingActivity.start(this, book);
    }

    /**
     * Update the UI using the data in {@link #book}.
     */
    @SuppressLint("SetTextI18n")
    private void updateUi() {
        // Set title.
        collapsibleToolbar.setTitle(book.title);

        // Fill in common views using book data.
        title.setText(book.title);
        author.setText(book.author);
        desc.setText(book.desc);
        chapCount.setText(String.valueOf(book.numChaps));
        rating.setRating(book.rating);
        path.setText(DefaultPrefs.get().getLibDir("") + book.relPath);

        lastReadDate.setText(book.lastReadDate == null ? C.getStr(R.string.never)
                : DateUtils.getRelativeDateTimeString(this, book.lastReadDate.getTime(),
                DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME));

        lastImportDate.setText(DateUtils.getRelativeDateTimeString(this, book.lastImportDate.getTime(),
                DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME));

        // Fill in tags or show empty text.
        if (book.tags.isEmpty()) tags.setText(R.string.no_tags);
        else tags.setText(TagBackgroundSpan.getSpannedTagString(book, tags.getMaxLines()),
                TextView.BufferType.SPANNABLE);

        // Fill in lists or show empty view.
        List<String> listNames = RBook.listsBookIsIn(book, realm);
        if (listNames.isEmpty()) togglePart(Part.LISTS, false);
        else {
            togglePart(Part.LISTS, true);
            lists.setText(Util.listToString(listNames, ", "));
        }

        // Fill in or hide conditional views using book data.
        String temp = book.subjects;
        if (temp == null || temp.isEmpty()) togglePart(Part.SUBJECTS, false);
        else {
            togglePart(Part.SUBJECTS, true);
            subjects.setText(temp);
        }

        temp = book.types;
        if (temp == null || temp.isEmpty()) togglePart(Part.TYPES, false);
        else {
            togglePart(Part.TYPES, true);
            types.setText(temp);
        }

        temp = book.format;
        if (temp == null || temp.isEmpty()) togglePart(Part.FORMAT, false);
        else {
            togglePart(Part.FORMAT, true);
            format.setText(temp);
        }

        temp = book.language;
        if (temp == null || temp.isEmpty()) togglePart(Part.LANGUAGE, false);
        else {
            togglePart(Part.LANGUAGE, true);
            language.setText(temp);
        }

        temp = book.language;
        if (temp == null || temp.isEmpty()) togglePart(Part.PUBLISHER, false);
        else {
            togglePart(Part.PUBLISHER, true);
            publisher.setText(temp);
        }

        temp = book.pubDate;
        if (temp == null || temp.isEmpty()) togglePart(Part.PUBLISH_DATE, false);
        else {
            togglePart(Part.PUBLISH_DATE, true);
            publishDate.setText(temp);
        }

        temp = book.modDate;
        if (temp == null || temp.isEmpty()) togglePart(Part.MOD_DATE, false);
        else {
            togglePart(Part.MOD_DATE, true);
            modDate.setText(temp);
        }
    }

    /**
     * Show/hide the given {@code part}.
     * @param part Part to show/hide.
     * @param show If true, show the part, otherwise hide it.
     */
    private void togglePart(Part part, boolean show) {
        switch (part) {
            case LISTS:
                lists.setVisibility(show ? View.VISIBLE : View.GONE);
                noLists.setVisibility(show ? View.GONE : View.VISIBLE);
                break;
            case SUBJECTS:
                lblSubjects.setVisibility(show ? View.VISIBLE : View.GONE);
                subjects.setVisibility(show ? View.VISIBLE : View.GONE);
                break;
            case TYPES:
                lblTypes.setVisibility(show ? View.VISIBLE : View.GONE);
                types.setVisibility(show ? View.VISIBLE : View.GONE);
                break;
            case FORMAT:
                lblFormat.setVisibility(show ? View.VISIBLE : View.GONE);
                format.setVisibility(show ? View.VISIBLE : View.GONE);
                break;
            case LANGUAGE:
                lblLanguage.setVisibility(show ? View.VISIBLE : View.GONE);
                language.setVisibility(show ? View.VISIBLE : View.GONE);
                break;
            case PUBLISHER:
                lblPublisher.setVisibility(show ? View.VISIBLE : View.GONE);
                publisher.setVisibility(show ? View.VISIBLE : View.GONE);
                break;
            case PUBLISH_DATE:
                lblPublishDate.setVisibility(show ? View.VISIBLE : View.GONE);
                publishDate.setVisibility(show ? View.VISIBLE : View.GONE);
                break;
            case MOD_DATE:
                lblModDate.setVisibility(show ? View.VISIBLE : View.GONE);
                modDate.setVisibility(show ? View.VISIBLE : View.GONE);
                break;
        }
    }

    @Override
    public Activity getCtx() {
        return this;
    }

    @Override
    public void onReImportFinished(boolean wasSuccess) {
        // Nothing, realm's change listener takes care of updating the UI for us.
    }

    @NonNull
    @Override
    public View getSnackbarAnchorView() {
        return coordinator;
    }
}
