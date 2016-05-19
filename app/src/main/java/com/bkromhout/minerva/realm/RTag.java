package com.bkromhout.minerva.realm;

import android.support.annotation.ColorInt;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.rrvl.UIDModel;
import com.bkromhout.ruqus.Hide;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

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
        this.textColor = Minerva.d().DEFAULT_TAG_TEXT_COLOR;
        this.bgColor = Minerva.d().DEFAULT_TAG_BG_COLOR;
    }

    /**
     * Create a new {@link RTag} with the given {@code name}.
     * @param name Name of the tag.
     */
    public RTag(String name) {
        this(name, Minerva.d().DEFAULT_TAG_TEXT_COLOR, Minerva.d().DEFAULT_TAG_BG_COLOR);
    }

    /**
     * Create a new {@link RTag} with the given {@code name} and {@code bgColor}.
     * @param name      Name of the tag.
     * @param textColor Text color of the tag.
     * @param bgColor   Background color of the tag.
     */
    public RTag(String name, @ColorInt int textColor, @ColorInt int bgColor) {
        this.name = name;
        this.sortName = name.toLowerCase();
        this.taggedBooks = null;
        this.textColor = textColor;
        this.bgColor = bgColor;
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
