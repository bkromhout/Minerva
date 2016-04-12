package com.bkromhout.minerva.realm;

import com.bkromhout.minerva.C;
import com.bkromhout.minerva.prefs.DBPrefs;
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
public class RBookListItem extends RealmObject {
    private static final String KEY_SEP = "##BLI_KEY##";
    /**
     * Primary key, created by taking the name of the owning list and the relative path of the book file (both of which
     * are themselves primary keys) and combining them like so: "[owning list's name]$$[book's relative path]".
     */
    @PrimaryKey
    @Hide
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
     */
    @Index
    @Hide
    private Long pos;
    /**
     * A unique long value.
     */
    @Index
    @Hide
    private long uniqueId;

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
        this.uniqueId = DBPrefs.get().getNextRBookListItemUid();
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
        this.key = makeBookListItemKey(owningList.getName(), book.getRelPath());

        // Position is the next position number from owningList. Then we update the next position number.
        this.pos = owningList.getNextPos();
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> owningList.setNextPos(this.pos + C.LIST_ITEM_GAP));
        }

        this.uniqueId = DBPrefs.get().getNextRBookListItemUid();
    }

    /**
     * Create a key for the given list name and relative book path.
     * @param listName    List name.
     * @param bookRelPath Relative book path.
     * @return The resultant key.
     */
    static String makeBookListItemKey(String listName, String bookRelPath) {
        return String.format("%s" + KEY_SEP + "%s", listName, bookRelPath);
    }

    /**
     * Checks whether two key strings are for items in the same {@link RBookList}.
     * @param item1Key An item's key string.
     * @param item2Key Another item's key string.
     * @return True if keys are for items from the same list, otherwise false.
     */
    static boolean areFromSameList(String item1Key, String item2Key) {
        // Check for nulls.
        if (item1Key == null || item1Key.isEmpty() || item2Key == null || item2Key.isEmpty()) return false;
        // Check that these are keys.
        if (!item1Key.contains(KEY_SEP) || !item2Key.contains(KEY_SEP)) return false;
        // Check keys.
        return item1Key.split("\\Q" + KEY_SEP + "\\E")[0].equals(item2Key.split("\\Q" + KEY_SEP + "\\E")[0]);
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
        for (RBookListItem listItem : listItems) books.add(listItem.getBook());
        return books;
    }

    /**
     * Deletes any {@link RBookListItem}s which exist for any of the given {@code books}.
     * @param books List of {@link RBook}s.
     */
    public static void deleteAnyForBooks(List<RBook> books) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm -> {
                for (RBook book : books)
                    tRealm.where(RBookListItem.class).contains("book.relPath", book.getRelPath()).findAll().clear();
            });
        }
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

    public long getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(long uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RBookListItem)) return false;

        RBookListItem that = (RBookListItem) o;

        if (getUniqueId() != that.getUniqueId()) return false;
        return getKey().equals(that.getKey());
    }

    @Override
    public int hashCode() {
        int result = getKey().hashCode();
        result = 31 * result + (int) (getUniqueId() ^ (getUniqueId() >>> 32));
        return result;
    }
}
