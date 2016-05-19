package com.bkromhout.minerva;

import com.bkromhout.minerva.data.ImportLogger;
import com.bkromhout.minerva.data.Importer;
import dagger.Component;

import javax.inject.Singleton;

/**
 * Component which injects app-level objects.
 */
@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {
    void inject(Minerva application);

    // Expose to sub-graphs.
    Minerva minerva();
    Prefs prefs();
    Importer importer();
    ImportLogger importLogger();
}
