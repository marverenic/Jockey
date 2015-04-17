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
import android.util.DisplayMetrics;

import com.marverenic.music.R;
import com.marverenic.music.activity.LibraryActivity;

public class Themes {

    private static int primary;
    private static int primaryDark;
    private static int accent;
    private static int uiText;
    private static int uiDetailText;

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

    public static int getUiText() {
        return uiText;
    }

    public static int getUiDetailText() {
        return uiDetailText;
    }

    public static int getListText() {
        return listText;
    }

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

    public static boolean hasChanged(Context context) {
        final int oldPrimary = primary;
        final int oldBackground = background;

        updateColors(context);

        return oldPrimary != primary || oldBackground != background;
    }

    public static boolean isLight(Context context) {
        return background == context.getResources().getColor(R.color.background_light);
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
                uiText = resources.getColor(R.color.ui_text_grey);
                uiDetailText = resources.getColor(R.color.ui_detail_text_grey);
                break;
            case 1: //Red
                primary = resources.getColor(R.color.primary_red);
                primaryDark = resources.getColor(R.color.primary_dark_red);
                accent = resources.getColor(R.color.accent_red);
                uiText = resources.getColor(R.color.ui_text_red);
                uiDetailText = resources.getColor(R.color.ui_detail_text_red);
                break;
            case 2: //Orange
                primary = resources.getColor(R.color.primary_orange);
                primaryDark = resources.getColor(R.color.primary_dark_orange);
                accent = resources.getColor(R.color.accent_orange);
                uiText = resources.getColor(R.color.ui_text_orange);
                uiDetailText = resources.getColor(R.color.ui_detail_text_orange);
                break;
            case 3: //Yellow
                primary = resources.getColor(R.color.primary_yellow);
                primaryDark = resources.getColor(R.color.primary_dark_yellow);
                accent = resources.getColor(R.color.accent_yellow);
                uiText = resources.getColor(R.color.ui_text_yellow);
                uiDetailText = resources.getColor(R.color.ui_detail_text_yellow);
                break;
            case 4: //Green
                primary = resources.getColor(R.color.primary_green);
                primaryDark = resources.getColor(R.color.primary_dark_green);
                accent = resources.getColor(R.color.accent_green);
                uiText = resources.getColor(R.color.ui_text_green);
                uiDetailText = resources.getColor(R.color.ui_detail_text_green);
                break;
            case 6: //Purple
                primary = resources.getColor(R.color.primary_purple);
                primaryDark = resources.getColor(R.color.primary_dark_purple);
                accent = resources.getColor(R.color.accent_purple);
                uiText = resources.getColor(R.color.ui_text_purple);
                uiDetailText = resources.getColor(R.color.ui_detail_text_purple);
                break;
            default: //Blue & Unknown
                primary = resources.getColor(R.color.primary);
                primaryDark = resources.getColor(R.color.primary_dark);
                accent = resources.getColor(R.color.accent);
                uiText = resources.getColor(R.color.ui_text);
                uiDetailText = resources.getColor(R.color.ui_detail_text);
                break;
        }

        switch (Integer.parseInt(prefs.getString("prefBaseTheme", "1"))) {
            case 1: // Material Light
                listText = resources.getColor(R.color.list_text_light);
                detailText = resources.getColor(R.color.detail_text_light);
                background = resources.getColor(R.color.background_light);
                backgroundElevated = resources.getColor(R.color.background_elevated_light);
                backgroundMiniplayer = resources.getColor(R.color.background_miniplayer_light);
                break;
            default: // Material Dark
                listText = resources.getColor(R.color.list_text);
                detailText = resources.getColor(R.color.detail_text);
                background = resources.getColor(R.color.background);
                backgroundElevated = resources.getColor(R.color.background_elevated);
                backgroundMiniplayer = resources.getColor(R.color.background_miniplayer);
                break;
        }
    }

    public static int getTheme(Context context) {
        int base = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("prefBaseTheme", "1"));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            if (base == 1){
                return R.style.AppThemeLight;
            }
            else{
                return R.style.AppTheme;
            }
        }

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
                    return R.style.AppThemeLight;
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
                    return R.style.AppTheme;
            }
        }
    }

    public static int getAlertTheme (Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            if (isLight(context)) return android.R.style.Theme_Material_Light_Dialog_Alert;
            else return android.R.style.Theme_Material_Dialog_Alert;
        }
        else {
            if (isLight(context)) return android.R.style.Theme_Holo_Light;
            else return android.R.style.Theme_Holo;
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

            switch (Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("prefColorPrimary", "5"))) {
                case 0:
                    return ((BitmapDrawable) context.getResources().getDrawableForDensity(R.drawable.ic_launcher_grey, density)).getBitmap();
                case 1:
                    return ((BitmapDrawable) context.getResources().getDrawableForDensity(R.drawable.ic_launcher_red, density)).getBitmap();
                case 2:
                    return ((BitmapDrawable) context.getResources().getDrawableForDensity(R.drawable.ic_launcher_orange, density)).getBitmap();
                case 3:
                    return ((BitmapDrawable) context.getResources().getDrawableForDensity(R.drawable.ic_launcher_yellow, density)).getBitmap();
                case 4:
                    return ((BitmapDrawable) context.getResources().getDrawableForDensity(R.drawable.ic_launcher_green, density)).getBitmap();
                case 6:
                    return ((BitmapDrawable) context.getResources().getDrawableForDensity(R.drawable.ic_launcher_purple, density)).getBitmap();
                default:
                    return ((BitmapDrawable) context.getResources().getDrawableForDensity(R.drawable.ic_launcher, density)).getBitmap();
            }
        } else {
            return getIcon(context);
        }
    }

    public static int getIconId(Context context) {
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

        /*Intent delIntent = new Intent();
        delIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launcherIntent);
        delIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getResources().getString(R.string.app_name));
        delIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");*/

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launcherIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getResources().getString(R.string.app_name));
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, getLargeIcon(context, -1));
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");

        //context.sendBroadcast(delIntent);
        context.sendBroadcast(addIntent);
    }

    public static void setApplicationIcon(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(activity.getResources().getString(R.string.app_name), getIcon(activity), primary);
            activity.setTaskDescription(taskDescription);
        }
    }
}
