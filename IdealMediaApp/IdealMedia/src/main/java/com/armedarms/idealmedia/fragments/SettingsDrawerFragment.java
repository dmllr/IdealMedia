package com.armedarms.idealmedia.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.dialogs.DialogSelectDirectory;
import com.armedarms.idealmedia.utils.ResUtils;

public class SettingsDrawerFragment extends Fragment implements View.OnClickListener {

    private OnSettingsInteractionListener mListener;
    private TextView textPurchasePremium;
    private TextView textMediaPath;
    private TextView textMediaMethod;
    private TextView textForeignVKPopular;
    private View viewMediaPathPref;

    public SettingsDrawerFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_drawer, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewMediaPathPref = view.findViewById(R.id.preference_media_path);
        view.setOnClickListener(this);
        view.findViewById(R.id.preference_purchase_premium).setOnClickListener(this);
        view.findViewById(R.id.preference_media_method).setOnClickListener(this);
        view.findViewById(R.id.preference_foreign_popular).setOnClickListener(this);
        view.findViewById(R.id.preference_logout_vk).setOnClickListener(this);
        view.findViewById(R.id.preference_equalizer).setOnClickListener(this);

        String mediaPath = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.key_mediapath), "/");
        textMediaPath = (TextView)view.findViewById(R.id.textMediaPath);
        textMediaPath.setText(mediaPath);

        boolean isForeignVK = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.key_foreign_vk_popular), false);
        textForeignVKPopular = (TextView)view.findViewById(R.id.textForeignVKPopular);
        textForeignVKPopular.setText(isForeignVK ? R.string.yes : R.string.no);

        boolean fullScan = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.key_media_method_full), false);
        textMediaMethod = (TextView)view.findViewById(R.id.textMediaMethod);
        textMediaMethod.setText(fullScan ? R.string.settings_media_method_full : R.string.settings_media_method_quick);
        viewMediaPathPref.setVisibility(fullScan ? View.VISIBLE : View.GONE);

        textPurchasePremium = (TextView)view.findViewById(R.id.textPurchasePremium);

        int themeIndex = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(getString(R.string.key_theme), 0);
        final Spinner spinnerThemes = (Spinner)view.findViewById(R.id.theme_list);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getActivity(), android.R.layout.simple_spinner_item, getResources().getTextArray(R.array.themes_array)) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView)view.findViewById(android.R.id.text1);
                text.setTextSize(getResources().getDimension(R.dimen.preference_text_size) / getResources().getDisplayMetrics().density);
                text.setTextColor(ResUtils.color(getActivity(), R.attr.colorPreferenceCellSubtext));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                int resId = ResUtils.resolve(getActivity(), R.attr.colorPreferences);
                if (resId != 0)
                    view.setBackgroundResource(resId);
                else
                    view.setBackgroundColor(ResUtils.color(getActivity(), R.attr.colorPreferences));
                TextView text = (TextView)view.findViewById(android.R.id.text1);
                text.setTextSize(getResources().getDimension(R.dimen.preference_text_size) / getResources().getDisplayMetrics().density);
                text.setTextColor(ResUtils.color(getActivity(), R.attr.colorPreferenceCellText));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerThemes.setAdapter(adapter);
        if (themeIndex < adapter.getCount())
            spinnerThemes.setSelection(themeIndex, false);

        spinnerThemes.post(new Runnable() {
            public void run() {
                spinnerThemes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        switchTheme(i);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }
        });
    }

    public void update() {
        NavigationActivity activity = (NavigationActivity)getActivity();
        if (activity.hasPremiumPurchase()) {
            textPurchasePremium.setText(R.string.settings_premium_you_are_premium);
        } else {
            textPurchasePremium.setText(R.string.settings_premium);
        }
    }

    private void switchTheme(int themeIndex) {
        if (mListener != null)
            mListener.switchTheme(themeIndex);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSettingsInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.preference_media_path) {
            selectMediaPath();
        }
        if (id == R.id.preference_media_method) {
            toggleMediaMethod();
        }
        if (id == R.id.preference_equalizer) {
            equalizer();
        }
        if (id == R.id.preference_purchase_premium) {
            purchasePremium();
        }
    }

    private void purchasePremium() {
        if (mListener != null)
            mListener.purchasePremium();
    }

    private void toggleMediaMethod() {
        boolean fullScan = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.key_media_method_full), false);
        fullScan = !fullScan;
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(getString(R.string.key_media_method_full), fullScan).commit();

        textMediaMethod.setText(fullScan ? R.string.settings_media_method_full : R.string.settings_media_method_quick);
        viewMediaPathPref.setVisibility(fullScan ? View.VISIBLE : View.GONE);

        if (mListener != null)
            mListener.onMediaMethodChanged(fullScan);
    }

    private void equalizer() {
        mListener.onEqualizerPreference();
    }

    private void selectMediaPath() {
        String mediaPath = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.key_mediapath), "");
        if ("".equals(mediaPath))
            mediaPath = "/";
        new DialogSelectDirectory(
                getActivity(),
                getFragmentManager(),
                new DialogSelectDirectory.Result() {
                    @Override
                    public void onChooseDirectory(String dir) {
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(getString(R.string.key_mediapath), dir).commit();
                        textMediaPath.setText(dir);

                        if (mListener != null)
                            mListener.onMediaPathChanged(dir);
                    }

                    @Override
                    public void onCancelChooseDirectory() {

                    }
                },
                mediaPath);
    }

    public interface OnSettingsInteractionListener {
        void onMediaPathChanged(String mediaPath);
        void onMediaMethodChanged(boolean isFullScan);
        void onEqualizerPreference();
        void switchTheme(int themeIndex);
        void purchasePremium();
    }

}
