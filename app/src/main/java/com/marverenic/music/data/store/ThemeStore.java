package com.marverenic.music.data.store;

import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NightMode;

import com.marverenic.colors.AccentColor;
import com.marverenic.colors.PrimaryColor;

public interface ThemeStore {

    PrimaryColor getPrimaryColor();
    AccentColor getAccentColor();

    @NightMode int getNightMode();

    void setTheme(AppCompatActivity activity);

    Drawable getLargeAppIcon();
    void createThemedLauncherIcon();

}
