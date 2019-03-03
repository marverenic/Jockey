package com.marverenic.music.player;

import android.media.audiofx.Equalizer;
import android.os.Parcel;
import android.os.Parcelable;

public class RemoteEqualizer implements Parcelable {

    private Band[] mBands;
    private Preset[] mPresets;

    private int mMinBandLevel;
    private int mMaxBandLevel;

    private int[] mBandLevels;
    private int mCurPreset;

    private static Band[] getEqualizerBands(Equalizer equalizer) {
        Band[] bands = new Band[equalizer.getNumberOfBands()];

        for (short i = 0; i < bands.length; i++) {
            int[] range = equalizer.getBandFreqRange(i);
            int centerFrequency = equalizer.getCenterFreq(i);
            int lowFrequency = range[0];
            int highFrequency = range[1];

            bands[i] = new Band(lowFrequency, highFrequency, centerFrequency);
        }

        return bands;
    }

    private static Preset[] getEqualizerPresets(Equalizer equalizer) {
        Preset[] presets = new Preset[equalizer.getNumberOfPresets()];

        for (short i = 0; i < presets.length; i++) {
            presets[i] = new Preset(equalizer.getPresetName(i));
        }

        return presets;
    }

    public RemoteEqualizer(Equalizer eq) {
        this(getEqualizerBands(eq), getEqualizerPresets(eq), eq.getProperties(),
                eq.getBandLevelRange());
    }

    public RemoteEqualizer(Band[] bands, Preset[] presets, Equalizer.Settings settings,
                           short[] bandLevelRange) {
        mBands = bands;
        mPresets = presets;

        mMinBandLevel = bandLevelRange[0];
        mMaxBandLevel = bandLevelRange[1];

        setProperties(settings);
    }

    protected RemoteEqualizer(Parcel in) {
        mBands = in.createTypedArray(Band.CREATOR);
        mPresets = in.createTypedArray(Preset.CREATOR);
        mBandLevels = in.createIntArray();
        mMinBandLevel = in.readInt();
        mMaxBandLevel = in.readInt();
        mCurPreset = in.readInt();
    }

    public static final Creator<RemoteEqualizer> CREATOR = new Creator<RemoteEqualizer>() {
        @Override
        public RemoteEqualizer createFromParcel(Parcel in) {
            return new RemoteEqualizer(in);
        }

        @Override
        public RemoteEqualizer[] newArray(int size) {
            return new RemoteEqualizer[size];
        }
    };

    public int getNumberOfBands() {
        return mBands.length;
    }

    public int[] getBandLevelRange() {
        return new int[] {mMinBandLevel, mMaxBandLevel};
    }

    public void setBandLevel(int band, int level) {
        mBandLevels[band] = level;
    }

    public int getBandLevel(int band) {
        return mBandLevels[band];
    }

    public int getCenterFreq(int band) {
        return mBands[band].getCenterFrequency();
    }

    public int getCurrentPreset() {
        return mCurPreset;
    }

    public void usePreset(int preset) {
        mCurPreset = preset;
    }

    public int getNumberOfPresets() {
        return mPresets.length;
    }

    public String getPresetName(short preset) {
        return mPresets[preset].getName();
    }

    private short[] intArrayToShortArray(int[] in) {
        if (in == null) {
            return null;
        }

        short[] out = new short[in.length];

        for (int i = 0; i < in.length; i++) {
            out[i] = (short) in[i];
        }

        return out;
    }

    public Equalizer.Settings getProperties() {
        Equalizer.Settings settings = new Equalizer.Settings();

        settings.bandLevels = intArrayToShortArray(mBandLevels);
        settings.curPreset = (short) mCurPreset;
        settings.numBands = (short) getNumberOfBands();

        return settings;
    }

    private int[] shortArrayToIntArray(short[] in) {
        if (in == null) {
            return null;
        }

        int[] out = new int[in.length];

        for (int i = 0; i < in.length; i++) {
            out[i] = in[i];
        }

        return out;
    }

    public void setProperties(Equalizer.Settings settings) {
        mBandLevels = shortArrayToIntArray(settings.bandLevels);
        mCurPreset = settings.curPreset;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedArray(mBands, i);
        parcel.writeTypedArray(mPresets, i);
        parcel.writeIntArray(mBandLevels);
        parcel.writeInt(mMinBandLevel);
        parcel.writeInt(mMaxBandLevel);
        parcel.writeInt(mCurPreset);
    }

    public static class Preset implements Parcelable {

        private String mName;

        public Preset(String name) {
            mName = name;
        }

        protected Preset(Parcel in) {
            mName = in.readString();
        }

        public static final Creator<Preset> CREATOR = new Creator<Preset>() {
            @Override
            public Preset createFromParcel(Parcel in) {
                return new Preset(in);
            }

            @Override
            public Preset[] newArray(int size) {
                return new Preset[size];
            }
        };

        public String getName() {
            return mName;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(mName);
        }
    }

    public static class Band implements Parcelable {

        private int mLowFrequency;
        private int mCenterFrequency;
        private int mHighFrequency;

        public Band(int lowFrequency, int centerFrequency, int highFrequency) {
            mLowFrequency = lowFrequency;
            mCenterFrequency = centerFrequency;
            mHighFrequency = highFrequency;
        }

        protected Band(Parcel in) {
            mLowFrequency = in.readInt();
            mCenterFrequency = in.readInt();
            mHighFrequency = in.readInt();
        }

        public static final Creator<Band> CREATOR = new Creator<Band>() {
            @Override
            public Band createFromParcel(Parcel in) {
                return new Band(in);
            }

            @Override
            public Band[] newArray(int size) {
                return new Band[size];
            }
        };

        public int getLowFrequency() {
            return mLowFrequency;
        }

        public int getCenterFrequency() {
            return mCenterFrequency;
        }

        public int getHighFrequency() {
            return mHighFrequency;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(mLowFrequency);
            parcel.writeInt(mCenterFrequency);
            parcel.writeInt(mHighFrequency);
        }
    }
}
