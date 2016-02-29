package com.bkromhout.minerva.realm;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a tag in Realm.
 */
public class RTag extends RealmObject {
    static final String TAG_STR_SEP = ";TAG_SEP;";
    /**
     * Tag name.
     */
    @PrimaryKey
    private String name;
    /**
     * TODO This is a work-around until Realm can do case-insensitive sorting.
     * <p>
     * Same as {@link #name}, but in lower-case.
     */
    @Index
    private String sortName;
    /**
     * {@link RBook}s which are tagged with this tag.
     */
    private RealmList<RBook> taggedBooks;

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
    }

    /**
     * Create a new {@link RTag} with the given {@code name}.
     * @param name Name of the tag.
     */
    public RTag(String name) {
        this.name = name;
        this.sortName = name.toLowerCase();
        this.taggedBooks = null;
    }

    /**
     * Gets (or makes, if one doesn't exist) an {@link RTag} with the given {@code name}.
     * @param name Tag name.
     * @return {@link RTag} with {@code name}.
     */
    public static RTag getOrMakeRTag(String name) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Name must not be null or empty.");
        try (Realm realm = Realm.getDefaultInstance()) {
            // Try to find existing tag with name.
            RTag tag = realm.where(RTag.class).equalTo("name", name).findFirst();
            if (tag != null) return tag;

            // If we didn't have an existing tag, we'll need to create a new one.
            realm.beginTransaction();
            tag = realm.copyToRealm(new RTag(name));
            realm.commitTransaction();
            return tag;
        }
    }

    /**
     * Creates a list of {@link RTag}s from a list of strings. <i><b>Will create new {@link RTag}s for any of the {@code
     * strings} which are not names of existing {@link RTag}s.</b></i>
     * @param strings List of strings.
     * @return List of {@link RTag}s, or {@code null} if {@code strings} is null.
     */
    public static List<RTag> stringListToTagList(List<String> strings) {
        if (strings == null) return null;
        ArrayList<RTag> tags = new ArrayList<>(strings.size());
        for (String string : strings) tags.add(getOrMakeRTag(string));
        return tags;
    }

    /**
     * Creates a list of tag string from a list of {@link RTag}s.
     * @param tags List of {@link RTag}s.
     * @return List of tag strings, or {@code null} if {@code tags} is null.
     */
    public static List<String> tagListToStringList(List<RTag> tags) {
        if (tags == null) return null;
        ArrayList<String> strTags = new ArrayList<>(tags.size());
        for (RTag tag : tags) strTags.add(tag.getName());
        return strTags;
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
                    if (!book.getTags().contains(tag)) {
                        // add the tag to the book,
                        book.getTags().add(tag);
                        // and add the book to the tag.
                        tag.getTaggedBooks().add(book);
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
                    if (book.getTags().remove(tag)) {
                        // and remove the book from the tag.
                        tag.getTaggedBooks().remove(book);
                    }
                }
            }
            realm.commitTransaction();
        }
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

    public RealmList<RBook> getTaggedBooks() {
        return taggedBooks;
    }

    public void setTaggedBooks(RealmList<RBook> taggedBooks) {
        this.taggedBooks = taggedBooks;
    }
}
