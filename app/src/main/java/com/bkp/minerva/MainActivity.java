package com.bkp.minerva;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkp.minerva.fragments.AllListsFragment;
import com.bkp.minerva.fragments.LibraryFragment;
import com.bkp.minerva.fragments.PowerSearchFragment;
import com.bkp.minerva.fragments.RecentFragment;
import com.bkp.minerva.prefs.DefaultPrefs;

import java.lang.reflect.Method;

/**
 * Main activity.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    // Represents the various fragments that this activity can show.
    public static final int FRAG_RECENT = 0;
    public static final int FRAG_LIBRARY = 1;
    public static final int FRAG_ALL_LISTS = 2;
    public static final int FRAG_POWER_SEARCH = 3;

    // Views.
    @Bind(R.id.drawer_layout)
    DrawerLayout drawer;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.nav_view)
    NavigationView navigationView;
    @Bind(R.id.main_frag_cont)
    FrameLayout fragCont;

    /**
     * Instance of the default preferences.
     */
    private DefaultPrefs defaultPrefs;
    /**
     * Nav Drawer Toggle (burger menu).
     */
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get prefs.
        defaultPrefs = DefaultPrefs.get();

        // Set theme, create and bind views.
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Set up toolbar.
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Set up the nav drawer and its toggle.
        initDrawer();

        // Ensure that the same fragment is selected as was last time.
        if (savedInstanceState == null) {
            // Make sure we show the library fragment if we don't have a saved instance state, don't have a saved
            // current fragment, or if the saved current fragment would need some bundle to help populate it.
            int frag = defaultPrefs.getCurrFrag(-1);
            switchFragments(frag != -1 ? frag : FRAG_LIBRARY);
            navigationView.setCheckedItem(navIdFromFragConst(frag));
        }
    }

    private void initDrawer() {
        drawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.acc_navigation_drawer_open,
                R.string.acc_navigation_drawer_close) {
            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        drawerToggle.setDrawerIndicatorEnabled(true);
        drawer.setDrawerListener(drawerToggle);
        navigationView.setNavigationItemSelectedListener(this);
        // Make sure that if we deactivate the toggle we still have a handler for the up button.
        drawerToggle.setToolbarNavigationClickListener(v -> onBackPressed());
    }

    @Override
    protected void onStart() {
        super.onStart();
        //EventBus.getDefault().register(this); // TODO turn on once we have event handlers in here.
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync drawer toggle.
        drawerToggle.syncState();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu != null) {
            // Make sure all icons are tinted the correct color, including those in the overflow menu.
            for (int i = 0; i < menu.size(); i++)
                menu.getItem(i).getIcon()
                    .setColorFilter(ContextCompat.getColor(this, R.color.textColorPrimary), PorterDuff.Mode.SRC_IN);
            // And use a bit of reflection to ensure we show icons even in the overflow menu.
            if (menu.getClass().equals(MenuBuilder.class)) {
                try {
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "onMenuOpened...unable to set icons for overflow menu", e);
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
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
    protected void onStop() {
        //EventBus.getDefault().unregister(this);  // TODO turn on once we have event handlers in here.
        super.onStop();
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_recent:
                switchFragments(FRAG_RECENT);
                break;
            case R.id.nav_library:
                switchFragments(FRAG_LIBRARY);
                break;
            case R.id.nav_all_lists:
                switchFragments(FRAG_ALL_LISTS);
                break;
            case R.id.nav_power_search:
                switchFragments(FRAG_POWER_SEARCH);
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
    private void switchFragments(int frag) {
        // Figure out which fragment to switch to.
        Fragment fragment = null;
        switch (frag) {
            case FRAG_RECENT:
                fragment = RecentFragment.newInstance();
                setTitle(R.string.nav_item_recent);
                break;
            case FRAG_LIBRARY:
                fragment = LibraryFragment.newInstance();
                setTitle(R.string.nav_item_library);
                break;
            case FRAG_ALL_LISTS:
                fragment = AllListsFragment.newInstance();
                setTitle(R.string.nav_item_lists);
                break;
            case FRAG_POWER_SEARCH:
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
        defaultPrefs.putCurrFrag(frag);
    }

    /**
     * Takes a fragment constant integer (see the top of {@link MainActivity}) and returns the Android resource ID for
     * the item in the nav drawer which corresponds to that fragment.
     * @param frag Fragment integer constant.
     * @return Nav drawer item resource ID.
     */
    @IdRes
    private static int navIdFromFragConst(int frag) {
        switch (frag) {
            case FRAG_RECENT:
                return R.id.nav_recent;
            case FRAG_LIBRARY:
                return R.id.nav_library;
            case FRAG_ALL_LISTS:
                return R.id.nav_all_lists;
            case FRAG_POWER_SEARCH:
                return R.id.nav_power_search;
            default:
                return -1;
        }
    }
}
