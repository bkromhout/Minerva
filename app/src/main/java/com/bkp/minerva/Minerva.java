package com.bkp.minerva;

import android.app.Application;
import com.tumblr.remember.Remember;

/**
 * Custom Application class.
 */
public class Minerva extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Init Remember.
        Remember.init(getApplicationContext(), "remember-prefs");
    }
}
