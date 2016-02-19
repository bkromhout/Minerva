package com.bkp.minerva.realm;

import io.realm.RealmObject;
import io.realm.annotations.Index;

/**
 * Represents an item in a book list in Realm.
 *
 * TODO add some sort of primary key to this!
 * TODO add a reference to the list for this!
 */
public class RBookListItem extends RealmObject {

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
        this.book = null;
        this.pos = Long.MIN_VALUE;
    }

    /**
     * Create a new {@link RBookListItem} using to hold the given {@link RBook} at the given position.
     * @param book {@link RBook} that this list item refers to.
     * @param pos  Position of this item in the list.
     */
    public RBookListItem(RBook book, Long pos) {
        this.book = book;
        this.pos = pos;
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
