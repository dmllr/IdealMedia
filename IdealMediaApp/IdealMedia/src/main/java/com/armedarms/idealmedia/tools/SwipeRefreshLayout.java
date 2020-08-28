package com.armedarms.idealmedia.tools;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.armedarms.idealmedia.R;

public class SwipeRefreshLayout extends androidx.swiperefreshlayout.widget.SwipeRefreshLayout {
    RecyclerView cachedListView = null;

    public SwipeRefreshLayout(@NonNull Context context) {
        super(context);
    }

    public SwipeRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void clearCache() {
        cachedListView = null;
    }

    @Override
    public boolean canChildScrollUp() {
        if (cachedListView == null)
            cachedListView = findList(this);

        if (cachedListView == null)
            return true;

        boolean isSwipeInAction = false;
        Object isSwipeInActionTag = cachedListView.getTag(R.string.key_scroll_in_action);
        if (isSwipeInActionTag != null)
            isSwipeInAction = (Boolean)isSwipeInActionTag;

        return isSwipeInAction || cachedListView.canScrollVertically(-1);
    }

    private RecyclerView findList(ViewGroup v) {
        for (int i = 0; i < v.getChildCount(); i++) {
            View child = v.getChildAt(i);

            if (child instanceof RecyclerView)
                return (RecyclerView)child;
            if (child instanceof ViewGroup) {
                RecyclerView l = findList((ViewGroup)child);
                if (l != null)
                    return l;
            }

        }
        return null;
    }
}
