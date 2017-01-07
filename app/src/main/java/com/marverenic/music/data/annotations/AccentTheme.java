package com.marverenic.music.data.annotations;

import android.support.annotation.IntDef;

@IntDef(value = {AccentTheme.GRAY, AccentTheme.RED, AccentTheme.ORANGE, AccentTheme.YELLOW,
        AccentTheme.GREEN, AccentTheme.CYAN, AccentTheme.PURPLE, AccentTheme.TEAL})
public @interface AccentTheme {
    int GRAY = 0;
    int RED = 1;
    int ORANGE = 2;
    int YELLOW = 3;
    int GREEN = 4;
    int CYAN = 5;
    int PURPLE = 6;
    int TEAL = 7;
}
