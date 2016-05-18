package com.bkromhout.minerva.enums;

import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import com.bkromhout.minerva.R;

/**
 * Represents the various fragments which can be shown by {@link com.bkromhout.minerva.activities.MainActivity}.
 */
public enum MainFrag {
    RECENT(0, R.string.nav_item_recent, R.id.nav_recent),
    LIBRARY(1, R.string.nav_item_library, R.id.nav_library),
    ALL_LISTS(2, R.string.nav_item_all_lists, R.id.nav_all_lists),
    POWER_SEARCH(3, R.string.nav_item_power_search, R.id.nav_power_search);

    private final int index;
    private final int titleRes;
    private final int idRes;

    MainFrag(int index, @StringRes int titleRes, @IdRes int idRes) {
        this.index = index;
        this.titleRes = titleRes;
        this.idRes = idRes;
    }

    /**
     * Get the index of the fragment.
     * @return Fragment index.
     */
    public final int getIndex() {
        return index;
    }

    /**
     * Get the title string resource to use for the fragment.
     * @return Title string resource.
     */
    @StringRes
    public final int getTitleRes() {
        return titleRes;
    }

    /**
     * Get the ID resource associated with the fragment.
     * @return ID resource.
     */
    @IdRes
    public final int getIdRes() {
        return idRes;
    }

    /**
     * Get the {@link MainFrag} for the given index.
     * @param index Index.
     * @return MainFrag, or null if not a valid index.
     */
    public static MainFrag fromIndex(int index) {
        for (MainFrag mainFrag : MainFrag.values()) if (mainFrag.index == index) return mainFrag;
        return null;
    }
}
