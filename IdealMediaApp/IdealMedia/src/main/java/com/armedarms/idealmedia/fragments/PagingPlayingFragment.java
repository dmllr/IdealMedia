package com.armedarms.idealmedia.fragments;

import androidx.recyclerview.widget.RecyclerView;

public class PagingPlayingFragment extends BasePlayingFragment {
    protected long page = 0;
    protected boolean isLoading = false;
    protected boolean isComplete = false;

    @Override
    public void onStart() {
        super.onStart();

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (isLoading)
                    return;

                if (!listView.canScrollVertically(1))
                    onLastListItemScrolling();
            }
        });
    }
}
