package com.bkromhout.minerva.enums;

import android.support.annotation.IdRes;
import android.support.annotation.IntRange;
import android.support.annotation.StringRes;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import io.realm.Sort;

/**
 * Represents the different sort directions.
 */
public enum SortDir {
    ASC(0, R.id.sort_asc, R.string.sort_asc, Sort.ASCENDING),
    DESC(1, R.id.sort_desc, R.string.sort_desc, Sort.DESCENDING);

    private final int num;
    private final int id;
    private final String name;
    private final Sort realmSort;

    /**
     * Create a new {@link SortDir}.
     * @param num       The number to associate.
     * @param id        The resource ID to associate.
     * @param name      The name to associate.
     * @param realmSort Realm's version of the sort direction.
     */
    SortDir(int num, @IdRes int id, @StringRes int name, Sort realmSort) {
        this.num = num;
        this.id = id;
        this.name = C.getStr(name);
        this.realmSort = realmSort;
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
     * Get Realm's version of the sort direction enum.
     * @return Realm sort enum.
     */
    public final Sort getRealmSort() {
        return realmSort;
    }

    /**
     * Get an array of names.
     * @return Names.
     */
    public static String[] names() {
        String[] names = new String[SortDir.values().length];
        for (int i = 0; i < SortDir.values().length; i++) names[i] = SortDir.values()[i].getName();
        return names;
    }

    /**
     * Get the {@link SortDir} for the given number.
     * @param number Number.
     * @return SortDir, or null if not a valid {@code number}.
     */
    public static SortDir fromNumber(@IntRange(from = 0, to = 1) int number) {
        for (SortDir dir : SortDir.values()) if (dir.getNum() == number) return dir;
        return null;
    }

    /**
     * Get the {@link SortDir} for the given resource ID.
     * @param idRes Resource ID.
     * @return SortDir, or null if not a valid resource ID.
     */
    public static SortDir fromResId(@IdRes int idRes) {
        for (SortDir dir : SortDir.values()) if (dir.getResId() == idRes) return dir;
        return null;
    }

    /**
     * Get the {@link SortDir} for the given name.
     * @param name Name.
     * @return SortDir, or null if not a valid name.
     */
    public static SortDir fromName(String name) {
        for (SortDir dir : SortDir.values()) if (dir.getName().equals(name)) return dir;
        return null;
    }
}
