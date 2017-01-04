package com.marverenic.music.data.store;

import android.content.Context;
import android.content.SharedPreferences;

import com.marverenic.music.R;

import static android.content.Context.MODE_PRIVATE;

/**
 * Remote Preferences that need to be maintained by the remote player service. Because
 * {@link android.content.SharedPreferences} does not support multiple processes, this should NOT
 * be opened on the main process.
 *
 * The intent of this PreferenceStore is to allow certain processes to be modifiable in the remote
 * service, and others on the main process. Because of this, this class should have no overlap with
 * {@link SharedPreferenceStore}, because the key value pairs are not kept in sync between the two
 * processes.
 */
public class RemotePreferenceStore {

    private Context mContext;
    private SharedPreferences mSharedPreferences;

    public RemotePreferenceStore(Context context) {
        mContext = context;

        String name = getSharedPreferencesName(context);
        mSharedPreferences = context.getSharedPreferences(name, MODE_PRIVATE);
    }

    private static String getSharedPreferencesName(Context context) {
        return context.getPackageName() + "%remote_preferences";
    }

    public int getMultiRepeatCount() {
        String key = mContext.getString(R.string.pref_key_multi_repeat);
        return mSharedPreferences.getInt(key, 0);
    }

    public void setMultiRepeatCount(int count) {
        String key = mContext.getString(R.string.pref_key_multi_repeat);
        mSharedPreferences.edit().putInt(key, count).apply();
    }

    public long getSleepTimerEndTime() {
        String key = mContext.getString(R.string.pref_key_sleep_timer);
        return mSharedPreferences.getLong(key, 0);
    }

    public void setSleepTimerEndTime(long timestampInMs) {
        String key = mContext.getString(R.string.pref_key_sleep_timer);
        mSharedPreferences.edit().putLong(key, timestampInMs).apply();
    }

}
