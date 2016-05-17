package com.bkromhout.minerva.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.ruqus.RealmQueryView;
import com.bkromhout.ruqus.RealmUserQuery;

/**
 * Allows the user to build a Realm Query using Ruqus.
 */
public class QueryBuilderActivity extends AppCompatActivity implements RealmQueryView.ModeListener {
    // Views.
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.rqv)
    RealmQueryView rqv;

    /**
     * What mode the {@link RealmQueryView} is in.
     */
    private RealmQueryView.Mode rqvMode = RealmQueryView.Mode.MAIN;

    /**
     * Convenience method to start this activity from a fragment.
     * @param fragment The fragment to return a result to.
     * @param ruq      The The {@link RealmUserQuery} to pre-fill.
     */
    public static void start(Fragment fragment, RealmUserQuery ruq) {
        Intent intent = new Intent(new Intent(fragment.getContext(), QueryBuilderActivity.class));
        if (ruq != null) intent.putExtra(C.RUQ, ruq);
        fragment.startActivityForResult(intent, C.RC_QUERY_BUILDER_ACTIVITY);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    /**
     * Convenience method to start this activity from an activity.
     * @param activity The activity to return a result to.
     * @param ruq      The {@link RealmUserQuery} to pre-fill.
     */
    public static void start(Activity activity, RealmUserQuery ruq) {
        Intent intent = new Intent(new Intent(activity, QueryBuilderActivity.class));
        if (ruq != null) intent.putExtra(C.RUQ, ruq);
        activity.startActivityForResult(intent, C.RC_QUERY_BUILDER_ACTIVITY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        // Create and bind views.
        setContentView(R.layout.activity_query_builder);
        ButterKnife.bind(this);

        // Set up toolbar.
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setHomeAsUpIndicator(Util.getTintedDrawable(this, R.drawable.ic_close,
                R.color.textColorPrimary));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Read extras bundle.
        Bundle b = getIntent().getExtras();
        if (b != null && b.containsKey(C.RUQ)) //noinspection ConstantConditions
            rqv.setRealmUserQuery(b.getParcelable(C.RUQ));
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Util.forceMenuIcons(menu, this, getClass().getSimpleName());
        menu.findItem(R.id.action_save).setVisible(rqvMode == RealmQueryView.Mode.MAIN);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.query_builder, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_save:
                if (!rqv.isQueryValid())
                    Toast.makeText(this, R.string.sb_err_invalid_query, Toast.LENGTH_LONG).show();
                else {
                    setResult(RESULT_OK, new Intent().putExtra(C.RUQ, rqv.getRealmUserQuery()));
                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        rqv.setModeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        rqv.clearModeListener();
    }

    @Override
    public void rqvModeChanged(RealmQueryView.Mode newMode) {
        rqvMode = newMode;
        invalidateOptionsMenu();
    }
}
