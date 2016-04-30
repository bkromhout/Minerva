package com.bkromhout.minerva.realm;

import com.bkromhout.minerva.C;
import com.google.common.math.LongMath;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Provides static methods to help with reordering {@link RBookListItem}s when they are dragged.
 */
public class ListItemPositionHelper {
    /**
     * Moves the {@link RBookListItem} whose unique ID is {@code itemToMoveId} to somewhere before the {@link
     * RBookListItem} whose unique ID is {@code targetItemId}.
     * @param itemToMoveId Unique ID of item to move.
     * @param targetItemId Unique ID of item which item whose unique ID is {@code itemToMoveId} will be moved before.
     */
    public static void moveItemToBefore(long itemToMoveId, long targetItemId) {
        try (Realm realm = Realm.getDefaultInstance()) {
            moveItemToBefore(realm.where(RBookListItem.class).equalTo("uniqueId", itemToMoveId).findFirst(),
                    realm.where(RBookListItem.class).equalTo("uniqueId", targetItemId).findFirst());
        }
    }

    /**
     * Moves {@code itemToMove} to somewhere before {@code targetItem}.
     * @param itemToMove Item to move.
     * @param targetItem Item which {@code itemToMove} will be moved before.
     */
    public static void moveItemToBefore(RBookListItem itemToMove, RBookListItem targetItem) {
        if (itemToMove == null || targetItem == null) throw new IllegalArgumentException("Neither item may be null.");
        if (itemToMove.uniqueId == targetItem.uniqueId) return;

        RBookList bookList = targetItem.owningList;
        // Get the items which come before targetItem.
        RealmResults<RBookListItem> beforeTarget = bookList.getListItems()
                                                           .where()
                                                           .lessThan("pos", targetItem.pos)
                                                           .findAllSorted("pos", Sort.DESCENDING);

        // Move itemToMove to between beforeTarget.first()/null and targetItem.
        moveItemToBetween(bookList, itemToMove, beforeTarget.isEmpty() ? null : beforeTarget.first(), targetItem);
    }

    /**
     * Moves the {@link RBookListItem} whose unique ID is {@code itemToMoveId} to somewhere after the {@link
     * RBookListItem} whose unique ID is {@code targetItemId}.
     * @param itemToMoveId Unique ID of item to move.
     * @param targetItemId Unique ID of item which item whose unique ID is {@code itemToMoveId} will be moved after.
     */
    public static void moveItemToAfter(long itemToMoveId, long targetItemId) {
        try (Realm realm = Realm.getDefaultInstance()) {
            moveItemToAfter(realm.where(RBookListItem.class).equalTo("uniqueId", itemToMoveId).findFirst(),
                    realm.where(RBookListItem.class).equalTo("uniqueId", targetItemId).findFirst());
        }
    }

    /**
     * Moves {@code itemToMove} to somewhere after {@code targetItem}.
     * @param itemToMove Item to move.
     * @param targetItem Item which {@code itemToMove} will be moved after.
     */
    public static void moveItemToAfter(RBookListItem itemToMove, RBookListItem targetItem) {
        if (itemToMove == null || targetItem == null) throw new IllegalArgumentException("Neither item may be null.");
        if (itemToMove.uniqueId == targetItem.uniqueId) return;

        RBookList bookList = targetItem.owningList;
        // Get the items which come after targetItem.
        RealmResults<RBookListItem> afterTarget = bookList.getListItems()
                                                          .where()
                                                          .greaterThan("pos", targetItem.pos)
                                                          .findAllSorted("pos");

        // Move itemToMove to between targetItem and afterTarget.first()/null.
        moveItemToBetween(bookList, itemToMove, targetItem, afterTarget.isEmpty() ? null : afterTarget.first());
    }

