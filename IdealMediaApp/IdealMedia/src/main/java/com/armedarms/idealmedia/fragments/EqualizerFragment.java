package com.armedarms.idealmedia.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.armedarms.idealmedia.PlayerService;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.tools.Wave;
import com.armedarms.idealmedia.utils.ResUtils;

public class EqualizerFragment extends BaseFragment implements IHasColor, Wave.IOnBandUpdateListener {


    public EqualizerFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_equalizer, container, false);

        setupEqualizerFxAndUI(activity, (ViewGroup)view);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Spinner spinnerPresets = (Spinner)view.findViewById(R.id.eq_presets);
        CharSequence[] presets = getResources().getTextArray(R.array.presets_array);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getActivity(), android.R.layout.simple_spinner_item, presets) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView)view.findViewById(android.R.id.text1)).setTextColor(ResUtils.color(getActivity(), R.attr.colorAccent));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPresets.setAdapter(adapter);
        spinnerPresets.post(new Runnable() {
            public void run() {
                spinnerPresets.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        setPreset(i);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }
        });

        view.findViewById(R.id.reset_eq).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetEqualizer();
            }
        });
    }

    private void setPreset(int presetIndex) {
        int presetId = 0;
        switch (presetIndex) {
            case 0:
                presetId = R.array.preset_reset; break;
            case 1:
                presetId = R.array.preset_headphones_ie; break;
            case 2:
                presetId = R.array.preset_headphones; break;
            case 3:
                presetId = R.array.preset_speaker; break;
            case 4:
                presetId = R.array.preset_loudspeaker; break;
        }

        if (presetId == 0) {
            resetEqualizer();
            return;
        }

        PlayerService service = activity.getService();
        int[] values = getResources().getIntArray(presetId);
        for(int i = 0; i < service.getFXBandValues().length; i++)
            service.updateFXBand(i, values[i]);
    }

    private void resetEqualizer() {
        PlayerService service = activity.getService();
        for(int i = 0; i < service.getFXBandValues().length; i++)
            service.updateFXBand(i, 15);
    }

    private void setupEqualizerFxAndUI(Context context, ViewGroup view) {
        Wave mWave = (Wave) view.findViewById(R.id.waveEqualizer);
        mWave.setAboveWaveColor(0xffffffff);
        mWave.setBlowWaveColor(0xffffffff);
        mWave.initializePainters();
        mWave.setFxValues(activity.getService().getFXBandValues());
        mWave.setUpdateListener(this);
    }

    @Override
    public int getColor() {
        return ResUtils.color(activity, R.attr.colorEqualizer);
    }

    @Override
    public void OnBandUpdated(int band, float value) {
        activity.getService().updateFXBand(band, value);
    }
}
