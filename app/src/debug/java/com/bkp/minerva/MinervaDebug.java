package com.bkp.minerva;

import com.facebook.stetho.Stetho;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

/**
 * Debug version of custom Application class.
 * @see Minerva
 */
public class MinervaDebug extends Minerva {

    @Override
    public void onCreate() {
        super.onCreate();
        initStetho();
    }

    /**
     * Initialize Stetho.
     */
    private void initStetho() {
        Stetho.initialize(Stetho.newInitializerBuilder(this)
                                .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                                .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                                .build());
    }
}
