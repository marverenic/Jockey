package com.marverenic.music.fragments;

import android.app.ActionBar;
import android.content.Context;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.player.OldPlayerController;
import com.marverenic.music.player.RemoteEqualizer;
import com.marverenic.music.utils.Util;

import javax.inject.Inject;

public class EqualizerFragment extends Fragment implements CompoundButton.OnCheckedChangeListener,
        FragmentManager.OnBackStackChangedListener {

    @Inject PreferenceStore mPrefStore;

    private RemoteEqualizer equalizer;
    private EqualizerFrame[] sliders;
    private TextView presetSpinnerPrefix;
    private Spinner presetSpinner;
    private SwitchCompat equalizerToggle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getSupportFragmentManager().addOnBackStackChangedListener(this);

        JockeyApplication.getComponent(this).inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_equalizer, container, false);
        presetSpinnerPrefix = (TextView) layout.findViewById(R.id.eq_preset_prefix);
        presetSpinner = (Spinner) layout.findViewById(R.id.eq_preset_spinner);

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            equalizerToggle = new SwitchCompat(getActivity());
            equalizerToggle.setOnCheckedChangeListener(this);

            Toolbar.LayoutParams params = new Toolbar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    Gravity.END);
            int padding = (int) (16 * getResources().getDisplayMetrics().density);
            params.setMargins(padding, 0, padding, 0);

            toolbar.addView(equalizerToggle, params);

            AlphaAnimation anim = new AlphaAnimation(0f, 1.0f);
            anim.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            anim.setInterpolator(getContext(), android.R.anim.decelerate_interpolator);
            equalizerToggle.startAnimation(anim);
        }

        LinearLayout equalizerPanel = (LinearLayout) layout.findViewById(R.id.equalizer_panel);

        equalizer = generateEqualizerConfig();
        int bandCount = (equalizer != null) ? equalizer.getNumberOfBands() : 0;

        sliders = new EqualizerFrame[bandCount];

        PresetAdapter presetAdapter = new PresetAdapter(getActivity(), equalizer, sliders);
        presetSpinner.setAdapter(presetAdapter);
        presetSpinner.setSelection(mPrefStore.getEqualizerPresetId() + 1);
        presetSpinner.setOnItemSelectedListener(presetAdapter);

        for (short i = 0; i < bandCount; i++) {
            inflater.inflate(R.layout.instance_eq_slider, equalizerPanel, true);
            sliders[i] = new EqualizerFrame(equalizerPanel.getChildAt(i), equalizer,
                    i, presetSpinner);
        }

        setEqualizerEnabled(mPrefStore.getEqualizerEnabled());

        // If this device already has an application that can handle equalizers system-wide, inform
        // the user of possible issues by using Jockey's built-in equalizer
        if (Util.getSystemEqIntent(getActivity()) != null) {
            ((TextView) layout.findViewById(R.id.equalizer_notes))
                    .setText(R.string.equalizerNoteSystem);
        }

        return layout;
    }

    private RemoteEqualizer generateEqualizerConfig() {
        // Obtain an instance of the system equalizer to discover available configuration options
        // for an equalizer including bands and presets. This equalizer is not used to control
        // audio settings and is released before this method ends
        Equalizer systemEqualizer = new Equalizer(0, 1);

        RemoteEqualizer eq = new RemoteEqualizer(systemEqualizer);
        Equalizer.Settings settings = mPrefStore.getEqualizerSettings();
        if (settings != null) {
            eq.setProperties(mPrefStore.getEqualizerSettings());
        }

        systemEqualizer.release();
        return eq;
    }

    private void applyEqualizer() {
        mPrefStore.setEqualizerPresetId((int) presetSpinner.getSelectedItemId());
        mPrefStore.setEqualizerSettings(equalizer.getProperties());
        mPrefStore.setEqualizerEnabled(equalizerToggle.isChecked());

        OldPlayerController.updatePlayerPreferences(mPrefStore);
    }

    @Override
    public void onResume() {
        super.onResume();
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.header_equalizer);
        }
    }

    private void setEqualizerEnabled(boolean enabled) {
        if (equalizerToggle.isChecked() != enabled) {
            equalizerToggle.setChecked(enabled);
        }
        presetSpinnerPrefix.setEnabled(enabled);
        presetSpinner.setEnabled(enabled);
        for (EqualizerFrame f : sliders) {
            f.update(equalizer.getCurrentPreset() == -1 && enabled);
        }

        applyEqualizer();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setEqualizerEnabled(isChecked);
        applyEqualizer();
    }

    @Override
    public void onBackStackChanged() {
        if (isRemoving()) {
            final Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);

            if (toolbar != null) {
                final int duration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
                AlphaAnimation anim = new AlphaAnimation(1.0f, 0f);
                anim.setDuration(duration);
                anim.setInterpolator(getContext(), android.R.anim.decelerate_interpolator);
                equalizerToggle.startAnimation(anim);

                new Handler().postDelayed(() -> toolbar.removeView(equalizerToggle), duration);
            }

            applyEqualizer();
        }
    }

    private class PresetAdapter extends BaseAdapter implements AdapterView.OnItemSelectedListener {

        private Context context;
        private String[] presets;
        private EqualizerFrame[] sliders;

        PresetAdapter(Context context, RemoteEqualizer equalizer, EqualizerFrame[] sliders) {
            this.context = context;
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
            equalizer.usePreset((short) id);
            applyEqualizer();

            for (EqualizerFrame f : sliders) {
                f.update(id == -1 && equalizerToggle.isChecked());
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    private class EqualizerFrame implements SeekBar.OnSeekBarChangeListener {

        final short bandNumber;
        final SeekBar bandSlider;
        final TextView bandLabel;
        final Spinner presetSpinner;

        final int minLevel;
        final int maxLevel;

        public EqualizerFrame(View root, RemoteEqualizer eq, short bandNumber,
                              Spinner presetSpinner) {

            this.bandNumber = bandNumber;
            this.presetSpinner = presetSpinner;

            bandSlider = (SeekBar) root.findViewById(R.id.eq_slider);
            bandLabel = (TextView) root.findViewById(R.id.eq_band_name);

            int frequency = eq.getCenterFreq(bandNumber) / 1000;

            if (frequency > 1000) {
                bandLabel.setText(frequency / 1000 + "K");
            } else {
                bandLabel.setText(Integer.toString(frequency));
            }

            int[] range = eq.getBandLevelRange();
            minLevel = range[0];
            maxLevel = range[1];

            bandSlider.setMax(Math.abs(minLevel) + maxLevel);
            bandSlider.setProgress(eq.getBandLevel(bandNumber) + Math.abs(range[0]));
            bandSlider.setOnSeekBarChangeListener(this);
        }

        public void update(boolean enabled) {
            bandSlider.setEnabled(enabled);
            bandLabel.setEnabled(enabled);
        }

        public void update(int level) {
            bandSlider.setProgress(level + Math.abs(minLevel));
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            equalizer.setBandLevel(bandNumber, (short) (progress - Math.abs(minLevel)));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            presetSpinner.setSelection(0);
            equalizer.usePreset(-1);
            applyEqualizer();
        }
    }
}
