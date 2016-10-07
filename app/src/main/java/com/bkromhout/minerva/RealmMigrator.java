package com.bkromhout.minerva;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

/**
 * Class responsible for migrating Realm data.
 */
class RealmMigrator implements RealmMigration {
    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();

        /*
         * Migrates to version 1 of the schema.
         * - Make uniqueId the primary key for the RBook class instead of the relative path.
         */
        if (oldVersion == 0) {
            schema.get("RBook")
                    .removePrimaryKey() // Remove @PrimaryKey from relPath.
                    .addIndex("relPath") // Add @Index to relPath.
                    .addPrimaryKey("uniqueId"); // Add @PrimaryKey to uniqueId.
            oldVersion++;
        }
    }
}
