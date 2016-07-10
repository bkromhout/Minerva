package com.bkromhout.minerva;

/**
 * Constants.
 */
public final class C {
    /**
     * 5 seconds duration for snackbar.
     */
    public static final int SB_LENGTH_5_SEC = 5000;
    /**
     * This is the item type integer we supply to a recycler view for our empty footer item.
     */
    public static final int FOOTER_ITEM_TYPE = -1;
    /**
     * The initial number of position numbers between each item in a book list.
     */
    public static final long LIST_ITEM_GAP = 100L;

    /* Keys */
    public static final String POS_TO_UPDATE = "position_to_update";
    public static final String NEEDS_POS_UPDATE = "needs_position_update";
    public static final String REL_PATH = "relative_path";
    public static final String UNIQUE_ID = "unique_id";
    public static final String IS_IN_ACTION_MODE = "is_in_action_mode";
    public static final String SEARCH_STRING = "search_string";
    public static final String RUQ = "realm_user_query";
    public static final String HAS_CHANGED = "has_changed";
    public static final String RESTORE_PATH = "restore_path";

    /* Request codes. */
    public static final int RC_WELCOME_ACTIVITY = 1;
    public static final int RC_TAG_ACTIVITY = 2;
    public static final int RC_QUERY_BUILDER_ACTIVITY = 3;
}
