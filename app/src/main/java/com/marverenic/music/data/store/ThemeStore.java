package com.marverenic.music.data.store;

import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.NightMode;

public interface ThemeStore {

    @ColorInt int getPrimaryColor();
    @ColorInt int getAccentColor();

    @NightMode int getNightMode();

    void setTheme(AppCompatActivity activity);

    Drawable getLargeAppIcon();
    void createThemedLauncherIcon();

}
