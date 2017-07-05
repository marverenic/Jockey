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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.app.NightMode;

import com.marverenic.music.R;
import com.marverenic.music.ui.library.LibraryActivity;
import com.marverenic.music.data.annotations.AccentTheme;
import com.marverenic.music.data.annotations.PrimaryTheme;
import com.marverenic.music.player.PlayerService;

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
    public int getPrimaryColor() {
        return ContextCompat.getColor(mContext, getPrimaryColorRes());
    }

    @ColorRes
    private int getPrimaryColorRes() {
        switch (mPreferenceStore.getPrimaryColor()) {
            case PrimaryTheme.GRAY:
                return R.color.primary_grey;
            case PrimaryTheme.RED:
                return R.color.primary_red;
            case PrimaryTheme.ORANGE:
                return R.color.primary_orange;
            case PrimaryTheme.YELLOW:
                return R.color.primary_yellow;
            case PrimaryTheme.GREEN:
                return R.color.primary_green;
            case PrimaryTheme.CYAN:
                return R.color.primary;
            case PrimaryTheme.BLUE:
                return R.color.primary_blue;
            case PrimaryTheme.PURPLE:
                return R.color.primary_purple;
            case PrimaryTheme.BLACK:
                return R.color.primary_black;
            default:
                return R.color.primary;
        }
    }

    @Override
    public int getAccentColor() {
        return ContextCompat.getColor(mContext, getAccentColorRes());
    }

    @ColorRes
    private int getAccentColorRes() {
        switch (mPreferenceStore.getAccentColor()) {
            case AccentTheme.GRAY:
                return R.color.accent_grey;
            case AccentTheme.RED:
                return R.color.accent_red;
            case AccentTheme.ORANGE:
                return R.color.accent_orange;
            case AccentTheme.YELLOW:
                return R.color.accent_yellow;
            case AccentTheme.GREEN:
                return R.color.accent_green;
            case AccentTheme.CYAN:
                return R.color.accent;
            case AccentTheme.BLUE:
                return R.color.accent_blue;
            case AccentTheme.PURPLE:
                return R.color.accent_purple;
            case AccentTheme.TEAL:
                return R.color.accent_black;
            default:
                return R.color.accent;
        }
    }

    @Override
    public void setTheme(AppCompatActivity activity) {
        applyNightMode(activity);
        activity.setTheme(R.style.AppTheme);
        activity.getTheme().applyStyle(getPrimaryThemeId(), true);
        activity.getTheme().applyStyle(getAccentThemeId(), true);

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

    @StyleRes
    private int getPrimaryThemeId() {
        switch (mPreferenceStore.getPrimaryColor()) {
            case PrimaryTheme.GRAY:
                return R.style.Primary_Grey;
            case PrimaryTheme.RED:
                return R.style.Primary_Red;
            case PrimaryTheme.ORANGE:
                return R.style.Primary_Orange;
            case PrimaryTheme.YELLOW:
                return R.style.Primary_Yellow;
            case PrimaryTheme.GREEN:
                return R.style.Primary_Green;
            case PrimaryTheme.CYAN:
                return R.style.Primary_Cyan;
            case PrimaryTheme.BLUE:
                return R.style.Primary_Blue;
            case PrimaryTheme.PURPLE:
                return R.style.Primary_Purple;
            case PrimaryTheme.BLACK:
                return R.style.Primary_Black;
            default:
                return R.style.Primary_Cyan;
        }
    }

    @StyleRes
    private int getAccentThemeId() {
        switch (mPreferenceStore.getAccentColor()) {
            case AccentTheme.GRAY:
                return R.style.Accent_Grey;
            case AccentTheme.RED:
                return R.style.Accent_Red;
            case AccentTheme.ORANGE:
                return R.style.Accent_Orange;
            case AccentTheme.YELLOW:
                return R.style.Accent_Yellow;
            case AccentTheme.GREEN:
                return R.style.Accent_Green;
            case AccentTheme.CYAN:
                return R.style.Accent_Cyan;
            case AccentTheme.BLUE:
                return R.style.Accent_Blue;
            case AccentTheme.PURPLE:
                return R.style.Accent_Purple;
            case AccentTheme.TEAL:
                return R.style.Accent_Black;
            default:
                return R.style.Accent_Cyan;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void applyTaskDescription(Activity activity) {
        String taskName = mContext.getString(R.string.app_name);
        int taskColor = getPrimaryColor();
        Bitmap taskIcon = getAppIcon();

        TaskDescription taskDescription = new TaskDescription(taskName, taskIcon, taskColor);
        activity.setTaskDescription(taskDescription);
    }

    private Bitmap getAppIcon() {
        return BitmapFactory.decodeResource(mContext.getResources(), getIconId());
    }

    @Override
    public Bitmap getLargeAppIcon() {
        Drawable icon = ResourcesCompat.getDrawableForDensity(mContext.getResources(), getIconId(),
                getLargerDisplayDensity(), mContext.getTheme());

        if (icon instanceof BitmapDrawable) {
            return ((BitmapDrawable) icon).getBitmap();
        } else {
            return null;
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
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
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
