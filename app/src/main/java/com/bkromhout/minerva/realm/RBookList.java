package com.bkromhout.minerva.realm;

import com.bkromhout.minerva.C;
import com.bkromhout.ruqus.Hide;
import com.bkromhout.ruqus.RealmUserQuery;
import com.google.common.math.LongMath;
import io.realm.*;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a book list in Realm.
 */
public class RBookList extends RealmObject {
    /**
     * Book list name. This must be unique!
     */
    @PrimaryKey
    private String name;
    /**
     * TODO This is a work-around until Realm can do case-insensitive sorting.
     * <p>
     * Same as {@link #name}, but in lower-case.
     */
    @Index
    @Hide
    private String sortName;
    /**
     * Position number for the next item to be added to this list.
     */
    @Hide
    private Long nextPos;
    /**
     * References to the {@link RBookListItem}s that this list contains.
     */
    @Hide
    private RealmList<RBookListItem> listItems;
    /**
     * Whether or not this list is a smart list.
     */
    @Hide
    private boolean isSmartList;
    /**
     * If {@link #isSmartList} is true, this will be the string representation of a {@link
     * com.bkromhout.ruqus.RealmUserQuery}.
     */
    @Hide
    private String smartListRuqString;

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
    private void throwIfSmartList() {
        if (isSmartList) throw new UnsupportedOperationException("Not supported by smart lists.");
    }

