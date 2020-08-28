package com.armedarms.idealmedia.fragments;



import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.adapters.LRViewPagerAdapter;
import com.armedarms.idealmedia.domain.Playlist;
import com.armedarms.idealmedia.utils.FileUtils;
import com.armedarms.idealmedia.utils.ResUtils;
import com.r0adkll.postoffice.PostOffice;
import com.r0adkll.postoffice.model.Design;
import com.r0adkll.postoffice.styles.EditTextStyle;

import java.util.UUID;

public class PlaylistsFragment extends BaseFragment implements IHasColor {

    RecyclerView listView;
    PlaylistsAdapter adapter;

    public PlaylistsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        adapter = new PlaylistsAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlists, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = (RecyclerView)view.findViewById(android.R.id.list);
        listView.setLayoutManager(new LinearLayoutManager(getActivity()));
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setAdapter(adapter);

        view.findViewById(R.id.fabNewPlaylist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newPlaylist();
            }
        });
    }

    private void newPlaylist() {
        PostOffice.newMail(activity)
                .setTitle(R.string.title_new_playlist)
                .setThemeColor(getColor())
                .setDesign(Design.MATERIAL_LIGHT)
                .showKeyboardOnDisplay(true)
                .setButtonTextColor(Dialog.BUTTON_POSITIVE, ResUtils.resolve(activity, R.attr.colorPositive))
                .setButton(Dialog.BUTTON_POSITIVE, android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setButton(Dialog.BUTTON_NEGATIVE, android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setStyle(new EditTextStyle.Builder(activity)
                        .setHint(getString(R.string.hint_new_playlist))
                        .setInputType(InputType.TYPE_CLASS_TEXT)
                        .setOnTextAcceptedListener(new EditTextStyle.OnTextAcceptedListener() {
                            @Override
                            public void onAccepted(String text) {
                                newPlaylist(text);
                            }
                        }).build())
                .build()
                .show(getFragmentManager());
    }

    private void editPlaylist(final Playlist playlist) {
        PostOffice.newMail(activity)
                .setTitle(R.string.title_new_playlist)
                .setThemeColor(getColor())
                .setDesign(Design.MATERIAL_LIGHT)
                .showKeyboardOnDisplay(true)
                .setButtonTextColor(Dialog.BUTTON_POSITIVE, ResUtils.resolve(activity, R.attr.colorPositive))
                .setButton(Dialog.BUTTON_POSITIVE, android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setButton(Dialog.BUTTON_NEGATIVE, android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setStyle(new EditTextStyle.Builder(activity)
                        .setHint(getString(R.string.hint_new_playlist))
                        .setInputType(InputType.TYPE_CLASS_TEXT)
                        .setText(playlist.getTitle())
                        .setOnTextAcceptedListener(new EditTextStyle.OnTextAcceptedListener() {
                            @Override
                            public void onAccepted(String text) {
                                editPlaylist(playlist, text);
                            }
                        }).build())
                .build()
                .show(getFragmentManager());
    }

    private void editPlaylist(Playlist playlist, String title) {
        playlist.setTitle(title);
        savePlaylist(playlist);
    }

    private void newPlaylist(String title) {
        Playlist p = new Playlist();
        p.setTitle(title);

        activity.getPlaylists().add(p);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });

        addPlaylist(p);
    }

    private void addPlaylist(Playlist playlist) {
        UUID[] ids = new UUID[activity.getPlaylists().size()];
        int i = 0;
        for (Playlist p : activity.getPlaylists()) {
            if (p.id == null)
                p.id = UUID.randomUUID();

            ids[i] = p.id;

            i++;
        }

        FileUtils.write(getString(R.string.key_playlists), activity.getApplicationContext(), ids);

        savePlaylist(playlist);
    }

    public void savePlaylist(Playlist playlist) {
        FileUtils.write(String.format("playlist_%s", playlist.id.toString()), activity.getApplicationContext(), playlist);
    }

    @Override
    public int getColor() {
        return ResUtils.color(activity, R.attr.colorPlaylists);
    }

    public void invalidate() {
        adapter.notifyDataSetChanged();
    }

    public class PlaylistsAdapter extends RecyclerView.Adapter<PlaylistsAdapter.PlaylistViewHolder> {

        public PlaylistsAdapter() {
        }

        @Override
        public PlaylistViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
            View view = activity.getLayoutInflater().inflate(R.layout.cell_playlist, parent, false);

            ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewPager);
            viewPager.setAdapter(new LRViewPagerAdapter(R.id.item_page_L, R.id.item_page_0, R.id.item_page_R));
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

            return new PlaylistViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PlaylistViewHolder holder, final int position) {
            final Playlist playlist = activity.getPlaylists().get(position);

            holder.title.setText(playlist.getTitle());
            holder.icon.findViewById(android.R.id.icon).setVisibility(playlist.equals(activity.getWorkingPlaylist()) ? View.VISIBLE : View.GONE);

            holder.actionSetPlaylistAsWorking.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.setPlaylistAsWorking(playlist);
                    notifyDataSetChanged();
                }
            });
            holder.actionEditPlaylist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    editPlaylist(playlist);
                }
            });
            holder.actionDropPlaylist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.dropPlaylist(position);
                    notifyDataSetChanged();
                }
            });

            holder.viewPager.setCurrentItem(1);

            holder.mainView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.openPlaylist(playlist);
                }
            });
        }

        @Override
        public int getItemCount() {
            if (activity.getPlaylists() != null)
                return activity.getPlaylists().size();
            else
                return 0;
        }

        class PlaylistViewHolder extends RecyclerView.ViewHolder {
            private final TextView title;
            private final View icon;
            private final View actionSetPlaylistAsWorking;
            private final View actionEditPlaylist;
            private final View actionDropPlaylist;

            private final ViewPager viewPager;
            public View mainView;

            public PlaylistViewHolder(View view) {
                super(view);

                title = (TextView)view.findViewById(android.R.id.text1);
                icon = view.findViewById(android.R.id.icon);
                actionSetPlaylistAsWorking = view.findViewById(R.id.actionSetPlaylistAsWorking);
                actionEditPlaylist = view.findViewById(R.id.actionEditPlaylist);
                actionDropPlaylist = view.findViewById(R.id.actionDropPlaylist);

                viewPager = (ViewPager)view.findViewById(R.id.viewPager);
                mainView = viewPager.findViewById(R.id.item_page_0);
            }
        }
    }
}
