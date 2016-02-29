package com.bkromhout.minerva;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.bkromhout.minerva.adapters.TagCardAdapter;
import com.bkromhout.minerva.realm.RTag;
import com.bkromhout.realmrecyclerview.RealmRecyclerView;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import java.util.ArrayList;

/**
 * Activity opened to apply tags to books.
 */
public class TaggingActivity extends AppCompatActivity {

    // Views.
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.recycler)
    RealmRecyclerView recyclerView;
    @Bind(R.id.tag_filter)
    EditText filter;
    @Bind(R.id.cancel)
    Button btnCancel;
    @Bind(R.id.save)
    Button btnSave;

    /**
     * Instance of Realm.
     */
    private Realm realm;
    /**
     * List of tag names which are checked.
     */
    private ArrayList<String> checkedItems;
    /**
     * The list of {@link RTag}s being shown.
     */
    private RealmResults<RTag> items;
    /**
     * Recycler view adapter.
     */
    private TagCardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create and bind views.
        setContentView(R.layout.activity_tagging);
        ButterKnife.bind(this);

        // Set up toolbar.
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Read extras bundle.
        readExtras(getIntent().getExtras());

        // Get Realm, then set up the rest of UI.
        realm = Realm.getDefaultInstance();
        initUi();
    }

    /**
     * Fill in variables using the extras bundle.
     * @param b Extras bundle from intent used to start this activity.
     */
    private void readExtras(Bundle b) {
        if (b == null) return;

    }

    /**
     * Init the UI.
     */
    private void initUi() {
        items = realm.allObjectsSorted(RTag.class, "sortName", Sort.ASCENDING);
        adapter = new TagCardAdapter(this, items, ); // TODO
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tagging, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onStart() {
        super.onStart();
        //EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        //EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Close adapter.
        if (adapter != null) adapter.close();
        // Close Realm.
        if (realm != null) {
            realm.close();
            realm = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
