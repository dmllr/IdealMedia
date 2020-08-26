package com.armedarms.idealmedia.adapters;

import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;

public class LRViewPagerAdapter extends PagerAdapter {
    private final int leftId;
    private final int mainId;
    private final int rightId;
    private final int count;

    public LRViewPagerAdapter(int leftId, int mainId, int rightId) {
        if (mainId == 0)
            throw new IllegalArgumentException("mainId could not be 0");

        this.leftId = leftId;
        this.mainId = mainId;
        this.rightId = rightId;

        count = 1 + (leftId != 0 ? 1 : 0) + (rightId != 0 ? 1 : 0);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        int resId = 0;
        switch (position) {
            case 0:
                resId = (leftId == 0 ? mainId : leftId);
                break;
            case 1:
                resId = (leftId == 0 ? rightId : mainId);
                break;
            case 2:
                resId = rightId;
                break;
        }
        return container.findViewById(resId);
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {

    }
}
