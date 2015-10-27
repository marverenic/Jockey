package com.marverenic.music.utils;

import android.content.Context;
import android.view.View;

/**
 * This class does nothing. In the Google Play flavor, updates are handled automatically by the
 * system and therefore we don't need to check for them. This Class allows us to preserve the
 * update functionality of the GitHub flavor while allowing the Google Play flavor to compile.
 */
public final class Updater {

    @SuppressWarnings("unused")
    public Updater(Context context, View view) {}

    public void execute() {}

}
