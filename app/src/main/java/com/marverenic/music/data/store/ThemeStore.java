package com.marverenic.music.data.store;

import android.graphics.Bitmap;
import android.support.annotation.ColorInt;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NightMode;

public interface ThemeStore {

    @ColorInt int getPrimaryColor();
    @ColorInt int getAccentColor();

    @NightMode int getNightMode();

    void setTheme(AppCompatActivity activity);

    Bitmap getLargeAppIcon();
    void createThemedLauncherIcon();

}
