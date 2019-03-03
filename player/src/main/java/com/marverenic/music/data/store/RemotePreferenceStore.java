package com.marverenic.music.data.store;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

/**
 * Remote Preferences that need to be maintained by the remote player service. Because
 * {@link android.content.SharedPreferences} does not support multiple processes, this should NOT
 * be opened on the main process.
 *
 * The intent of this PreferenceStore is to allow certain processes to be modifiable in the remote
 * service, and others on the main process. Because of this, this class should have no overlap with
 * {@code SharedPreferenceStore}, because the key value pairs are not kept in sync between the two
 * processes.
 */
public class RemotePreferenceStore {

    private static final String KEY_MULTI_REPEAT_COUNT = "Player.multiRepeat";
    private static final String KEY_SLEEP_TIMER_END = "Player.sleepTimer";

    private SharedPreferences mSharedPreferences;

    public RemotePreferenceStore(Context context) {
        String name = getSharedPreferencesName(context);
        mSharedPreferences = context.getSharedPreferences(name, MODE_PRIVATE);
    }

    private static String getSharedPreferencesName(Context context) {
        return context.getPackageName() + "%remote_preferences";
    }

    public int getMultiRepeatCount() {
        return mSharedPreferences.getInt(KEY_MULTI_REPEAT_COUNT, 0);
    }

    public void setMultiRepeatCount(int count) {
        mSharedPreferences.edit().putInt(KEY_MULTI_REPEAT_COUNT, count).apply();
    }

    public long getSleepTimerEndTime() {
        return mSharedPreferences.getLong(KEY_SLEEP_TIMER_END, 0);
    }

    public void setSleepTimerEndTime(long timestampInMs) {
        mSharedPreferences.edit().putLong(KEY_SLEEP_TIMER_END, timestampInMs).apply();
    }

}
