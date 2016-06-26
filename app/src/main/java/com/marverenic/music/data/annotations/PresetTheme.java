package com.marverenic.music.data.annotations;

import android.support.annotation.IntDef;

@IntDef(value = {PresetTheme.GRAY, PresetTheme.RED, PresetTheme.ORANGE, PresetTheme.YELLOW,
        PresetTheme.GREEN, PresetTheme.BLUE, PresetTheme.PURPLE})
public @interface PresetTheme {
    int GRAY = 0;
    int RED = 1;
    int ORANGE = 2;
    int YELLOW = 3;
    int GREEN = 4;
    int BLUE = 5;
    int PURPLE = 6;
}
