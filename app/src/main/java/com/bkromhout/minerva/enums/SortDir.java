package com.bkromhout.minerva.enums;

import android.support.annotation.IdRes;
import android.support.annotation.IntRange;
import android.support.annotation.StringRes;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
import io.realm.Sort;

import java.util.Arrays;

/**
 * Represents the different sort directions.
 */
public enum SortDir {
    ASC(0, R.id.sort_asc, R.string.sort_asc, Sort.ASCENDING),
    DESC(1, R.id.sort_desc, R.string.sort_desc, Sort.DESCENDING);

    private final int num;
    private final int id;
    private final int name;
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
        this.name = name;
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
    @StringRes
    public final int getName() {
        return name;
    }

    /**
     * Get an array of Realm's version of the sort direction enum. All items will be the same.
     * @param size How large the array should be.
     * @return An array of Realm sort enums.
     */
    public final Sort[] getRealmSort(int size) {
        Sort[] sortArr = new Sort[size];
        Arrays.fill(sortArr, realmSort);
        return sortArr;
    }

    /**
     * Get an array of names.
     * @return Names.
     */
    public static String[] names() {
        String[] names = new String[SortDir.values().length];
        for (int i = 0; i < SortDir.values().length; i++)
            names[i] = Minerva.get().getString(SortDir.values()[i].getName());
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
    public static SortDir fromName(@StringRes int name) {
        for (SortDir dir : SortDir.values()) if (dir.getName() == name) return dir;
        return null;
    }
}
