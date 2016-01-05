package com.bkp.minerva.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.view.menu.MenuBuilder;
import android.util.Log;
import android.view.*;
import com.bkp.minerva.R;

import java.lang.reflect.Method;

/**
 *
 */
public class Library extends Fragment {

    public Library() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment using the provided parameters.
     * @return A new instance of fragment Recent.
     */
    // TODO: Rename and change types and number of parameters
    public static Library newInstance() {
        Library fragment = new Library();
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_library, container, false);
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (menu != null) {
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
}
