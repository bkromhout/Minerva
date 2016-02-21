package com.bkp.minerva.realm;

import com.bkp.minerva.C;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

/**
 * Represents an item in a book list in Realm.
 */
public class RBookListItem extends RealmObject {
    /**
     * Primary key, created by taking the name of the owning list and the relative path of the book file (both of which
     * are themselves primary keys) and combining them like so: "[owning list's name]$$[book's relative path]".
     */
    @PrimaryKey
    private String key;
    /**
     * The {@link RBookList} which this item belongs to.
     */
    private RBookList owningList;
    /**
     * {@link RBook} that this item refers to.
     */
    private RBook book;
    /**
     * Position of {@link RBook} in its owning {@link RBookList}.
     * <p>
     * Positions start out as 100 spaces apart (so, first item is 0, next is 100, next is 200, etc); this allows us to
     * more easily update positions of the books in the database (hopefully) without having to do cascading updates of
     * multiple items.
     * <p>
     * TODO At some point, we'll have a task that runs once per day to reset the positions for all items in all lists.
     */
    @Index
    private Long pos;

    /**
     * Create a default {@link RBookListItem}.
     * <p>
     * Note: This really shouldn't ever be called, it's only here because it has to be for Realm. If a new {@link
     * RBookListItem} is created using this, it isn't being given an {@link RBook} to hold or a valid position.
     */
    public RBookListItem() {
        this.key = "DEF_BOOK_LIST_ITEM_KEY";
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
        this.owningList = owningList;
        this.book = book;

        // Key = "[owningList's name]$$[book's rel path]".
        this.key = String.format("%s$$%s", owningList.getName(), book.getRelPath());

        // Position is the next position number from owningList. Then we update the next position number.
        this.pos = owningList.getNextPos();
        owningList.setNextPos(this.pos + C.LIST_ITEM_GAP);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public RBookList getOwningList() {
        return owningList;
    }

    public void setOwningList(RBookList owningList) {
        this.owningList = owningList;
    }

    public RBook getBook() {
        return book;
    }

    public void setBook(RBook book) {
        this.book = book;
    }

    public Long getPos() {
        return pos;
    }

    public void setPos(Long pos) {
        this.pos = pos;
    }
}
