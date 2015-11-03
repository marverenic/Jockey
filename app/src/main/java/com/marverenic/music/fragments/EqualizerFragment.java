package com.marverenic.music.fragments;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.utils.Prefs;

public class EqualizerFragment extends Fragment {

    private EqualizerFrame[] sliders;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

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

            for (short i = 0; i < bandCount; i++) {
                inflater.inflate(R.layout.instance_eq_slider, equalizerPanel, true);
                sliders[i] = new EqualizerFrame(equalizerPanel.getChildAt(i), equalizer, i);
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

            prefsEditor.apply();
        }
    }

    public static class EqualizerFrame implements SeekBar.OnSeekBarChangeListener {

        final Equalizer equalizer;
        final short bandNumber;
        final SeekBar bandSlider;
        final TextView bandLabel;

        final short minLevel;
        final short maxLevel;

        public EqualizerFrame(View root, Equalizer eq, short bandNumber) {
            this.equalizer = eq;
            this.bandNumber = bandNumber;

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

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            equalizer.setBandLevel(bandNumber, (short) (progress - Math.abs(minLevel)));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    }
}
