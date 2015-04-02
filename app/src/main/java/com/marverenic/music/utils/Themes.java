package com.marverenic.music.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.marverenic.music.LibraryActivity;
import com.marverenic.music.R;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (isLight(context)) return android.R.style.Theme_Material_Light_Dialog_Alert;
            else return android.R.style.Theme_Material_Dialog_Alert;
        }
        else{
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

    // Public Theme Methods
    public static void themeActivity(int layoutId, View contentView, Activity activity) {
        updateColors(activity);
        setApplicationIcon(activity);
        contentView.setBackgroundColor(background);

        switch (layoutId) {
            case R.layout.activity_library:
                themeLibraryActivity(contentView, activity);
                break;
            case R.layout.activity_now_playing:
                themeNowPlayingActivity(contentView, activity);
                break;
            case R.layout.fragment_list:
                themeLibraryPageActivity(contentView, activity);
                break;
            case R.layout.page_editable_list:
                themePlaylistEditor(contentView, activity);
                break;
            case R.layout.about:
                themeAboutActivity(contentView, activity);
                break;
            case android.R.layout.preference_category:
                themePreferenceActivity(contentView, activity);
                break;
        }

        if (contentView.findViewById(R.id.miniplayer) != null) {
            themeMiniplayer((View) contentView.findViewById(R.id.miniplayer).getParent(), activity);
        }
    }

    public static void themeFragment(int layoutId, View contentView, Fragment fragment) {
        updateColors(fragment.getActivity());

        switch (layoutId) {
            case R.layout.fragment_list:
                themeListFragment(contentView, fragment);
                break;
            case R.layout.fragment_grid:
                themeGridFragment(contentView, fragment);
                break;
        }
    }

    // Activity Theme Methods
    private static void themeLibraryActivity(View contentView, Activity activity) {
        contentView.findViewById(R.id.pagerSlidingTabs).setBackgroundColor(primary);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            contentView.findViewById(R.id.pagerSlidingTabs).setElevation(0);
        }
        LayerDrawable backgroundDrawable = (LayerDrawable) activity.getResources().getDrawable(R.drawable.header_frame);
        GradientDrawable bodyDrawable = ((GradientDrawable) backgroundDrawable.findDrawableByLayerId(R.id.body));
        GradientDrawable topDrawable = ((GradientDrawable) backgroundDrawable.findDrawableByLayerId(R.id.top));
        bodyDrawable.setColor(background);
        topDrawable.setColor(primary);
        contentView.findViewById(R.id.pager).setBackground(backgroundDrawable);
    }

    private static void themeNowPlayingActivity(View contentView, Activity activity) {
        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (activity.getActionBar() != null) {
                activity.getActionBar().setBackgroundDrawable(new ColorDrawable(primary));
            } else {
                Debug.log(Debug.LogLevel.WTF, "Themes", "Couldn't find the action bar", activity);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SeekBar seekBar = (SeekBar) contentView.findViewById(R.id.songSeekBar);

            Drawable thumb = seekBar.getThumb();
            thumb.setColorFilter(accent, PorterDuff.Mode.SRC_IN);

            Drawable progress = seekBar.getProgressDrawable();
            progress.setTint(accent);

            seekBar.setThumb(thumb);
            seekBar.setProgressDrawable(progress);
        } else {
            // For whatever reason, the control frame seems to need a reminder as to what color it should be
            contentView.findViewById(R.id.playerControlFrame).setBackgroundColor(activity.getResources().getColor(R.color.player_control_background));
            if (activity.getActionBar() != null) {
                activity.getActionBar().setIcon(new ColorDrawable(activity.getResources().getColor(android.R.color.transparent)));
            } else {
                Debug.log(Debug.LogLevel.WTF, "Themes", "Couldn't find the action bar", activity);
            }
        }
    }

    private static void themeLibraryPageActivity(View contentView, Activity activity) {
        contentView.findViewById(R.id.list).setBackgroundColor(backgroundElevated);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (activity.getActionBar() != null) activity.getActionBar().setElevation(0);
        }

        ListView list = (ListView) contentView.findViewById(R.id.list);
        list.setDividerHeight((int) activity.getResources().getDisplayMetrics().density);

        LayerDrawable backgroundDrawable = (LayerDrawable) activity.getResources().getDrawable(R.drawable.header_frame);
        GradientDrawable bodyDrawable = ((GradientDrawable) backgroundDrawable.findDrawableByLayerId(R.id.body));
        GradientDrawable topDrawable = ((GradientDrawable) backgroundDrawable.findDrawableByLayerId(R.id.top));
        bodyDrawable.setColor(background);
        topDrawable.setColor(primary);
        contentView.findViewById(R.id.list_container).setBackground(backgroundDrawable);
    }

    private static void themePlaylistEditor(View contentView, Activity activity) {
        contentView.findViewById(R.id.list).setBackgroundColor(backgroundElevated);

        ListView list = (ListView) contentView.findViewById(R.id.list);
        list.setDividerHeight((int) activity.getResources().getDisplayMetrics().density);

        LayerDrawable backgroundDrawable = (LayerDrawable) activity.getResources().getDrawable(R.drawable.header_frame);
        GradientDrawable bodyDrawable = ((GradientDrawable) backgroundDrawable.findDrawableByLayerId(R.id.body));
        GradientDrawable topDrawable = ((GradientDrawable) backgroundDrawable.findDrawableByLayerId(R.id.top));
        bodyDrawable.setColor(background);
        topDrawable.setColor(primary);
        ((ViewGroup) contentView.findViewById(R.id.list).getParent()).setBackground(backgroundDrawable);
    }

    private static void themeAboutActivity(View contentView, Activity activity) {

        contentView.findViewById(R.id.aboutScroll).setBackgroundColor(primary);

        int[] primaryText = {R.id.aboutAppName, R.id.lastFmHeader, R.id.aboutAOSPHeader, R.id.aboutAOSPTabsHeader,
                R.id.aboutPicassoHeader, R.id.aboutDSLVHeader, R.id.aboutApolloHeader, R.id.aboutStackOverflowHeader};
        int[] detailText = {R.id.aboutDescription, R.id.aboutLicense, R.id.aboutUsesHeader,
                R.id.aboutVersion, R.id.aboutAOSPDetail, R.id.aboutAOSPTabsDetail, R.id.aboutUILDetail, R.id.aboutDSLVDetail};

        for (int aPrimaryText : primaryText)
            ((TextView) contentView.findViewById(aPrimaryText)).setTextColor(uiText);
        for (int aDetailText : detailText)
            ((TextView) contentView.findViewById(aDetailText)).setTextColor(uiDetailText);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            ((ImageView) contentView.findViewById(R.id.aboutAppIcon)).setImageBitmap(getLargeIcon(activity, DisplayMetrics.DENSITY_XXXHIGH));
        } else {
            ((ImageView) contentView.findViewById(R.id.aboutAppIcon)).setImageBitmap(getLargeIcon(activity, DisplayMetrics.DENSITY_XXHIGH));
        }
    }

    private static void themePreferenceActivity(View contentView, Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (activity.getActionBar() != null)
                activity.getActionBar().setElevation(activity.getResources().getDimension(R.dimen.header_elevation));
            else Debug.log(Debug.LogLevel.WTF, "Themes", "Couldn't find the action bar", activity);
        }
    }

    public static void setApplicationIcon(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(activity.getResources().getString(R.string.app_name), getIcon(activity), primary);
            activity.setTaskDescription(taskDescription);
        }
    }

    // Fragment Theme Methods
    private static void themeListFragment(View contentView, Fragment fragment) {
        contentView.findViewById(R.id.list).setBackgroundColor(backgroundElevated);

        ListView list = (ListView) contentView.findViewById(R.id.list);
        list.setDividerHeight((int) fragment.getResources().getDisplayMetrics().density);
    }

    private static void themeGridFragment(View contentView, Fragment fragment) {
        contentView.findViewById(R.id.albumGrid).setBackgroundColor(backgroundElevated);
    }

    private static void themeMiniplayer(View miniplayer, Context context) {
        miniplayer.setBackgroundColor(backgroundMiniplayer);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((ImageButton) miniplayer.findViewById(R.id.skipButton)).setImageTintList(ColorStateList.valueOf(listText));
            ((ImageButton) miniplayer.findViewById(R.id.playButton)).setImageTintList(ColorStateList.valueOf(listText));
        } else {
            if (!isLight(context)) {
                ((ImageButton) miniplayer.findViewById(R.id.skipButton)).setImageResource(R.drawable.ic_skip_next_miniplayer);
                ((ImageButton) miniplayer.findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_miniplayer);
            } else {
                ((ImageButton) miniplayer.findViewById(R.id.skipButton)).setImageResource(R.drawable.ic_skip_next_miniplayer_light);
                ((ImageButton) miniplayer.findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_miniplayer_light);
            }
        }

        ((TextView) miniplayer.findViewById(R.id.textNowPlayingTitle)).setTextColor(listText);
        ((TextView) miniplayer.findViewById(R.id.textNowPlayingDetail)).setTextColor(detailText);
    }
}
