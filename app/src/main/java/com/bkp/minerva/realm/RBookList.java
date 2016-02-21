package com.bkp.minerva.realm;

import com.bkp.minerva.C;
import com.google.common.math.LongMath;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

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
     * Reset the positions of the given list's items so that they are spaced evenly using the standard position gap
     * (which can be found at {@link com.bkp.minerva.C#LIST_ITEM_GAP}).
     * <p>
     * Note that this can take a bit, since it must iterate through all items in the list.
     * @param bookList The list whose items will be re-spaced.
     */
    public static void resetPositions(RBookList bookList) {
        if (bookList == null) throw new IllegalArgumentException("bookList is null.");

        // Get Realm and the list's items.
        Realm realm = Realm.getDefaultInstance();
        RealmResults<RBookListItem> items = bookList.getListItems().where().findAllSorted("pos");

        // Since the list will be rearranging itself on the fly, we must iterate it backwards. This also means we need
        // to know in advance what our largest position number will be. Since the first item is 0, the last item will be
        // numItems * gap - gap. The value we store in the list for the nextPos field is numItems * gap.
        Long nextPos = LongMath.checkedMultiply(items.size(), C.LIST_ITEM_GAP);

        realm.beginTransaction();
        // Change list's nextPos.
        bookList.setNextPos(nextPos);

        // Loop through items backwards and set their positions.
        // TODO I can easily see this breaking because it makes the assumption that all of the current positions are
        // TODO less than or equal to numItems * gap - gap. If we had even two items greater than that, and the items
        // TODO get rearranged as we set the positions, we'll end up setting something twice and breaking the
        // TODO order... need to unit test this!
        for (int i = items.size() - 1; i >= 0; i--) {
            nextPos -= C.LIST_ITEM_GAP;
            items.get(i).setPos(nextPos);
        }
        realm.commitTransaction();

        // Close Realm instance.
        realm.close();
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
     * The current spacing gap can be found at {@link com.bkp.minerva.C#LIST_ITEM_GAP}.
     * @param list       List that all of the given items are a part of.
     * @param itemToMove The item which is being moved.
     * @param item1      The item which will now precede {@code itemToMove}.
     * @param item2      The item which will now follow {@code itemToMove}.
     */
    public static void moveItemToBetween(RBookList list, RBookListItem itemToMove, RBookListItem item1,
                                         RBookListItem item2) {
        // Check nulls.
        if (list == null || itemToMove == null || (item1 == null && item2 == null))
            throw new IllegalArgumentException("list, itemToMove, or both of item1 and item2 are null.");

        // Check if itemToMove is the same as either item1 or item2.
        if ((item1 != null && itemToMove.getKey().equals(item1.getKey()))
                || (item2 != null && itemToMove.getKey().equals(item2.getKey()))) return;

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
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        itemToMove.setPos(newPos);
        realm.commitTransaction();
        realm.close();
    }

    /**
     * Find the position number that is between the two given items. If there are no positions between the items, {@code
     * null} is returned. If {@code item1} and {@code item2} aren't consecutive items, this will potentially result in
     * the returned position already being taken.
     * <p>
     * {@code null} can be passed for ONE of {@code item1} or {@code item2}:<br/>If {@code item1} is null, the number
     * returned will be {@code item1.getPos() + gap}<br/>If {@code item2} is null, the number returned will be {@code
     * item2.getPos() - gap}.<br/>(The current spacing gap can be found at {@link com.bkp.minerva.C#LIST_ITEM_GAP}.)
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
