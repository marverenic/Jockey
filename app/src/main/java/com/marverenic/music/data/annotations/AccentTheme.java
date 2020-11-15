package com.marverenic.music.data.annotations;

import androidx.annotation.IntDef;

@IntDef(value = {AccentTheme.GRAY, AccentTheme.RED, AccentTheme.ORANGE, AccentTheme.YELLOW,
        AccentTheme.GREEN, AccentTheme.CYAN, AccentTheme.PURPLE, AccentTheme.TEAL,
        AccentTheme.BLUE})
public @interface AccentTheme {
    int GRAY = 0;
    int RED = 1;
    int ORANGE = 2;
    int YELLOW = 3;
    int GREEN = 4;
    int CYAN = 5;
    int PURPLE = 6;
    int TEAL = 7;
    int BLUE = 8;
}
