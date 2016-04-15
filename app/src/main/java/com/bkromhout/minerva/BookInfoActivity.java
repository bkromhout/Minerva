package com.bkromhout.minerva;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.bkromhout.minerva.data.CoverHelper;
import com.bkromhout.minerva.data.ReImporter;
import com.bkromhout.minerva.events.ActionEvent;
import com.bkromhout.minerva.events.UpdatePosEvent;
import com.bkromhout.minerva.prefs.DefaultPrefs;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RTag;
import com.bkromhout.minerva.util.Dialogs;
import com.bkromhout.minerva.util.Util;
import com.bumptech.glide.Glide;
import com.google.common.collect.Lists;
import com.greenfrvr.hashtagview.HashtagView;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.List;

/**
 * Displays information for some {@link com.bkromhout.minerva.realm.RBook}.
 */
public class BookInfoActivity extends AppCompatActivity {
    // Key strings for the bundle passed when this activity is started.
    private static final String REL_PATH = "REL_PATH";
    private static final String UPDATE_POS = "UPDATE_POS";

    /**
     * Part of the information for which views are conditionally shown.
     * @see #togglePart(Part, boolean)
     */
    private enum Part {
        TAGS, LISTS, SUBJECTS, TYPES, FORMAT, LANGUAGE, PUBLISHER, PUBLISH_DATE, MOD_DATE
    }

