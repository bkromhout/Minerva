package com.bkromhout.minerva.data;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.ColorInt;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.webkit.MimeTypeMap;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.activities.TaggingActivity.TaggingHelper;
import com.bkromhout.minerva.enums.MarkType;
import com.bkromhout.minerva.prefs.DefaultPrefs;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookList;
import com.bkromhout.minerva.realm.RBookListItem;
import com.bkromhout.minerva.realm.RTag;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.ruqus.RealmUserQuery;
import com.google.common.collect.Lists;
import io.realm.Realm;
import io.realm.RealmResults;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Convenience class which provides static methods to execute actions, preventing the need for duplicate code.
 */
public class ActionHelper {
    /*
     * Book Actions.
     */

    /**
     * Add {@code book} to the list named {@code listName}.
     * @param realm    Instance of Realm to use.
     * @param book     Book to add to list called {@code listName}.
     * @param listName Name of list to add {@code book} to.
     */
    public static void addBookToList(Realm realm, RBook book, String listName) {
        realm.where(RBookList.class)
             .equalTo("name", listName)
             .findFirst()
             .addBook(book);
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
            for (RBook book : books) book.rating = rating;
        });
    }

    /**
     * Adds the {@code tags} to the {@code books}.
     * @param books List of {@link RBook}s to add tags to.
     * @param tags  List {@link RTag}s.
     */
    public static void addTagsToBooks(List<RBook> books, List<RTag> tags) {
        if (books == null || tags == null) throw new IllegalArgumentException("No nulls allowed.");
        if (books.isEmpty() || tags.isEmpty()) return;

        // Get names of new/updated book tags.
        String newBookTagName = DefaultPrefs.get().getNewBookTag(null);
        String updatedBookTagName = DefaultPrefs.get().getUpdatedBookTag(null);

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();
            // Loop through books and add tags to them.
            for (int i = books.size() - 1; i >= 0; i--) {
                RBook book = books.get(i);
                for (RTag tag : tags) {
                    // If the book doesn't already have the tag,
                    if (!book.tags.contains(tag)) {
                        // add the tag to the book,
                        book.tags.add(tag);
                        // and add the book to the tag.
                        tag.taggedBooks.add(book);
                    }
                    // Make sure that we set new/updated state to true if those tags were added (and it wasn't already).
                    if (newBookTagName != null && newBookTagName.equals(tag.name) && !book.isNew)
                        book.isNew = true;
                    if (updatedBookTagName != null && updatedBookTagName.equals(tag.name) && !book.isUpdated)
                        book.isUpdated = true;
                }
            }
            realm.commitTransaction();
        }
    }

    /**
     * Removes the {@code tags} from the {@code books}.
     * @param books List of {@link RBook}s to remove tags from.
     * @param tags  List {@link RTag}s.
     */
    public static void removeTagsFromBooks(List<RBook> books, List<RTag> tags) {
        if (books == null || tags == null) throw new IllegalArgumentException("No nulls allowed.");
        if (books.isEmpty() || tags.isEmpty()) return;

        // Get names of new/updated book tags.
        String newBookTagName = DefaultPrefs.get().getNewBookTag(null);
        String updatedBookTagName = DefaultPrefs.get().getUpdatedBookTag(null);

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();
            // Loop through books and remove tags from them.
            for (int i = books.size() - 1; i >= 0; i--) {
                RBook book = books.get(i);
                for (RTag tag : tags) {
                    // If the book has the tag, remove it,
                    if (book.tags.remove(tag)) {
                        // and remove the book from the tag.
                        tag.taggedBooks.remove(book);
                    }
                    // Make sure that we new/updated state if we had those tags removed.
                    if (newBookTagName != null && newBookTagName.equals(tag.name)) book.isNew = false;
                    if (updatedBookTagName != null && updatedBookTagName.equals(tag.name)) book.isUpdated = false;
                }
            }
            realm.commitTransaction();
        }
    }

    /**
     * Replaces an {@link RTag} whose name is {@code oldTagName} with an {@link RTag} whose name is {@code newTagName}
     * on any {@link RBook}s for which the given {@code markType} is set to {@code true}.
     * <p>
     * Any books already tagged with {@code newTagName} but whose value for the given {@code markType} isn't set to
     * {@code true} will be changed so that it is set to {@code true} in order to stay in sync.
     * @param markType   The mark whose associated tag is being replaced.
     * @param oldTagName Name of the tag to replace.
     * @param newTagName Name of the replacement tag.
     */
    public static void replaceMarkTagOnBooks(MarkType markType, String oldTagName, String newTagName) {
        // At least one of the tag names can't be null.
        if (oldTagName == null && newTagName == null)
            throw new IllegalArgumentException("At least one of oldTagName, newTagName must not be null.");

        try (Realm realm = Realm.getDefaultInstance()) {
            // Get a list of books whose given mark type is set to true.
            RealmResults<RBook> books = realm.where(RBook.class)
                                             .equalTo(markType.getFieldName(), true)
                                             .findAll();

            // Get tags.
            RTag oldTag = oldTagName != null ? realm.where(RTag.class).equalTo("name", oldTagName).findFirst() : null;
            RTag newTag = newTagName != null ? realm.where(RTag.class).equalTo("name", newTagName).findFirst() : null;

            realm.beginTransaction();
            // Loop through the books whose given mark type is set to true.
            for (RBook book : books) {
                // First, remove the old tag (if there is one).
                if (oldTag != null) {
                    book.tags.remove(oldTag);
                    oldTag.taggedBooks.remove(book);
                }

                // Then, add the new tag (if there is one, and if it isn't already).
                if (newTag != null && !book.tags.contains(newTag)) {
                    book.tags.add(newTag);
                    newTag.taggedBooks.add(book);
                }
            }

            // If we have a new tag, update the given mark's value on books already tagged with the new tag but not
            // marked accordingly so that we stay in sync.
            if (newTag != null) {
                // Get a list of such books.
                books = newTag.taggedBooks.where()
                                          .equalTo(markType.getFieldName(), false)
                                          .findAll();

                // Loop backwards over list and set given mark's value to true.
                for (int i = books.size() - 1; i >= 0; i--) {
                    if (markType == MarkType.NEW) books.get(i).isNew = true;
                    else if (markType == MarkType.UPDATED) books.get(i).isUpdated = true;
                }
            }

            realm.commitTransaction();
        }
    }

    /**
     * Mark the given {@code book}.
     * @param book     {@link RBook} to mark.
     * @param markType Type of mark.
     * @param marked   Whether the mark should be true or false.
     */
    public static void markBook(RBook book, MarkType markType, boolean marked) {
        markBooks(Lists.newArrayList(book), markType, marked);
    }

    /**
     * Mark the given {@code books}.
     * @param books    List of {@link RBook}s to mark.
     * @param markType Type of mark.
     * @param marked   Whether the mark should be true or false.
     */
    public static void markBooks(List<RBook> books, MarkType markType, boolean marked) {
        // Get the associated tag's name, or null if there isn't one.
        String tagName = markType.getTagName();

        try (Realm realm = Realm.getDefaultInstance()) {
            // If we have an associated tag, we can just use our add/remove tags methods to help us do this, since they
            // handle updating the mark for us as well.
            if (tagName != null) {
                RTag tag = realm.where(RTag.class).equalTo("name", tagName).findFirst();
                if (marked) addTagsToBooks(books, Lists.newArrayList(tag));
                else removeTagsFromBooks(books, Lists.newArrayList(tag));
                return;
            }

            // Otherwise, loop through the books and just set the mark's new value.
            realm.beginTransaction();
            for (RBook book : books) {
                if (markType == MarkType.NEW) book.isNew = marked;
                else if (markType == MarkType.UPDATED) book.isUpdated = marked;
            }
            realm.commitTransaction();
        }
    }

    /**
     * Open the book's real file in an application which will allow the user to read it. This will also put the book at
     * the top of the recents list.
     * @param book    Book to open.
     * @param context The context to use to open the file.
     */
    public static void openBookUsingIntent(RBook book, Context context) {
        if (!Util.checkForStoragePermAndFireEventIfNeeded()) return;
        File file = Util.getFileFromRelPath(book.relPath);

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
                book.lastReadDate = Calendar.getInstance().getTime();
                book.isInRecents = true;
            });
        } catch (ActivityNotFoundException e) {
            // Tell the user there aren't any apps which advertise the ability to handle the book's file type.
            SnackKiosk.snack(R.string.sb_err_no_apps, R.string.dismiss, Snackbar.LENGTH_LONG);
        }
    }

    /**
     * Begin re-import process for the given {@code book}.
     * @param book Book to re-import.
     */
    public static void reImportBook(RBook book) {
        reImportBooks(Lists.newArrayList(book));
    }

    /**
     * Begin re-import process for the given {@code books}.
     * <p>
     * Checks if we have permission first, and if we don't then will trigger a visual indication to the user that we
     * need the permission.
     * @param books Books to re-import.
     */
    public static void reImportBooks(List<RBook> books) {
        if (!Util.checkForStoragePermAndFireEventIfNeeded()) return;
        Importer.get().queueReImport(books);
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

        // If deleteRealFiles is true, check permissions before doing anything.
        if (deleteRealFiles && !Util.checkForStoragePermAndFireEventIfNeeded()) return;

        List<String> relPaths = new ArrayList<>(books.size());
        // Delete what we created.
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> {
                for (RBook book : books) {
                    // Delete any RBookListItems which may exist for these books.
                    tRealm.where(RBookListItem.class)
                          .contains("book.relPath", book.relPath)
                          .findAll()
                          .deleteAllFromRealm();
                    // Get the relative path of the book, in case we wish to delete the real files too.
                    String relPath = book.relPath;
                    relPaths.add(relPath);
                    // Be sure to delete the cover file, if we have one.
                    if (book.hasCoverImage) DataUtils.deleteCoverImage(relPath);
                    // Delete the actual RBook from Realm.
                    book.deleteFromRealm();
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
     * List Actions.
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
        if (!list.isSmartList) throw new IllegalStateException("list is not a smart list.");
        realm.executeTransaction(tRealm -> list.smartListRuqString = ruqString);
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
        if (!list.isSmartList) list.listItems.deleteAllFromRealm();
        // Then, delete the book list.
        list.deleteFromRealm();
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
            list.name = newName;
            list.sortName = newName.toLowerCase();
        });
    }

    /**
     * Moves the given {@link RBookListItem}s to the start of {@code bookList}.
     * @param bookList    Book list which owns {@code itemsToMove}.
     * @param itemsToMove Items to move. Items must already exist in Realm and be owned by {@code bookList}.
     */
    public static void moveItemsToStart(RBookList bookList, List<RBookListItem> itemsToMove) {
        bookList.throwIfSmartList();
        if (itemsToMove == null || itemsToMove.isEmpty()) return;
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> {
                // Get the next first open position.
                Long nextFirstPos = bookList.listItems.where().min("pos").longValue() - C.LIST_ITEM_GAP;
                // Loop through itemsToMove backwards and move those items to the start of this list.
                for (int i = itemsToMove.size() - 1; i >= 0; i--) {
                    itemsToMove.get(i).pos = nextFirstPos;
                    nextFirstPos -= C.LIST_ITEM_GAP;
                }
            });
        }
    }

    /**
     * Moves the given {@link RBookListItem}s to the end of {@code bookList}.
     * @param bookList    Book list which owns {@code itemsToMove}.
     * @param itemsToMove Items to move. Items must already exist in Realm and be owned by {@code bookList}.
     */
    public static void moveItemsToEnd(RBookList bookList, List<RBookListItem> itemsToMove) {
        bookList.throwIfSmartList();
        if (itemsToMove == null || itemsToMove.isEmpty()) return;
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> {
                // Get the next last open position.
                Long nextLastPos = bookList.nextPos;
                // Loop through itemsToMove and move those items to the end of this list.
                for (RBookListItem item : itemsToMove) {
                    item.pos = nextLastPos;
                    nextLastPos += C.LIST_ITEM_GAP;
                }
                // Set bookList's nextPos.
                bookList.nextPos = nextLastPos;
            });
        }
    }

    /*
     * Tag Actions.
     */

    /**
     * Create a new {@link RTag}.
     * @param realm   Instance of Realm to use.
     * @param tagName Name of new tag.
     */
    public static void createNewTag(Realm realm, String tagName) {
        realm.executeTransaction(tRealm -> tRealm.copyToRealm(new RTag(tagName)));
    }

    /**
     * Set the given {@code tag}'s {@code textColor}.
     * @param realm     Instance of Realm to use.
     * @param tag       {@link RTag} to assign new text color to.
     * @param textColor New text color.
     */
    public static void setTagTextColor(Realm realm, RTag tag, @ColorInt int textColor) {
        realm.executeTransaction(tRealm -> tag.textColor = textColor);
    }

    /**
     * Set the given {@code tag}'s {@code bgColor}.
     * @param realm   Instance of Realm to use.
     * @param tag     {@link RTag} to assign new background color to.
     * @param bgColor New background color.
     */
    public static void setTagBgColor(Realm realm, RTag tag, @ColorInt int bgColor) {
        realm.executeTransaction(tRealm -> tag.bgColor = bgColor);
    }

    /**
     * Rename the given {@code tag}.
     * @param realm   Instance of Realm to use.
     * @param tag     {@link RTag} to rename.
     * @param newName New tag name.
     */
    public static void renameTag(Realm realm, RTag tag, String newName) {
        String oldName = tag.name;
        // Name is available; rename the RTag.
        realm.executeTransaction(tRealm -> {
            tag.name = newName;
            tag.sortName = newName.toLowerCase();
        });

        // Now make sure that we swap the old name for the new one in the old/new lists.
        TaggingHelper th = TaggingHelper.get();
        if (th.oldCheckedItems.remove(oldName)) th.oldCheckedItems.add(newName);
        if (th.oldPartiallyCheckedItems.remove(oldName)) th.oldPartiallyCheckedItems.add(newName);
        if (th.newCheckedItems.remove(oldName)) th.newCheckedItems.add(newName);
        if (th.newPartiallyCheckedItems.remove(oldName)) th.newPartiallyCheckedItems.add(newName);
    }

    /**
     * Delete the given {@code tag}.
     * @param realm Instance of Realm to use.
     * @param tag   {@link RTag} to delete.
     */
    public static void deleteTag(Realm realm, RTag tag) {
        TaggingHelper th = TaggingHelper.get();
        // Indicate that we might need an explicit update.
        th.markForExplicitUpdateIfNecessary();

        // Delete the tag from Realm.
        String tagName = tag.name;
        realm.executeTransaction(tRealm -> tag.deleteFromRealm());

        // If this was one of the new/updated book tags, be sure that we null that preference's value.
        String prefVal = DefaultPrefs.get().getNewBookTag(null);
        if (prefVal != null && prefVal.equals(tagName)) DefaultPrefs.get().putNewBookTag(null);
        prefVal = DefaultPrefs.get().getUpdatedBookTag(null);
        if (prefVal != null && prefVal.equals(tagName)) DefaultPrefs.get().putUpdatedBookTag(null);

        // Remove tag name from the lists (if present).
        th.oldCheckedItems.remove(tagName);
        th.oldPartiallyCheckedItems.remove(tagName);
        th.newCheckedItems.remove(tagName);
        th.newPartiallyCheckedItems.remove(tagName);
    }
}
