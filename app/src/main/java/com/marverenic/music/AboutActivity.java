package com.marverenic.music;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.marverenic.music.utils.Themes;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Themes.setTheme(this);

        setContentView(R.layout.about);

        try {
            ((TextView) findViewById(R.id.aboutVersion)).setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Themes.themeActivity(R.layout.about, getWindow().findViewById(android.R.id.content), this);
    }

    @Override
    public void onResume() {
        Themes.setApplicationIcon(this);
        super.onResume();
    }
}