    // AppBarLayout views.
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.collapsing_toolbar)
    CollapsingToolbarLayout collapsibleToolbar;
    @Bind(R.id.cover_image)
    ImageView coverImage;
    @Bind(R.id.fab)
    FloatingActionButton fab;

    // Views which always are shown.
    @Bind(R.id.title)
    TextView title;
    @Bind(R.id.author)
    TextView author;
    @Bind(R.id.desc)
    TextView desc;
    @Bind(R.id.chap_count)
    TextView chapCount;
    @Bind(R.id.rating)
    RatingBar rating;
    @Bind(R.id.edit_rating)
    ImageButton editRating;
    @Bind(R.id.edit_tags)
    ImageButton editTags;
    @Bind(R.id.path)
    TextView path;
    @Bind(R.id.last_read_date)
    TextView lastReadDate;
    @Bind(R.id.last_import_date)
    TextView lastImportDate;

    // Views which are conditionally shown.
    @Bind(R.id.tags)
    HashtagView tags;
    @Bind(R.id.no_tags)
    TextView noTags;
    @Bind(R.id.lists)
    TextView lists;
    @Bind(R.id.no_lists)
    TextView noLists;
    @Bind(R.id.lbl_subjects)
    TextView lblSubjects;
    @Bind(R.id.subjects)
    TextView subjects;
    @Bind(R.id.lbl_types)
    TextView lblTypes;
    @Bind(R.id.types)
    TextView types;
    @Bind(R.id.lbl_format)
    TextView lblFormat;
    @Bind(R.id.format)
    TextView format;
    @Bind(R.id.lbl_language)
    TextView lblLanguage;
    @Bind(R.id.language)
    TextView language;
    @Bind(R.id.lbl_publisher)
    TextView lblPublisher;
    @Bind(R.id.publisher)
    TextView publisher;
    @Bind(R.id.lbl_publish_date)
    TextView lblPublishDate;
    @Bind(R.id.publish_date)
    TextView publishDate;
    @Bind(R.id.lbl_mod_date)
    TextView lblModDate;
    @Bind(R.id.mod_date)
    TextView modDate;

    /**
     * Position of the book card
     */
    private int updatePos;
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
    private RealmChangeListener bookListener = this::updateUi;

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
        context.startActivity(new Intent(context, BookInfoActivity.class).putExtra(REL_PATH, relPath)
                                                                         .putExtra(UPDATE_POS, updatePos));
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
        getSupportActionBar().setHomeButtonEnabled(true);

        // Get Realm and read extras bundle.
        String relPath = getIntent().getStringExtra(REL_PATH);
        updatePos = getIntent().getIntExtra(UPDATE_POS, -1);
        realm = Realm.getDefaultInstance();

        // Get RBook.
        book = realm.where(RBook.class).equalTo("relPath", relPath).findFirst();
        if (book == null) throw new IllegalArgumentException("Invalid relative path, no matching RBook found.");

        // Set cover image (we do this separately since we don't want flickering if the change listener fires).
        if (book.hasCoverImage()) {
            Glide.with(this)
                 .load(CoverHelper.get().getCoverImageFile(book.getRelPath()))
                 .centerCrop()
                 .into(coverImage);
        } else coverImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.epub_logo_color));
        // Set up the rest of the UI.
        updateUi();

        // Add the change listener to the RBook.
        book.addChangeListener(bookListener);
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
    public void onStop() {
        super.onStop();
        EventBus.getDefault().postSticky(new UpdatePosEvent(updatePos));
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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_read:
                // Open the book file.
                book.openFileUsingIntent(this);
                return true;
            case R.id.action_add_to_list:
                Dialogs.addToListDialogOrToast(this, realm);
                return true;
            case R.id.action_tag:
                //noinspection unchecked
                TaggingActivity.start(this, book);
                return true;
            case R.id.action_rate:
                Dialogs.ratingDialog(this, book.getRating());
                return true;
            case R.id.action_re_import:
                Dialogs.simpleYesNoDialog(this, R.string.title_re_import_books, R.string.prompt_re_import_books,
                        R.id.action_re_import);
                return true;
            case R.id.action_delete:

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
                // Save rating and update UI.
                realm.executeTransaction(tRealm -> book.setRating((Integer) event.getData()));
                break;
            }
            case R.id.action_add_to_list: {
                //RBookList list = realm.where(RBookList.class).equalTo("name", (String) event.getData()).findFirst();
                //RBookList.addBooks(list, selectedItems);
                break;
            }
            case R.id.action_re_import: {
                ReImporter.reImportBooks(Lists.newArrayList(book), this);
                return;
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
    }

    @OnClick(R.id.cover_image)
    void onHeaderImageClicked(View v) {
        // TODO Show cover image in full screen if book actually has a cover image.
    }

    /**
     * Open the book when the FAB is clicked.
     */
    @OnClick(R.id.fab)
    void onReadFabClicked() {
        // Open the book file.
        book.openFileUsingIntent(this);
    }

    /**
     * Open the rating dialog when the edit rating button is clicked.
     */
    @OnClick(R.id.edit_rating)
    void onEditRatingClicked() {
        // Open the rating dialog.
        Dialogs.ratingDialog(this, book.getRating());
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
        collapsibleToolbar.setTitle(book.getTitle());

        // Fill in common views using book data.
        title.setText(book.getTitle());
        author.setText(book.getAuthor());
        desc.setText(book.getDesc());
        chapCount.setText(String.valueOf(book.getNumChaps()));
        rating.setRating(book.getRating());
        path.setText(DefaultPrefs.get().getLibDir("") + book.getRelPath());

        lastReadDate.setText(book.getLastReadDate() == null ? C.getStr(R.string.never)
                : DateUtils.getRelativeDateTimeString(this, book.getLastReadDate().getTime(),
                DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME));

        lastImportDate.setText(DateUtils.getRelativeDateTimeString(this, book.getLastImportDate().getTime(),
                DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME));

        // Fill in tags or show empty view.
        if (book.getTags().isEmpty()) togglePart(Part.TAGS, false);
        else {
            togglePart(Part.TAGS, true);
            tags.setData(RTag.tagListToStringList(book.getTags()));
        }

        // Fill in lists or show empty view.
        List<String> listNames = RBook.listsBookIsIn(book, realm);
        if (listNames.isEmpty()) togglePart(Part.LISTS, false);
        else {
            togglePart(Part.LISTS, true);
            lists.setText(Util.listToString(listNames, ", "));
        }

        // Fill in or hide conditional views using book data.
        String temp = book.getSubjects();
        if (temp == null || temp.isEmpty()) togglePart(Part.SUBJECTS, false);
        else {
            togglePart(Part.SUBJECTS, true);
            subjects.setText(temp);
        }

        temp = book.getTypes();
        if (temp == null || temp.isEmpty()) togglePart(Part.TYPES, false);
        else {
            togglePart(Part.TYPES, true);
            types.setText(temp);
        }

        temp = book.getFormat();
        if (temp == null || temp.isEmpty()) togglePart(Part.FORMAT, false);
        else {
            togglePart(Part.FORMAT, true);
            format.setText(temp);
        }

        temp = book.getLanguage();
        if (temp == null || temp.isEmpty()) togglePart(Part.LANGUAGE, false);
        else {
            togglePart(Part.LANGUAGE, true);
            language.setText(temp);
        }

        temp = book.getPublisher();
        if (temp == null || temp.isEmpty()) togglePart(Part.PUBLISHER, false);
        else {
            togglePart(Part.PUBLISHER, true);
            publisher.setText(temp);
        }

        temp = book.getPubDate();
        if (temp == null || temp.isEmpty()) togglePart(Part.PUBLISH_DATE, false);
        else {
            togglePart(Part.PUBLISH_DATE, true);
            publishDate.setText(temp);
        }

        temp = book.getModDate();
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
            case TAGS:
                tags.setVisibility(show ? View.VISIBLE : View.GONE);
                noTags.setVisibility(show ? View.GONE : View.VISIBLE);
                break;
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
}
