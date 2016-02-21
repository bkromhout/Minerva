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
import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import com.bkp.minerva.R;
import com.bkp.minerva.realm.RBook;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;

import java.lang.reflect.Method;

/**
 * Fragment in charge of letting the user power search.
 */
public class PowerSearchFragment extends Fragment {
    // Views.
    @Bind(R.id.fab)
    FloatingActionButton fabViewOpts;
    @Bind(R.id.recycler)
    RealmRecyclerView recyclerView;

    /**
     * Instance of Realm.
     */
    private Realm realm;
    /**
     * {@link RBook}s currently shown in the recycler view.
     */
    private RealmResults<RBook> books;
    /**
     * Adapter currently being used by the recycler view.
     */
    private RealmBasedRecyclerViewAdapter adapter;

    public PowerSearchFragment() {
        // Required empty public constructor
    }

    public static PowerSearchFragment newInstance() {
        PowerSearchFragment fragment = new PowerSearchFragment();
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
        View root = inflater.inflate(R.layout.fragment_power_search, container, false);
        ButterKnife.bind(this, root);
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Get Realm.
        realm = Realm.getDefaultInstance();

        initUi();
    }

    /**
     * Initialize the UI.
     */
    private void initUi() {

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
        inflater.inflate(R.menu.power_search, menu);
        super.onCreateOptionsMenu(menu, inflater);
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
}
