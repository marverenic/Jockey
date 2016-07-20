package com.marverenic.music.utils;

import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.marverenic.music.R;

public final class Navigate {

    /**
     * This class is never instantiated
     */
    private Navigate() {

    }

    public static void to(FragmentActivity activity, Fragment fragment, @IdRes int viewId) {
        activity.getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(viewId, fragment)
                .addToBackStack(null)
                .commit();
    }
}
