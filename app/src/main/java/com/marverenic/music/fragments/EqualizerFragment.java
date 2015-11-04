package com.marverenic.music.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.utils.Prefs;

public class EqualizerFragment extends Fragment {

    private EqualizerFrame[] sliders;
    private Spinner presetSpinner;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_equalizer, container, false);

        LinearLayout equalizerPanel = (LinearLayout) layout.findViewById(R.id.equalizer_panel);

        int audioSession = PlayerController.getAudioSessionId();
        // Make sure that the audio session is valid before displaying the equalizer
        if (audioSession != 0) {
            Equalizer equalizer = new Equalizer(0, PlayerController.getAudioSessionId());
            equalizer.setEnabled(true);
            int bandCount = equalizer.getNumberOfBands();

            sliders = new EqualizerFrame[bandCount];

            presetSpinner = (Spinner) layout.findViewById(R.id.equalizerPresetSpinner);
            PresetAdapter presetAdapter = new PresetAdapter(getActivity(), equalizer, sliders);
            presetSpinner.setAdapter(presetAdapter);
            presetSpinner.setSelection(Prefs.getPrefs(getActivity()).getInt(Prefs.EQ_PRESET_ID, -1) + 1);
            presetSpinner.setOnItemSelectedListener(presetAdapter);

            for (short i = 0; i < bandCount; i++) {
                inflater.inflate(R.layout.instance_eq_slider, equalizerPanel, true);
                sliders[i] = new EqualizerFrame(equalizerPanel.getChildAt(i), equalizer, i, presetSpinner);
            }
        }

        return layout;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (sliders != null) {
            SharedPreferences.Editor prefsEditor = Prefs.getPrefs(getActivity()).edit();

            for (int i = 0; i < sliders.length; i++) {
                int value = sliders[i].bandSlider.getProgress() - Math.abs(sliders[i].minLevel);
                prefsEditor.putInt(Prefs.EQ_BAND_PREFIX + i, value);
            }

            prefsEditor.putInt(Prefs.EQ_PRESET_ID, (int) presetSpinner.getSelectedItemId());
            prefsEditor.apply();
        }
    }

    private static class PresetAdapter extends BaseAdapter implements AdapterView.OnItemSelectedListener {

        private Context context;
        private Equalizer equalizer;
        private String[] presets;
        private EqualizerFrame[] sliders;

        public PresetAdapter(Context context, Equalizer equalizer, EqualizerFrame[] sliders) {
            this.context = context;
            this.equalizer = equalizer;
            this.sliders = sliders;

            presets = new String[equalizer.getNumberOfPresets() + 1];
            presets[0] = "Custom"; // TODO String resource

            for (short i = 0; i < presets.length - 1; i++) {
                presets[i + 1] = equalizer.getPresetName(i);
            }
        }

        @Override
        public int getCount() {
            return presets.length;
        }

        @Override
        public Object getItem(int position) {
            return presets[position];
        }

        @Override
        public long getItemId(int position) {
            return position - 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater
                        .from(context)
                        .inflate(android.R.layout.simple_spinner_item, parent, false);
            }

            TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
            textView.setText(presets[position]);

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater
                        .from(context)
                        .inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            }

            return getView(position, convertView, parent);
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (id != -1) {
                equalizer.usePreset((short) id);

                for (short i = 0; i < sliders.length; i++) {
                    sliders[i].update(equalizer.getBandLevel(i));
                }
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    }

    public static class EqualizerFrame implements SeekBar.OnSeekBarChangeListener {

        final Equalizer equalizer;
        final short bandNumber;
        final SeekBar bandSlider;
        final TextView bandLabel;
        final Spinner presetSpinner;

        final short minLevel;
        final short maxLevel;

        public EqualizerFrame(View root, Equalizer eq, short bandNumber, Spinner presetSpinner) {
            this.equalizer = eq;
            this.bandNumber = bandNumber;
            this.presetSpinner = presetSpinner;

            bandSlider = (SeekBar) root.findViewById(R.id.eq_slider);
            bandLabel = (TextView) root.findViewById(R.id.eq_band_name);

            int frequency = eq.getBandFreqRange(bandNumber)[0] / 1000;

            if (frequency > 1000) {
                bandLabel.setText(frequency / 1000 + "K");
            } else {
                bandLabel.setText(Integer.toString(frequency));
            }

            short[] range = eq.getBandLevelRange();
            minLevel = range[0];
            maxLevel = range[1];

            bandSlider.setMax(Math.abs(minLevel) + maxLevel);
            bandSlider.setProgress(eq.getBandLevel(bandNumber) + Math.abs(range[0]));
            bandSlider.setOnSeekBarChangeListener(this);
        }

        public void update(int level) {
            bandSlider.setProgress(level + Math.abs(minLevel));
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            equalizer.setBandLevel(bandNumber, (short) (progress - Math.abs(minLevel)));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            presetSpinner.setSelection(0); // Disable any preset
        }
    }
}
