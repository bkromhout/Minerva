package com.bkromhout.minerva.realm;

import com.bkromhout.minerva.C;
import com.google.common.math.LongMath;
import io.realm.*;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import rx.Observable;

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
    private String sortName;
    /**
     * Position number for the next item to be added to this list.
     */
    private Long nextPos;
    /**
     * References to the {@link RBookListItem}s that this list contains.
     */
    private RealmList<RBookListItem> listItems;

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
    }

    /**
     * Checks to see if {@code book} is already in {@code list}.
     * @param list List to check.
     * @param book Book to check for.
     * @return True if {@code book} is in {@code list}, otherwise false.
     */
    public static boolean isBookInList(RBookList list, RBook book) {
        return list.getListItems()
                   .where()
                   .equalTo("key", RBookListItem.makeBookListItemKey(list.getName(), book.getRelPath()))
                   .findFirst() != null;
    }

    /**
     * Add a single {@link RBook} to an {@link RBookList}.
     * <p>
     * Won't add {@code book} again if it's already in {@code list}.
     * @param list List to add {@code book} to.
     * @param book Book to add to {@code list}.
     */
    public static void addBook(RBookList list, RBook book) {
        // Don't re-add the book if it's already in the list.
        if (isBookInList(list, book)) return;

        RBookListItem newItem = new RBookListItem(list, book);
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> list.getListItems().add(newItem));
        }
    }

    /**
     * Adds multiple {@link RBook}s to an {@link RBookList}.
     * <p>
     * If any of the books are already in {@code list}, they won't be added again.
     * @param list  List to add {@code books} to.
     * @param books Books to add to {@code list}.
     */
    public static void addBooks(RBookList list, Iterable<RBook> books) {
        // Create a list of RBookListItems from books, ignoring any RBooks which are already in the given list.
        List<RBookListItem> newItems = Observable.from(books)
                                                 .filter(book -> !isBookInList(list, book))
                                                 .map(book -> new RBookListItem(list, book))
                                                 .toList()
                                                 .toBlocking()
                                                 .single();

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> list.getListItems().addAll(newItems));
        }
    }

    /**
     * Reset the positions of the given list's items so that they are spaced evenly using the standard position gap
     * (which can be found at {@link com.bkromhout.minerva.C#LIST_ITEM_GAP}).
     * <p>
     * Note that this can take a bit, since it must iterate through all items in the list.
     * @param bookList The list whose items will be re-spaced.
     */
    public static void resetPositions(RBookList bookList) {
        if (bookList == null) throw new IllegalArgumentException("bookList is null.");

        // Get the list's items.
        RealmResults<RBookListItem> items = bookList.getListItems().where().findAllSorted("pos");

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> {
                // Since the list will be rearranging itself on the fly, we must iterate it backwards. This also
                // means we need to know in advance what our largest position number will be. Since the first item is
                // 0, the last item will be numItems * gap - gap. The value we store in the list for the nextPos
                // field is numItems * gap.
                Long nextPos = LongMath.checkedMultiply(items.size(), C.LIST_ITEM_GAP);

                // Change list's nextPos.
                bookList.setNextPos(nextPos);

                // Loop through items backwards and set their positions.
                // TODO I can easily see this breaking because it makes the assumption that all of the current
                // TODO positions are less than or equal to numItems * gap - gap. If we had even two items greater than
                // TODO that, and the items get rearranged as we set the positions, we'll end up setting something
                // TODO twice and breaking the order... need to unit test this!
                for (int i = items.size() - 1; i >= 0; i--) {
                    nextPos -= C.LIST_ITEM_GAP;
                    items.get(i).setPos(nextPos);
                }
            });
        }
    }

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
        moveItemToBetween(bookList, itemToMove, beforeTarget.isEmpty() ? null : beforeTarget.first(), targetItem);
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
        moveItemToBetween(bookList, itemToMove, targetItem, afterTarget.isEmpty() ? null : afterTarget.first());
    }

    /**
     * Moves {@code itemToMove} to between {@code item1} and {@code item2} in the given {@code list}. If {@code item1}
     * and {@code item2} aren't consecutive items, behavior is undefined.
     * <p>
     * If {@code itemToMove} is the same as either {@code item1} or {@code item2} then this does nothing.<br/>If {@code
     * item1} is {@code null}, then {@code itemToMove} will be put after {@code item1} with the standard position
     * gap.<br/>If {@code item2} is null, then {@code itemToMove} will be put before {@code item2} with the standard
     * position gap.
     * <p>
     * Please note that passing {@code null} for one of the items assumes that the non-null item is either the first (if
     * it's {@code item2}), or the last (if it's {@code item1}) item in the list. If this isn't the case, you'll likely
     * end up with multiple items in the same list in the same position!
     * <p>
     * If there's no space between {@code item1} and {@code item2}, the whole list will have its items re-spaced before
     * moving the item.
     * <p>
     * The current spacing gap can be found at {@link com.bkromhout.minerva.C#LIST_ITEM_GAP}.
     * @param list       List that all of the given items are a part of.
     * @param itemToMove The item which is being moved.
     * @param item1      The item which will now precede {@code itemToMove}.
     * @param item2      The item which will now follow {@code itemToMove}.
     */
    public static void moveItemToBetween(RBookList list, RBookListItem itemToMove, RBookListItem item1,
                                         RBookListItem item2) {
        if (list == null || itemToMove == null || (item1 == null && item2 == null))
            throw new IllegalArgumentException("list, itemToMove, or both of item1 and item2 are null.");

        // Check if itemToMove is the same as either item1 or item2.
        if ((item1 != null && itemToMove.getUniqueId() == item1.getUniqueId())
                || (item2 != null && itemToMove.getUniqueId() == item2.getUniqueId())) return;

        // Try to find the new position for the item, and make sure we didn't get a null back.
        Long newPos = findMiddlePos(list, item1, item2);
        if (newPos == null) {
            // If newPos is null, we need to re-sort the items before moving itemToMove.
            resetPositions(list);
            newPos = findMiddlePos(list, item1, item2);
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
     * it's {@code item2}), or the last (if it's {@code item1}) item in the list. If this isn't the case, the returned
     * position might already be taken!
     * @param list  The list which the items belong to.
     * @param item1 The earlier item (which the returned position will follow).
     * @param item2 The later item (which the returned position will precede).
     * @return The position number between the two items, or {@code null} if there's no space between the items.
     */
    public static Long findMiddlePos(RBookList list, RBookListItem item1, RBookListItem item2) {
        // Handle nulls which should throw IllegalArgumentException.
        if (list == null || (item1 == null && item2 == null))
            throw new IllegalArgumentException("Null list or both items are null.");

        // Handle acceptable nulls.
        if (item1 == null) return LongMath.checkedSubtract(item2.getPos(), C.LIST_ITEM_GAP);
        if (item2 == null) return LongMath.checkedAdd(item1.getPos(), C.LIST_ITEM_GAP);

        // Get positions, make sure that item2 doesn't precede item1 and isn't in the same position as item1.
        Long p1 = item1.getPos(), p2 = item2.getPos();
        if (p2 <= p1) throw new IllegalArgumentException("item2 was before or at the same position as item1.");

        // Calculate middle.
        Long pos = LongMath.mean(p1, p2);

        // Make sure there isn't an item in the calculated position. If there is, return null.
        return list.getListItems().where().equalTo("pos", pos).findFirst() == null ? pos : null;
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
}
