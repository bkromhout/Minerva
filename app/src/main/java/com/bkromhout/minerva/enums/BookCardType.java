package com.bkromhout.minerva.enums;

import android.support.annotation.IdRes;
import android.support.annotation.IntRange;
import android.support.annotation.StringRes;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;

/**
 * Represents the different types of book card layout types.
 */
public enum BookCardType {
    NORMAL(0, R.id.card_normal, R.string.card_normal),
    NO_COVER(1, R.id.card_no_cover, R.string.card_no_cover),
    COMPACT(2, R.id.card_compact, R.string.card_compact);

    private final int num;
    private final int id;
    private final int name;

    /**
     * Create a new {@link BookCardType}.
     * @param num  The number to associate.
     * @param id   The resource ID to associate.
     * @param name The name to associate.
     */
    BookCardType(int num, @IdRes int id, @StringRes int name) {
        this.num = num;
        this.id = id;
        this.name = name;
    }

    /**
     * Get the associated number.
     * @return Number.
     */
    public final int getNum() {
        return this.num;
    }

    /**
     * Get the associated resource ID.
     * @return Resource ID
     */
    @IdRes
    public final int getResId() {
        return this.id;
    }

    /**
     * Get the associated name.
     * @return Name.
     */
    @StringRes
    public final int getName() {
        return this.name;
    }

    /**
     * Get an array of names.
     * @return Names.
     */
    public static String[] names() {
        String[] names = new String[BookCardType.values().length];
        for (int i = 0; i < BookCardType.values().length; i++)
            names[i] = Minerva.get().getString(BookCardType.values()[i].getName());
        return names;
    }

    /**
     * Get the {@link BookCardType} for the given number.
     * @param number Number.
     * @return BookCardType, or null if not a valid {@code number}.
     */
    public static BookCardType fromNumber(@IntRange(from = 0, to = 2) int number) {
        for (BookCardType type : BookCardType.values()) if (type.getNum() == number) return type;
        return null;
    }

    /**
     * Get the {@link BookCardType} for the given resource ID.
     * @param idRes Resource ID.
     * @return BookCardType, or null if not a valid resource ID.
     */
    public static BookCardType fromResId(@IdRes int idRes) {
        for (BookCardType type : BookCardType.values()) if (type.getResId() == idRes) return type;
        return null;
    }

    /**
     * Get the {@link BookCardType} for the given name.
     * @param name Name.
     * @return BookCardType, or null if not a valid name.
     */
    public static BookCardType fromName(@StringRes int name) {
        for (BookCardType type : BookCardType.values()) if (type.getName() == name) return type;
        return null;
    }
}
