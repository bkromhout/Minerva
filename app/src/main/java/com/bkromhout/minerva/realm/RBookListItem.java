package com.bkromhout.minerva.realm;

import com.bkromhout.minerva.data.UniqueIdFactory;
import com.bkromhout.rrvl.UIDModel;
import com.bkromhout.ruqus.Hide;
import com.bkromhout.ruqus.Queryable;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

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

    public RBookListItem() {
    }

    /**
     * Create a new {@link RBookListItem} using to hold the given {@link RBook} in the given {@link RBookList}.
     * @param realm      Instance of Realm to use for updating the value of {@link RBookList#nextPos} when the {@link
     *                   RBookListItem} is created.
     * @param owningList {@link RBookList} this this list item belongs to.
     * @param book       {@link RBook} that this list item refers to.
     */
    public RBookListItem(Realm realm, RBookList owningList, RBook book) {
        this(owningList, book, owningList.getThenIncrementNextPos(realm),
                UniqueIdFactory.getInstance().nextId(RBookListItem.class));
    }

    RBookListItem(RBookList owningList, RBook book, long pos, long uniqueId) {
        this.owningList = owningList;
        this.book = book;
        this.pos = pos;
        this.uniqueId = uniqueId;
    }

    @Override
    public Object getUID() {
        return uniqueId;
    }

    // TODO Workaround for an issue where the realm transformer doesn't work on androidTest sources :(
    RBook getBook() {
        return this.book;
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
