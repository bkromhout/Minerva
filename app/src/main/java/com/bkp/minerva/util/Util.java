package com.bkp.minerva.util;

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
import android.support.annotation.IdRes;
import android.support.v4.content.ContextCompat;
import com.bkp.minerva.MainActivity;
import com.bkp.minerva.Minerva;
import com.bkp.minerva.R;
import com.bkp.minerva.prefs.DefaultPrefs;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;

import java.io.*;

/**
 * Utility functions class.
 */
public class Util {

    /**
     * Mutates the given {@code drawable}, then tints it using {@link android.graphics.PorterDuff.Mode#SRC_IN} to the
     * color defined by the given {@code colorResId}.
     * @param drawable   Drawable to tint.
     * @param colorResId The resource ID for a color resource.
     */
    public static void tintDrawable(Context ctx, Drawable drawable, @ColorRes int colorResId) {
        int color = ContextCompat.getColor(ctx, colorResId);
        drawable.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
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
     * Takes a fragment constant integer (see the top of {@link MainActivity}) and returns the Android resource ID for
     * the item in the nav drawer which corresponds to that fragment.
     * @param frag Fragment integer constant.
     * @return Nav drawer item resource ID.
     */
    @IdRes
    public static int navIdFromFragConst(int frag) {
        switch (frag) {
            case MainActivity.FRAG_RECENT:
                return R.id.nav_recent;
            case MainActivity.FRAG_LIBRARY:
                return R.id.nav_library;
            case MainActivity.FRAG_ALL_LISTS:
                return R.id.nav_all_lists;
            case MainActivity.FRAG_POWER_SEARCH:
                return R.id.nav_power_search;
            default:
                return -1;
        }
    }

    /**
     * Checks to see if we currently hold the given {@code permission}.
     * @param permission The permission to check.
     * @return True if we currently have been granted the permission, otherwise false.
     */
    public static boolean hasPerm(String permission) {
        return ContextCompat.checkSelfPermission(Minerva.getAppCtx(), permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Opens the system app settings activity. Usually used so that the user can grant permissions.
     * @param context The context to use to build the intent.
     */
    public static void openAppInfo(Context context) {
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + context.getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(myAppSettings);
    }

    /**
     * Checks whether there is a present, valid library directory set.
     * @return True if there is a path set and the path points to a valid folder that is readable.
     */
    public static boolean hasValidLibDir() {
        return tryResolveDir(DefaultPrefs.get().getLibDir(null)) != null;
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
     * Get an ePub Book object from a file object.
     * @param file The file to try and read as an ePub
     * @return Book object, or null if there were issues.
     */
    public static Book readEpubFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) return null;

        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            return new EpubReader().readEpub(in);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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
}
