package com.bkromhout.minerva.realm;

import io.realm.*;
import timber.log.Timber;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Slightly modified version of the PrimaryKeyFactory class from Github user zacheusz on <a
 * href="https://github.com/realm/realm-java/issues/469#issuecomment-197621062">this issue</a> in realm/realm-java.
 */
public class UniqueIdFactory {
    /**
     * Unique ID field name.
     */
    private static final String UNIQUE_ID_FIELD = "uniqueId";

    /**
     * Singleton instance.
     */
    private final static UniqueIdFactory instance = new UniqueIdFactory();

    /**
     * Maximum unique ID values.
     */
    private Map<Class<? extends RealmModel>, AtomicLong> ids;

    /**
     * Get the singleton instance.
     * @return singleton instance.
     */
    public static UniqueIdFactory getInstance() {
        return instance;
    }

    /**
     * Initialize the factory. Must be called before any unique IDs are generated - preferably from application class.
     */
    public synchronized void initialize(final Realm realm) {
        if (ids != null) throw new IllegalStateException("Already initialized.");
        // ids field is used as an initialization flag at the same time.
        ids = new HashMap<>();
        final RealmConfiguration configuration = realm.getConfiguration();
        final RealmSchema realmSchema = realm.getSchema();

        // Using RealmConfiguration#getRealmObjectClasses because RealmSchema#getAll() returns RealmObjectSchema with
        // simple class names only.
        for (final Class<? extends RealmModel> c : configuration.getRealmObjectClasses()) {

            final RealmObjectSchema objectSchema = realmSchema.get(c.getSimpleName());
            if (objectSchema != null && objectSchema.hasPrimaryKey()) {
                Timber.d("Attempting to initialize unique ID factory for %s.", objectSchema.getClassName());
                try {
                    Number keyValue = realm.where(c).max(UNIQUE_ID_FIELD);
                    if (keyValue != null) ids.put(c, new AtomicLong(keyValue.longValue()));
                } catch (ArrayIndexOutOfBoundsException ignored) {
                    // Some classes don't have uniqueId fields.
                    Timber.d("%s doesn't have a uniqueId field.", objectSchema.getClassName());
                } catch (IllegalArgumentException ignored) {
                    // If we don't have any data yet, then Realm will think the field doesn't exist, and throws this.
                    Timber.e(ignored, "Couldn't initialize unique ID factory for %s.", objectSchema.getClassName());
                }
            }
        }
    }

    /**
     * Automatically create next unique ID for a given class.
     */
    public synchronized long nextId(final Class<? extends RealmModel> clazz) {
        if (ids == null) throw new IllegalStateException("Not initialized yet.");

        AtomicLong l = ids.get(clazz);
        if (l == null) {
            // RealmConfiguration#getRealmObjectClasses() returns only classes with existing instances so we need to
            // store value for the first instance created.
            l = new AtomicLong(0);
            ids.put(clazz, l);
        }
        return l.incrementAndGet();
    }

}
