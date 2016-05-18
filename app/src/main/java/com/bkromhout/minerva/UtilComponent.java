package com.bkromhout.minerva;

import com.bkromhout.minerva.activities.*;
import com.bkromhout.minerva.fragments.AllListsFragment;
import com.bkromhout.minerva.fragments.LibraryFragment;
import com.bkromhout.minerva.fragments.PowerSearchFragment;
import com.bkromhout.minerva.fragments.RecentFragment;
import dagger.Component;

import javax.inject.Singleton;

/**
 * Component which injects utility-related objects.
 */
@Singleton
@Component(modules = {AppModule.class, UtilModule.class})
public interface UtilComponent {
    void inject(Minerva application);

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
}
