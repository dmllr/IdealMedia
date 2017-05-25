package com.armedarms.idealmedia.adapters;

import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.Settings;
import com.armedarms.idealmedia.domain.Playlist;
import com.armedarms.idealmedia.domain.PlaylistItem;
import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.fragments.BasePlayingFragment;
import com.armedarms.idealmedia.fragments.PlaylistFragment;
import com.armedarms.idealmedia.tasks.TaskGetArtwork;
import com.armedarms.idealmedia.utils.MediaUtils;
import libs.CircularProgressButton;

import com.armedarms.idealmedia.utils.ResUtils;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.r0adkll.postoffice.PostOffice;
import com.r0adkll.postoffice.model.Design;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class PlayerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final BasePlayingFragment fragment;

    private boolean _adEnabled;
    private int _adBlockLength = 7;

    LayoutInflater inflater;
    List<Playlist> playlists = new ArrayList<Playlist>();
    List<PlaylistItem> displayList = new ArrayList<PlaylistItem>();
    private long cachedSize = -1;

    public PlayerAdapter(Context context, BasePlayingFragment fragment){
        this.context = context;
        this.fragment = fragment;

        Calendar adStartDate = Calendar.getInstance();
        adStartDate.set(2015, Calendar.JANUARY, 1);

        //noinspection ConstantConditions, PointlessBooleanExpression
        _adEnabled = false; //!(Settings.PREMIUM || ((NavigationActivity)context).hasPremiumPurchase()) && Calendar.getInstance().after(adStartDate);
    }

    public PlayerAdapter(Context context, BasePlayingFragment fragment, ArrayList<Track> data){
        this(context, fragment);

        playlists.add(new Playlist(data));
        amend();
    }

    public PlayerAdapter(Context context, BasePlayingFragment fragment, Playlist... playlistArgs){
        this(context, fragment);

        Collections.addAll(playlists, playlistArgs);
        amend();
    }

    private void amend() {
        displayList = new ArrayList<PlaylistItem>(20);
        for (Playlist p : playlists) {
            if (playlists.size() > 1)
                displayList.add(PlaylistItem.title(p.getTitle()));
            if (playlists.size() > 1 && p.size() == 0)
                displayList.add(PlaylistItem.messageEmpty());
            int i = 0;
            for (Track t : p.getTracks()) {
                i++;
                displayList.add(PlaylistItem.track(i, t));

                if (_adEnabled && i % _adBlockLength == 0)
                    displayList.add(PlaylistItem.ad());
            }
        }
    }

    public int listToVisiblePosition(int position) {
        int plus = 0;
        int translated = position;
        for (Playlist playlist : playlists) {
            int size = playlist.size();

            if (playlists.size() > 1)
                plus++;
            if (playlists.size() > 1 && size == 0)
                plus++;

            if (translated < size) {
                if (_adEnabled)
                    plus += translated / _adBlockLength;
                return translated +  plus;
                //return translated * (_adBlockLength) / (_adBlockLength - 1) + plus;
            } else {
                translated -= size;
                plus += size;
                if (_adEnabled)
                    plus += size / _adBlockLength;
            }
        }

        return -1;
    }

    public int visibleToListPosition(int position) {
        int plus = 0;
        int translated = position;
        for (Playlist playlist : playlists) {
            int minus = 0;
            int size = playlist.size();
            if (playlists.size() > 1)
                minus++;
            if (playlists.size() > 1 && size == 0)
                minus++;

            int adsBefore = translated / (_adBlockLength + 1);

            if (_adEnabled)
                minus += adsBefore;

            if (translated - minus < size)
                return translated - minus + plus;
            else {
                if (_adEnabled) {
                    minus -= adsBefore;
                    minus += size / (_adBlockLength + 1);
                }
                translated -= size;
                plus += size;
            }

            translated = translated - minus;
        }

        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        return displayList.get(position).type.ordinal();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (inflater == null)
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (viewType == PlaylistItem.ItemType.AD.ordinal())
            return getAdViewHolder(parent);
        if (viewType == PlaylistItem.ItemType.TITLE.ordinal())
            return getTitleViewHolder(parent);
        if (viewType == PlaylistItem.ItemType.TRACK.ordinal())
            return getTrackViewHolder(parent);
        if (viewType == PlaylistItem.ItemType.MESSAGE_EMPTY.ordinal())
            return getMessageViewHolder(parent);

        throw new UnsupportedOperationException(String.format("onCreateViewHolder(parent, %d) has unknown viewType", viewType));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TitleViewHolder)
            bindTitleViewHolder((TitleViewHolder)holder, position);
        if (holder instanceof MessageViewHolder)
            bindMessageViewHolder((MessageViewHolder) holder, position);
        if (holder instanceof TrackViewHolder)
            bindTrackViewHolder((TrackViewHolder) holder, position);
    }

    @Override
    public int getItemCount() {
        if (displayList == null)
            return 0;

        long newSize = 0;
        for (Playlist p : playlists)
            newSize += p.size();

        if (newSize != cachedSize) {
            cachedSize = newSize;
            amend();
        }

        return displayList.size();
    }

    private RecyclerView.ViewHolder getTrackViewHolder(final ViewGroup parent) {
        View view = inflater.inflate(R.layout.cell_track, parent, false);

        ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewPager);
        viewPager.setAdapter(new LRViewPagerAdapter(R.id.item_page_L, R.id.item_page_0, R.id.item_page_R));
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
            @Override public void onPageSelected(int position) { }

            @Override
            public void onPageScrollStateChanged(int state) {
                fragment.getListView().setTag(R.string.key_scroll_in_action, state == ViewPager.SCROLL_STATE_DRAGGING);
            }
        });

        return new TrackViewHolder(view);
    }

    private RecyclerView.ViewHolder getTitleViewHolder(ViewGroup parent) {
        View view = inflater.inflate(R.layout.cell_track_title, parent, false);

        return new TitleViewHolder(view);
    }

    private RecyclerView.ViewHolder getMessageViewHolder(ViewGroup parent) {
        View view = inflater.inflate(R.layout.cell_track_message, parent, false);

        return new MessageViewHolder(view);
    }

    private RecyclerView.ViewHolder getAdViewHolder(ViewGroup parent) {
        View view = inflater.inflate(R.layout.cell_track_ads, parent, false);

        AdView adView = (AdView)view.findViewById(R.id.adView);

        final View finalView = view;
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                finalView.post(new Runnable() {
                    @Override
                    public void run() {
                        View v = finalView.findViewById(R.id.adViewOffline);
                        if (v != null)
                            v.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                finalView.post(new Runnable() {
                    @Override
                    public void run() {
                        View v = finalView.findViewById(R.id.adViewOffline);
                        if (v != null) {
                            v.setVisibility(View.VISIBLE);
                            v.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    openPremiumAtMarket();
                                }
                            });
                        }
                    }
                });
            }
        });

        AdRequest.Builder builder = new AdRequest.Builder();
        AdRequest adRequest = builder.build();
        adView.loadAd(adRequest);


        return new AdViewHolder(view);
    }

    private void bindTrackViewHolder(final TrackViewHolder holder, final int position) {
        final int index = displayList.get(position).index;

        final Track track = (Track) displayList.get(position).data;
        int sec = track.getDuration();
        int min = sec / 60;
        sec %= 60;

        holder.track = track;

        if (!fragment.isSnapshotMatch() || visibleToListPosition(position) != fragment.getPlayingPosition()) {
            holder.index.setProgress(0);
            holder.index.setText(String.valueOf(100+index));
        }
        holder.index.setText(String.valueOf(index));
        holder.index.setIdleText(String.valueOf(index));
        holder.artist.setText(track.getArtist());
        holder.title.setText(track.getTitle());
        holder.duration.setText(String.format("%2d:%02d", min, sec));
        holder.textSearchAuthor.setText(String.format(context.getString(R.string.track_action__search_artist), track.getArtist()));
        holder.downloadProgress.setProgress(0);
        holder.downloadProgress.setVisibility(View.GONE);

        holder.actionSetRingtone.setVisibility("".equals(track.getPath()) ? View.GONE : View.VISIBLE);
        holder.actionSetRingtone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setTrackAsRingtone(context, fragment.getFragmentManager(), track);
            }
        });
        holder.actionShare.setVisibility("".equals(track.getPath()) ? View.GONE : View.VISIBLE);
        holder.actionShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareTrack(context, track);
            }
        });
        holder.actionSearchArtist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchArtist(track);
            }
        });
        holder.actionDownload.setVisibility("".equals(track.getPath()) ? View.VISIBLE : View.GONE);
        holder.actionDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (track.isDownloadInAction)
                    stopDownload(context, holder);
                else
                    download(PlayerAdapter.this, track, holder);
            }
        });
        holder.actionToPlaylist.setVisibility(fragment instanceof PlaylistFragment ? View.GONE : View.VISIBLE);
        holder.actionToPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addToPlaylist(track);
            }
        });
        holder.actionFromPlaylist.setVisibility(fragment instanceof PlaylistFragment ? View.VISIBLE : View.GONE);
        holder.actionFromPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeFromPlaylist(track);
            }
        });

        holder.mainView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.index.setIndeterminateProgressMode(true);
                holder.index.setProgress(1);
                fragment.itemClick(position);
            }
        });

        holder.viewPager.setCurrentItem(1);

        Bitmap artwork = MediaUtils.getTrackCellArtwork(context, track);
        if (artwork == null)
            new TaskGetArtwork(context).execute(track);
        holder.artwork.setImageBitmap(artwork);

        holder.itemView.setTag(holder);
    }

    private void bindMessageViewHolder(MessageViewHolder holder, int position) {
        holder.text.setText(context.getString(R.string.no_search_results));
    }

    private void bindTitleViewHolder(TitleViewHolder holder, int position) {
        holder.text.setText((String) displayList.get(position).data);
    }

    private void removeFromPlaylist(Track track) {
        int index = -1;
        Playlist playlist = null;
        for (Playlist p : playlists) {
            playlist = p;
            index = p.getTracks().indexOf(track);
        }

        if (playlist != null && index > -1) {
            playlist.getTracks().remove(index);
            notifyDataSetChanged();

            if (playlist.id != null)
                ((NavigationActivity)fragment.getActivity()).savePlaylist(playlist);
        }
    }

    private static void stopDownload(Context context, TrackViewHolder holder) {
        final DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.remove(holder.track.downloadID);

        holder.track.isDownloadInAction = false;
        holder.downloadProgress.setProgress(0);
        holder.downloadProgress.setVisibility(View.GONE);
    }

    private static void download(final PlayerAdapter adapter, final Track track, final TrackViewHolder holder) {
        String url = track.getUrl();

        if (url == null || "".equals(url))
            return;

        holder.downloadProgress.setIndeterminateProgressMode(true);
        holder.downloadProgress.setProgress(1);
        holder.downloadProgress.setVisibility(View.VISIBLE);

        String fileTitle = String.format("%s - %s", track.getArtist(), track.getTitle());
        String fileExt = url.substring(url.lastIndexOf(".") + 1, url.lastIndexOf(".") + 4);
        String fileName = String.format("%s.%s", fileTitle, fileExt);
        final String filePath = String.format("%s/%s", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath(), fileName);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(fileTitle);
        request.setDescription(filePath);
        request.setDestinationUri(Uri.parse("file://" + filePath));

        final DownloadManager manager = (DownloadManager) adapter.context.getSystemService(Context.DOWNLOAD_SERVICE);

        try {
            final long downloadId = manager.enqueue(request);

            track.isDownloadInAction = true;
            track.downloadID = downloadId;

        } catch (Exception ignored) {
            holder.downloadProgress.setIndeterminateProgressMode(false);
            holder.downloadProgress.setProgress(-1);
            holder.downloadProgress.setVisibility(View.GONE);
        }

        Tracker t = ((NavigationActivity) adapter.fragment.getActivity()).getTracker(NavigationActivity.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder().setCategory("Track").setAction("download").build());

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean downloading = true;

                while (downloading) {
                    if (holder.track == track) {
                        DownloadManager.Query q = new DownloadManager.Query();
                        q.setFilterById(track.downloadID);

                        Cursor cursor = manager.query(q);

                        if (cursor.getCount() == 0) {
                            Thread.currentThread().interrupt();
                            return;
                        }

                        cursor.moveToFirst();
                        int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                        boolean indeterminate = true;
                        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        switch (status) {
                            case DownloadManager.STATUS_PENDING:
                                indeterminate = true;
                                break;
                            case DownloadManager.STATUS_RUNNING:
                                indeterminate = false;
                                break;
                            case DownloadManager.STATUS_FAILED:
                                downloading = false;
                                adapter.onDownloadFailed(holder);
                                break;
                            case DownloadManager.STATUS_SUCCESSFUL:
                                downloading = false;
                                adapter.onDownloadSuccessful(holder, filePath);
                                break;
                        }

                        final int progress = (int) ((bytes_downloaded * 100L) / bytes_total);

                        final boolean finalIndeterminate = indeterminate || progress == 0;
                        if (holder.downloadProgress != null) {
                            holder.downloadProgress.post(new Runnable() {
                                @Override
                                public void run() {
                                    holder.downloadProgress.setIndeterminateProgressMode(finalIndeterminate);
                                    holder.downloadProgress.setProgress(finalIndeterminate ? 1 : progress);
                                }
                            });
                        }

                        cursor.close();

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ignored) { }
                    }
                }
            }
        }).start();
    }

    private void onDownloadFailed(final TrackViewHolder holder) {
        holder.track.isDownloadInAction = false;
        holder.track.downloadID = -1;

        holder.downloadProgress.post(new Runnable() {
            @Override
            public void run() {
                holder.downloadProgress.setProgress(-1);
                holder.downloadProgress.setVisibility(View.GONE);
            }
        });
    }

    private void onDownloadSuccessful(final TrackViewHolder holder, String filePath) {
        holder.track.isDownloadInAction = false;
        holder.track.downloadID = -1;
        holder.track.setPath(filePath);

        holder.actionDownload.post(new Runnable() {
            @Override
            public void run() {
                holder.actionDownload.setVisibility(View.GONE);
                holder.actionSetRingtone.setVisibility(View.VISIBLE);
                holder.actionShare.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addToPlaylist(final Track track) {
        fragment.addToQuickPlaylist(track);
    }

    private static void setTrackAsRingtone(final Context context, FragmentManager fragmentManager, final Track track) {

        PostOffice.newMail(context)
                .setTitle(R.string.set_as_ringtone_question)
                .setMessage(track.getDisplay())
                .setDesign(Design.MATERIAL_LIGHT)
                .setButtonTextColor(Dialog.BUTTON_POSITIVE, ResUtils.resolve(context, R.attr.colorPositive))
                .setButton(Dialog.BUTTON_POSITIVE, android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        MediaUtils.setRingtone(context, track);
                    }
                })
                .setButton(Dialog.BUTTON_NEGATIVE, android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .build()
                .show(fragmentManager);
    }

    private static void shareTrack(Context context, final Track track) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(track.getPath())));
        shareIntent.setType("audio/*");
        context.startActivity(Intent.createChooser(shareIntent, context.getResources().getText(R.string.track_action__share)));
    }

    private void searchArtist(Track track) {
        fragment.startSearch(track.getArtist());
    }

    private void openPremiumAtMarket() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.armedarms.idealmedia.premium"));

        try {
            fragment.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.armedarms.idealmedia.premium"));
            try {
                fragment.startActivity(intent);
            } catch (ActivityNotFoundException e1) {
                Toast.makeText(fragment.getActivity(), fragment.getString(R.string.text_no_market_app), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public class TrackViewHolder extends RecyclerView.ViewHolder {
        public Track track;

        public View actionSetRingtone;
        public View actionShare;
        public View actionSearchArtist;
        public View actionDownload;
        public View actionToPlaylist;
        public View actionFromPlaylist;

        public CircularProgressButton index;

        public TextView artist;
        public TextView title;
        public TextView duration;
        public TextView textSearchAuthor;

        public ImageView artwork;

        public CircularProgressButton downloadProgress;

        private final ViewPager viewPager;
        private final View mainView;

        public TrackViewHolder(View view) {
            super(view);

            index = (CircularProgressButton) view.findViewById(R.id.cell_index);
            artist = (TextView) view.findViewById(R.id.cell_artist);
            title = (TextView) view.findViewById(R.id.cell_title);
            duration = (TextView) view.findViewById(R.id.cell_duration);
            textSearchAuthor = (TextView) view.findViewById(R.id.textSearchAuthor);
            actionSetRingtone = view.findViewById(R.id.actionSetRingtone);
            actionShare = view.findViewById(R.id.actionShare);
            actionSearchArtist = view.findViewById(R.id.actionSearchAuthor);
            actionDownload = view.findViewById(R.id.actionDownload);
            actionToPlaylist = view.findViewById(R.id.actionToPlaylist);
            actionFromPlaylist = view.findViewById(R.id.actionFromPlaylist);
            artwork = (ImageView) view.findViewById(R.id.cell_artwork);
            downloadProgress = (CircularProgressButton) view.findViewById(R.id.track_cell_progress_download);

            viewPager = (ViewPager) view.findViewById(R.id.viewPager);
            mainView = viewPager.findViewById(R.id.item_page_0);
        }
    }


    class AdViewHolder extends RecyclerView.ViewHolder {
        public AdViewHolder(View view) {
            super(view);
        }
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView text;

        public MessageViewHolder(View view) {
            super(view);
            text = (TextView)view.findViewById(android.R.id.text1);
        }
    }

    class TitleViewHolder extends RecyclerView.ViewHolder {
        TextView text;

        public TitleViewHolder(View view) {
            super(view);
            text = (TextView)view.findViewById(android.R.id.text1);
        }
    }

}