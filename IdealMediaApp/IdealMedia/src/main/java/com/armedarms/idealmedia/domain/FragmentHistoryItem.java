package com.armedarms.idealmedia.domain;

import androidx.fragment.app.Fragment;

public class FragmentHistoryItem {
    public Fragment fragment;
    public String method;
    public String param;

    public static FragmentHistoryItem get(Fragment fragment) {
        FragmentHistoryItem fhi = new FragmentHistoryItem();

        fhi.fragment = fragment;
        fhi.method = "";
        fhi.param = "";

        return fhi;
    }
}
