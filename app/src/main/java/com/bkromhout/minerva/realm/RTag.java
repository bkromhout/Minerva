package com.bkromhout.minerva.realm;

import com.bkromhout.minerva.C;
import com.bkromhout.rrvl.UIDModel;
import com.bkromhout.ruqus.Hide;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;
import rx.Observable;

import java.util.List;

/**
 * Represents a tag in Realm.
 */
public class RTag extends RealmObject implements UIDModel {
    /**
     * Tag name.
     */
    @PrimaryKey
    @Required
    public String name;
    /**
     * Same as {@link #name}, but in lower-case.
     */
    @Index
    @Hide
    public String sortName;
    /**
     * Color to use for the tag's text.
     */
    @Hide
    public int textColor;
    /**
     * Color to use for the tag's background.
     */
    @Hide
    public int bgColor;
    /**
     * {@link RBook}s which are tagged with this tag.
     */
    @Hide
    public RealmList<RBook> taggedBooks;

    /**
     * Create a default {@link RTag}.
     * <p>
     * Note: This really shouldn't ever be called, it's only here because it has to be for Realm. If a new {@link RTag}
     * is created using this, it risks a situation where we have primary key collisions.
     */
    public RTag() {
        this.name = "DEF_TAG_NAME";
        this.sortName = name.toLowerCase();
        this.taggedBooks = null;
        this.textColor = C.DEFAULT_TAG_TEXT_COLOR;
        this.bgColor = C.DEFAULT_TAG_BG_COLOR;
    }

    /**
     * Create a new {@link RTag} with the given {@code name}.
     * @param name Name of the tag.
     */
    public RTag(String name) {
        this.name = name;
        this.sortName = name.toLowerCase();
        this.taggedBooks = null;
        this.textColor = C.DEFAULT_TAG_TEXT_COLOR;
        this.bgColor = C.DEFAULT_TAG_BG_COLOR;
    }

    /**
     * Gets an {@link RTag} with the given {@code name}.
     * @param name            Tag name.
     * @param makeNonexistent If true, an {@link RTag} will be made if one doesn't exist with the given {@code name}.
     * @return {@link RTag} with {@code name}, or null if there isn't one and {@code makeNonexistent} is false.
     */
    public static RTag getRTag(String name, boolean makeNonexistent) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Name must not be null or empty.");
        try (Realm realm = Realm.getDefaultInstance()) {
            // Try to find existing tag with name.
            RTag tag = realm.where(RTag.class).equalTo("name", name).findFirst();
            if (tag != null || !makeNonexistent) return tag;

            // If we didn't have an existing tag, we'll need to create a new one.
            realm.beginTransaction();
            tag = realm.copyToRealm(new RTag(name));
            realm.commitTransaction();
            return tag;
        }
    }

    /**
     * Creates a list of {@link RTag}s from a list of strings.
     * @param strings         List of strings.
     * @param makeNonexistent If true, strings which aren't the name of any existing {@link RTag}s will cause new {@link
     *                        RTag}s to be made.
     * @return List of {@link RTag}s, or {@code null} if {@code strings} is null.
     */
    public static List<RTag> stringListToTagList(List<String> strings, boolean makeNonexistent) {
        if (strings == null) return null;
        return Observable.from(strings)
                         .map(string -> getRTag(string, makeNonexistent))
                         .filter(tag -> tag != null)
                         .toList()
                         .toBlocking()
                         .single();
    }

    /**
     * Adds the {@code tags} to the {@code books}.
     * @param books List of {@link RBook}s to add tags to.
     * @param tags  List {@link RTag}s.
     */
    public static void addTagsToBooks(List<RBook> books, List<RTag> tags) {
        if (books == null || tags == null) throw new IllegalArgumentException("No nulls allowed.");
        if (books.isEmpty() || tags.isEmpty()) return;

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();
            // Loop through books and add tags to them.
            for (RBook book : books) {
                for (RTag tag : tags) {
                    // If the book doesn't already have the tag,
                    if (!book.tags.contains(tag)) {
                        // add the tag to the book,
                        book.tags.add(tag);
                        // and add the book to the tag.
                        tag.taggedBooks.add(book);
                    }
                }
            }
            realm.commitTransaction();
        }
    }

    /**
     * Removes the {@code tags} from the {@code books}.
     * @param books List of {@link RBook}s to remove tags from.
     * @param tags  List {@link RTag}s.
     */
    public static void removeTagsFromBooks(List<RBook> books, List<RTag> tags) {
        if (books == null || tags == null) throw new IllegalArgumentException("No nulls allowed.");
        if (books.isEmpty() || tags.isEmpty()) return;

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();
            // Loop through books and remove tags from them.
            for (RBook book : books) {
                for (RTag tag : tags) {
                    // If the book has the tag, remove it,
                    if (book.tags.remove(tag)) {
                        // and remove the book from the tag.
                        tag.taggedBooks.remove(book);
                    }
                }
            }
            realm.commitTransaction();
        }
    }

    @Override
    public Object getUID() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RTag)) return false;

        RTag rTag = (RTag) o;

        return name.equals(rTag.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
