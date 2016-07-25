package com.bkromhout.minerva.data;

import io.realm.*;
import timber.log.Timber;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class which ensures that we can generate unique IDs for our objects.
 * <p>
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
     * Map of Realm model classes to {@code AtomicLong} objects used to generate unique IDs under normal circumstances
     * (i.e., {@link #tempIds} is {@code null}).
     */
    private Map<Class<? extends RealmModel>, AtomicLong> ids;
    /**
     * Same thing as {@link #ids}, but will be used instead of {@link #ids} when not {@code null}.
     */
    private Map<Class<? extends RealmModel>, AtomicLong> tempIds;

    /**
     * Get the singleton instance.
     * @return singleton instance.
     */
    public static UniqueIdFactory getInstance() {
        return instance;
    }

    /**
     * Initialize the factory and the default ID map. Must be called before any unique IDs are generated, preferably
     * from application class.
     * @param realm Instance of Realm to generate IDs for by default.
     */
    public synchronized void initializeDefault(final Realm realm) {
        if (ids != null) throw new IllegalStateException("Already initialized default ID map.");
        ids = new HashMap<>();
        initIdMap(realm, ids, false);
    }

    /**
     * Returns whether or not temporary unique IDs are currently set up.
     * @return {@code true} if temporary IDs are set up and will be provided, {@code false} if not (default IDs will be
     * returned).
     */
    public synchronized boolean areTempIdsSetUp() {
        return tempIds != null;
    }

    /**
     * Initializes a new map to be used for creating unique IDs based on the given {@code realm}. Until {@link
     * #tearDownTempIds()} is called, this temporary map will be used to return unique IDs when {@link #nextId(Class)}
     * is called.
     * @param realm Instance of Realm to use to set up the temporary unique IDs map.
     */
    public synchronized void setUpTempIds(final Realm realm) {
        if (ids == null) throw new IllegalStateException("Default ID map must be initialized first.");
        if (tempIds != null) throw new IllegalStateException("Already initialized temporary ID map.");
        tempIds = new HashMap<>();
        initIdMap(realm, tempIds, true);
    }

    /**
     * Removes the previously set-up temporary ID map so that the default one will be used again.
     */
    public synchronized void tearDownTempIds() {
        tempIds = null;
    }

    /**
     * Automatically create next unique ID for a given class.
     * <p>
     * If {@link #areTempIdsSetUp()} would return {@code true}, this will return unique IDs from the currently set up
     * temporary IDs map instead of the normal one. This can be useful in situations (such as testing) where we wish to
     * ensure specific conditions for IDs.
     */
    public synchronized long nextId(final Class<? extends RealmModel> clazz) {
        // Don't allow any IDs to be retrieved if we haven't
        if (ids == null) throw new IllegalStateException("Default ID map not initialized yet.");
        return nextIdInternal(clazz, tempIds == null ? ids : tempIds);
    }

    /**
     * Returns a unique ID for the given {@code clazz} using the {@code AtomicLong}s which are stored in the given
     * {@code idMap}.
     * <p>
     * If the given {@code clazz} doesn't currently have an entry in the map, one will be created.
     * @param clazz Realm model class to return a unique ID for.
     * @param idMap Map of model classes to {@code AtomicLong}s.
     * @return Unique ID.
     */
    private static long nextIdInternal(final Class<? extends RealmModel> clazz,
                                       Map<Class<? extends RealmModel>, AtomicLong> idMap) {
        AtomicLong l = idMap.get(clazz);
        if (l == null) {
            // RealmConfiguration#getRealmObjectClasses() returns only classes with existing instances so we need to
            // store value for the first instance created.
            l = new AtomicLong(0);
            idMap.put(clazz, l);
        }
        return l.incrementAndGet();
    }

    /**
     * Initializes a unique ID map for the given {@code realm}, and stores it in {@code idMap}.
     * @param realm  Realm to initialize a unique ID map for.
     * @param idMap  Reference to actual map object to store entries in.
     * @param isTemp If this map will be used for temporary IDs.
     */
    private static void initIdMap(Realm realm, Map<Class<? extends RealmModel>, AtomicLong> idMap, boolean isTemp) {
        final RealmConfiguration configuration = realm.getConfiguration();
        final RealmSchema realmSchema = realm.getSchema();

        // Using RealmConfiguration#getRealmObjectClasses because RealmSchema#getAll() returns RealmObjectSchema with
        // simple class names only.
        for (final Class<? extends RealmModel> c : configuration.getRealmObjectClasses()) {
            final RealmObjectSchema objectSchema = realmSchema.get(c.getSimpleName());
            if (objectSchema != null && objectSchema.hasPrimaryKey()) {
                Timber.d("Attempting to initialize %s unique ID factory for %s.", isTemp ? "temporary" : "default",
                        objectSchema.getClassName());
                try {
                    Number keyValue = realm.where(c).max(UNIQUE_ID_FIELD);
                    if (keyValue != null) idMap.put(c, new AtomicLong(keyValue.longValue()));
                } catch (ArrayIndexOutOfBoundsException ignored) {
                    // Some classes don't have uniqueId fields.
                    Timber.d("%s doesn't have a uniqueId field.", objectSchema.getClassName());
                } catch (IllegalArgumentException ignored) {
                    // If we don't have any data yet, then Realm will think the field doesn't exist, and throws this.
                    Timber.d("Couldn't initialize %s unique ID factory for %s.", isTemp ? "temporary" : "default",
                            objectSchema.getClassName());
                }
            }
        }
    }
}
