package com.armedarms.idealmedia.fragments;

import android.app.Activity;

import androidx.fragment.app.Fragment;

import com.armedarms.idealmedia.NavigationActivity;

public class BaseFragment extends Fragment {
    protected NavigationActivity activity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.activity = (NavigationActivity)activity;
    }
}
