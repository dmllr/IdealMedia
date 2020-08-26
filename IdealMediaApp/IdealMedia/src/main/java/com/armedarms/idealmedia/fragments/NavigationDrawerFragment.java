package com.armedarms.idealmedia.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.Settings;
import com.armedarms.idealmedia.tools.TwitterShare;

import java.util.List;

public class NavigationDrawerFragment extends Fragment implements View.OnClickListener {

    public enum MenuLine {
        PLAYLISTS,
        PLAYLISTS_ALL_MUSIC,
        PLAYLISTS_NOW_PLAYING,
        PLAYLISTS_PLAYBACK_HISTORY,
        VK,
        SEARCH,
        VK_POPULAR,
        VK_RECOMMENDATIONS,
        DEVICE,
        DEVICE_TRACKS,
        DEVICE_ARTISTS,
        DEVICE_ALBUMS
    }

    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    private NavigationDrawerCallbacks mCallbacks;

    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private View mDrawerView;
    private View mFragmentContainerView;
    private String storedSearchQuery;
    private View vkSearchItem;


    private int mCurrentSelectedPosition = 0;
    private boolean mUserLearnedDrawer;

    EditText textSearch;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            boolean mFromSavedInstanceState = true;
        }

        // Select either the default item (0) or the last selected item.
        //selectItem(mCurrentSelectedPosition);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mDrawerView = inflater.inflate(R.layout.fragment_navigation_drawer, container,  true);

        setOnClickListeners();

        setSocialListeners();
        mDrawerView.findViewById(R.id.icon_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.runFinalization();
                System.exit(0);
            }
        });

        textSearch = (EditText)mDrawerView.findViewById(R.id.textSearch);
        textSearch.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_UP && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    mCallbacks.onNavigationDrawerItemSelected(MenuLine.SEARCH, textSearch.getText().toString());
                }
                return false;
            }
        });

        update();

        return mDrawerView;
    }

    public void update() {
        mDrawerView.findViewById(R.id.menu_playlists_playback_history).setVisibility(Settings.PREMIUM || ((NavigationActivity)getActivity()).hasPremiumPurchase() ? View.VISIBLE : View.GONE);
    }

    private void setOnClickListeners() {
        mDrawerView.findViewById(R.id.menu_playlists).setOnClickListener(this);
        mDrawerView.findViewById(R.id.menu_playlists_all_music).setOnClickListener(this);
        mDrawerView.findViewById(R.id.menu_playlists_playing).setOnClickListener(this);
        mDrawerView.findViewById(R.id.menu_playlists_playback_history).setOnClickListener(this);
        mDrawerView.findViewById(R.id.menu_vk).setOnClickListener(this);
        (vkSearchItem = mDrawerView.findViewById(R.id.menu_vk_search)).setOnClickListener(this);
        mDrawerView.findViewById(R.id.menu_vk_popular).setOnClickListener(this);
        mDrawerView.findViewById(R.id.menu_vk_recommendations).setOnClickListener(this);
        mDrawerView.findViewById(R.id.menu_device).setOnClickListener(this);
        mDrawerView.findViewById(R.id.menu_device_tracks).setOnClickListener(this);
        mDrawerView.findViewById(R.id.menu_device_artists).setOnClickListener(this);
        mDrawerView.findViewById(R.id.menu_device_albums).setOnClickListener(this);
    }

    private void setSocialListeners() {
        mDrawerView.findViewById(R.id.icon_social_tw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareTwitter();
            }
        });
        mDrawerView.findViewById(R.id.icon_social_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rateThisApp();
            }
        });
        mDrawerView.findViewById(R.id.icon_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings();
            }
        });
    }

    private void rateThisApp() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + getActivity().getPackageName()));

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + getActivity().getPackageName()));
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e1) {
                Toast.makeText(getActivity(), getString(R.string.text_no_market_app), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openSettings() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            mDrawerLayout.openDrawer(GravityCompat.END);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void shareTwitter() {
        Intent twitterIntent = new Intent();
        twitterIntent.setType("text/plain");
        twitterIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.like_text));

        final PackageManager packageManager = getActivity().getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(twitterIntent, PackageManager.MATCH_DEFAULT_ONLY);

        boolean found = false;
        for (ResolveInfo resolveInfo : list) {
            String p = resolveInfo.activityInfo.packageName;
            if (p != null && p.startsWith("com.twitter.android")) {
                found = true;
                twitterIntent.setPackage(p);
                startActivity(twitterIntent);
            }
        }

        if (!found) {
            ProgressDialog progressDialog = new ProgressDialog(getActivity());

            TwitterShare d = new TwitterShare(
                    getActivity(), progressDialog,
                    "http://twitter.com/share?text=" + Uri.encode(getString(R.string.like_text_twitter)) + "&url=" + Uri.parse(getString(R.string.like_url)));
            d.show();

            progressDialog.setMessage(getString(R.string.text_loading));
            progressDialog.setCancelable(true);
            progressDialog.show();
        }
    }


    private void showThankYouToast() {
        Toast.makeText(getActivity(), String.format(getString(R.string.like_thanks_for_sharing), getString(R.string.app_name)), Toast.LENGTH_LONG).show();
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    public void closeDrawer() {
        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawers();
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the drawer is open, show the global app actions in the action bar. See also
        // showGlobalContextActionBar, which controls the top-left area of the action bar.
        if (mDrawerLayout != null && isDrawerOpen()) {
            inflater.inflate(R.menu.global, menu);
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        //if (item.getItemId() == R.id.action_example) {
        //    Toast.makeText(getActivity(), "Example action.", Toast.LENGTH_SHORT).show();
        //    return true;
        //}

        return super.onOptionsItemSelected(item);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        androidx.appcompat.app.ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(R.string.app_name);
    }

    private androidx.appcompat.app.ActionBar getActionBar() {
        return ((NavigationActivity)getActivity()).getSupportActionBar();
    }

    @Override
    public void onClick(View view) {
        MenuLine item = MenuLine.DEVICE;

        if (view.getId() == R.id.menu_playlists)
            item = MenuLine.PLAYLISTS;
        if (view.getId() == R.id.menu_playlists_all_music)
            item = MenuLine.PLAYLISTS_ALL_MUSIC;
        if (view.getId() == R.id.menu_playlists_playing)
            item = MenuLine.PLAYLISTS_NOW_PLAYING;
        if (view.getId() == R.id.menu_playlists_playback_history)
            item = MenuLine.PLAYLISTS_PLAYBACK_HISTORY;
        if (view.getId() == R.id.menu_vk)
            item = MenuLine.VK;
        if (view.getId() == R.id.menu_vk_popular)
            item = MenuLine.VK_POPULAR;
        if (view.getId() == R.id.menu_vk_recommendations)
            item = MenuLine.VK_RECOMMENDATIONS;
        if (view.getId() == R.id.menu_vk_search) {
            if ("".equals(storedSearchQuery)) {
                TextView textStoredSearch = (TextView)vkSearchItem.findViewById(R.id.textStoredSearch);
                storedSearchQuery = textStoredSearch.getText().toString().replace("\"", "");
            }
            if (!"".equals(storedSearchQuery))
                item = MenuLine.SEARCH;
            else
                item = MenuLine.VK_POPULAR;
        }
        if (view.getId() == R.id.menu_device)
            item = MenuLine.DEVICE;
        if (view.getId() == R.id.menu_device_tracks)
            item = MenuLine.DEVICE_TRACKS;
        if (view.getId() == R.id.menu_device_artists)
            item = MenuLine.DEVICE_ARTISTS;
        if (view.getId() == R.id.menu_device_albums)
            item = MenuLine.DEVICE_ALBUMS;

        mCallbacks.onNavigationDrawerItemSelected(item, storedSearchQuery);
    }

    public void storeSearchQuery(String query) {
        TextView textStoredSearch = (TextView)vkSearchItem.findViewById(R.id.textStoredSearch);
        if (textStoredSearch != null) {
            storedSearchQuery = query;
            textStoredSearch.setText(String.format("\"%s\"", storedSearchQuery));
            vkSearchItem.setVisibility(View.VISIBLE);
        }
    }

    public void clearStoredSearch() {
        TextView textStoredSearch = (TextView)vkSearchItem.findViewById(R.id.textStoredSearch);
        if (textStoredSearch != null) {
            storedSearchQuery = "";
            textStoredSearch.setText("");
            vkSearchItem.setVisibility(View.GONE);
        }
    }

    public static interface NavigationDrawerCallbacks {
        void onNavigationDrawerItemSelected(MenuLine item, String param);
    }
}
