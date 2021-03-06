/*
 * Copyright 2016 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bkromhout.minerva.test;

import android.content.Context;
import android.content.res.AssetManager;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertTrue;

/**
 * Rule that creates the {@link RealmConfiguration } in a temporary directory and deletes the Realm created with that
 * configuration once the test finishes. Be sure to close all Realm instances before finishing the test. Otherwise
 * {@link Realm#deleteRealm(RealmConfiguration)} will throw an exception in the {@link #after()} method. The temp
 * directory will be deleted regardless if the {@link Realm#deleteRealm(RealmConfiguration)} fails or not.
 * <p>
 * This class was taken from the realm/realm-java repo, I place no claims on it, or any modifications I may have made to
 * it.
 */
public class TestRealmConfigurationFactory extends TemporaryFolder {
    private Map<RealmConfiguration, Boolean> map = new ConcurrentHashMap<>();
    private Set<RealmConfiguration> configurations = Collections.newSetFromMap(map);
    private boolean unitTestFailed = false;
    private final Context context;

    public TestRealmConfigurationFactory(Context context) {
        this.context = context;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    base.evaluate();
                } catch (Throwable throwable) {
                    unitTestFailed = true;
                    throw throwable;
                } finally {
                    after();
                }
            }
        };
    }

    @Override
    protected void before() throws Throwable {
        super.before();
    }

    @Override
    protected void after() {
        try {
            for (RealmConfiguration configuration : configurations)
                Realm.deleteRealm(configuration);
        } catch (IllegalStateException e) {
            // Only throw the exception caused by deleting the opened Realm if the test case itself doesn't throw.
            if (!unitTestFailed) throw e;
        } finally {
            // This will delete the temp folder.
            super.after();
        }
    }

    public RealmConfiguration createConfiguration() {
        RealmConfiguration configuration = new RealmConfiguration.Builder()
                .directory(getRoot())
                .build();

        configurations.add(configuration);
        return configuration;
    }

    public RealmConfiguration createConfiguration(String subDir, String name) {
        final File folder = new File(getRoot(), subDir);
        assertTrue(folder.mkdirs());
        RealmConfiguration configuration = new RealmConfiguration.Builder()
                .directory(folder)
                .name(name)
                .build();

        configurations.add(configuration);
        return configuration;
    }

    public RealmConfiguration createConfiguration(String name) {
        RealmConfiguration configuration = new RealmConfiguration.Builder()
                .directory(getRoot())
                .name(name)
                .build();

        configurations.add(configuration);
        return configuration;
    }

    public RealmConfiguration createConfiguration(String name, byte[] key) {
        RealmConfiguration configuration = new RealmConfiguration.Builder()
                .directory(getRoot())
                .name(name)
                .encryptionKey(key)
                .build();

        configurations.add(configuration);
        return configuration;
    }

    public RealmConfiguration.Builder createConfigurationBuilder() {
        return new RealmConfiguration.Builder().directory(getRoot());
    }

    // Copies a Realm file from assets to temp dir
    public void copyRealmFromAssets(Context context, String realmPath, String newName) throws IOException {
        // Delete the existing file before copy
        RealmConfiguration configToDelete = new RealmConfiguration.Builder()
                .directory(getRoot())
                .name(newName)
                .build();
        Realm.deleteRealm(configToDelete);

        AssetManager assetManager = context.getAssets();
        InputStream is = assetManager.open(realmPath);
        File file = new File(getRoot(), newName);
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] buf = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(buf)) > -1)
            outputStream.write(buf, 0, bytesRead);
        outputStream.close();
        is.close();
    }
}