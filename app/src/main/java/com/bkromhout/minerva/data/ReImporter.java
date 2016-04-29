package com.bkromhout.minerva.data;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.prefs.DefaultPrefs;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.util.Util;
import io.realm.Realm;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Class which can be used to reimport selected books. Displays a dialog with progress.
 */
public class ReImporter {
    /**
     * Various state the importer can be in.
     */
    private enum State {
        PREP, IMPORTING, SAVING, CANCELLING, FINISHING, ERROR
    }

    /**
     * Instance of the ReImporter.
     */
    private static ReImporter INSTANCE = null;

    /**
     * The content which the progress dialog should display. We keep this around even if the dialog isn't being shown
     * for simplicity.
     */
    private View dialogContent;
    @BindView(R.id.message)
    TextView message;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.error_messages)
    TextView errors;

    /**
     * The current state of the importer.
     */
    private State currState;
    /**
     * What directory the importer is currently importing from.
     */
    private File currDir;
    /**
     * List of {@link RBook}s to re-import.
     */
    private List<RBook> books;
    /**
     * List of {@link RBook}s that have been created from book files.
     */
    private Queue<RBook> bookQueue;
    /**
     * Composite subscription, for easy unsubscribing.
     */
    private CompositeSubscription subs;
    /**
     * Instance of Realm.
     */
    private Realm realm;
    /**
     * Who is listening to our progress?
     */
    private IReImportListener listener;
    /**
     * Actual dialog.
     */
    private MaterialDialog dialog;

    /**
     * Re-imports the given {@link RBook}s.
     * @param books    List of {@link RBook}s to re-import.
     * @param listener The listener to attach.
     */
    public static void reImportBooks(List<RBook> books, IReImportListener listener) {
        if (INSTANCE != null || books == null || books.isEmpty() || listener == null) return;

        INSTANCE = new ReImporter(books, listener);
        EventBus.getDefault().post(new StartImportEvent());
    }

    /**
     * Reattach a listener to the ReImporter so that the dialog can be shown again.
     * @param listener Listener to attach.
     */
    public static void reAttachIfExists(IReImportListener listener) {
        if (INSTANCE == null) return;
        if (INSTANCE.listener != null) throw new IllegalStateException("Listener already attached!");

        INSTANCE.listener = listener;
        INSTANCE.showDialog();
    }

    /**
     * Hides the dialog and removes the reference to the listener.
     */
    public static void detachListener() {
        if (INSTANCE == null || INSTANCE.listener == null) return;

        INSTANCE.dismissDialog();
        INSTANCE.listener = null;
    }

    // Private initializer.
    @SuppressLint("InflateParams")
    private ReImporter(List<RBook> books, IReImportListener listener) {
        EventBus.getDefault().register(this);
        this.books = books;
        this.listener = listener;

        // Get importer ready.
        currState = State.PREP;
        subs = new CompositeSubscription();
        bookQueue = new LinkedList<>();

        // Create the view we will use in the dialog, then bind our local view references.
        LayoutInflater inflater = LayoutInflater.from(Minerva.getAppCtx());
        dialogContent = inflater.inflate(R.layout.dialog_re_import, null);
        ButterKnife.bind(this, dialogContent);

        // Get views ready.
        progressBar.setIndeterminate(true);
        message.setText(R.string.ril_starting);

        // Go ahead and create the dialog and show it.
        showDialog();
    }

    /**
     * Start the re-import flow.
     * @param event The trigger event. Will throw an IllegalArgumentException if null, preventing outside calls.
     */
    @Subscribe
    public void startImport(StartImportEvent event) {
        if (event == null) throw new IllegalArgumentException("Must be triggered by event.");
        // We no longer need to be registered with the event bus.
        EventBus.getDefault().unregister(this);

        // Make sure we should continue.
        if (currState != State.PREP) {
            if (!hasErrors()) currState = State.FINISHING;
            tearDown(false);
            return;
        }

        // Ensure before we go on that we have a valid library directory.
        if ((currDir = Util.tryResolveDir(DefaultPrefs.get().getLibDir(null))) == null) {
            // We don't have a valid library directory.
            currState = State.ERROR;
            errors.append(C.getStr(R.string.fil_err_invalid_lib_dir));
            dismissDialogUnlessErrors();
            return;
        }

        // Convert RBooks to a list of Files.
        message.setText(R.string.ril_build_file_list);
        subs.add(Observable.from(books)
                           .map(RBook::getRelPath)
                           .map(relPath -> {
                               File file = Util.getFileFromRelPath(currDir, relPath);
                               if (file == null) errors.append(C.getStr(R.string.ril_err_getting_file,
                                       currDir.getAbsolutePath() + relPath));
                               return file;
                           })
                           .filter(file -> file != null)
                           .toList()
                           .single()
                           .subscribe(this::onGotFiles, throwable -> {
                               currState = State.ERROR;
                               throwable.printStackTrace();
                               message.setText(R.string.ril_err_getting_files);
                               tearDown(false);
                               dismissDialogUnlessErrors();
                           })
        );
    }

    /**
     * Called when the list of files to re-import has been obtained.
     * @param files List of files to re-import.
     */
    private void onGotFiles(List<File> files) {
        // Make sure we should continue.
        if (currState != State.PREP) {
            if (!hasErrors()) currState = State.FINISHING;
            tearDown(false);
            return;
        }

        // Update views.
        progressBar.setIndeterminate(false);
        progressBar.setMax(files.size());
        progressBar.setProgress(0);

        // Start re-importing files.
        doImportFiles(files);
    }

    /**
     * Called to start re-importing the {@code files}.
     * @param files List of files to re-import.
     */
    private void doImportFiles(List<File> files) {
        // Make sure we should continue.
        if (currState != State.PREP) {
            if (!hasErrors()) currState = State.FINISHING;
            tearDown(false);
            return;
        }

        // Update state and start re-importing.
        currState = State.IMPORTING;
        message.setText(R.string.ril_reading_files);
        subs.add(Observable.from(files)
                           .subscribeOn(Schedulers.io())
                           .map(this::convertFileToSuperBook) // Create a SuperBook from the file.
                           .filter(sb -> sb != null)
                           .map(RBook::new) // Create an RBook from the SuperBook.
                           .observeOn(AndroidSchedulers.mainThread())
                           .subscribe(this::onImportedBook, this::onBookImportingError, this::onAllBooksImported));
    }

    /**
     * Convert the given file to a {@link SuperBook}.
     * @param file File.
     * @return New SuperBook, or null if there were issues.
     */
    private SuperBook convertFileToSuperBook(File file) {
        String relPath = file.getAbsolutePath().replace(currDir.getAbsolutePath(), "");
        try {
            return Util.readEpubFile(file, relPath);
        } catch (IllegalArgumentException e) {
            errors.append(C.getStr(R.string.ril_err_reading_file, file.getAbsolutePath()));
            return null;
        }
    }

    /**
     * What to do when we've finished importing and processing a book file.
     * <p>
     * This is called at the end of the import flow, it should be lightweight!!
     * @param book The {@link RBook} we created using info from the file.
     */
    private void onImportedBook(RBook book) {
        // Make sure we should continue.
        if (currState != State.IMPORTING) {
            if (!hasErrors()) currState = State.FINISHING;
            tearDown(false);
            return;
        }

        // Add RBook to queue, update message and progress.
        bookQueue.add(book);
        message.setText(C.getStr(R.string.ril_read_file, book.getTitle()));
        progressBar.incrementProgressBy(1);
    }

    /**
     * What to do if an error is thrown during import.
     * @param t Throwable.
     */
    private void onBookImportingError(Throwable t) {
        currState = State.ERROR;
        t.printStackTrace();
        message.setText(R.string.ril_err_reading_files);
        tearDown(false);
        dismissDialogUnlessErrors();
    }

    /**
     * What to do after we've finished importing all books.
     */
    private void onAllBooksImported() {
        // Make sure we should continue.
        if (currState != State.IMPORTING) {
            if (!hasErrors()) currState = State.FINISHING;
            tearDown(false);
            return;
        }

        // Update state and disable cancel button.
        currState = State.SAVING;
        disableCancelButtonIfNeeded();
        progressBar.setIndeterminate(true);
        message.setText(R.string.ril_saving_data);

        // Get Realm and update books.
        realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(
                bgRealm -> {
                    for (RBook book : bookQueue) {
                        // Try to find existing RBook before adding a new one.
                        RBook existingBook = bgRealm.where(RBook.class)
                                                    .equalTo("relPath", book.getRelPath())
                                                    .findFirst();

                        // If we have an existing RBook for this file, just update the fields which we read from the
                        // file. If we don't have one, create one.
                        if (existingBook != null) existingBook.updateFromOtherRBook(bgRealm, book);
                        else bgRealm.copyToRealmOrUpdate(book);
                    }
                },
                this::finished,
                error -> {
                    currState = State.ERROR;
                    error.printStackTrace();
                    message.setText(R.string.ril_err_realm);
                    tearDown(false);
                    dismissDialogUnlessErrors();
                });
    }

    /**
     * Called when we've finished saving all of the re-imported data.
     */
    private void finished() {
        // Update state and views.
        currState = State.FINISHING;
        progressBar.setIndeterminate(false);
        progressBar.setProgress(progressBar.getMax());
        message.setText(hasErrors() ? R.string.ril_done_with_errors : R.string.ril_done);

        // Notify listener that we're finished, tear down, then dismiss dialog (unless there are errors, give the
        // user a chance to see them).
        tearDown(true);
        dismissDialogUnlessErrors();
    }

    /**
     * Show the dialog using the context of the listener.
     */
    private void showDialog() {
        if (listener == null || dialog != null) return;

        dialog = new MaterialDialog.Builder(listener.getCtx())
                .title(R.string.title_dialog_re_import_progress)
                .customView(dialogContent, true)
                .autoDismiss(false) // Do not dismiss when button is clicked.
                .cancelable(false) // Do not dismiss when back is pressed.
                .negativeText(R.string.cancel_re_import)
                .onNegative((iDialog, which) -> {
                    cancel();
                    dismissDialog();
                })
                .build();
        disableCancelButtonIfNeeded();

        dialog.show();
    }

    /**
     * Disables the cancel button on the dialog if the {@link #dialog} isn't null and we're in the SAVING, FINISHING, or
     * CANCELLING state.
     */
    private void disableCancelButtonIfNeeded() {
        if ((currState == State.SAVING || currState == State.FINISHING || currState == State.CANCELLING) &&
                dialog != null)
            dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
    }

    /**
     * Dismisses the dialog unless we have errors in the error messages text view. In that case, switches the text of
     * the cancel button to "Done" so that the user has a chance to look at the errors.
     */
    private void dismissDialogUnlessErrors() {
        if (!hasErrors()) dismissDialog();
        else {
            dialog.setActionButton(DialogAction.NEGATIVE, R.string.ok);
            dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(true);
        }
    }

    /**
     * Dismiss the dialog.
     */
    private void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    /**
     * Get whether or not there are currently errors.
     * @return True if there are error messages or we've explicitly set the error state.
     */
    private boolean hasErrors() {
        return currState == State.ERROR || errors.length() != 0;
    }

    /**
     * Cancel the import. Does nothing if <i>not</i> in the PREP, IMPORTING, or ERROR state.
     */
    private void cancel() {
        if (currState == State.PREP || currState == State.IMPORTING || currState == State.ERROR) {
            currState = State.CANCELLING;
            disableCancelButtonIfNeeded();
            tearDown(false);
        }
    }

    /**
     * Tear down. This will remove the reference to the {@link ReImporter} stored in {@link #INSTANCE}.
     * @param wasSuccess Whether or not we're calling this because we successfully finished.
     */
    private void tearDown(boolean wasSuccess) {
        // Unsubscribe from any Rx subscriptions.
        subs.unsubscribe();
        // Close Realm.
        if (realm != null) {
            realm.close();
            realm = null;
        }
        // Tell listener that we finished, then remove reference to it.
        if (listener != null) {
            listener.onReImportFinished(wasSuccess);
            listener = null;
        }
        // Remove reference to INSTANCE.
        INSTANCE = null;
    }

    /**
     * Listener interface.
     */
    public interface IReImportListener {
        /**
         * Called by {@link ReImporter} so that it has access to a context it can use to create a dialog.
         * @return Activity context.
         */
        Activity getCtx();

        /**
         * Called by {@link ReImporter} when the re-import is finished.
         * @param wasSuccess If true, the re-import was successful and the listener should assume it needs to update the
         *                   UI to reflect new data.
         */
        void onReImportFinished(boolean wasSuccess);
    }

    public static class StartImportEvent {}
}
