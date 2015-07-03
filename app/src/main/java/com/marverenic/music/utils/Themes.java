package com.marverenic.music.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.StyleRes;
import android.util.DisplayMetrics;

import com.marverenic.music.R;
import com.marverenic.music.activity.LibraryActivity;

public class Themes {

    private static int primary;
    private static int primaryDark;
    private static int accent;

    private static int listText;
    private static int detailText;
    private static int background;
    private static int backgroundElevated;
    private static int backgroundMiniplayer;

    // Get Methods
    public static int getPrimary() {
        return primary;
    }

    public static int getPrimaryDark() {
        return primaryDark;
    }

    public static int getAccent() {
        return accent;
    }

    @Deprecated
    public static int getListText() {
        return listText;
    }

    @Deprecated
    public static int getDetailText() {
        return detailText;
    }

    public static int getBackground() {
        return background;
    }

    public static int getBackgroundElevated(){
        return backgroundElevated;
    }

    public static int getBackgroundMiniplayer() {
        return backgroundMiniplayer;
    }

    public static boolean isLight(Context context) {
        return background == context.getResources().getColor(R.color.background);
    }

    // Update method
    public static void updateColors(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Resources resources = context.getResources();

        switch (Integer.parseInt(prefs.getString("prefColorPrimary", "5"))) {
            case 0: //Black
                primary = resources.getColor(R.color.primary_grey);
                primaryDark = resources.getColor(R.color.primary_dark_grey);
                accent = resources.getColor(R.color.accent_grey);
                break;
            case 1: //Red
                primary = resources.getColor(R.color.primary_red);
                primaryDark = resources.getColor(R.color.primary_dark_red);
                accent = resources.getColor(R.color.accent_red);
                break;
            case 2: //Orange
                primary = resources.getColor(R.color.primary_orange);
                primaryDark = resources.getColor(R.color.primary_dark_orange);
                accent = resources.getColor(R.color.accent_orange);
                break;
            case 3: //Yellow
                primary = resources.getColor(R.color.primary_yellow);
                primaryDark = resources.getColor(R.color.primary_dark_yellow);
                accent = resources.getColor(R.color.accent_yellow);
                break;
            case 4: //Green
                primary = resources.getColor(R.color.primary_green);
                primaryDark = resources.getColor(R.color.primary_dark_green);
                accent = resources.getColor(R.color.accent_green);
                break;
            case 6: //Purple
                primary = resources.getColor(R.color.primary_purple);
                primaryDark = resources.getColor(R.color.primary_dark_purple);
                accent = resources.getColor(R.color.accent_purple);
                break;
            default: //Blue & Unknown
                primary = resources.getColor(R.color.primary);
                primaryDark = resources.getColor(R.color.primary_dark);
                accent = resources.getColor(R.color.accent);
                break;
        }

        switch (Integer.parseInt(prefs.getString("prefBaseTheme", "1"))) {
            case 1: // Material Light
                listText = resources.getColor(R.color.list_text);
                detailText = resources.getColor(R.color.detail_text);
                background = resources.getColor(R.color.background);
                backgroundElevated = resources.getColor(R.color.background_elevated);
                backgroundMiniplayer = resources.getColor(R.color.background_miniplayer);
                break;
            default: // Material Dark
                listText = resources.getColor(R.color.list_text_dark);
                detailText = resources.getColor(R.color.detail_text_dark);
                background = resources.getColor(R.color.background_dark);
                backgroundElevated = resources.getColor(R.color.background_elevated_dark);
                backgroundMiniplayer = resources.getColor(R.color.background_miniplayer_dark);
                break;
        }
    }

    public static @StyleRes int getTheme(Context context) {
        int base = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("prefBaseTheme", "1"));

