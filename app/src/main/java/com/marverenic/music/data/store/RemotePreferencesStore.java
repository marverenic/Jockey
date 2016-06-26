package com.marverenic.music.data.store;

import android.media.audiofx.Equalizer;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

public class RemotePreferencesStore implements ReadOnlyPreferencesStore, Parcelable {

    private static final String TAG = "RemotePreferencesStore";

    private final boolean mShowFirstStart;
    private final boolean mAllowLogging;
    private final boolean mUseMobileNetwork;
    private final boolean mOpenNowPlayingOnNewQueue;
    private final boolean mEnableNowPlayingGestures;
    private final int mDefaultPage;
    private final int mPrimaryColor;
    private final int mBaseColor;
    private final boolean mShuffled;
    private final int mRepeatMode;
    private final int mEqualizerPresetId;
    private final boolean mEqualizerEnabled;
    private final String mEqualizerSettings;

    public RemotePreferencesStore(ReadOnlyPreferencesStore preferencesStore) {
        mShowFirstStart = preferencesStore.showFirstStart();
        mAllowLogging = preferencesStore.allowLogging();
        mUseMobileNetwork = preferencesStore.useMobileNetwork();
        mOpenNowPlayingOnNewQueue = preferencesStore.openNowPlayingOnNewQueue();
        mEnableNowPlayingGestures = preferencesStore.enableNowPlayingGestures();
        mDefaultPage = preferencesStore.getDefaultPage();
        mPrimaryColor = preferencesStore.getPrimaryColor();
        mBaseColor = preferencesStore.getBaseColor();
        mShuffled = preferencesStore.isShuffled();
        mRepeatMode = preferencesStore.getRepeatMode();
        mEqualizerPresetId = preferencesStore.getEqualizerPresetId();
        mEqualizerEnabled = preferencesStore.getEqualizerEnabled();

        Equalizer.Settings eqSettings = preferencesStore.getEqualizerSettings();
        if (eqSettings != null) {
            mEqualizerSettings = eqSettings.toString();
        } else {
            mEqualizerSettings = null;
        }
    }

    protected RemotePreferencesStore(Parcel in) {
        mShowFirstStart = in.readByte() != 0;
        mAllowLogging = in.readByte() != 0;
        mUseMobileNetwork = in.readByte() != 0;
        mOpenNowPlayingOnNewQueue = in.readByte() != 0;
        mEnableNowPlayingGestures = in.readByte() != 0;
        mDefaultPage = in.readInt();
        mPrimaryColor = in.readInt();
        mBaseColor = in.readInt();
        mShuffled = in.readByte() != 0;
        mRepeatMode = in.readInt();
        mEqualizerPresetId = in.readInt();
        mEqualizerEnabled = in.readByte() != 0;
        mEqualizerSettings = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (mShowFirstStart ? 1 : 0));
        dest.writeByte((byte) (mAllowLogging ? 1 : 0));
        dest.writeByte((byte) (mUseMobileNetwork ? 1 : 0));
        dest.writeByte((byte) (mOpenNowPlayingOnNewQueue ? 1 : 0));
        dest.writeByte((byte) (mEnableNowPlayingGestures ? 1 : 0));
        dest.writeInt(mDefaultPage);
        dest.writeInt(mPrimaryColor);
        dest.writeInt(mBaseColor);
        dest.writeByte((byte) (mShuffled ? 1 : 0));
        dest.writeInt(mRepeatMode);
        dest.writeInt(mEqualizerPresetId);
        dest.writeByte((byte) (mEqualizerEnabled ? 1 : 0));
        dest.writeString(mEqualizerSettings);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RemotePreferencesStore> CREATOR = new Creator<RemotePreferencesStore>() {
        @Override
        public RemotePreferencesStore createFromParcel(Parcel in) {
            return new RemotePreferencesStore(in);
        }

        @Override
        public RemotePreferencesStore[] newArray(int size) {
            return new RemotePreferencesStore[size];
        }
    };

    @Override
    public boolean showFirstStart() {
        return mShowFirstStart;
    }

    @Override
    public boolean allowLogging() {
        return mAllowLogging;
    }

    @Override
    public boolean useMobileNetwork() {
        return mUseMobileNetwork;
    }

    @Override
    public boolean openNowPlayingOnNewQueue() {
        return mOpenNowPlayingOnNewQueue;
    }

    @Override
    public boolean enableNowPlayingGestures() {
        return mEnableNowPlayingGestures;
    }

    @Override
    public int getDefaultPage() {
        return mDefaultPage;
    }

    @Override
    public int getPrimaryColor() {
        return mPrimaryColor;
    }

    @Override
    public int getBaseColor() {
        return mBaseColor;
    }

    @Override
    public boolean isShuffled() {
        return mShuffled;
    }

    @Override
    public int getRepeatMode() {
        return mRepeatMode;
    }

    @Override
    public int getEqualizerPresetId() {
        return mEqualizerPresetId;
    }

    @Override
    public boolean getEqualizerEnabled() {
        return mEqualizerEnabled;
    }

    @Override
    public Equalizer.Settings getEqualizerSettings() {
        if (mEqualizerSettings != null) {
            try {
                return new Equalizer.Settings(mEqualizerSettings);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "getEqualizerSettings: failed to parse equalizer settings", e);
                Crashlytics.logException(e);
            }
        }
        return null;
    }
}
