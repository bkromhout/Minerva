package com.bkp.minerva;

import android.app.Application;
import android.content.Context;

/**
 * Custom Application class.
 */
public class Minerva extends Application {
    /**
     * Static instance to (CAREFULLY) allow getting Application Context anywhere.
     */
    private static Minerva instance;

    @Override
    public void onCreate() {
        super.onCreate();
        // Stash application context.
        instance = this;
    }

    /**
     * Get the application context. DO NOT use the context returned by this method in methods which affect the UI (such
     * as when inflating a layout, for example).
     * @return Application context.
     */
    public static Context getAppCtx() {
        return instance.getApplicationContext();
    }

}
