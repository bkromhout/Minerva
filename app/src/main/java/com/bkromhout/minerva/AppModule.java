package com.bkromhout.minerva;

import android.preference.PreferenceManager;
import com.bkromhout.minerva.data.ImportLogger;
import com.bkromhout.minerva.data.Importer;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Application module.
 */
@Module
public class AppModule {
    private final Minerva application;

    public AppModule(Minerva application) {
        this.application = application;
    }

    /**
     * Provide {@link Minerva}.
     * @return {@link Minerva}.
     */
    @Provides
    @Singleton
    Minerva providesApplication() {
        return application;
    }

    /**
     * Provide {@link Prefs}.
     * @return {@link Prefs}
     */
    @Provides
    @Singleton
    Prefs providesPrefs() {
        return new Prefs(PreferenceManager.getDefaultSharedPreferences(application));
    }

    @Provides
    @Singleton
    Importer providesImporter() {
        return Importer.get();
    }

    @Provides
    @Singleton
    ImportLogger providesImportLogger() {
        return ImportLogger.get();
    }
}
