package com.bkromhout.minerva;

import android.app.Application;
import android.support.annotation.PluralsRes;
import android.support.v4.content.ContextCompat;
import com.bkromhout.minerva.data.UniqueIdFactory;
import com.bkromhout.minerva.realm.RTag;
import com.bkromhout.ruqus.Ruqus;
import com.karumi.dexter.Dexter;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;
import java.util.Arrays;

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

    /**
     * Utility component.
     */
    private AppComponent appComponent;

    /**
     * Dynamically loaded constants.
     */
    public D d;
    /**
     * Preferences.
     */
    @Inject
    public Prefs prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        // Stash application context.
        instance = this;
        d = new D(this);

        // Init components.
        appComponent = DaggerAppComponent.builder()
                                         .appModule(new AppModule(this))
                                         .build();
        appComponent.inject(this);

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
        if (prefs.doneFirstTimeInit()) return;

        // Put default new/updated book tag names.
        prefs.putNewBookTag(getString(R.string.default_new_book_tag));
        prefs.putUpdatedBookTag(getString(R.string.default_updated_book_tag));

        prefs.setFirstTimeInitDone();
    }

    /**
     * Set up Realm's default configuration.
     */
    protected void setupRealm() {
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
        int newBgColor = ContextCompat.getColor(this, R.color.green700);
        int updatedBgColor = ContextCompat.getColor(this, R.color.blue700);
        // Create default tags for new and updated books.
        realm.copyToRealm(Arrays.asList(
                new RTag(getString(R.string.default_new_book_tag), d.DEFAULT_TAG_TEXT_COLOR, newBgColor),
                new RTag(getString(R.string.default_updated_book_tag), d.DEFAULT_TAG_TEXT_COLOR, updatedBgColor)));
    }

    public static Minerva get() {
        if (instance == null) throw new IllegalStateException("The application isn't available yet.");
        return instance;
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }

    /**
     * Get a quantity string resource using the application context.
     * @param resId String resource ID.
     * @param count Quantity.
     * @return Quantity string.
     */
    public String getQString(@PluralsRes int resId, int count) {
        return getResources().getQuantityString(resId, count);
    }

    /**
     * Get a formatted quantity string resource using the application context.
     * @param resId      String resource ID.
     * @param count      Quantity.
     * @param formatArgs Format arguments.
     * @return Formatted quantity string.
     */
    public String getQString(@PluralsRes int resId, int count, Object... formatArgs) {
        return getResources().getQuantityString(resId, count, formatArgs);
    }
}