    /**
     * Moves {@code itemToMove} to between {@code item1} and {@code item2} in {@code bookList}. If {@code item1} and
     * {@code item2} aren't consecutive items, behavior is undefined. It is assumed that {@code itemToMove}, {@code
     * item1}, and {@code item2} are all owned by {@code bookList}.
     * <p>
     * If {@code itemToMove} is the same as either {@code item1} or {@code item2} then this method does nothing.<br/>If
     * {@code item1} is {@code null}, then {@code itemToMove} will be put after {@code item1} with the standard position
     * gap.<br/>If {@code item2} is {@code null}, then {@code itemToMove} will be put before {@code item2} with the
     * standard position gap.
     * <p>
     * Please note that passing {@code null} for one of the items assumes that the non-null item is either the first (if
     * it's {@code item2}), or the last (if it's {@code item1}) item in {@code bookList}. If this isn't the case, some
     * items will likely have colliding position values when this method finishes.
     * <p>
     * If there's no space between {@code item1} and {@code item2}, the whole list will have its items re-spaced before
     * moving the item.
     * <p>
     * The current spacing gap can be found at {@link C#LIST_ITEM_GAP}.
     * @param bookList   The book list which owns {@code itemToMove}, {@code item1}, and {@code item2}.
     * @param itemToMove The item which is being moved.
     * @param item1      The item which will now precede {@code itemToMove}.
     * @param item2      The item which will now follow {@code itemToMove}.
     */
    private static void moveItemToBetween(RBookList bookList, RBookListItem itemToMove, RBookListItem item1,
                                          RBookListItem item2) {
        bookList.throwIfSmartList();
        if (itemToMove == null || (item1 == null && item2 == null))
            throw new IllegalArgumentException("itemToMove, or both of item1 and item2 are null.");

        // Check if itemToMove is the same as either item1 or item2.
        if ((item1 != null && itemToMove.equals(item1)) || (item2 != null && itemToMove.equals(item2))) return;

        // Try to find the new position for the item, and make sure we didn't get a null back.
        Long newPos = findMiddlePos(bookList, item1, item2);
        if (newPos == null) {
            // If newPos is null, we need to re-sort the items before moving itemToMove.
            bookList.resetPositions();
            newPos = findMiddlePos(bookList, item1, item2);
            if (newPos == null)
                throw new IllegalArgumentException("Couldn't find space between item1 and item2 after re-spacing");
        }

        // Get Realm, update itemToMove, then close Realm.
        try (Realm realm = Realm.getDefaultInstance()) {
            final Long finalNewPos = newPos;
            realm.executeTransaction(tRealm -> itemToMove.pos = finalNewPos);
        }
    }

    /**
     * Find the position number that is between the two given items. It is assumed that both {@code item1} and {@code
     * item2} are owned by {@code bookList}. If there are no positions between the items, or if {@code item1} and {@code
     * item2} aren't consecutive items (and there is already an item halfway between them), {@code null} is returned.
     * <p>
     * {@code null} can be passed for ONE of {@code item1} or {@code item2}:<br/>If {@code item1} is {@code null}, the
     * number returned will be {@code item1.getPosition() + gap}<br/>If {@code item2} is {@code null}, the number
     * returned will be {@code item2.getPosition() - gap}.<br/>(The current spacing gap can be found at {@link
     * com.bkromhout.minerva.C#LIST_ITEM_GAP}.)
     * <p>
     * Please note that passing {@code null} for one of the items assumes that the non-null item is either the first (if
     * it's {@code item2}), or the last (if it's {@code item1}) item in {@code bookList}. If this isn't the case, the
     * returned position might already be in use.
     * @param bookList The list which owns {@code item1} and {@code item2}.
     * @param item1    The earlier item (which the returned position will follow).
     * @param item2    The later item (which the returned position will precede).
     * @return The position number between the two items, or {@code null} if there's no space between the items.
     */
    private static Long findMiddlePos(RBookList bookList, RBookListItem item1, RBookListItem item2) {
        // Handle nulls which should throw IllegalArgumentException.
        if (item1 == null && item2 == null) throw new IllegalArgumentException("Both items are null.");

        // Handle acceptable nulls.
        if (item1 == null) return LongMath.checkedSubtract(item2.pos, C.LIST_ITEM_GAP);
        if (item2 == null) return LongMath.checkedAdd(item1.pos, C.LIST_ITEM_GAP);

        // Get positions, make sure that item2 doesn't precede item1 and isn't in the same position as item1.
        Long p1 = item1.pos, p2 = item2.pos;
        if (p2 <= p1) throw new IllegalArgumentException("item2 was before or at the same position as item1.");

        // Calculate middle.
        Long pos = LongMath.mean(p1, p2);

        // Make sure there isn't an item in the calculated position. If there is, return null.
        return bookList.getListItems().where().equalTo("pos", pos).findFirst() == null ? pos : null;
    }
}
