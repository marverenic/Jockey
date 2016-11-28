package com.marverenic.music.data.store;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager.TaskDescription;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.app.NightMode;

import com.marverenic.music.R;

import static android.util.DisplayMetrics.DENSITY_HIGH;
import static android.util.DisplayMetrics.DENSITY_LOW;
import static android.util.DisplayMetrics.DENSITY_MEDIUM;
import static android.util.DisplayMetrics.DENSITY_XHIGH;
import static android.util.DisplayMetrics.DENSITY_XXHIGH;
import static android.util.DisplayMetrics.DENSITY_XXXHIGH;

import static com.marverenic.music.data.annotations.BaseTheme.*;
import static com.marverenic.music.data.annotations.PresetTheme.*;

public class PresetThemeStore implements ThemeStore {

    private Context mContext;
    private PreferencesStore mPreferencesStore;

    public PresetThemeStore(Context context, PreferencesStore preferencesStore) {
        mContext = context;
        mPreferencesStore = preferencesStore;
    }

    @Override
    public int getPrimaryColor() {
        return ContextCompat.getColor(mContext, getPrimaryColorRes());
    }

    @ColorRes
    private int getPrimaryColorRes() {
        switch (mPreferencesStore.getPrimaryColor()) {
            case GRAY:
                return R.color.primary_grey;
            case RED:
                return R.color.primary_red;
            case ORANGE:
                return R.color.primary_orange;
            case YELLOW:
                return R.color.primary_yellow;
            case GREEN:
                return R.color.primary_green;
            case BLUE:
                return R.color.primary;
            case PURPLE:
                return R.color.primary_purple;
            case BLACK:
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
        switch (mPreferencesStore.getPrimaryColor()) {
            case GRAY:
                return R.color.accent_grey;
            case RED:
                return R.color.accent_red;
            case ORANGE:
                return R.color.accent_orange;
            case YELLOW:
                return R.color.accent_yellow;
            case GREEN:
                return R.color.accent_green;
            case BLUE:
                return R.color.accent;
            case PURPLE:
                return R.color.accent_purple;
            case BLACK:
                return R.color.accent_black;
            default:
                return R.color.accent;
        }
    }

    @Override
    public void setTheme(AppCompatActivity activity) {
        applyNightMode(activity);
        activity.setTheme(getThemeId());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            applyTaskDescription(activity);
        }
    }

    private void applyNightMode(AppCompatActivity activity) {
        AppCompatDelegate.setDefaultNightMode(getNightMode());
        activity.getDelegate().applyDayNight();
    }

    @NightMode
    private int getNightMode() {
        switch (mPreferencesStore.getBaseColor()) {
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
    private int getThemeId() {
        switch (mPreferencesStore.getPrimaryColor()) {
            case GRAY:
                return R.style.AppTheme_Grey;
            case RED:
                return R.style.AppTheme_Red;
            case ORANGE:
                return R.style.AppTheme_Orange;
            case YELLOW:
                return R.style.AppTheme_Yellow;
            case GREEN:
                return R.style.AppTheme_Green;
            case BLUE:
                return R.style.AppTheme_Blue;
            case PURPLE:
                return R.style.AppTheme_Purple;
            case BLACK:
                return R.style.AppTheme_Black;
            default:
                return R.style.AppTheme_Blue;
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
        switch (mPreferencesStore.getPrimaryColor()) {
            case GRAY:
                return R.mipmap.ic_launcher_grey;
            case RED:
                return R.mipmap.ic_launcher_red;
            case ORANGE:
                return R.mipmap.ic_launcher_orange;
            case YELLOW:
                return R.mipmap.ic_launcher_yellow;
            case GREEN:
                return R.mipmap.ic_launcher_green;
            case BLUE:
                return R.mipmap.ic_launcher;
            case PURPLE:
                return R.mipmap.ic_launcher_purple;
            case BLACK:
                return R.mipmap.ic_launcher_black;
            default:
                return R.mipmap.ic_launcher;
        }
    }
}
