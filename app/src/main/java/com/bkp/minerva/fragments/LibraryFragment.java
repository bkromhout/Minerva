package com.bkp.minerva.fragments;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.menu.MenuBuilder;
import android.util.Log;
import android.view.*;
import android.widget.RadioGroup;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bkp.minerva.C;
import com.bkp.minerva.R;
import com.tumblr.remember.Remember;

import java.lang.reflect.Method;

/**
 *
 */
public class LibraryFragment extends Fragment {
    // Preference key strings.
    public static final String SORT_TYPE = "SORT_TYPE";
    public static final String SORT_DIR = "SORT_DIRECTION";
    public static final String CARD_TYPE = "CARD_TYPE";

    // Views
    @Bind(R.id.fab)
    FloatingActionButton fabViewOpts;

    /**
     * The current sort type.
     */
    private String sortType;
    /**
     * The current sort direction.
     */
    private String sortDir;
    /**
     * The current card type.
     */
    private String cardType;

    public LibraryFragment() {
        // Required empty public constructor
    }

    public static LibraryFragment newInstance() {
        LibraryFragment fragment = new LibraryFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We have menu items we'd like to add.
        setHasOptionsMenu(true);
        // Read prefs to fill in vars.
        readPrefs();
    }

    /**
     * Read preferences into variables.
     */
    private void readPrefs() {
        sortType = Remember.getString(SORT_TYPE, C.SORT_TITLE);
        sortDir = Remember.getString(SORT_DIR, C.SORT_ASC);
        cardType = Remember.getString(CARD_TYPE, C.CARD_NORMAL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment, then bind and set up views.
        View root = inflater.inflate(R.layout.fragment_library, container, false);
        ButterKnife.bind(this, root);
        initUi();
        return root;
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
        inflater.inflate(R.menu.library, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * Show the view options dialog when the FAB is clicked.
     */
    @OnClick(R.id.fab)
    void onFabClick() {
        // Inflate dialog view and get views.
        View view = LayoutInflater.from(getContext()).inflate(R.layout.library_view_opts, null);
        final RadioGroup rgSortType = ButterKnife.findById(view, R.id.rg_sort_type);
        final RadioGroup rgSortDir = ButterKnife.findById(view, R.id.rg_sort_dir);
        final RadioGroup rgCardType = ButterKnife.findById(view, R.id.rg_card_type);

        // Set up views.
        rgSortType.check(idFromStr(sortType));
        rgSortDir.check(idFromStr(sortDir));
        rgCardType.check(idFromStr(cardType));

        // Construct material dialog.
        new MaterialDialog.Builder(getContext())
                .title(R.string.action_view_opts)
                .customView(view, false)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive((dialog, which) -> {
                    // Figure out choices.
                    sortType = strFromId(rgSortType.getCheckedRadioButtonId());
                    Remember.putString(SORT_TYPE, sortType);
                    sortDir = strFromId(rgSortDir.getCheckedRadioButtonId());
                    Remember.putString(SORT_DIR, sortDir);
                    cardType = strFromId(rgCardType.getCheckedRadioButtonId());
                    Remember.putString(CARD_TYPE, cardType);
                    // TODO refresh recycler view
                })
                .cancelable(true)
                .show();
    }

    /**
     * Convert a view ID to the string constant that that view represents. Super nifty for
     * @param id A view ID.
     * @return A string constant.
     */
    private String strFromId(@IdRes int id) {
        switch (id) {
            // Sort types.
            case R.id.sort_title:
                return C.SORT_TITLE;
            case R.id.sort_author:
                return C.SORT_AUTHOR;
            case R.id.sort_time_added:
                return C.SORT_TIME_ADDED;
            case R.id.sort_rating:
                return C.SORT_RATING;
            // Sort directions.
            case R.id.sort_asc:
                return C.SORT_ASC;
            case R.id.sort_desc:
                return C.SORT_DESC;
            // Card types.
            case R.id.card_normal:
                return C.CARD_NORMAL;
            case R.id.card_no_cover:
                return C.CARD_NO_COVER;
            case R.id.card_compact:
                return C.CARD_COMPACT;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Convert a string constant to the id of a view that represents it.
     * @param str String constant.
     * @return A view ID.
     */
    private Integer idFromStr(String str) {
        switch (str) {
            // Sort types.
            case C.SORT_TITLE:
                return R.id.sort_title;
            case C.SORT_AUTHOR:
                return R.id.sort_author;
            case C.SORT_TIME_ADDED:
                return R.id.sort_time_added;
            case C.SORT_RATING:
                return R.id.sort_rating;
            // Sort directions.
            case C.SORT_ASC:
                return R.id.sort_asc;
            case C.SORT_DESC:
                return R.id.sort_desc;
            // Card types.
            case C.CARD_NORMAL:
                return R.id.card_normal;
            case C.CARD_NO_COVER:
                return R.id.card_no_cover;
            case C.CARD_COMPACT:
                return R.id.card_compact;
            default:
                throw new IllegalArgumentException();
        }
    }
}
