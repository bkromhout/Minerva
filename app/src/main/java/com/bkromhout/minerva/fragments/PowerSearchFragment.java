package com.bkromhout.minerva.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.*;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.QueryBuilderActivity;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.adapters.*;
import com.bkromhout.minerva.enums.BookCardType;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RBookListItem;
import com.bkromhout.minerva.util.Util;
import com.bkromhout.realmrecyclerview.RealmRecyclerView;
import com.bkromhout.ruqus.RealmUserQuery;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmObject;
import io.realm.RealmResults;

/**
 * Fragment in charge of letting the user power search.
 * <p>
 * TODO add ability to create "Smart List" from here.
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
     * Current {@link RealmUserQuery} in the view.
     */
    private RealmUserQuery ruq;
    /**
     * Current query results.
     */
    private RealmResults<? extends RealmObject> results;
    /**
     * Which type of card to use. TODO add ability to change this.
     */
    private BookCardType cardType = BookCardType.COMPACT;
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

        // Restore RealmUserQuery if we're coming back from a configuration change.
        if (savedInstanceState != null && savedInstanceState.containsKey(C.RUQ))
            ruq = savedInstanceState.getParcelable(C.RUQ);

        // TODO restore scroll state?

        updateUi();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Util.forceMenuIcons(menu, getContext(), getClass().getSimpleName());
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.power_search, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (ruq != null) outState.putParcelable(C.RUQ, ruq);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // TODO add all of the actions and such.

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
//            case C.RC_TAG_ACTIVITY: {
//                // Came back from TaggingActivity.
//                if (resultCode == Activity.RESULT_OK) {
//                    // We've changed the tags on some books.
//                    if (actionMode != null) actionMode.finish();
//                }
//                break;
//            }
            case C.RC_QUERY_BUILDER_ACTIVITY: {
                // Came back from QueryBuilderActivity.
                if (resultCode == Activity.RESULT_OK) {
                    // We've changed our query.
                    ruq = data.getParcelableExtra(C.RUQ);
                    updateUi();
                }
            }
        }
    }

    /**
     * Show the {@link com.bkromhout.minerva.QueryBuilderActivity} when the FAB is clicked.
     */
    @OnClick(R.id.fab)
    void onFabClick() {
        // Start QueryBuilderActivity, passing it the currently help RealmUserQuery.
        QueryBuilderActivity.start(this, ruq);
    }

    /**
     * Update the UI.
     */
    private void updateUi() {
        if (ruq != null) results = ruq.execute(realm);
        adapter = makeAdapter();
        recyclerView.setAdapter(adapter);
    }

    /**
     * Make adapter based on {@link RealmUserQuery#getQueryClass() ruq#getQueryClass()} and {@link #cardType}.
     * @return Adapter, or null if {@link #ruq} is null/invalid or {@link #cardType} is null/invalid.
     */
    @SuppressWarnings("unchecked")
    private RealmBasedRecyclerViewAdapter makeAdapter() {
        if (ruq == null || !ruq.isQueryValid()) return null;
        else if (RBook.class.getCanonicalName().equals(ruq.getQueryClass().getCanonicalName())) {
            switch (cardType) {
                case NORMAL:
                    return new BookCardAdapter(getContext(), (RealmResults<RBook>) results);
                case NO_COVER:
                    return new BookCardNoCoverAdapter(getContext(), (RealmResults<RBook>) results);
                case COMPACT:
                    return new BookCardCompactAdapter(getContext(), (RealmResults<RBook>) results);
                default:
                    return null;
            }
        } else if (RBookListItem.class.getCanonicalName().equals(ruq.getQueryClass().getCanonicalName())) {
            switch (cardType) {
                case NORMAL:
                    return new BookItemCardAdapter(getContext(), (RealmResults<RBookListItem>) results);
                case NO_COVER:
                    return new BookItemCardNoCoverAdapter(getContext(), (RealmResults<RBookListItem>) results);
                case COMPACT:
                    return new BookItemCardCompactAdapter(getContext(), (RealmResults<RBookListItem>) results);
                default:
                    return null;
            }
        } else return null;
    }
}
