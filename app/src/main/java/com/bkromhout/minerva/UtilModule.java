package com.bkromhout.minerva;

import android.app.Application;
import android.preference.PreferenceManager;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Utility Module.
 */
@Module
public class UtilModule {

    @Provides
    @Singleton
    Prefs providesPrefs(Application application) {
        return new Prefs(PreferenceManager.getDefaultSharedPreferences(application));
    }
}
