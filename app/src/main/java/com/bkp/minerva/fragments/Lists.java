package com.bkp.minerva.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.*;
import com.bkp.minerva.R;

/**
 *
 */
public class Lists extends Fragment {

    public Lists() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment using the provided parameters.
     * @return A new instance of fragment Recent.
     */
    // TODO: Rename and change types and number of parameters
    public static Lists newInstance() {
        Lists fragment = new Lists();
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
        return inflater.inflate(R.layout.fragment_lists, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.lists, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
