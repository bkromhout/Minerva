package com.bkromhout.minerva;

import android.app.Application;
import android.content.Context;
import com.bkromhout.ruqus.Ruqus;
import com.karumi.dexter.Dexter;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import org.greenrobot.eventbus.EventBus;

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
        // Set up EventBus to use the generated index.
        EventBus.builder().addIndex(new EventBusIndex()).installDefaultEventBus();
        // Init Dexter.
        Dexter.initialize(this);
        // Set up Realm.
        setupRealm();
        // Init Ruqus.
        Ruqus.init(this);
    }

    /**
     * Set up Realm's default configuration.
     */
    private void setupRealm() {
        RealmConfiguration config = new RealmConfiguration.Builder(this)
                .name("minerva.realm")
                .schemaVersion(0)
                .build();
        Realm.setDefaultConfiguration(config);
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
