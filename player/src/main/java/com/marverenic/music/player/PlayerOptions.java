package com.marverenic.music.player;

import android.media.audiofx.Equalizer;
import android.os.Parcel;
import android.os.Parcelable;

public class PlayerOptions implements Parcelable {

    private boolean shuffle;
    private int repeat;
    private boolean equalizerEnabled;
    private String equalizerSettingsString;
    private boolean resumeOnHeadphonesConnected;

    public PlayerOptions(boolean shuffle, int repeat, boolean equalizerEnabled,
                         Equalizer.Settings equalizerSettings,
                         boolean resumeOnHeadphonesConnected) {
        this.shuffle = shuffle;
        this.repeat = repeat;
        this.equalizerEnabled = equalizerEnabled;
        this.equalizerSettingsString = equalizerSettings.toString();
        this.resumeOnHeadphonesConnected = resumeOnHeadphonesConnected;
    }

    protected PlayerOptions(Parcel in) {
        shuffle = in.readByte() != 0;
        repeat = in.readInt();
        equalizerEnabled = in.readByte() != 0;
        equalizerSettingsString = in.readString();
        resumeOnHeadphonesConnected = in.readByte() != 0;
    }

    public boolean isShuffleEnabled() {
        return shuffle;
    }

    public int getRepeatMode() {
        return repeat;
    }

    public boolean isEqualizerEnabled() {
        return equalizerEnabled;
    }

    public Equalizer.Settings getEqualizerSettings() {
        return new Equalizer.Settings(equalizerSettingsString);
    }

    public boolean shouldResumeOnHeadphonesConnected() {
        return resumeOnHeadphonesConnected;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (shuffle ? 1 : 0));
        dest.writeInt(repeat);
        dest.writeByte((byte) (equalizerEnabled ? 1 : 0));
        dest.writeString(equalizerSettingsString);
        dest.writeByte((byte) (resumeOnHeadphonesConnected ? 1 : 0));
    }

    public static final Creator<PlayerOptions> CREATOR = new Creator<PlayerOptions>() {
        @Override
        public PlayerOptions createFromParcel(Parcel in) {
            return new PlayerOptions(in);
        }

        @Override
        public PlayerOptions[] newArray(int size) {
            return new PlayerOptions[size];
        }
    };
}
