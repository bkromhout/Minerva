package com.bkromhout.minerva.activities;

import android.support.v7.app.AppCompatActivity;
import com.bkromhout.minerva.mvp.PerActivity;
import dagger.Module;
import dagger.Provides;

/**
 * Activity module.
 */
@Module
public class ActivityModule {
    private final AppCompatActivity activity;

    public ActivityModule(AppCompatActivity activity) {
        this.activity = activity;
    }

    @Provides
    @PerActivity
    AppCompatActivity providesActivity() {
        return activity;
    }
}
