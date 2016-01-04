package com.bkp.minerva.util;

import android.app.Activity;
import android.content.Intent;
import com.bkp.minerva.R;

/**
 * Helps with theme-related tasks.
 */
public class ThemeUtils {
    private static int sTheme;

    public final static int THEME_LIGHT = 0;
    public final static int THEME_DARK = 1;

    public static void changeToTheme(Activity activity, int theme) {
        sTheme = theme;
        activity.finish();
        activity.startActivity(new Intent(activity, activity.getClass()));
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    public static void onActivityCreateSetTheme(Activity activity) {
        switch (sTheme) {
            default:
            case THEME_LIGHT:
                activity.setTheme(R.style.AppTheme);
                break;
            case THEME_DARK:
                activity.setTheme(R.style.AppTheme_Dark);
                break;
        }
    }

}
