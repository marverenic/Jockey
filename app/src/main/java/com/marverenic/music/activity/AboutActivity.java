package com.marverenic.music.activity;

import android.os.Bundle;
import android.widget.TextView;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.R;
import com.marverenic.music.utils.Themes;

public class AboutActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentLayout(R.layout.about);
        setContentView(R.id.aboutScroll);
        super.onCreate(savedInstanceState);

        ((TextView) findViewById(R.id.aboutVersion)).setText(BuildConfig.VERSION_NAME);
    }

    @Override
    public void themeActivity() {
        Themes.themeActivity(R.layout.about, getWindow().findViewById(android.R.id.content), this);
    }

    @Override
    public void updateMiniplayer(){}
}