    /**
     * Checks to see if {@code book} is already in this list.
     * @param book Book to check for.
     * @return True if {@code book} is in this list, otherwise false.
     */
    public boolean isBookInList(RBook book) {
        throwIfSmartList();
        return getListItems()
                .where()
                .equalTo("key", RBookListItem.makeBookListItemKey(name, book.getRelPath()))
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
            realm.executeTransaction(tRealm -> getListItems().add(new RBookListItem(this, book)));
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
                         .filter(book -> !isBookInList(book))
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
                    listItems.where().equalTo("book.relPath", book.getRelPath()).findFirst().removeFromRealm();
            });
        }
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
                    listItem.setPos(nextPos);
                    nextPos += C.LIST_ITEM_GAP;
                }
            });
        }
    }

    /**
     * Moves the given {@link RBookListItem}s to the start of this list.
     * @param itemsToMove Items to move. Items must already exist in Realm and be from this list.
     */
    public void moveItemsToStart(List<RBookListItem> itemsToMove) {
        throwIfSmartList();
        if (itemsToMove == null || itemsToMove.isEmpty()) return;
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> {
                // Get the next first open position.
                Long nextFirstPos = getListItems().where().findAllSorted("pos").first().getPos() - C.LIST_ITEM_GAP;
                // Loop through itemsToMove backwards and move those items to the start of this list.
                for (int i = itemsToMove.size() - 1; i >= 0; i--) {
                    itemsToMove.get(i).setPos(nextFirstPos);
                    nextFirstPos -= C.LIST_ITEM_GAP;
                }
            });
        }
    }

    /**
     * Moves the given {@link RBookListItem}s to the end of this list.
     * @param itemsToMove Items to move. Items must already exist in Realm and be from this list.
     */
    public void moveItemsToEnd(List<RBookListItem> itemsToMove) {
        throwIfSmartList();
        if (itemsToMove == null || itemsToMove.isEmpty()) return;
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> {
                // Get the next last open position.
                Long nextLastPos = getNextPos();
                // Loop through itemsToMove and move those items to the end of this list.
                for (RBookListItem item : itemsToMove) {
                    item.setPos(nextLastPos);
                    nextLastPos += C.LIST_ITEM_GAP;
                }
            });
        }
    }

    /**
     * Moves {@code itemToMove} to between {@code item1} and {@code item2} in this list. If {@code item1} and {@code
     * item2} aren't consecutive items, behavior is undefined.
     * <p>
     * If {@code itemToMove} is the same as either {@code item1} or {@code item2} then this does nothing.<br/>If {@code
     * item1} is {@code null}, then {@code itemToMove} will be put after {@code item1} with the standard position
     * gap.<br/>If {@code item2} is null, then {@code itemToMove} will be put before {@code item2} with the standard
     * position gap.
     * <p>
     * Please note that passing {@code null} for one of the items assumes that the non-null item is either the first (if
     * it's {@code item2}), or the last (if it's {@code item1}) item in this list. If this isn't the case, you'll likely
     * end up with multiple items in the same position!
     * <p>
     * If there's no space between {@code item1} and {@code item2}, the whole list will have its items re-spaced before
     * moving the item.
     * <p>
     * The current spacing gap can be found at {@link com.bkromhout.minerva.C#LIST_ITEM_GAP}.
     * @param itemToMove The item which is being moved.
     * @param item1      The item which will now precede {@code itemToMove}.
     * @param item2      The item which will now follow {@code itemToMove}.
     */
    public void moveItemToBetween(RBookListItem itemToMove, RBookListItem item1, RBookListItem item2) {
        throwIfSmartList();
        if (itemToMove == null || (item1 == null && item2 == null))
            throw new IllegalArgumentException("itemToMove, or both of item1 and item2 are null.");

        // Check if itemToMove is the same as either item1 or item2.
        if ((item1 != null && itemToMove.equals(item1)) || (item2 != null && itemToMove.equals(item2))) return;

        // Try to find the new position for the item, and make sure we didn't get a null back.
        Long newPos = findMiddlePos(item1, item2);
        if (newPos == null) {
            // If newPos is null, we need to re-sort the items before moving itemToMove.
            resetPositions();
            newPos = findMiddlePos(item1, item2);
            if (newPos == null)
                throw new IllegalArgumentException("Couldn't find space between item1 and item2 after re-spacing");
        }

        // Get Realm, update itemToMove, then close Realm.
        try (Realm realm = Realm.getDefaultInstance()) {
            final Long finalNewPos = newPos;
            realm.executeTransaction(tRealm -> itemToMove.setPos(finalNewPos));
        }
    }

    /**
     * Find the position number that is between the two given items. If there are no positions between the items, {@code
     * null} is returned. If {@code item1} and {@code item2} aren't consecutive items, this will potentially result in
     * the returned position already being taken.
     * <p>
     * {@code null} can be passed for ONE of {@code item1} or {@code item2}:<br/>If {@code item1} is null, the number
     * returned will be {@code item1.getPos() + gap}<br/>If {@code item2} is null, the number returned will be {@code
     * item2.getPos() - gap}.<br/>(The current spacing gap can be found at
     * {@link com.bkromhout.minerva.C#LIST_ITEM_GAP}.)
     * <p>
     * Please note that passing {@code null} for one of the items assumes that the non-null item is either the first (if
     * it's {@code item2}), or the last (if it's {@code item1}) item in this list. If this isn't the case, the returned
     * position might already be taken!
     * @param item1 The earlier item (which the returned position will follow).
     * @param item2 The later item (which the returned position will precede).
     * @return The position number between the two items, or {@code null} if there's no space between the items.
     */
    public Long findMiddlePos(RBookListItem item1, RBookListItem item2) {
        throwIfSmartList();
        // Handle nulls which should throw IllegalArgumentException.
        if (item1 == null && item2 == null) throw new IllegalArgumentException("Null list or both items are null.");

        // Handle acceptable nulls.
        if (item1 == null) return LongMath.checkedSubtract(item2.getPos(), C.LIST_ITEM_GAP);
        if (item2 == null) return LongMath.checkedAdd(item1.getPos(), C.LIST_ITEM_GAP);

        // Get positions, make sure that item2 doesn't precede item1 and isn't in the same position as item1.
        Long p1 = item1.getPos(), p2 = item2.getPos();
        if (p2 <= p1) throw new IllegalArgumentException("item2 was before or at the same position as item1.");

        // Calculate middle.
        Long pos = LongMath.mean(p1, p2);

        // Make sure there isn't an item in the calculated position. If there is, return null.
        return getListItems().where().equalTo("pos", pos).findFirst() == null ? pos : null;
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
                realm.executeTransaction(tRealm -> setSmartList(false));
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
            setSmartList(false);
            getListItems().addAll(bookListItems);
            setSmartListRuqString(null);
        });
    }

    /*
     * Static methods to help with drag and drop in the adapter.
     */

    /**
     * Swaps the positions of {@code item1} and {@code item2}. Will do nothing if the items are the same.
     * @param item1Key An item's key.
     * @param item2Key Another item's key.
     * @throws IllegalArgumentException if either item is null or items aren't from the same list.
     */
    public static void swapItemPositions(String item1Key, String item2Key) {
        if (item1Key == null || item2Key == null) throw new IllegalArgumentException("No nulls allowed.");
        if (!RBookListItem.areFromSameList(item1Key, item2Key)) throw new IllegalArgumentException(
                "Items must be part of the same list.");

        try (Realm realm = Realm.getDefaultInstance()) {
            RBookListItem innerItem1 = realm.where(RBookListItem.class).equalTo("key", item1Key).findFirst();
            RBookListItem innerItem2 = realm.where(RBookListItem.class).equalTo("key", item2Key).findFirst();

            realm.beginTransaction();
            // Swap the positions.
            Long temp = innerItem1.getPos();
            innerItem1.setPos(innerItem2.getPos());
            innerItem2.setPos(temp);
            realm.commitTransaction();
        }
    }

    /**
     * Swaps the positions of {@code item1} and {@code item2}. Will do nothing if the items are the same.
     * @param item1 An item.
     * @param item2 Another item.
     * @throws IllegalArgumentException if either item is null or items aren't from the same list.
     */
    public static void swapItemPositions(RBookListItem item1, RBookListItem item2) {
        if (item1 == null || item2 == null) throw new IllegalArgumentException("No nulls allowed.");
        if (!RBookListItem.areFromSameList(item1.getKey(), item2.getKey())) throw new IllegalArgumentException(
                "Items must be part of the same list.");

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();
            // Swap the positions.
            Long temp = item1.getPos();
            item1.setPos(item2.getPos());
            item2.setPos(temp);
            realm.commitTransaction();
        }
    }

    /**
     * Moves the {@link RBookListItem} whose key is {@code itemToMoveKey} to somewhere before the {@link RBookListItem}
     * whose key is {@code targetItemKey}.
     * @param itemToMoveKey Key of item to move.
     * @param targetItemKey Key of item which item whose key is {@code itemToMove} will be moved before.
     */
    public static void moveItemToBefore(String itemToMoveKey, String targetItemKey) {
        try (Realm realm = Realm.getDefaultInstance()) {
            moveItemToBefore(realm.where(RBookListItem.class).equalTo("key", itemToMoveKey).findFirst(),
                    realm.where(RBookListItem.class).equalTo("key", targetItemKey).findFirst());
        }
    }

    /**
     * Moves {@code itemToMove} to somewhere before {@code targetItem}.
     * @param itemToMove Item to move.
     * @param targetItem Item which {@code itemToMove} will be moved before.
     */
    public static void moveItemToBefore(RBookListItem itemToMove, RBookListItem targetItem) {
        if (itemToMove == null || targetItem == null) throw new IllegalArgumentException("Neither item may be null.");
        if (itemToMove.getUniqueId() == targetItem.getUniqueId()) return;

        RBookList bookList = targetItem.getOwningList();
        // Get the items which come before targetItem.
        RealmResults<RBookListItem> beforeTarget = bookList.getListItems()
                                                           .where()
                                                           .lessThan("pos", targetItem.getPos())
                                                           .findAllSorted("pos", Sort.DESCENDING);

        // Move itemToMove to between beforeTarget.first()/null and targetItem.
        bookList.moveItemToBetween(itemToMove, beforeTarget.isEmpty() ? null : beforeTarget.first(), targetItem);
    }

    /**
     * Moves the {@link RBookListItem} whose key is {@code itemToMoveKey} to somewhere after the {@link RBookListItem}
     * whose key is {@code targetItemKey}.
     * @param itemToMoveKey Key of item to move.
     * @param targetItemKey Key of item which item whose key is {@code itemToMove} will be moved after.
     */
    public static void moveItemToAfter(String itemToMoveKey, String targetItemKey) {
        try (Realm realm = Realm.getDefaultInstance()) {
            moveItemToAfter(realm.where(RBookListItem.class).equalTo("key", itemToMoveKey).findFirst(),
                    realm.where(RBookListItem.class).equalTo("key", targetItemKey).findFirst());
        }
    }

    /**
     * Moves {@code itemToMove} to somewhere after {@code targetItem}.
     * @param itemToMove Item to move.
     * @param targetItem Item which {@code itemToMove} will be moved after.
     */
    public static void moveItemToAfter(RBookListItem itemToMove, RBookListItem targetItem) {
        if (itemToMove == null || targetItem == null) throw new IllegalArgumentException("Neither item may be null.");
        if (itemToMove.getUniqueId() == targetItem.getUniqueId()) return;

        RBookList bookList = targetItem.getOwningList();
        // Get the items which come after targetItem.
        RealmResults<RBookListItem> afterTarget = bookList.getListItems()
                                                          .where()
                                                          .greaterThan("pos", targetItem.getPos())
                                                          .findAllSorted("pos");

        // Move itemToMove to between targetItem and afterTarget.first()/null.
        bookList.moveItemToBetween(itemToMove, targetItem, afterTarget.isEmpty() ? null : afterTarget.first());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSortName() {
        return sortName;
    }

    public void setSortName(String sortName) {
        this.sortName = sortName;
    }

    public Long getNextPos() {
        return nextPos;
    }

    public void setNextPos(Long nextPos) {
        this.nextPos = nextPos;
    }

    public RealmList<RBookListItem> getListItems() {
        return listItems;
    }

    public void setListItems(RealmList<RBookListItem> listItems) {
        this.listItems = listItems;
    }

    public boolean isSmartList() {
        return isSmartList;
    }

    public void setSmartList(boolean smartList) {
        isSmartList = smartList;
    }

    public String getSmartListRuqString() {
        return smartListRuqString;
    }

    public void setSmartListRuqString(String smartListRuqString) {
        this.smartListRuqString = smartListRuqString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RBookList)) return false;

        RBookList rBookList = (RBookList) o;

        return getName().equals(rBookList.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
