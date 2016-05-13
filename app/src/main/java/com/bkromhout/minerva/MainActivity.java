package com.bkromhout.minerva;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.IdRes;
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
import com.bkromhout.minerva.fragments.AllListsFragment;
import com.bkromhout.minerva.fragments.LibraryFragment;
import com.bkromhout.minerva.fragments.PowerSearchFragment;
import com.bkromhout.minerva.fragments.RecentFragment;
import com.bkromhout.minerva.prefs.DefaultPrefs;
import com.bkromhout.minerva.util.Util;
import io.realm.Realm;
import org.greenrobot.eventbus.EventBus;

/**
 * Main activity, responsible for hosting fragments.
 */
public class MainActivity extends PermCheckingActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TITLE = "TITLE";
    // Represents the various fragments that this activity can show.
    public static final int FRAG_RECENT = 0;
    public static final int FRAG_LIBRARY = 1;
    public static final int FRAG_ALL_LISTS = 2;
    public static final int FRAG_POWER_SEARCH = 3;

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
     * Instance of the default preferences.
     */
    private DefaultPrefs defaultPrefs;
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
        defaultPrefs = DefaultPrefs.get();
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
            int frag = defaultPrefs.getCurrFrag(-1);
            switchFragments(frag != -1 ? frag : FRAG_LIBRARY);
            navigationView.setCheckedItem(navIdFromFragConst(frag));
        } else {
            // Make sure we set the title back to what it was otherwise.
            setTitle(savedInstanceState.getString(TITLE));
        }

        // Check to see if we need to show the welcome activity.
        if (!defaultPrefs.hasFirstImportBeenTriggered())
            startActivityForResult(new Intent(this, WelcomeActivity.class), C.RC_WELCOME_ACTIVITY);

        // Handle permissions. Make sure we continue a request process if applicable.
        initAndContinuePermChecksIfNeeded();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == C.RC_WELCOME_ACTIVITY) {
            // If we came back from the welcome activity successfully, open the import activity. Otherwise, close.
            if (resultCode == RESULT_OK) Util.startAct(this, ImportActivity.class, null);
            else finish();
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

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
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
        Util.forceMenuIcons(menu, this, getClass().getSimpleName());
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

    /**
     * Override this method so that we remove focus from our filter EditText when we click outside of its bounds.
     */
//    @Override
//    public boolean dispatchTouchEvent(MotionEvent event) {
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            View v = getCurrentFocus();
//            if (v instanceof SearchView.SearchAutoComplete) {
//                Rect outRect = new Rect();
//                v.getGlobalVisibleRect(outRect);
//                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
//                    v.clearFocus();
//                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
//                    // Consume this touch event, we don't want to accidentally toggle one of the tag cards.
//                    return true;
//                }
//            }
//        }
//        return super.dispatchTouchEvent(event);
//    }
}
