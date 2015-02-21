package com.marverenic.music.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import com.marverenic.music.LibraryActivity;

import java.util.ArrayList;

public class Navigate {

    private static ArrayList<FrozenActivity> history = new ArrayList<>();

    public static void to(Context parent, Class target) {
        to(parent, target, null, null);
    }

    public static void to(Context parent, Class target, String extraName, Parcelable extra) {
        Intent intent = new Intent(parent, target);
        FrozenActivity activity = new FrozenActivity();

        activity.targetClass = target;

        if (extraName != null && extra != null) {
            intent.putExtra(extraName, extra);
            activity.extraName = extraName;
            activity.extra = extra;
        }

        history.add(activity);
        parent.startActivity(intent);
    }

    public static void up(Activity parent) {
        if (history.size() >= 2) {
            history.remove(history.size() - 1);
            FrozenActivity target = history.get(history.size() - 1);

            Intent intent = new Intent(parent, target.targetClass);
            if (target.extraName != null && target.extra != null) {
                intent.putExtra(target.extraName, target.extra);
            }

            parent.startActivity(intent);
        } else {
            Navigate.home(parent);
        }
        parent.finish();
    }

    public static void back(Activity parent) {
        // Something interesting may eventually happen here
        up(parent);
    }

    public static void home(Context parent) {
        history.clear();
        parent.startActivity(new Intent(parent, LibraryActivity.class));
    }

    private static class FrozenActivity {
        // Store previous activity history in memory
        public Class targetClass;
        public String extraName;
        public Parcelable extra;
    }
}
