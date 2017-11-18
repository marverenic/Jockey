package com.marverenic.music.data.store;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager.TaskDescription;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.app.NightMode;

import com.marverenic.colors.AccentColor;
import com.marverenic.colors.PrimaryColor;
import com.marverenic.music.R;
import com.marverenic.music.data.annotations.AccentTheme;
import com.marverenic.music.data.annotations.PrimaryTheme;
import com.marverenic.music.player.PlayerService;
import com.marverenic.music.ui.library.LibraryActivity;

import static android.util.DisplayMetrics.DENSITY_HIGH;
import static android.util.DisplayMetrics.DENSITY_LOW;
import static android.util.DisplayMetrics.DENSITY_MEDIUM;
import static android.util.DisplayMetrics.DENSITY_XHIGH;
import static android.util.DisplayMetrics.DENSITY_XXHIGH;
import static android.util.DisplayMetrics.DENSITY_XXXHIGH;
import static com.marverenic.music.data.annotations.BaseTheme.AUTO;
import static com.marverenic.music.data.annotations.BaseTheme.DARK;
import static com.marverenic.music.data.annotations.BaseTheme.LIGHT;

public class PresetThemeStore implements ThemeStore {

    private Context mContext;
    private PreferenceStore mPreferenceStore;

    public PresetThemeStore(Context context, PreferenceStore preferenceStore) {
        mContext = context;
        mPreferenceStore = preferenceStore;
    }

    @Override
    public PrimaryColor getPrimaryColor() {
        switch (mPreferenceStore.getPrimaryColor()) {
            case PrimaryTheme.GRAY:
                return null;
            case PrimaryTheme.RED:
                return PrimaryColor.RED_700;
            case PrimaryTheme.ORANGE:
                return PrimaryColor.ORANGE_700;
            case PrimaryTheme.YELLOW:
                return PrimaryColor.YELLOW_700;
            case PrimaryTheme.GREEN:
                return PrimaryColor.GREEN_700;
            case PrimaryTheme.BLUE:
                return PrimaryColor.BLUE_700;
            case PrimaryTheme.PURPLE:
                return PrimaryColor.PURPLE_700;
            case PrimaryTheme.BLACK:
                return null;
            case PrimaryTheme.CYAN:
            default:
                return PrimaryColor.CYAN_700;
        }
    }

    @Override
    public AccentColor getAccentColor() {
        switch (mPreferenceStore.getAccentColor()) {
            case AccentTheme.GRAY:
                return null;
            case AccentTheme.RED:
                return AccentColor.RED_A400;
            case AccentTheme.ORANGE:
                return AccentColor.ORANGE_A400;
            case AccentTheme.YELLOW:
                return AccentColor.YELLOW_A400;
            case AccentTheme.GREEN:
                return AccentColor.GREEN_A400;
            case AccentTheme.BLUE:
                return AccentColor.BLUE_A400;
            case AccentTheme.PURPLE:
                return AccentColor.PURPLE_A400;
            case AccentTheme.TEAL:
                return AccentColor.TEAL_A400;
            case AccentTheme.CYAN:
            default:
                return AccentColor.CYAN_A400;
        }
    }

    @Override
    public void setTheme(AppCompatActivity activity) {
        activity.setTheme(R.style.AppTheme);
        applyNightMode(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            applyTaskDescription(activity);
        }
    }

    private void applyNightMode(AppCompatActivity activity) {
        AppCompatDelegate.setDefaultNightMode(getNightMode());
        activity.getDelegate().applyDayNight();
    }

