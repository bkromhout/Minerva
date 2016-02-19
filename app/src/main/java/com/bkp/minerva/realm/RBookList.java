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
