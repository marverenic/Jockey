package com.marverenic.music.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.marverenic.music.R;
import com.marverenic.music.activity.LibraryActivity;

public final class Navigate {

    /**
     * This class is never instantiated
     */
    private Navigate() {

    }

    public static void to(Context parent, Class<?  extends Activity> target) {
        to(parent, target, null, null);
    }

    public static void to(Context parent, Class<? extends Activity> target, String extraName,
                          Parcelable extra) {
        Intent intent = new Intent(parent, target);

        if (extraName != null && extra != null) {
            intent.putExtra(extraName, extra);
        }

        parent.startActivity(intent);
    }

    public static void to(FragmentActivity activity, Fragment fragment, @IdRes int viewId) {
        activity.getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(viewId, fragment)
                .addToBackStack(null)
                .commit();
    }

    public static void up(Activity parent) {
        parent.finish();
    }

    public static void back(Activity parent) {
        parent.finish();
    }

    public static void home(Context parent) {
        parent.startActivity(new Intent(parent, LibraryActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }
}
