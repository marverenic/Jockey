package com.marverenic.music.data.annotations;

import android.support.annotation.IntDef;

@IntDef(value = {PrimaryTheme.GRAY, PrimaryTheme.RED, PrimaryTheme.ORANGE, PrimaryTheme.YELLOW,
        PrimaryTheme.GREEN, PrimaryTheme.BLUE, PrimaryTheme.PURPLE, PrimaryTheme.BLACK})
public @interface PrimaryTheme {
    int GRAY = 0;
    int RED = 1;
    int ORANGE = 2;
    int YELLOW = 3;
    int GREEN = 4;
    int BLUE = 5;
    int PURPLE = 6;
    int BLACK = 7;
}
