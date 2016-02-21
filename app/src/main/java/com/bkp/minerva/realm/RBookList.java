package com.bkp.minerva.realm;

import io.realm.RealmList;
import io.realm.RealmObject;
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
     * Position number for the next item to be added to this list.
     * <p>
     * TODO be sure that this gets pushed up if we do a hell of a lot of rearranging...
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
        this.nextPos = 0L;
        this.listItems = null;
    }

    /**
     * Create a new {@link RBookList} with the given {@code name}.
     * @param name Name of the book list. This MUST be unique!
     */
    public RBookList(String name) {
        this.name = name;
        this.nextPos = 0L;
        this.listItems = null;
    }

    /**
     * Reset the positions of the given list's items so that they are spaced evenly using the standard position gap
     * (which can be found at {@link com.bkp.minerva.C#LIST_ITEM_GAP}).
     * <p>
     * Note that this can take a bit, since it must iterate through all items in the list.
     * @param list List whose items will be re-spaced.
     */
    public static void resetPositions(RBookList list) {
        Long nextPos = 0L;
        // TODO.

    }

    /**
     * Moves {@code itemToMove} to between {@code item1} and {@code item2} in the given {@code list}.
     * <p>
     * If {@code itemToMove} is the same as either {@code item1} or {@code item2} then this does nothing.<br/>If {@code
     * item1} is {@code null}, then {@code itemToMove} will be put after {@code item1} with the standard position
     * gap.<br/>If {@code item2} is null, then {@code itemToMove} will be put before {@code item2} with the standard
     * position gap.
     * <p>
     * In the process of moving the item, it is possible that the whole list might have its items re-spaced.
     * <p>
     * The current spacing gap can be found at {@link com.bkp.minerva.C#LIST_ITEM_GAP}.
     * @param list       List that all of the given items are a part of.
     * @param itemToMove The item which is being moved.
     * @param item1      The item which will now precede {@code itemToMove}.
     * @param item2      The item which will now follow {@code itemToMove}.
     */
    public static void moveItemToBetween(RBookList list, RBookListItem itemToMove, RBookListItem item1,
                                         RBookListItem item2) {
        // TODO.
    }

    /**
     * Find the position value that is between the two given items.
     * <p>
     * Best case scenario here, the distance between the items is divisible by 2 and we're good. If the items aren't
     * divisible by two, then we'll have to make sure to check the long we get back (it will be rounded down) to see if
     * there's already an item in that slot. If there is, we'll have to re-space some items (we'll just re-space them
     * all), then we can try again.
     * <p>
     * Null can be passed for ONE of {@code item1} or {@code item2}. Depending on which is null, the number returned
     * will be {@code item1.getPos() + GAP} or {@code item2.getPos() - GAP}.
     * <p>
     * The current spacing gap can be found at {@link com.bkp.minerva.C#LIST_ITEM_GAP}.
     * @param list
     * @param item1
     * @param item2
     * @return
     */
    public static Long findMiddlePos(RBookList list, RBookListItem item1, RBookListItem item2) {
        // TODO handle nulls.

        // Get positions.
        Long l1 = item1.getPos(), l2 = item2.getPos();
        // Calculate middle.
        Long bw = (l1 & l2) + ((l1 ^ l2) >> 1);
        // TODO make sure there isn't an item with that position.
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
