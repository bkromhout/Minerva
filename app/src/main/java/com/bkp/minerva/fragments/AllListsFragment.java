package com.bkp.minerva.fragments;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.menu.MenuBuilder;
import android.util.Log;
import android.view.*;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import com.bkp.minerva.R;
import com.bkp.minerva.events.BookListCardClickEvent;
import com.bkp.minerva.prefs.AllListsPrefs;
import com.bkp.minerva.realm.RBookList;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.reflect.Method;

/**
 * Fragment in charge of showing all of the book lists.
 */
public class AllListsFragment extends Fragment {
    // Views.
    @Bind(R.id.fab)
    FloatingActionButton fabViewOpts;
    @Bind(R.id.recycler)
    RealmRecyclerView recyclerView;

    /**
     * Preferences.
     */
    private AllListsPrefs allListsPrefs;
    /**
     * Instance of Realm.
     */
    private Realm realm;
    /**
     * {@link RBookList}s currently shown in the recycler view.
     */
    private RealmResults<RBookList> lists;
    /**
     * Adapter currently being used by the recycler view.
     */
    private RealmBasedRecyclerViewAdapter adapter;

    public AllListsFragment() {
        // Required empty public constructor
    }

    public static AllListsFragment newInstance() {
        AllListsFragment fragment = new AllListsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We have menu items we'd like to add.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment, then bind views.
        View root = inflater.inflate(R.layout.fragment_all_lists, container, false);
        ButterKnife.bind(this, root);
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Read prefs to fill in vars.
        allListsPrefs = AllListsPrefs.get();
        readPrefs();

        // Get Realm.
        realm = Realm.getDefaultInstance();

        initUi();
    }

    /**
     * Read preferences into variables.
     */
    private void readPrefs() {

    }

    /**
     * Initialize the UI.
     */
    private void initUi() {
        lists = realm.where(RBookList.class)
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (menu != null) {
            // Make sure all icons are tinted the correct color, including those in the overflow menu.
            for (int i = 0; i < menu.size(); i++)
                menu.getItem(i).getIcon()
                    .setColorFilter(ContextCompat.getColor(getContext(), R.color.textColorPrimary),
                            PorterDuff.Mode.SRC_IN);
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
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.all_lists, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Close Realm.
        if (realm != null) {
            realm.close();
            realm = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Called when one of the cards is clicked.
     * @param event {@link BookListCardClickEvent}.
     */
    @Subscribe
    public void onCardClicked(BookListCardClickEvent event) {
        // Get the associated RBookList.
        RBookList bookList = lists.where().equalTo("name", event.getName()).findFirst();

        // Do something based on the click type.
        switch (event.getType()) {
            case NORMAL:
                // TODO Open BookListActivity.
                break;
            case LONG:
                // TODO Start multi-select.
                break;
            case ACTIONS:
                // TODO Pop open actions menu.
                break;
        }
    }

    /**
     * Show the new list dialog when the FAB is clicked.
     */
    @OnClick(R.id.fab)
    void onFabClick() {

    }
}
