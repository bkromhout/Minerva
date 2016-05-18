package com.bkromhout.minerva;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Constants.
 */
public final class C {
    /**
     * Maximum number of past logs to keep.
     */
    public static final int MAX_LOGS = 30;
    /**
     * 5 seconds duration for snackbar.
     */
    public static final int SB_LENGTH_5_SEC = 5000;
    /**
     * URL of Minerva's GitHub repository.
     */
    public static final String GITHUB_REPO = "https://github.com/bkromhout/Minerva";
    /**
     * This is the item type integer we supply to a recycler view for our empty footer item.
     */
    public static final int FOOTER_ITEM_TYPE = -1;
    /**
     * The initial number of position numbers between each item in a book list.
     */
    public static final long LIST_ITEM_GAP = 100L;
    /**
     * Valid file extensions.
     */
    public static final List<String> VALID_EXTENSIONS = ImmutableList.of("epub");

    /* Keys */
    public static final String POS_TO_UPDATE = "position_to_update";
    public static final String NEEDS_POS_UPDATE = "needs_position_update";
    public static final String REL_PATH = "relative_path";
    public static final String IS_IN_ACTION_MODE = "is_in_action_mode";
    public static final String SEARCH_STRING = "search_string";
    public static final String RUQ = "realm_user_query";

    /* Request codes. */
    public static final int RC_WELCOME_ACTIVITY = 1;
    public static final int RC_TAG_ACTIVITY = 2;
    public static final int RC_QUERY_BUILDER_ACTIVITY = 3;
}
