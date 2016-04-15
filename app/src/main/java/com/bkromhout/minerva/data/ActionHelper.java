package com.bkromhout.minerva.data;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.FileProvider;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.realm.RBookListItem;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.ruqus.RealmUserQuery;
import com.google.common.collect.Lists;
import io.realm.Realm;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Convenience class which provides static methods to execute actions, preventing the need for duplicate code.
 */
public class ActionHelper {
    /*
    Book Actions.
     */

    /**
     * Add {@code book} to the list named {@code listName}.
     * @param realm    Instance of Realm to use.
     * @param book     Book to add to list called {@code listName}.
     * @param listName Name of list to add {@code book} to.
     */
    public static void addBookToList(Realm realm, RBook book, String listName) {
        addBooksToList(realm, Lists.newArrayList(book), listName);
    }

    /**
     * Add {@code books} to the list named {@code listName}.
     * @param realm    Instance of Realm to use.
     * @param books    Books to add to list called {@code listName}.
     * @param listName Name of list to add {@code books} to.
     */
    public static void addBooksToList(Realm realm, List<RBook> books, String listName) {
        realm.where(RBookList.class)
             .equalTo("name", listName)
             .findFirst()
             .addBooks(books);
    }

    /**
     * Apply {@code rating} to the given {@code book}.
     * @param realm  Instance of Realm to use.
     * @param book   Book to apply {@code rating} to.
     * @param rating Rating to apply to {@code book}.
     */
    public static void rateBook(Realm realm, RBook book, int rating) {
        rateBooks(realm, Lists.newArrayList(book), rating);
    }

    /**
     * Apply {@code rating} to the given {@code books}.
     * @param realm  Instance of Realm to use.
     * @param books  Books to apply {@code rating} to.
     * @param rating Rating to apply to {@code books}.
     */
    public static void rateBooks(Realm realm, List<RBook> books, int rating) {
        realm.executeTransaction(tRealm -> {
            for (RBook book : books) book.setRating(rating);
        });
    }

