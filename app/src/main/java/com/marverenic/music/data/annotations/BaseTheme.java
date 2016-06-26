package com.marverenic.music.data.annotations;

import android.support.annotation.IntDef;

@IntDef(value = {BaseTheme.DARK, BaseTheme.LIGHT, BaseTheme.AUTO})
public @interface BaseTheme {
    int DARK = 0;
    int LIGHT = 1;
    int AUTO = 2;
}
