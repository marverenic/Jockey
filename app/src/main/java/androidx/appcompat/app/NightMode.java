package androidx.appcompat.app;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;

@IntDef({AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES,
        AppCompatDelegate.MODE_NIGHT_AUTO, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM})
@Retention(RetentionPolicy.SOURCE)
public @interface NightMode {}
