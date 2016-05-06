package com.bkromhout.minerva.realm;

import com.bkromhout.minerva.C;
import com.bkromhout.rrvl.UIDModel;
import com.bkromhout.ruqus.Hide;
import com.bkromhout.ruqus.Queryable;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an item in a book list in Realm.
 */
@Queryable(name = "Books in normal lists")
public class RBookListItem extends RealmObject implements UIDModel {
    /**
     * A unique long value.
     */
    @PrimaryKey
    @Hide
    public long uniqueId;
    /**
     * The {@link RBookList} which this item belongs to.
     */
    public RBookList owningList;
    /**
     * {@link RBook} that this item refers to.
     */
    public RBook book;
    /**
     * Position of {@link RBook} in its owning {@link RBookList}.
     * <p>
     * Positions start out as 100 spaces apart (so, first item is 0, next is 100, next is 200, etc); this allows us to
     * more easily update positions of the books in the database (hopefully) without having to do cascading updates of
     * multiple items.
     */
    @Index
    @Hide
    public Long pos;

    /**
     * Create a default {@link RBookListItem}.
     * <p>
     * Note: This really shouldn't ever be called, it's only here because it has to be for Realm. If a new {@link
     * RBookListItem} is created using this, it isn't being given an {@link RBook} to hold or a valid position.
     */
    public RBookListItem() {
        this.uniqueId = UniqueIdFactory.getInstance().nextId(RBookListItem.class);
        this.owningList = null;
        this.book = null;
        this.pos = Long.MIN_VALUE;
    }

    /**
     * Create a new {@link RBookListItem} using to hold the given {@link RBook} in the given {@link RBookList}.
     * @param owningList {@link RBookList} this this list item belongs to.
     * @param book       {@link RBook} that this list item refers to.
     */
    public RBookListItem(RBookList owningList, RBook book) {
        this.uniqueId = UniqueIdFactory.getInstance().nextId(RBookListItem.class);
        this.owningList = owningList;
        this.book = book;

        // Position is the next position number from owningList. Then we update the next position number.
        this.pos = owningList.nextPos;
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> owningList.nextPos = this.pos + C.LIST_ITEM_GAP);
        }
    }

    /**
     * Checks whether two {@link RBookListItem}s are in the same {@link RBookList}.
     * @param item1 An item.
     * @param item2 Another item.
     * @return True if items are from the same list, otherwise false.
     */
    static boolean areFromSameList(RBookListItem item1, RBookListItem item2) {
        // Check for nulls.
        if (item1 == null || item2 == null) return false;
        // Check if from same list.
        return item1.owningList.equals(item2.owningList);
    }

    /**
     * Convert the given {@code listItems} to a list of {@link RBook}s.
     * @param listItems A list of {@link RBookListItem}s.
     * @return List of {@link RBook}s.
     */
    public static List<RBook> booksFromBookListItems(List<RBookListItem> listItems) {
        if (listItems == null) throw new IllegalArgumentException("listItems may not be null.");
        if (listItems.isEmpty()) return new ArrayList<>();

        ArrayList<RBook> books = new ArrayList<>(listItems.size());
        for (RBookListItem listItem : listItems) books.add(listItem.book);
        return books;
    }

    @Override
    public Object getUID() {
        return uniqueId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RBookListItem)) return false;

        RBookListItem that = (RBookListItem) o;
        return uniqueId == that.uniqueId;
    }

    @Override
    public int hashCode() {
        return (int) (uniqueId ^ (uniqueId >>> 32));
    }
}
