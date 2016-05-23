package com.bkromhout.minerva.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.data.ActionHelper;
import com.bkromhout.minerva.data.DataUtils;
import com.bkromhout.minerva.enums.MarkType;
import com.bkromhout.minerva.events.ActionEvent;
import com.bkromhout.minerva.events.UpdatePosEvent;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.ui.AnimUtils;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.ui.TagBackgroundSpan;
import com.bkromhout.minerva.util.Dialogs;
import com.bkromhout.minerva.util.Util;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Displays information for some {@link com.bkromhout.minerva.realm.RBook}.
 */
public class BookInfoActivity extends PermCheckingActivity implements SnackKiosk.Snacker {
    private static final String CARDS_HAVE_COVERS = "card_have_covers";

    /**
     * Part of the information for which views are conditionally shown.
     * @see #togglePart(Part, boolean)
     */
    private enum Part {
        LISTS, SUBJECTS, TYPES, FORMAT, LANGUAGE, PUBLISHER, PUBLISH_DATE, MOD_DATE
    }

    // General views.
    @BindView(R.id.transition_bg)
    View bg;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;
    @BindView(R.id.appbar)
    AppBarLayout appBar;
    @BindView(R.id.collapsing_toolbar)
    CollapsingToolbarLayout collapsibleToolbar;
    @BindView(R.id.cover_image)
    ImageView coverImage;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.content)
    RelativeLayout content;

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
    private RealmChangeListener<RBook> bookListener = newBook -> {
        if (!newBook.title.equals(getTitle().toString())) setTitle(newBook.title);
        updateUi();
    };

    /**
     * Start the {@link BookInfoActivity} for the {@link RBook} with the given {@code relPath}.
     * @param activity       Context to use to start the activity.
     * @param relPath        Relative path which will be used to get the {@link RBook}.
     * @param updatePos      Position which should be updated when the activity closes.
     * @param includeCover   Whether or not to include the cover image in the shared elements transition.
     * @param sharedElements Array of shared element names and their associated views.
     */
    @SafeVarargs
    public static void startWithTransition(Activity activity, String relPath, int updatePos, boolean includeCover,
                                           Pair<View, String>... sharedElements) {
        if (relPath == null || relPath.isEmpty())
            throw new IllegalArgumentException("Must supply non-null, non-empty relative path.");
        if (updatePos < 0)
            throw new IllegalArgumentException("Must supply a position >= 0.");

        activity.startActivity(new Intent(activity, BookInfoActivity.class)
                        .putExtra(C.REL_PATH, relPath)
                        .putExtra(C.POS_TO_UPDATE, updatePos)
                        .putExtra(CARDS_HAVE_COVERS, includeCover),
                ActivityOptions.makeSceneTransitionAnimation(activity, sharedElements).toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_info);
        ButterKnife.bind(this);
        realm = Realm.getDefaultInstance();
        posToUpdate = getIntent().getIntExtra(C.POS_TO_UPDATE, -1);

        // Get RBook and set title immediately.
        book = realm.where(RBook.class).equalTo("relPath", getIntent().getStringExtra(C.REL_PATH)).findFirst();
        if (book == null) throw new IllegalArgumentException("Invalid relative path, no matching RBook found.");
        collapsibleToolbar.setTitle(book.title);

        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setWindowSharedElementTransitions(getIntent().getBooleanExtra(CARDS_HAVE_COVERS, false));

        // Set cover image (we do this separately since we don't want flickering if the change listener fires).
        if (book.hasCoverImage) {
            // Prevent flickering by waiting until the image is loaded before starting the transition.
            postponeEnterTransition();
            Glide.with(this)
                 .load(DataUtils.getCoverImageFile(book.relPath))
                 .dontTransform()
                 .dontAnimate()
                 .listener(new RequestListener<File, GlideDrawable>() {
                     @Override
                     public boolean onException(Exception e, File model, Target<GlideDrawable> target,
                                                boolean isFirstResource) {
                         return false;
                     }

                     @Override
                     public boolean onResourceReady(GlideDrawable resource, File model, Target<GlideDrawable> target,
                                                    boolean isFromMemoryCache, boolean isFirstResource) {
                         startPostponedEnterTransition();
                         return false;
                     }
                 })
                 .into(coverImage);
        } else coverImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.default_cover));

        // Set the movement methods for the certain TextViews just once.
        MovementMethod mm = LinkMovementMethod.getInstance();
        desc.setMovementMethod(mm);
        publisher.setMovementMethod(mm);

        // Set up the rest of the UI.
        updateUi();

        // Add the change listener to the RBook.
        book.addChangeListener(bookListener);

        // Handle permissions. Make sure we continue a request process if applicable.
        initAndContinuePermChecksIfNeeded();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Util.forceMenuIcons(menu, getClass().getSimpleName());
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
    public void onStop() {
        super.onStop();
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
                ActionHelper.openBookUsingIntent(book);
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
            case R.id.action_mark_as:
                Dialogs.markAsDialog(this);
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
            case R.id.action_add_to_list:
                ActionHelper.addBooksToList(realm, Collections.singletonList(book), (String) event.getData());
                break;
            case R.id.action_rate:
                ActionHelper.rateBooks(realm, Collections.singletonList(book), (Integer) event.getData());
                EventBus.getDefault().post(new UpdatePosEvent(posToUpdate));
                break;
            case R.id.action_mark_as:
                int whichMark = (int) event.getData();
                ActionHelper.markBooks(Collections.singletonList(book), whichMark < 2 ? MarkType.NEW : MarkType.UPDATED,
                        whichMark % 2 == 0);
                break;
            case R.id.action_re_import:
                ActionHelper.reImportBooks(Collections.singletonList(book));
                break;
            case R.id.action_delete:
                ActionHelper.deleteBooks(Collections.singletonList(book), (boolean) event.getData());
                finish();
                break;
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
        ActionHelper.openBookUsingIntent(book);
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
     * Set the shared element transitions based on whether or not the book card which was clicked to start this activity
     * had a cover image view. If it did, then we want to include that in the set of shared elements.
     * @param includeCover Whether or not to include the cover image view.
     */
    private void setWindowSharedElementTransitions(boolean includeCover) {
        TransitionInflater ti = TransitionInflater.from(this);
        Transition enterTrans = includeCover ? ti.inflateTransition(R.transition.book_info_shared_enter_with_cover) :
                ti.inflateTransition(R.transition.book_info_shared_enter);
        Transition returnTrans = includeCover ? ti.inflateTransition(R.transition.book_info_shared_return_with_cover) :
                ti.inflateTransition(R.transition.book_info_shared_return);

        // Add listener for shared element enter transition.
        enterTrans.addListener(new AnimUtils.TransitionListenerAdapter() {
            @Override
            public void onTransitionStart(Transition transition) {
                super.onTransitionStart(transition);
                // We don't want to see the toolbar's stuff in the background while we're transitioning in.
                appBar.setVisibility(View.INVISIBLE);
                // Fade out the dummy background as we transition in so that the content sliding up is visible and
                // the change from card background color to our background color isn't so jarring.
                bg.animate()
                  .alpha(0f)
                  .setDuration(300)
                  //.setInterpolator() // TODO needs to come in slow.
                  .setListener(new AnimatorListenerAdapter() {
                      @Override
                      public void onAnimationEnd(Animator animation) {
                          super.onAnimationEnd(animation);
                          bg.setAlpha(0f);
                      }
                  })
                  .start();
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                super.onTransitionEnd(transition);
                // Now we can show the toolbar. TODO Find a way to fade this in, but not affect the cover image.
                appBar.setVisibility(View.VISIBLE);
                // Show the FAB once the enter transition finishes.
                // TODO Make this wait until we've made the toolbar visible, lest it be half obscured by it.
                fab.show();
            }
        });

        // Add listener for shared element return transition.
        returnTrans.addListener(new AnimUtils.TransitionListenerAdapter() {
            @Override
            public void onTransitionStart(Transition transition) {
                super.onTransitionStart(transition);
                // We don't want to see the toolbar's stuff in the background while we're transitioning.
                appBar.setVisibility(View.INVISIBLE);
                // Hide the FAB so that it doesn't flicker-jump its way across the screen.
                fab.setVisibility(View.INVISIBLE);
                // Fade in the dummy background as we transition in so that the content sliding down is visible and
                // the change from our background color back to the card background color isn't so jarring.
                bg.animate()
                  .alpha(1f)
                  .setDuration(300)
                  //.setInterpolator() // TODO Needs to come in fast.
                  .setListener(new AnimatorListenerAdapter() {
                      @Override
                      public void onAnimationEnd(Animator animation) {
                          super.onAnimationEnd(animation);
                          bg.setAlpha(1f);
                      }
                  })
                  .start();
            }
        });

        Window window = getWindow();
        window.setSharedElementEnterTransition(enterTrans);
        window.setSharedElementReturnTransition(returnTrans);
    }

    /**
     * Update the UI using the data in {@link #book}.
     */
    @SuppressLint("SetTextI18n")
    private void updateUi() {
        // Fill in common views using book data.
        title.setText(book.title);
        author.setText(book.author);
        desc.setText(DataUtils.toSpannedHtml(book.desc)); // Allow HTML tags in the description.
        chapCount.setText(String.valueOf(book.numChaps));
        rating.setRating(book.rating);
        path.setText(Minerva.prefs().getLibDir("") + book.relPath);

        lastReadDate.setText(book.lastReadDate == null ? Minerva.get().getString(R.string.never)
                : DateUtils.getRelativeDateTimeString(this, book.lastReadDate.getTime(),
                DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME));

        lastImportDate.setText(DateUtils.getRelativeDateTimeString(this, book.lastImportDate.getTime(),
                DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME));

        // Fill in tags or show empty text.
        if (book.tags.isEmpty()) tags.setText(R.string.no_tags);
        else tags.setText(TagBackgroundSpan.getSpannedTagString(book, tags.getMaxLines()),
                TextView.BufferType.SPANNABLE);

        // Fill in lists or show empty view.
        List<String> listNames = DataUtils.listsBookIsIn(book, realm);
        if (listNames.isEmpty()) togglePart(Part.LISTS, false);
        else {
            togglePart(Part.LISTS, true);
            lists.setText(DataUtils.listToString(listNames, ", "));
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

        temp = book.publisher;
        if (temp == null || temp.isEmpty()) togglePart(Part.PUBLISHER, false);
        else {
            togglePart(Part.PUBLISHER, true);
            publisher.setText(DataUtils.toSpannedHtml(temp)); // Allow links in publisher text.
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

    @NonNull
    @Override
    public View getSnackbarAnchorView() {
        return coordinator;
    }
}
