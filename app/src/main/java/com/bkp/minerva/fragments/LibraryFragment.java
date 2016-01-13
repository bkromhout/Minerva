package com.bkp.minerva.fragments;

import android.graphics.PorterDuff;
import android.os.Bundle;
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
import com.bkp.minerva.R;
import com.bkp.minerva.prefs.LibraryPrefs;
import net.orange_box.storebox.StoreBox;

import java.lang.reflect.Method;

/**
 *
 */
public class LibraryFragment extends Fragment {


    // Views
    @Bind(R.id.fab)
    FloatingActionButton fabViewOpts;

    /**
     * Instance of library fragment's preferences.
     */
    LibraryPrefs libraryPrefs;

    public LibraryFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment using the provided parameters.
     * @return A new instance of {@link LibraryFragment}.
     */
    // TODO: Rename and change types and number of parameters
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

        libraryPrefs = StoreBox.create(getContext(), LibraryPrefs.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment, then bind views.
        View root = inflater.inflate(R.layout.fragment_library, container, false);
        ButterKnife.bind(this, root);
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
    private void onFabClick() {
        // Construct material dialog.
        MaterialDialog md = new MaterialDialog.Builder(getContext())
                .title(R.string.action_view_opts)
                .iconRes(R.drawable.ic_eye)
                .customView(R.layout.library_view_opts, false)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive((dialog, which) -> {
                    View view = dialog.getCustomView();
                    assert view != null;
                    RadioGroup rgSortType = ButterKnife.findById(view, R.id.rg_sort_type);
                    RadioGroup rgSortDir = ButterKnife.findById(view, R.id.rg_sort_dir);
                    RadioGroup rgCardType = ButterKnife.findById(view, R.id.rg_card_type);

                    // TODO finish OK button handler.
                })
                .cancelable(true)
                .build();

        // TODO finish setting up dialog's views using prefs and showing dialog
    }
}
