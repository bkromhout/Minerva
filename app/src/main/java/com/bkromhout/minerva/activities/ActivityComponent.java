package com.bkromhout.minerva.activities;

import android.support.v7.app.AppCompatActivity;
import com.bkromhout.minerva.AppComponent;
import com.bkromhout.minerva.fragments.AllListsFragment;
import com.bkromhout.minerva.fragments.LibraryFragment;
import com.bkromhout.minerva.fragments.PowerSearchFragment;
import com.bkromhout.minerva.fragments.RecentFragment;
import com.bkromhout.minerva.mvp.PerActivity;
import dagger.Component;

/**
 * Created by bkromhout on 5/18/16.
 */
@PerActivity
@Component(dependencies = {AppComponent.class}, modules = {ActivityModule.class})
public interface ActivityComponent {
    void inject(BookInfoActivity activity);

    void inject(BookListActivity activity);

    void inject(ImportActivity activity);

    void inject(MainActivity activity);

    void inject(SettingsActivity activity);

    void inject(TaggingActivity activity);

    void inject(WelcomeActivity activity);

    void inject(AllListsFragment fragment);

    void inject(LibraryFragment fragment);

    void inject(PowerSearchFragment fragment);

    void inject(RecentFragment fragment);

    void inject(SettingsActivity.SettingsFragment fragment);

    // Exposed to sub-graphs.
    AppCompatActivity activity();
}
