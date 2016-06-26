package com.bkromhout.minerva;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;
import android.support.annotation.PluralsRes;
import android.support.v4.content.ContextCompat;
import com.bkromhout.minerva.data.BackupUtils;
import com.bkromhout.minerva.data.Importer;
import com.bkromhout.minerva.data.UniqueIdFactory;
import com.bkromhout.minerva.events.ShowRateMeDialogEvent;
import com.bkromhout.minerva.realm.RTag;
import com.bkromhout.minerva.util.CrashlyticsTree;
import com.bkromhout.ruqus.Ruqus;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.crashlytics.android.ndk.CrashlyticsNdk;
import com.karumi.dexter.Dexter;
import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.exceptions.RealmIOException;
import org.greenrobot.eventbus.EventBus;
import timber.log.Timber;

import java.util.Arrays;

/**
 * Custom Application class.
 * <p>
 * Minerva was the Roman goddess of wisdom, as well as one of my favorite characters from the Harry Potter series; what
 * more could one want in a name?
 */
public class Minerva extends Application {
    /**
     * Realm filename.
     */
    public static final String REALM_FILE_NAME = "minerva.realm";
    /**
     * Realm schema version.
     */
    private static final long REALM_SCHEMA_VERSION = 0;

    /**
     * Static INSTANCE of application context. Beware, this isn't available before the application starts.
     */
    private static Minerva INSTANCE;

    /**
     * Dynamically loaded constants.
     */
    private D d;
    /**
     * Preferences.
     */
    private Prefs prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();
        Fabric.with(this, crashlyticsKit, new CrashlyticsNdk());
        Timber.plant(new CrashlyticsTree());

        // Stash application context, then check to see if we need to restore things, and do so if necessary.
        INSTANCE = this;
        BackupUtils.restoreRealmFileIfApplicable();

        // Get global instances of certain classes.
        this.prefs = new Prefs(PreferenceManager.getDefaultSharedPreferences(this));
        this.d = new D(this);

        // Set up EventBus to use the generated index.
        EventBus.builder().addIndex(new EventBusIndex()).installDefaultEventBus();
        // Init Dexter.
        Dexter.initialize(this);
        // Init Ruqus.
        Ruqus.init(this);
        // Do first time init if needed.
        doFirstTimeInitIfNeeded();

        // Set up default RealmConfiguration.
        Realm.setDefaultConfiguration(new RealmConfiguration.Builder(this)
                .name(REALM_FILE_NAME)
                .schemaVersion(REALM_SCHEMA_VERSION)
                .initialData(this::initialRealmData)
                .build());

        // Do init that requires Realm. This also serves the purpose of allowing us to check and see if a DB restore
        // was successful (if one was performed).
        try (Realm realm = Realm.getDefaultInstance()) {
            // Initialize default unique ID factory.
            UniqueIdFactory.getInstance().initializeDefault(realm);
            // We got through Realm initialization, so we're good to delete the temporary Realm file that might exist
            // if we just restored the Realm.
            BackupUtils.removeTempRealmFile();
            // Validate a few things now that we've successfully restored the Realm DB.
            BackupUtils.doPostRestoreValidations(realm, prefs);
        } catch (RealmIOException e) {
            // We failed to open the restored Realm file, so try to roll back the changes.
            BackupUtils.rollBackFromDBRestore();

            // Try to do init again. If this still fails...well, I'm not really sure to be honest :(
            try (Realm realm = Realm.getDefaultInstance()) {
                // Initialize default unique ID factory.
                UniqueIdFactory.getInstance().initializeDefault(realm);
            }
        }

        // Trigger auto-import if needed.
        if (prefs.isLibAutoImport(false)) Importer.get().queueFullImport();

        // Send event to have MainActivity trigger "Rate Minerva" dialog if need be.
        if (prefs.shouldShowRateMeDialog()) EventBus.getDefault().postSticky(new ShowRateMeDialogEvent());
    }

    /**
     * Initializes some default data for the app the first time it runs.
     * <p>
     * This method is called BEFORE Realm is available!
     */
    private void doFirstTimeInitIfNeeded() {
        if (prefs.doneFirstTimeInit()) return;

        // Put default new/updated book tag names.
        prefs.putNewBookTag(getString(R.string.default_new_book_tag));
        prefs.putUpdatedBookTag(getString(R.string.default_updated_book_tag));

        prefs.setFirstTimeInitDone();
    }

    /**
     * Add initial data to Realm. Only runs on first app run (or after data has been cleared).
     * <p>
     * Keep in mind that currently this gets called before our {@link UniqueIdFactory} is ready.
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

    /**
     * Get a static reference to the application.
     * @return Static reference to Minerva.
     */
    public static Minerva get() {
        if (INSTANCE == null) throw new IllegalStateException("Minerva isn't available yet.");
        return INSTANCE;
    }

    /**
     * Get the preference wrapper class.
     * @return {@link Prefs}.
     */
    public static Prefs prefs() {
        return get().prefs;
    }

    /**
     * Get the dynamically-loaded "constants" holder class.
     * @return {@link D}.
     */
    public static D d() {
        return get().d;
    }

    /**
     * Restarts the application.
     * <p>
     * 10 points to Gryffindor if you're just browsing my code and get the reference, and 10 additional points if you
     * understand that the name isn't quite apt given what the method does ;)
     * @param activity Activity to finish.
     */
    public static void rennervate(Activity activity) {
        INSTANCE.startActivity(getRestartIntent());
        activity.finish();
        // Avada Kedavra!
        Runtime.getRuntime().exit(0);
    }

    /**
     * Get the intent which would normally be used by the launcher to start Minerva.
     * @return App launch intent.
     */
    private static Intent getRestartIntent() {
        Intent restartIntent = new Intent(Intent.ACTION_MAIN, null);
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        restartIntent.addCategory(Intent.CATEGORY_DEFAULT);

        String packageName = INSTANCE.getPackageName();
        PackageManager packageManager = INSTANCE.getPackageManager();
        for (ResolveInfo resolveInfo : packageManager.queryIntentActivities(restartIntent, 0)) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo.packageName.equals(packageName)) {
                restartIntent.setComponent(new ComponentName(packageName, activityInfo.name));
                return restartIntent;
            }
        }

        throw new IllegalStateException("Unable to determine default activity for " + packageName
                + ". Does an activity specify the DEFAULT category in its intent filter?");
    }
}