        int primary = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("prefColorPrimary", "5"));
        if (base == 1) {
            // Light Base
            switch (primary) {
                case 0: // Black
                    return R.style.AppThemeLight_Black;
                case 1: // Red
                    return R.style.AppThemeLight_Red;
                case 2: // Orange
                    return R.style.AppThemeLight_Orange;
                case 3: // Yellow
                    return R.style.AppThemeLight_Yellow;
                case 4: // Green
                    return R.style.AppThemeLight_Green;
                case 6: // Purple
                    return R.style.AppThemeLight_Purple;
                default: // Blue or Unknown
                    return R.style.AppThemeLight_Blue;
            }
        } else {
            // Dark or Unknown Base
            switch (primary) {
                case 0: // Black
                    return R.style.AppTheme_Black;
                case 1: // Red
                    return R.style.AppTheme_Red;
                case 2: // Orange
                    return R.style.AppTheme_Orange;
                case 3: // Yellow
                    return R.style.AppTheme_Yellow;
                case 4: // Green
                    return R.style.AppTheme_Green;
                case 6: // Purple
                    return R.style.AppTheme_Purple;
                default: // Blue or Unknown
                    return R.style.AppTheme_Blue;
            }
        }
    }

    public static void setTheme(Activity activity) {
        updateColors(activity);

        activity.setTheme(getTheme(activity));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(activity.getResources().getString(R.string.app_name), getIcon(activity), primary);
            activity.setTaskDescription(taskDescription);
        } else {
            if (activity.getActionBar() != null) {
                activity.getActionBar().setBackgroundDrawable(new ColorDrawable(primary));
                if (!activity.getClass().equals(LibraryActivity.class)) {
                    activity.getActionBar().setIcon(new ColorDrawable(activity.getResources().getColor(android.R.color.transparent)));
                } else {
                    activity.getActionBar().setIcon(getIconId(activity));
                }
            }
        }
    }

    public static Bitmap getIcon(Context context) {
        switch (Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("prefColorPrimary", "5"))) {
            case 0:
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_grey);
            case 1:
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_red);
            case 2:
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_orange);
            case 3:
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_yellow);
            case 4:
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_green);
            case 6:
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_purple);
            default:
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        }
    }

    public static Bitmap getLargeIcon(Context context, int density) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

            // Use a density 1 level higher than the display in case the launcher uses large icons
            if (density <= 0) {
                switch (context.getResources().getDisplayMetrics().densityDpi) {
                    case DisplayMetrics.DENSITY_LOW:
                        density = DisplayMetrics.DENSITY_MEDIUM;
                        break;
                    case DisplayMetrics.DENSITY_MEDIUM:
                        density = DisplayMetrics.DENSITY_HIGH;
                        break;
                    case DisplayMetrics.DENSITY_HIGH:
                        density = DisplayMetrics.DENSITY_XHIGH;
                        break;
                    case DisplayMetrics.DENSITY_XHIGH:
                        density = DisplayMetrics.DENSITY_XXHIGH;
                        break;
                    default:
                        density = DisplayMetrics.DENSITY_XXXHIGH;
                }
            }

            @SuppressWarnings("deprecation")
            BitmapDrawable icon = (BitmapDrawable) context.getResources().getDrawableForDensity(getIconId(context), density);
            if (icon != null) return icon.getBitmap();

        }
        return getIcon(context);
    }

    public static @DrawableRes int getIconId(Context context) {
        switch (Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("prefColorPrimary", "5"))) {
            case 0:
                return R.drawable.ic_launcher_grey;
            case 1:
                return R.drawable.ic_launcher_red;
            case 2:
                return R.drawable.ic_launcher_orange;
            case 3:
                return R.drawable.ic_launcher_yellow;
            case 4:
                return R.drawable.ic_launcher_green;
            case 6:
                return R.drawable.ic_launcher_purple;
            default:
                return R.drawable.ic_launcher;
        }
    }

    public static void updateLauncherIcon(Context context) {
        Intent launcherIntent = new Intent(context, LibraryActivity.class);

        // Uncomment to delete Jockey icons from the launcher
        // Don't forget to add permission "com.android.launcher.permission.UNINSTALL_SHORTCUT" to AndroidManifest
        /*
        Intent delIntent = new Intent();
        delIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launcherIntent);
        delIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getResources().getString(R.string.app_name));
        delIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
        context.sendBroadcast(delIntent);
        */

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launcherIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getResources().getString(R.string.app_name));
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, getLargeIcon(context, -1));
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        context.sendBroadcast(addIntent);
    }

    public static void setApplicationIcon(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(activity.getResources().getString(R.string.app_name), getIcon(activity), primary);
            activity.setTaskDescription(taskDescription);
        }
    }
}