    @NightMode
    public int getNightMode() {
        switch (mPreferenceStore.getBaseColor()) {
            case AUTO:
                return AppCompatDelegate.MODE_NIGHT_AUTO;
            case DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            case LIGHT:
            default:
                return AppCompatDelegate.MODE_NIGHT_NO;
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void applyTaskDescription(Activity activity) {
        String taskName = mContext.getString(R.string.app_name);
        int taskColor = ContextCompat.getColor(activity, getPrimaryColor().getPrimaryColorRes());
        Bitmap taskIcon = getAppIcon();

        TaskDescription taskDescription = new TaskDescription(taskName, taskIcon, taskColor);
        activity.setTaskDescription(taskDescription);
    }

    private Bitmap getAppIcon() {
        return BitmapFactory.decodeResource(mContext.getResources(), getIconId());
    }

    @Override
    public Drawable getLargeAppIcon() {
        Drawable icon = ResourcesCompat.getDrawableForDensity(mContext.getResources(), getIconId(),
                getLargerDisplayDensity(), mContext.getTheme());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon instanceof AdaptiveIconDrawable) {
            return ((AdaptiveIconDrawable) icon).getForeground();
        } else {
            return icon;
        }
    }

    private int getLargerDisplayDensity() {
        int screenDensity = mContext.getResources().getDisplayMetrics().densityDpi;

        if (screenDensity == DENSITY_LOW) {
            return DENSITY_MEDIUM;
        } else if (screenDensity == DENSITY_MEDIUM) {
            return DENSITY_HIGH;
        } else if (screenDensity == DENSITY_HIGH) {
            return DENSITY_XHIGH;
        } else if (screenDensity == DENSITY_XHIGH) {
            return DENSITY_XXHIGH;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return DENSITY_XXHIGH;
        } else {
            return DENSITY_XXXHIGH;
        }
    }

    @DrawableRes
    private int getIconId() {
        switch (mPreferenceStore.getPrimaryColor()) {
            case PrimaryTheme.GRAY:
                return R.mipmap.ic_launcher_grey;
            case PrimaryTheme.RED:
                return R.mipmap.ic_launcher_red;
            case PrimaryTheme.ORANGE:
                return R.mipmap.ic_launcher_orange;
            case PrimaryTheme.YELLOW:
                return R.mipmap.ic_launcher_yellow;
            case PrimaryTheme.GREEN:
                return R.mipmap.ic_launcher_green;
            case PrimaryTheme.CYAN:
                return R.mipmap.ic_launcher;
            case PrimaryTheme.BLUE:
                return R.mipmap.ic_launcher_blue;
            case PrimaryTheme.PURPLE:
                return R.mipmap.ic_launcher_purple;
            case PrimaryTheme.BLACK:
                return R.mipmap.ic_launcher_black;
            default:
                return R.mipmap.ic_launcher;
        }
    }

    @Override
    public void createThemedLauncherIcon() {
        String[] activityThemeSuffixes = {
                "$Grey",
                "$Red",
                "$Orange",
                "$Yellow",
                "$Green",
                "", // The cyan theme does not have an Activity name suffix
                "$Purple",
                "$Black",
                "$Blue"
        };

        String launchActivityName = "com.marverenic.music.activity.LibraryActivity";
        int nextIcon = mPreferenceStore.getPrimaryColor();
        int currIcon = mPreferenceStore.getIconColor();

        if (nextIcon == currIcon) {
            return;
        }

        mPreferenceStore.setIconColor(nextIcon);
        mPreferenceStore.commit();

        restartApplication();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // On Nougat and newer versions of Android, PackageManager will automatically restart
            // Jockey. On older versions, it will just kill Jockey after disabling the current
            // launcher component. Therefore, we schedule an alarm 100 ms in the future to restart
            // Jockey.
            scheduleRestart();
        }

        setComponentEnabled(launchActivityName + activityThemeSuffixes[nextIcon], true);
        setComponentEnabled(launchActivityName + activityThemeSuffixes[currIcon], false);
    }

    private void scheduleRestart() {
        PendingIntent intent = PendingIntent.getActivity(mContext, 0,
                new Intent(mContext, LibraryActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        long restartTime = SystemClock.elapsedRealtime() + 100;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            mgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, restartTime, intent);
        } else {
            mgr.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, restartTime, intent);
        }
    }

    private void restartApplication() {
        mContext.stopService(new Intent(mContext, PlayerService.class));

        Intent restartIntent = new Intent(mContext, LibraryActivity.class);
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(restartIntent);
    }

    private void setComponentEnabled(String fullyQualifiedName, boolean enabled) {
        mContext.getPackageManager().setComponentEnabledSetting(
                new ComponentName(mContext.getPackageName(), fullyQualifiedName),
                (enabled)
                        ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                (enabled) ? PackageManager.DONT_KILL_APP : 0);
    }
}
