package com.bkromhout.minerva;

import com.facebook.stetho.Stetho;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;
import timber.log.Timber;

/**
 * Debug version of custom Application class.
 * @see Minerva
 */
public class MinervaDebug extends Minerva {

    @Override
    public void onCreate() {
        Timber.plant(new Timber.DebugTree());
        super.onCreate();
        initStetho();
    }

    /**
     * Initialize Stetho.
     */
    private void initStetho() {
        Stetho.initialize(Stetho.newInitializerBuilder(this)
                                .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                                .enableWebKitInspector(RealmInspectorModulesProvider.builder(this)
                                                                                    .withMetaTables()
                                                                                    .build())
                                .build());
    }
}
