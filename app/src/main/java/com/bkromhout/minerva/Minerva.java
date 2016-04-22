package com.bkromhout.minerva;

import android.app.Application;
import android.content.Context;
import com.bkromhout.minerva.realm.UniqueIdFactory;
import com.bkromhout.ruqus.Ruqus;
import com.karumi.dexter.Dexter;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import org.greenrobot.eventbus.EventBus;

/**
 * Custom Application class.
 */
public class Minerva extends Application {
    private static final String REALM_FILE_NAME = "minerva.realm";
    private static final long REALM_SCHEMA_VERSION = 0;

    /**
     * Static instance to (CAREFULLY) allow getting Application Context anywhere.
     */
    private static Minerva instance;

    @Override
    public void onCreate() {
        super.onCreate();
        // Stash application context.
        instance = this;
        // Set up EventBus to use the generated index.
        EventBus.builder().addIndex(new EventBusIndex()).installDefaultEventBus();
        // Init Dexter.
        Dexter.initialize(this);
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
     * Set up Realm's default configuration.
     */
    private void setupRealm() {
        Realm.setDefaultConfiguration(new RealmConfiguration.Builder(this)
                .name(REALM_FILE_NAME)
                .schemaVersion(REALM_SCHEMA_VERSION)
                .build());
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
