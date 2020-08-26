package com.armedarms.idealmedia.adapters;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.domain.Playlist;
import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.utils.StringUtils;

import java.util.ArrayList;

public class FoldersAdapter extends RecyclerView.Adapter<FoldersAdapter.FoldersViewHolder> {

    public ArrayList<Playlist> data;
    NavigationActivity activity;

    public FoldersAdapter(NavigationActivity activity, ArrayList<Playlist> data){
        this.data = data;

        this.activity = activity;
    }

    @Override
    public FoldersViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.cell_folder, parent, false);

        ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewPager);
        viewPager.setAdapter(new LRViewPagerAdapter(R.id.item_page_L, R.id.item_page_0, 0));
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
            @Override public void onPageSelected(int position) { }

            @Override
            public void onPageScrollStateChanged(int state) {
                parent.setTag(R.string.key_scroll_in_action, state == ViewPager.SCROLL_STATE_DRAGGING);
            }
        });

        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        viewPager.getLayoutParams().width = displayMetrics.widthPixels;
        viewPager.requestLayout();

        return new FoldersViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onBindViewHolder(FoldersViewHolder holder, int position) {
        final Playlist o = data.get(position);

        holder.section.setText(StringUtils.capitalizeFully(o.getArtists()));
        holder.title.setText(StringUtils.capitalizeFully(o.getTitle()));

        holder.viewPager.setCurrentItem(1);

        holder.actionToPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activity.getIsWorkingPlaylistSet()) {
                    addToQuickPlaylist(o.getTracks());
                } else {
                    activity.setWorkingPlaylistDialog(new NavigationActivity.OnWorkingPlaylistSetListener() {
                        @Override
                        public void OnWorkingPlaylistSet() {
                            addToQuickPlaylist(o.getTracks());
                        }
                    });
                }
            }
        });

        holder.mainView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.exhibit(o.getTracks());
            }
        });
    }

    private void addToQuickPlaylist(ArrayList<Track> tracks) {
        for (Track t : tracks)
            activity.addToQuickPlaylist(t, false, false);

        activity.savePlaylist(activity.getWorkingPlaylist());

        Toast.makeText(activity, String.format(activity.getString(R.string.added_to_playlist), activity.getWorkingPlaylist().getTitle()), Toast.LENGTH_SHORT).show();
    }

    static class FoldersViewHolder extends RecyclerView.ViewHolder {

        private final TextView section;
        private final TextView title;
        private final View actionToPlaylist;
        private final ViewPager viewPager;
        public View mainView;

        public FoldersViewHolder(View view) {
            super(view);

            section = (TextView)view.findViewById(R.id.section);
            title = (TextView)view.findViewById(R.id.textView);
            actionToPlaylist = view.findViewById(R.id.actionToPlaylist);

            viewPager = view.findViewById(R.id.viewPager);
            mainView = viewPager.findViewById(R.id.item_page_0);
        }
    }
}

