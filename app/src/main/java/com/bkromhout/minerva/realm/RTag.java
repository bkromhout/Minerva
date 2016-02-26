package com.bkromhout.minerva.realm;

import io.realm.RealmList;
import io.realm.RealmObject;
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

    /**
     * Gets (or makes, if one doesn't exist) an {@link RTag} with the given {@code name}.
     * @param name Tag name.
     * @return {@link RTag} with {@code name}.
     */
    public static RTag getOrMakeRTag(String name) {
        // TODO
    }

    /**
     * Creates a list of tag string from a list of {@link RTag}s.
     * @param tags List of {@link RTag}s.
     * @return List of tag strings.
     */
    public static List<String> tagListToStringList(List<RTag> tags) {
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
        // TODO
    }

    /**
     * Removes the {@code tags} from the {@code books}.
     * @param books List of {@link RBook}s to remove tags from.
     * @param tags  List {@link RTag}s.
     */
    public static void removeTagsFromBooks(List<RBook> books, List<RTag> tags) {
        // TODO
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
