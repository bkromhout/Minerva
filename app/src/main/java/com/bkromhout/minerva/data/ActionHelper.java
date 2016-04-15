package com.bkromhout.minerva.data;

import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.realm.RBookListItem;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.ruqus.RealmUserQuery;
import com.google.common.collect.Lists;
import io.realm.Realm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Convenience class which provides static methods to execute actions, preventing the need for duplicate code.
 */
public class ActionHelper {
    /*
    Book Actions.
     */

    public static void addBookToList(Realm realm, RBook book, String listName) {
        addBooksToList(realm, Lists.newArrayList(book), listName);
    }

    public static void addBooksToList(Realm realm, List<RBook> books, String listName) {
        realm.where(RBookList.class)
             .equalTo("name", listName)
             .findFirst()
             .addBooks(books);
    }

    public static void rateBook(Realm realm, RBook book, int rating) {
        rateBooks(realm, Lists.newArrayList(book), rating);
    }

    public static void rateBooks(Realm realm, List<RBook> books, int rating) {
        realm.executeTransaction(tRealm -> {
            for (RBook book : books) book.setRating(rating);
        });
    }

    public static void reImportBook(RBook book, ReImporter.IReImportListener listener) {
        reImportBooks(Lists.newArrayList(book), listener);
    }

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

    public static void createNewList(Realm realm, String listName) {
        realm.executeTransaction(tRealm -> tRealm.copyToRealm(new RBookList(listName)));
    }

    public static RBookList createNewSmartList(Realm realm, String listName, RealmUserQuery realmUserQuery) {
        realm.beginTransaction();
        RBookList newSmartList = realm.copyToRealm(new RBookList(listName, realmUserQuery));
        realm.commitTransaction();
        return newSmartList;
    }

    public static void updateSmartList(Realm realm, RBookList list, String ruqString) {
        realm.executeTransaction(tRealm -> list.setSmartListRuqString(ruqString));
    }

    /**
     * Deletes the {@code list}, and all of its {@link RBookListItem}s, from Realm.
     * @param realm Instance of Realm to use.
     * @param list  List to delete.
     */
    public static void deleteList(Realm realm, RBookList list) {
        if (list != null) {
            realm.executeTransaction(tRealm -> {
                // First, delete the list items (unless this is a smart list).
                if (!list.isSmartList()) list.getListItems().deleteAllFromRealm();
                // Then, delete the book list.
                list.removeFromRealm();
            });
        }
    }

    public static void renameList(Realm realm, RBookList list, String newName) {
        realm.executeTransaction(tRealm -> {
            list.setName(newName);
            list.setSortName(newName.toLowerCase()); // TODO Work-around
        });
    }
}
