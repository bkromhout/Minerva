package com.bkromhout.minerva.data;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.Bind;
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
import rx.subscriptions.CompositeSubscription;

import java.io.File;
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
    @Bind(R.id.message)
    TextView message;
    @Bind(R.id.progress_bar)
    private ProgressBar progressBar;
    @Bind(R.id.error_messages)
    private TextView errors;

    /**
     * Key for current instance.
     */
    private long key;
    /**
     * The current state of the importer.
     */
    private State currState;
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
        if (INSTANCE == null) return;
        if (INSTANCE.listener == null) throw new IllegalStateException("No Listener attached!");

        INSTANCE.dismissDialog();
        INSTANCE.listener = null;
    }

    // Private initializer.
    private ReImporter(List<RBook> books, IReImportListener listener) {
        EventBus.getDefault().register(this);
        this.books = books;
        this.listener = listener;

        // Get importer ready.
        currState = State.PREP;
        realm = Realm.getDefaultInstance();

        // Create the view we will use in the dialog, then bind our local view references.
        LayoutInflater inflater = LayoutInflater.from(Minerva.getAppCtx());
        dialogContent = inflater.inflate(R.layout.dialog_re_import, null);
        ButterKnife.bind(this, dialogContent);

        // Get views ready.
        progressBar.setIndeterminate(true);
        progressBar.setMax(books.size());
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

        // Ensure before we go on that we have a non-null library path.
        if (DefaultPrefs.get().getLibDir(null) == null) {
            currState = State.ERROR;
            errors.append(C.getStr(R.string.fil_err_invalid_lib_dir));
            dismissDialogUnlessErrors();
            return;
        }

        // Convert RBooks to a list of Files.
        message.setText(R.string.ril_build_file_list);
        subs.add(Observable.from(books)
                           .map(book -> {
                               File file = Util.getFileFromRelPath(book.getRelPath());
                               if (file == null) errors.append(C.getStr(R.string.ril_err_reading, book.getTitle()));
                               return file;
                           })
                           .filter(file -> file != null)
                           .toList()
                           .single()
                           .subscribe(this::onGotFiles, this::onErrorWhileGettingFiles)
        );
    }

    private void onGotFiles(List<File> files) {
        // TODO
    }

    private void onErrorWhileGettingFiles(Throwable e) {
        // TODO
    }

    /**
     * Show the dialog using the context of the listener.
     */
    private void showDialog() {
        if (listener == null || dialog != null) return;

        dialog = new MaterialDialog.Builder(listener.getContext())
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
     * Disables the cancel button on the dialog if the {@link #dialog} isn't null and we aren't in the PREP or IMPORTING
     * state.
     */
    private void disableCancelButtonIfNeeded() {
        if (dialog != null && currState != State.PREP && currState != State.IMPORTING && currState != State.ERROR)
            dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
    }

    /**
     * Dismisses the dialog unless we have errors in the error messages text view. In that case, switches the text of
     * the cancel button to "Done" so that the user has a chance to look at the errors.
     */
    private void dismissDialogUnlessErrors() {
        if (errors.length() == 0) dismissDialog();
        else {
            dialog.setActionButton(DialogAction.NEGATIVE, R.string.ok);
            dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(true);
        }
    }

    /**
     * Dismiss the dialog.
     */
    private void dismissDialog() {
        if (dialog == null) return;
        dialog.dismiss();
        dialog = null;
    }

    /**
     * Cancel the import. Does nothing if <i>not</i> in the PREP, IMPORTING, or ERROR state.
     */
    private void cancel() {
        if (currState != State.PREP && currState != State.IMPORTING && currState != State.ERROR) return;
        currState = State.CANCELLING;
        disableCancelButtonIfNeeded();
        tearDown();
    }

    /**
     * Tear down. This will remove the reference to the {@link ReImporter} stored in {@link #INSTANCE}.
     */
    private void tearDown() {
        // Unsubscribe from any Rx subscriptions.
        subs.unsubscribe();
        // Close Realm.
        if (realm != null) {
            realm.close();
            realm = null;
        }
        // Remove reference to listener.
        listener = null;
        // Remove reference to INSTANCE.
        INSTANCE = null;
    }

    /**
     * Interface of listener.
     */
    public interface IReImportListener {
        /**
         * Obtain an activity context so that we may draw a dialog.
         * @return Activity context.
         */
        Activity getContext();

        /**
         * Called when the re-import is finished.
         */
        void onReImportFinished();
    }

    private static class StartImportEvent {}
}