    /**
     * Open the book's real file in an application which will allow the user to read it. This will also put the book at
     * the top of the recents list.
     * @param book    Book to open.
     * @param context The context to use to open the file.
     */
    public static void openBookUsingIntent(RBook book, Context context) {
        File file = Util.getFileFromRelPath(book.getRelPath());
        // TODO Make the user aware if the underlying file doesn't exist!
        if (file == null) return;

        // Construct intent to use to open the file.
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        newIntent.setDataAndType(FileProvider.getUriForFile(context, "com.bkromhout.minerva.Minerva.files", file),
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(Util.getExtFromFName(file.getName())));
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        try (Realm realm = Realm.getDefaultInstance()) {
            // Try to open the book file in the app of the user's choice.
            context.startActivity(newIntent);
            // Put book at the top of the recents list.
            realm.executeTransaction(tRealm -> {
                book.setLastReadDate(Calendar.getInstance().getTime());
                book.setInRecents(true);
            });
        } catch (ActivityNotFoundException e) {
            // Tell the user there aren't any apps which advertise the ability to handle the book's file type.
            Toast.makeText(context, C.getStr(R.string.toast_err_no_apps), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Begin re-import process for the given {@code book}.
     * @param book     Book to re-import.
     * @param listener Object to provide a context for drawing a dialog and to call back to when re-import is finished.
     */
    public static void reImportBook(RBook book, ReImporter.IReImportListener listener) {
        reImportBooks(Lists.newArrayList(book), listener);
    }

    /**
     * Begin re-import process for the given {@code books}.
     * @param books    Books to re-import.
     * @param listener Object to provide a context for drawing a dialog and to call back to when re-import is finished.
     */
    public static void reImportBooks(List<RBook> books, ReImporter.IReImportListener listener) {
        ReImporter.reImportBooks(books, listener);
    }

    /**
     * Delete {@code book} from Realm, along with any {@link RBookListItem}s which may exist for it, and its cover image
     * if it has one. Optionally delete the real file it was imported from as well.
     * @param book           Book to delete.
     * @param deleteRealFile If true, also delete the book's corresponding file.
     */
    public static void deleteBook(RBook book, boolean deleteRealFile) {
        deleteBooks(Lists.newArrayList(book), deleteRealFile);
    }

    /**
     * Delete {@code books} from Realm, along with any {@link RBookListItem}s which may exist for them, and their cover
     * images if they have them. Optionally delete the real files they were imported from as well.
     * @param books           Books to delete.
     * @param deleteRealFiles If true, also delete the books' corresponding files.
     */
    public static void deleteBooks(List<RBook> books, boolean deleteRealFiles) {
        // Null checks.
        if (books == null || books.isEmpty()) return;

        List<String> relPaths = new ArrayList<>(books.size());
        // Delete what we created.
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> {
                for (RBook book : books) {
                    // Delete any RBookListItems which may exist for these books.
                    tRealm.where(RBookListItem.class).contains("book.relPath", book.getRelPath()).findAll().clear();
                    // Get the relative path of the book, in case we wish to delete the real files too.
                    String relPath = book.getRelPath();
                    relPaths.add(relPath);
                    // Be sure to delete the cover file, if we have one.
                    if (book.hasCoverImage()) CoverHelper.get().deleteCoverImage(relPath);
                    // Delete the actual RBook from Realm.
                    book.removeFromRealm();
                }
            });
        }

        // If the user wants us to, also try to delete the corresponding files from the device.
        if (deleteRealFiles) {
            for (String relPath : relPaths) {
                File file = Util.getFileFromRelPath(relPath);
                if (file != null) //noinspection ResultOfMethodCallIgnored
                    file.delete();
            }
        }
    }

    /*
    List Actions.
     */

    /**
     * Create a new {@link RBookList}.
     * @param realm    Instance of Realm to use.
     * @param listName Name to use for new list. Assumed to not already be in use.
     */
    public static void createNewList(Realm realm, String listName) {
        if (listName == null || listName.isEmpty())
            throw new IllegalArgumentException("listName must be non-null and non-empty.");
        realm.executeTransaction(tRealm -> tRealm.copyToRealm(new RBookList(listName)));
    }

    /**
     * Create a new {@link RBookList} as a smart list.
     * @param realm          Instance of Realm to use.
     * @param listName       Name to use for new smart list. Assumed to not already be in use.
     * @param realmUserQuery RealmUserQuery to use to get a RealmUserQuery string to store in the list. Can be null.
     * @return The newly created and persisted smart list.
     */
    public static RBookList createNewSmartList(Realm realm, String listName, RealmUserQuery realmUserQuery) {
        if (listName == null || listName.isEmpty())
            throw new IllegalArgumentException("listName must be non-null and non-empty.");
        realm.beginTransaction();
        RBookList newSmartList = realm.copyToRealm(new RBookList(listName, realmUserQuery));
        realm.commitTransaction();
        return newSmartList;
    }

    /**
     * Update the RealmUserQuery string in a smart list.
     * @param realm     Instance of Realm to use.
     * @param list      List to update {@link RBookList#smartListRuqString} for. Must be configured as a smart list.
     * @param ruqString RealmUserQuery string to put into {@code list}. Can be empty or null.
     * @throws IllegalStateException if {@code list} isn't a smart list.
     */
    public static void updateSmartList(Realm realm, RBookList list, String ruqString) {
        if (list == null) return;
        if (!list.isSmartList()) throw new IllegalStateException("list is not a smart list.");
        realm.executeTransaction(tRealm -> list.setSmartListRuqString(ruqString));
    }

    /**
     * Deletes the {@code list}, and all of its {@link RBookListItem}s, from Realm.
     * @param realm Instance of Realm to use.
     * @param list  List to delete.
     */
    public static void deleteList(Realm realm, RBookList list) {
        if (list != null) {
            realm.executeTransaction(tRealm -> _deleteList(list));
        }
    }

    /**
     * Deletes the {@code lists}, and all of their {@link RBookListItem}s, from Realm.
     * @param realm Instance of Realm to use.
     * @param lists Lists to delete.
     */
    public static void deleteLists(Realm realm, List<RBookList> lists) {
        if (lists != null && !lists.isEmpty()) {
            realm.executeTransaction(tRealm -> {
                for (RBookList list : lists) _deleteList(list);
            });
        }
    }

    /**
     * Deletes all {@link RBookListItem}s owned by {@code list} from Realm, then deletes {@code list} from Realm.
     * <p>
     * This MUST be called while inside of a Realm transaction!
     * @param list List to delete.
     */
    private static void _deleteList(RBookList list) {
        // First, delete the list items (unless this is a smart list).
        if (!list.isSmartList()) list.getListItems().deleteAllFromRealm();
        // Then, delete the book list.
        list.removeFromRealm();
    }

    /**
     * Rename the {@code list} to {@code newName}, which is assumed to not already be in use.
     * @param realm   Instance of Realm to use.
     * @param list    List to rename.
     * @param newName New name for {@code list}.
     */
    public static void renameList(Realm realm, RBookList list, String newName) {
        if (newName == null || newName.isEmpty())
            throw new IllegalArgumentException("newName must be non-null and non-empty.");
        realm.executeTransaction(tRealm -> {
            list.setName(newName);
            list.setSortName(newName.toLowerCase()); // TODO Work-around
        });
    }
}
