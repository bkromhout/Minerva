package com.bkromhout.minerva.realm;

import com.bkromhout.minerva.C;
import com.bkromhout.rrvl.UIDModel;
import com.bkromhout.ruqus.Hide;
import com.bkromhout.ruqus.RealmUserQuery;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a book list in Realm.
 */
public class RBookList extends RealmObject implements UIDModel {
    /**
     * Book list name. This must be unique!
     */
    @PrimaryKey
    @Required
    public String name;
    /**
     * TODO This is a work-around until Realm can do case-insensitive sorting.
     * <p>
     * Same as {@link #name}, but in lower-case.
     */
    @Index
    @Hide
    public String sortName;
    /**
     * Position number for the next item to be added to this list.
     */
    @Hide
    public Long nextPos;
    /**
     * References to the {@link RBookListItem}s that this list contains.
     */
    @Hide
    public RealmList<RBookListItem> listItems;
    /**
     * Whether or not this list is a smart list.
     */
    @Hide
    public boolean isSmartList;
    /**
     * If {@link #isSmartList} is true, this will be the string representation of a {@link
     * com.bkromhout.ruqus.RealmUserQuery}.
     */
    @Hide
    public String smartListRuqString;

    /**
     * Create a default {@link RBookList}.
     * <p>
     * Note: There's nothing necessarily <i>bad</i> about using this constructor, but using one with parameters is still
     * a better choice.
     */
    public RBookList() {
        this.name = null;
        this.sortName = null;
        this.nextPos = 0L;
        this.listItems = null;
        this.isSmartList = false;
        this.smartListRuqString = null;
    }

    /**
     * Create a new {@link RBookList} with the given {@code name}.
     * @param name Name of the book list. This MUST be unique!
     */
    public RBookList(String name) {
        this.name = name;
        this.sortName = name.toLowerCase();
        this.nextPos = 0L;
        this.listItems = null;
        this.isSmartList = false;
        this.smartListRuqString = null;
    }

    /**
     * Create a new {@link RBookList} with the given {@code name} and the given {@code realmUserQuery}.
     * @param name           Name of the book list. This MUST be unique!
     * @param realmUserQuery Realm user query to use for the smart list. Can be null.
     */
    public RBookList(String name, RealmUserQuery realmUserQuery) {
        this.name = name;
        this.sortName = name.toLowerCase();
        this.nextPos = 0L;
        this.listItems = null;
        this.isSmartList = true;
        this.smartListRuqString = realmUserQuery == null ? null : realmUserQuery.toRuqString();
    }

    /**
     * Very simply throws an UnsupportedOperationException if {@link #isSmartList} is true.
     */
    public final void throwIfSmartList() {
        if (isSmartList) throw new UnsupportedOperationException("Not supported by smart lists.");
    }

    /**
     * Checks to see if {@code book} is already in this list.
     * @param book Book to check for.
     * @return True if {@code book} is in this list, otherwise false.
     */
    public boolean isBookInList(RBook book) {
        throwIfSmartList();
        return _isBookInList(book);
    }

    /**
     * Checks to see if {@code book} is already in this list.
     * @param book Book to check for.
     * @return True if {@code book} is in this list, otherwise false.
     */
    private boolean _isBookInList(RBook book) {
        return listItems.where()
                        .equalTo("owningList.name", name)
                        .equalTo("book.relPath", book.relPath)
                        .findFirst() != null;
    }

