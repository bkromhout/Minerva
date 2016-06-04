package com.bkromhout.minerva.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.view.menu.MenuBuilder;
import android.text.format.DateUtils;
import android.view.Menu;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.events.MissingPermEvent;
import org.greenrobot.eventbus.EventBus;
import timber.log.Timber;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Utility functions class.
 */
public class Util {
    /**
     * Uses some clever trickery to make it so that menu items in the popup menu still show their icons. (Very hacky)
     * @param menu            Menu to force icons for.
     * @param classSimpleName Class name, used for potential logging.
     */
    public static void forceMenuIcons(Menu menu, String classSimpleName) {
        if (menu != null) {
            // Make sure all icons are tinted the correct color, including those in the overflow menu.
            for (int i = 0; i < menu.size(); i++)
                menu.getItem(i).getIcon().setColorFilter(
                        ContextCompat.getColor(Minerva.get(), R.color.textColorPrimary), PorterDuff.Mode.SRC_IN);
            // And use a bit of reflection to ensure we show icons even in the overflow menu.
            if (menu.getClass().equals(MenuBuilder.class)) {
                try {
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (Exception e) {
                    Timber.tag(classSimpleName);
                    Timber.e(e, "onMenuOpened...unable to set icons for overflow menu");
                }
            }
        }
    }

    /**
     * Using a long time value, get a string which expresses the date and time relative to now. Minimum resolution is
     * seconds, transition resolution is one week, and we show the time when we transition to absolute.
     * @param time The time in milliseconds.
     * @return Date time string.
     */
    public static String getRelTimeString(long time) {
        return DateUtils.getRelativeDateTimeString(Minerva.get(), time, DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME).toString();
    }

    /**
     * Get a tinted drawable.
     * @param drawableRes The drawable resource to use.
     * @param colorRes    The color resource to use.
     * @return Tinted drawable.
     */
    public static Drawable getTintedDrawable(@DrawableRes int drawableRes, @ColorRes int colorRes) {
        Drawable drawable = ContextCompat.getDrawable(Minerva.get(), drawableRes);
        DrawableCompat.setTint(drawable, ContextCompat.getColor(Minerva.get(), colorRes));
        return drawable;
    }

    /**
     * Start the given activity from the given context, passing the given bundle of extras to it.
     * @param ctx      The context to start the activity from.
     * @param actClass The class of the activity to start.
     * @param params   The bundle of extras to pass to the activity. Can be null.
     */
    public static void startAct(Context ctx, Class<? extends Activity> actClass, Bundle params) {
        Intent intent = new Intent(ctx, actClass);
        if (params != null) intent.putExtras(params);
        ctx.startActivity(intent);
    }

    /**
     * Opens the system app settings activity. Usually used so that the user can grant permissions.
     */
    public static void openAppInfo() {
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + Minerva.get().getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Minerva.get().startActivity(myAppSettings);
    }

    /**
     * Checks to see if the app currently holds the WRITE_EXTERNAL_STORAGE permission, and fires a {@link
     * MissingPermEvent} if it doesn't.
     * @param actionId ID of the action to take if we're granted the storage permission after asking for it.
     * @return False if we had to fire the event because we don't have the permission. True if we have the permission.
     */
    public static boolean checkForStoragePermAndFireEventIfNeeded(@IdRes int actionId) {
        if (hasPerm(Manifest.permission.WRITE_EXTERNAL_STORAGE)) return true;

        EventBus.getDefault().post(new MissingPermEvent(Manifest.permission.WRITE_EXTERNAL_STORAGE, actionId));
        return false;
    }

    /**
     * Checks to see if we currently hold the given {@code permission}.
     * @param permission The permission to check.
     * @return True if we currently have been granted the permission, otherwise false.
     */
    public static boolean hasPerm(String permission) {
        return ContextCompat.checkSelfPermission(Minerva.get(), permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Try to resolve the given path to a File object representing a valid, readable directory.
     * @param dirPath The path to the directory.
     * @return The File object for the directory, or null if we had issues.
     */
    public static File tryResolveDir(String dirPath) {
        if (dirPath == null || dirPath.isEmpty()) return null;
        // Check that the directory exists and that we can read it.
        File dir = new File(dirPath);
        return (dir.exists() && dir.isDirectory() && dir.canRead()) ? dir : null;
    }

    /**
     * Returns the extension from a file name in lowercase.
     * @param fileName File name.
     * @return Extension (in lowercase), or null.
     */
    public static String getExtFromFName(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) return null;
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Returns a File using the given {@code relPath} and the currently defined library path.
     * @param relPath The path to the book file, relative to the library path.
     * @return A File, or null.
     */
    public static File getFileFromRelPath(String relPath) {
        String libDir = Minerva.prefs().getLibDir(null);
        if (libDir == null || libDir.isEmpty() || relPath == null || relPath.isEmpty()) return null;
        File file = new File(libDir, relPath);
        return file.exists() ? file : null;
    }

    /**
     * Returns a File using the given {@code baseDir} and the given {@code relPath}.
     * @param baseDir The base directory of the file. Must exist.
     * @param relPath The path to the file relative to {@code baseDir}.
     * @return A File, or null.
     */
    public static File getFileFromRelPath(File baseDir, String relPath) {
        if (baseDir == null || !baseDir.exists() || relPath == null || relPath.isEmpty()) return null;
        File file = new File(baseDir, relPath);
        return file.exists() ? file : null;
    }
}
