package com.bkromhout.minerva.realm;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Represents a tag in Realm.
 */
public class RTag extends RealmObject {
    /**
     * Tag name.
     */
    @PrimaryKey
    private String name;
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
        this.taggedBooks = null;
    }

    /**
     * Create a new {@link RTag} with the given {@code name}.
     * @param name Name of the tag.
     */
    public RTag(String name) {
        this.name = name;
        this.taggedBooks = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RealmList<RBook> getTaggedBooks() {
        return taggedBooks;
    }

    public void setTaggedBooks(RealmList<RBook> taggedBooks) {
        this.taggedBooks = taggedBooks;
    }
}