    /**
     * Add a single {@link RBook} to this list if it isn't already.
     * @param book Book to add to this list.
     */
    public void addBook(RBook book) {
        throwIfSmartList();
        // Don't re-add the book if it's already in this list.
        if (isBookInList(book)) return;

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> listItems.add(new RBookListItem(this, book)));
        }
    }

    /**
     * Adds multiple {@link RBook}s to this list, ignoring any which are already in it.
     * @param books Books to add to this list.
     */
    public void addBooks(Iterable<RBook> books) {
        throwIfSmartList();
        // Create a list of RBookListItems from books, ignoring any RBooks which are already in this list.
        List<RBookListItem> newItems = booksToBookListItems(books);

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> listItems.addAll(newItems));
        }
    }

    /**
     * Convert a list of {@link RBook}s to a list of {@link RBookListItem}s.
     * @param books Books to convert to {@link RBookListItem}s.
     * @return List of {@link RBookListItem}s.
     */
    private List<RBookListItem> booksToBookListItems(Iterable<RBook> books) {
        return Observable.from(books)
                         .filter(book -> !_isBookInList(book))
                         .map(book -> new RBookListItem(this, book))
                         .toList()
                         .toBlocking()
                         .single();
    }

    /**
     * Removes multiple {@link RBook}s from this list.
     * @param books Books to remove from this list.
     */
    public void removeBooks(Iterable<RBook> books) {
        throwIfSmartList();
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> {
                // Delete the book list items.
                for (RBook book : books)
                    listItems.where().equalTo("book.relPath", book.relPath).findFirst().deleteFromRealm();
            });
        }
    }

    /**
     * If this is a smart list, convert it to a normal list by getting the RealmUserQuery and adding the items that it
     * returns when it is executed. If any books are included in the query multiple times, they will only be added
     * once.
     */
    public void convertToNormalList() {
        // If this isn't a smart list, do nothing.
        if (!isSmartList) return;
        try (Realm realm = Realm.getDefaultInstance()) {
            // If this is a smart list but it doesn't actually have a query, just toggle the boolean.
            // If there is a query, we'll need to actually add the items, which means we need to get a
            // RealmUserQuery first.
            if (smartListRuqString == null || smartListRuqString.isEmpty())
                realm.executeTransaction(tRealm -> isSmartList = false);
            else convertToNormalListUsingRuq(realm, new RealmUserQuery(smartListRuqString));
        }
    }

    /**
     * Convert this smart list to a normal list using the given RealmUserQuery. If any books are included in the query
     * multiple times, they will only be added once.
     * @param ruq The RealmUserQuery from which to get the books to add to this list.
     */
    public void convertToNormalListUsingRuq(Realm realm, RealmUserQuery ruq) {
        // Don't allow this to occur unless this list is a smart list currently.
        if (!isSmartList) return;
        if (ruq == null || !ruq.isQueryValid()) throw new IllegalArgumentException("ruq must be non-null and valid.");

        // Create RBookListItems from RBooks returned by query.
        List<RBookListItem> bookListItems = booksToBookListItems(ruq.execute(realm));
        realm.executeTransaction(tRealm -> {
            // Turn this into a normal list.
            isSmartList = false;
            listItems.addAll(bookListItems);
            smartListRuqString = null;
        });
    }

    /**
     * Reset the positions of the given list's items so that they are spaced evenly using the standard position gap
     * (which can be found at {@link com.bkromhout.minerva.C#LIST_ITEM_GAP}).
     * <p>
     * Note that this can take a bit, since it must iterate through all items in the list.
     * <p>
     * TODO At some point, we'll have a task that runs once per day to reset the positions for all items in all lists.
     */
    public void resetPositions() {
        throwIfSmartList();
        // Get the list's items in their current position-based order, but put them into an ArrayList instead of
        // using the RealmResults. By doing this we prevent the possibility of bugs which could be caused by items
        // being rearranged in the RealmResults as we update their positions.
        ArrayList<RBookListItem> orderedItems = new ArrayList<>(listItems.where().findAllSorted("pos"));

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> {
                // Start nextPos back at 0.
                nextPos = 0L;
                // Loop through the items and set their new position values, then increment nextPos.
                for (RBookListItem listItem : orderedItems) {
                    listItem.pos = nextPos;
                    nextPos += C.LIST_ITEM_GAP;
                }
            });
        }
    }

    @Override
    public Object getUID() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RBookList)) return false;

        RBookList rBookList = (RBookList) o;

        return name.equals(rBookList.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
