package com.marverenic.music.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import com.marverenic.music.LibraryActivity;
import com.marverenic.music.NowPlayingActivity;

import java.util.ArrayList;

public class Navigate {

    private static ArrayList<Intent> history = new ArrayList<>();

    public static void to(Context parent, Class target) {
        to(parent, target, null, null);
    }

    public static void to(Context parent, Class target, String extraName, Parcelable extra) {
        Intent intent = new Intent(parent, target);

        if (extraName != null && extra != null) {
            intent.putExtra(extraName, extra);
        }
        if (!target.equals(NowPlayingActivity.class)) history.add(intent);

        parent.startActivity(intent);
    }

    public static void up(Activity parent) {
        if (history.size() > 1) {
            history.remove(history.size() - 1);
            parent.finish();
        }
        else{
            home(parent);
        }
    }

    public static void back(Activity parent) {
        // Something interesting may happen here eventually
        if (history.size() > 0) history.remove(history.size() - 1);
        parent.finish();
    }

    public static void home(Context parent) {
        history.clear();
        parent.startActivity(new Intent(parent, LibraryActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).setAction(LibraryActivity.ACTION_LIBRARY));
    }
}
