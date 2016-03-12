package com.bkromhout.minerva.enums;

import android.support.annotation.IdRes;
import android.support.annotation.IntRange;
import android.support.annotation.StringRes;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;

/**
 * Represents the different methods of sorting.
 */
public enum SortType {
    TITLE(0, R.id.sort_title, R.string.sort_title, "title"),
    AUTHOR(1, R.id.sort_author, R.string.sort_author, "author"),
    TIME_ADDED(2, R.id.sort_time_added, R.string.sort_time_added, "lastImportDate"),
    RATING(3, R.id.sort_rating, R.string.sort_rating, "rating");

    private final int num;
    private final int id;
    private final String name;
    private final String realmField;

    /**
     * Create a new {@link SortType}.
     * @param num        The number to associate.
     * @param id         The resource ID to associate.
     * @param name       The name to associate.
     * @param realmField The {@link com.bkromhout.minerva.realm.RBook} field to associate.
     */
    SortType(int num, @IdRes int id, @StringRes int name, String realmField) {
        this.num = num;
        this.id = id;
        this.name = C.getStr(name);
        this.realmField = realmField;
    }

    /**
     * Get the associated number.
     * @return Number.
     */
    public final int getNum() {
        return num;
    }

    /**
     * Get the associated resource ID.
     * @return Resource ID
     */
    @IdRes
    public final int getResId() {
        return id;
    }

    /**
     * Get the associated name.
     * @return Name.
     */
    public final String getName() {
        return name;
    }

    /**
     * Get the associated {@link com.bkromhout.minerva.realm.RBook} field name.
     * @return Field name.
     */
    public final String getRealmField() {
        return realmField;
    }

    /**
     * Get an array of names.
     * @return Names.
     */
    public static String[] names() {
        String[] names = new String[SortType.values().length];
        for (int i = 0; i < SortType.values().length; i++) names[i] = SortType.values()[i].getName();
        return names;
    }

    /**
     * Get the {@link SortType} for the given number.
     * @param number Number.
     * @return SortType, or null if not a valid {@code number}.
     */
    public static SortType fromNumber(@IntRange(from = 0, to = 3) int number) {
        switch (number) {
            case 0:
                return TITLE;
            case 1:
                return AUTHOR;
            case 2:
                return TIME_ADDED;
            case 3:
                return RATING;
            default:
                return null;
        }
    }

    /**
     * Get the {@link SortType} for the given resource ID.
     * @param idRes Resource ID.
     * @return SortType, or null if not a valid resource ID.
     */
    public static SortType fromResId(@IdRes int idRes) {
        switch (idRes) {
            case R.id.sort_title:
                return TITLE;
            case R.id.sort_author:
                return AUTHOR;
            case R.id.sort_time_added:
                return TIME_ADDED;
            case R.id.sort_rating:
                return RATING;
            default:
                return null;
        }
    }

    /**
     * Get the {@link SortType} for the given name.
     * @param name Name.
     * @return SortType, or null if not a valid name.
     */
    public static SortType fromName(String name) {
        if (TITLE.name.equals(name)) return TITLE;
        else if (AUTHOR.name.equals(name)) return AUTHOR;
        else if (TIME_ADDED.name.equals(name)) return TIME_ADDED;
        else if (RATING.name.equals(name)) return RATING;
        else return null;
    }
}
