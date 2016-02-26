package com.bkromhout.minerva;

import android.Manifest;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
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
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkromhout.minerva.events.MissingPermEvent;
import com.bkromhout.minerva.fragments.AllListsFragment;
import com.bkromhout.minerva.fragments.LibraryFragment;
import com.bkromhout.minerva.fragments.PowerSearchFragment;
import com.bkromhout.minerva.fragments.RecentFragment;
import com.bkromhout.minerva.prefs.DefaultPrefs;
import com.bkromhout.minerva.util.Util;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.EmptyPermissionListener;
import com.karumi.dexter.listener.single.PermissionListener;
import io.realm.Realm;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Main activity, responsible for hosting fragments.
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
     * Instance of Realm.
     */
    private Realm realm;
    /**
     * Nav Drawer Toggle (burger menu).
     */
    private ActionBarDrawerToggle drawerToggle;
    /**
     * Permission listener for the Read External Storage permission.
     */
    PermissionListener storagePL;
    /**
     * Whether or not the permissions nag snackbar is currently showing.
     */
    private boolean isPermSnackbarShown = false;

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
        getSupportActionBar().setHomeButtonEnabled(true);

        // Set up the nav drawer and its toggle.
        initDrawer();

        // Handle permissions. Make sure we continue a request process if applicable.
        initPLs();
        Dexter.continuePendingRequestIfPossible(storagePL);

        // Ensure that the same fragment is selected as was last time.
        if (savedInstanceState == null) {
            // Make sure we show the library fragment if we don't have a saved instance state, don't have a saved
            // current fragment, or if the saved current fragment would need some bundle to help populate it.
            int frag = defaultPrefs.getCurrFrag(-1);
            switchFragments(frag != -1 ? frag : FRAG_LIBRARY);
            navigationView.setCheckedItem(Util.navIdFromFragConst(frag));
        }
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

    /**
     * Create PermissionListeners.
     */
    private void initPLs() {
        storagePL = new EmptyPermissionListener() {
            @Override
            public void onPermissionDenied(PermissionDeniedResponse response) {
                super.onPermissionDenied(response);
                // For a regular denial, just show the snackbar. For a permanent denial, show a dialog which has a
                // link to the app info screen.
                if (!response.isPermanentlyDenied()) showPermNagSnackbar();
                else showRationaleDialog(null);
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                super.onPermissionRationaleShouldBeShown(permission, token);
                if (permission.getName().equals(Manifest.permission.READ_EXTERNAL_STORAGE))
                    showRationaleDialog(token);
            }
        };
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Util.forceMenuIcons(menu, this, getClass().getSimpleName());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

        // Check for permissions if not already doing so.
        if (!Dexter.isRequestOngoing()) Dexter.checkPermission(storagePL, Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync drawer toggle.
        drawerToggle.syncState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Show snackbar if necessary.
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
     * Called when one of the fragments has indicated that we are missing a permission that we need.
     * @param event {@link MissingPermEvent}.
     */
    @Subscribe
    public void onMissingPermEvent(MissingPermEvent event) {
        // Currently we only have one permission we'd need to check for, read external storage, so we don't bother
        // checking the permission string in the event.
        showPermNagSnackbar();
    }

    /**
     * Show dialog explaining why we need permission.
     * @param token Token to continue request. If this is nonnull, then we know we're showing this dialog because the
     *              permission was already permanently denied, not simply to provide rationale before requesting it.
     */
    private void showRationaleDialog(PermissionToken token) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(MainActivity.this)
                .title(R.string.storage_permission);

        // Add dependant parts.
        if (token != null) {
            // This dialog is simply to provide rationale prior to showing the system's permission request dialog.
            builder.content(R.string.storage_permission_rationale)
                   .positiveText(R.string.ok)
                   .dismissListener(dialog -> token.continuePermissionRequest());
        } else {
            // This dialog needs to provide a way to open the app info screen, because the permission was already
            // permanently denied.
            builder.content(R.string.storage_permission_rationale_long)
                   .positiveText(R.string.app_info)
                   .negativeText(R.string.cancel)
                   .onPositive((dialog, which) -> Util.openAppInfo(MainActivity.this))
                   .onNegative((dialog, which) -> dialog.cancel())
                   .cancelListener(dialog -> showPermNagSnackbar());
        }

        builder.show();
    }

    /**
     * Show a snackbar to nag user to grant permission.
     */
    private void showPermNagSnackbar() {
        // Don't queue a second snackbar.
        if (isPermSnackbarShown) return;

        // This snackbar with make Dexter try to get the permission again.
        Snackbar.make(fragCont, R.string.storage_permission_needed, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.retry, v -> Dexter.checkPermission(storagePL,
                        Manifest.permission.READ_EXTERNAL_STORAGE))
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        isPermSnackbarShown = false;
                    }

                    @Override
                    public void onShown(Snackbar snackbar) {
                        super.onShown(snackbar);
                        isPermSnackbarShown = true;
                    }
                })
                .show();
    }
}
