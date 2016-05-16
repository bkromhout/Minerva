package com.bkromhout.minerva;

import android.app.Application;
import android.content.Context;
import com.bkromhout.minerva.data.UniqueIdFactory;
import com.bkromhout.minerva.prefs.DefaultPrefs;
import com.bkromhout.minerva.realm.RTag;
import com.bkromhout.ruqus.Ruqus;
import com.karumi.dexter.Dexter;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

/**
 * Custom Application class.
 */
public class Minerva extends Application {
    /**
     * Realm filename.
     */
    private static final String REALM_FILE_NAME = "minerva.realm";
    /**
     * Realm schema version.
     */
    private static final long REALM_SCHEMA_VERSION = 0;

    /**
     * Static instance of application context. Beware, this isn't available before the application starts.
     */
    private static Minerva instance;

    @Override
    public void onCreate() {
        super.onCreate();
        // Stash application context.
        instance = this;
        // Load certain resources into memory for fast access.
        C.init(this);
        // Set up EventBus to use the generated index.
        EventBus.builder().addIndex(new EventBusIndex()).installDefaultEventBus();
        // Init Dexter.
        Dexter.initialize(this);
        // Do first time init if needed.
        doFirstTimeInitIfNeeded();
        // Set up Realm.
        setupRealm();
        // Initialize UniqueIdFactory.
        try (Realm realm = Realm.getDefaultInstance()) {
            UniqueIdFactory.getInstance().initialize(realm);
        }
        // Init Ruqus.
        Ruqus.init(this);
    }

    /**
     * Initializes some default data for the app the first time it runs.
     */
    private void doFirstTimeInitIfNeeded() {
        if (DefaultPrefs.get().doneFirstTimeInit()) return;

        // Put default new/updated book tag names.
        DefaultPrefs.get().putNewBookTag(C.getStr(R.string.default_new_book_tag));
        DefaultPrefs.get().putUpdatedBookTag(C.getStr(R.string.default_updated_book_tag));
    }

    /**
     * Set up Realm's default configuration.
     */
    private void setupRealm() {
        Realm.setDefaultConfiguration(new RealmConfiguration.Builder(this)
                .name(REALM_FILE_NAME)
                .schemaVersion(REALM_SCHEMA_VERSION)
                .initialData(this::initialRealmData)
                .build());
    }

    /**
     * Add initial data to Realm. Only runs on first app run (or after data has been cleared).
     * @param realm Instance of Realm to use to add data.
     */
    private void initialRealmData(Realm realm) {
        // Create default tags for new and updated books.
        ArrayList<RTag> defaultTags = new ArrayList<RTag>() {{
            new RTag(C.getStr(R.string.default_new_book_tag));
            new RTag(C.getStr(R.string.default_updated_book_tag));
        }};
        realm.copyToRealm(defaultTags);
    }

    /**
     * Get the application context. DO NOT use the context returned by this method in methods which affect the UI (such
     * as when inflating a layout, for example).
     * @return Application context.
     */
    public static Context getAppCtx() {
        if (instance == null) throw new IllegalStateException("The application context isn't available yet.");
        return instance.getApplicationContext();
    }
}
