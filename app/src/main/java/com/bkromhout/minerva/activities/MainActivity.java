package com.bkromhout.minerva.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.Prefs;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.data.BackupUtils;
import com.bkromhout.minerva.data.Importer;
import com.bkromhout.minerva.enums.MainFrag;
import com.bkromhout.minerva.events.PermGrantedEvent;
import com.bkromhout.minerva.events.ShowRateMeDialogEvent;
import com.bkromhout.minerva.fragments.AllListsFragment;
import com.bkromhout.minerva.fragments.LibraryFragment;
import com.bkromhout.minerva.fragments.PowerSearchFragment;
import com.bkromhout.minerva.fragments.RecentFragment;
import com.bkromhout.minerva.util.Dialogs;
import com.bkromhout.minerva.util.Util;
import io.realm.Realm;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;

/**
 * Main activity, responsible for hosting fragments.
 */
public class MainActivity extends PermCheckingActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TITLE = "TITLE";

    // Views.
    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.nav_view)
    NavigationView navigationView;
    @BindView(R.id.main_frag_cont)
    FrameLayout fragCont;

    /**
     * Instance of Realm.
     */
    private Realm realm;
    /**
     * Nav Drawer Toggle (burger menu).
     */
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get prefs and Realm.
        realm = Realm.getDefaultInstance();

        // Set theme, create and bind views.
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Set up toolbar.
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set up the nav drawer and its toggle.
        initDrawer();

        // Ensure that the same fragment is selected as was last time.
        if (savedInstanceState == null) {
            // Make sure we show the library fragment if we don't have a saved instance state, don't have a saved
            // current fragment, or if the saved current fragment would need some bundle to help populate it.
            MainFrag frag = Minerva.prefs().getCurrFrag(MainFrag.LIBRARY);
            switchFragments(frag);
            navigationView.setCheckedItem(frag.getIdRes());
        } else {
            // Make sure we set the title back to what it was otherwise.
            setTitle(savedInstanceState.getString(TITLE));
        }

        // Check to see if we need to show the welcome activity.
        if (savedInstanceState == null && !Minerva.prefs().introCompleted())
            startActivityForResult(new Intent(this, WelcomeActivity.class), C.RC_WELCOME_ACTIVITY);

        // Handle permissions. Make sure we continue a request process if applicable.
        initAndContinuePermChecksIfNeeded();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == C.RC_WELCOME_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                Minerva.prefs().setIntroCompleted();
                // If we want to restore a database backup, we'll do it now. Otherwise, we'll start importing.
                if (data != null && data.hasExtra(C.RESTORE_PATH)) {
                    // We're going to go under the assumption that a user savvy enough to want to restore a backup will
                    // understand Minerva needs the storage permission to do so, and won't have taken the effort to
                    // leave during the intro flow, open App Info, deny the permission, then come back and try anyway.
                    File restoreFile = new File(data.getStringExtra(C.RESTORE_PATH));
                    BackupUtils.prepareToRestoreRealmFile(this, restoreFile);
                    finish();
                } else {
                    if (!Util.checkForStoragePermAndFireEventIfNeeded(R.id.action_first_import)) return;
                    doFirstImport();
                }
            } else finish();
        }
        // Pass down to fragments.
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Initializes the Navigation Drawer.
     */
    private void initDrawer() {
        drawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.acc_navigation_drawer_open,
                R.string.acc_navigation_drawer_close) {
            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        drawerToggle.setDrawerIndicatorEnabled(true);
        drawer.addDrawerListener(drawerToggle);
        navigationView.setNavigationItemSelectedListener(this);
        // Make sure that if we deactivate the toggle we still have a handler for the up button.
        drawerToggle.setToolbarNavigationClickListener(v -> onBackPressed());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Util.forceMenuIcons(menu, getClass().getSimpleName());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // We don't have any events, but PermCheckingActivity does.
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync drawer toggle.
        drawerToggle.syncState();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        // Show the "Rate Minerva" dialog if a sticky event is present for it.
        if (EventBus.getDefault().removeStickyEvent(ShowRateMeDialogEvent.class) != null)
            Dialogs.rateMinervaDialog(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Keep the drawer toggle informed.
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) drawer.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(TITLE, getTitle().toString());
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close Realm.
        if (realm != null) {
            realm.close();
            realm = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle nav drawer.
        if (drawerToggle.onOptionsItemSelected(item)) return true;
        // Handle other options.
        switch (item.getItemId()) {
            case android.R.id.home:
                // For non-base fragments, the up arrow will be shown instead of the drawer toggle, so we want it to
                // act like the back button.
                onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_recent:
                switchFragments(MainFrag.RECENT);
                break;
            case R.id.nav_library:
                switchFragments(MainFrag.LIBRARY);
                break;
            case R.id.nav_all_lists:
                switchFragments(MainFrag.ALL_LISTS);
                break;
            case R.id.nav_power_search:
                switchFragments(MainFrag.POWER_SEARCH);
                break;
            case R.id.nav_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.nav_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Switch fragments based on the id of the item that was clicked in the nav drawer.
     * @param frag Which fragment to switch to.
     */
    private void switchFragments(MainFrag frag) {
        // Figure out which fragment to switch to.
        Fragment fragment = null;
        switch (frag) {
            case RECENT:
                fragment = RecentFragment.newInstance();
                setTitle(R.string.nav_item_recent);
                break;
            case LIBRARY:
                fragment = LibraryFragment.newInstance();
                setTitle(R.string.nav_item_library);
                break;
            case ALL_LISTS:
                fragment = AllListsFragment.newInstance();
                setTitle(R.string.nav_item_all_lists);
                break;
            case POWER_SEARCH:
                fragment = PowerSearchFragment.newInstance();
                setTitle(R.string.nav_item_power_search);
                break;
        }

        // Switch to the new fragment.
        if (fragment != null) {
            FragmentManager fragMan = getSupportFragmentManager();
            fragMan.beginTransaction().replace(R.id.main_frag_cont, fragment).commit();
        }

        // Save things to prefs.
        Minerva.prefs().putCurrFrag(frag);
    }

    /**
     * Ensure that retry the first full import when the storage permission has been granted. Will do nothing if we've
     * already done the first full import, or if we haven't finished the intro flow.
     * @param event {@link PermGrantedEvent}.
     */
    @Subscribe
    public void onStoragePermissionGranted(PermGrantedEvent event) {
        if (event.getActionId() == R.id.action_first_import) doFirstImport();
    }

    /**
     * If we haven't already done so, but we have finished the intro flow, trigger the first full import. Go ahead and
     * open the {@link ImportActivity} too.
     */
    private void doFirstImport() {
        Prefs prefs = Minerva.prefs();
        if (!prefs.hasFirstImportBeenTriggered() && prefs.introCompleted()) {
            Importer.get().queueFullImport();
            prefs.setFirstImportTriggered();
            Util.startAct(this, ImportActivity.class, null);
        }
    }
}
