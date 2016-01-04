package com.bkp.minerva;

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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkp.minerva.fragments.Library;
import com.bkp.minerva.fragments.Lists;
import com.bkp.minerva.fragments.PowerSearch;
import com.bkp.minerva.fragments.Recent;

/**
 * Main activity.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    // Views.
    @Bind(R.id.drawer_layout)
    DrawerLayout mDrawer;
    @Bind(R.id.nav_view)
    NavigationView mNavigationView;
    @Bind(R.id.main_frag_cont)
    FrameLayout mFragCont;

    /**
     * Nav Drawer Toggle (burger menu).
     */
    ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.AppTheme_Dark);
        // Create and bind views.
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Set up toolbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Set up the nav drawer and its toggle.
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close) {
            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawer.setDrawerListener(mDrawerToggle);
        mNavigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync drawer toggle.
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Keep the drawer toggle informed.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) mDrawer.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle nav drawer
        if (mDrawerToggle.onOptionsItemSelected(item)) return true;

        // Handle other options.
        switch (item.getItemId()) {
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
            case R.id.nav_library:
            case R.id.nav_lists:
            case R.id.nav_power_search:
                switchFragments(item.getItemId());
                break;
            case R.id.nav_settings:

                break;
            case R.id.nav_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
        }
        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Switch fragments based on the id of the item that was clicked in the nav drawer.
     * @param itemId Nav drawer item id.
     */
    private void switchFragments(int itemId) {
        // Figure out which fragment to switch to.
        Fragment fragment = null;
        switch (itemId) {
            case R.id.nav_recent:
                fragment = Recent.newInstance();
                setTitle(R.string.nav_item_recent);
                break;
            case R.id.nav_library:
                fragment = Library.newInstance();
                setTitle(R.string.nav_item_library);
                break;
            case R.id.nav_lists:
                fragment = Lists.newInstance();
                setTitle(R.string.nav_item_lists);
                break;
            case R.id.nav_power_search:
                fragment = PowerSearch.newInstance();
                setTitle(R.string.nav_item_power_search);
                break;
        }

        // Switch to the new fragment and change the title.
        if (fragment != null) {
            FragmentManager fragMan = getSupportFragmentManager();
            fragMan.beginTransaction().replace(R.id.main_frag_cont, fragment).commit();
        }
    }
}
