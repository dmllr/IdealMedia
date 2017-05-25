package com.armedarms.idealmedia.domain;

import android.support.v4.app.Fragment;

import com.armedarms.idealmedia.fragments.VKAudioFragment;

public class FragmentHistoryItem {
    public Fragment fragment;
    public String method;
    public String param;

    public static FragmentHistoryItem get(Fragment fragment) {
        FragmentHistoryItem fhi = new FragmentHistoryItem();

        fhi.fragment = fragment;
        fhi.method = "";
        fhi.param = "";
        if (fragment instanceof VKAudioFragment) {
            fhi.method = ((VKAudioFragment) fragment).getMethod();
            fhi.param = ((VKAudioFragment) fragment).getSearchQuery();
        }

        return fhi;
    }
}
