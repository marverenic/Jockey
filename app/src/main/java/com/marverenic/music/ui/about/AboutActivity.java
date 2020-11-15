package com.marverenic.music.ui.about;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import android.widget.ImageView;
import android.widget.TextView;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.ui.BaseActivity;

import javax.inject.Inject;

public class AboutActivity extends BaseActivity {

    @Inject ThemeStore mThemeStore;

    public static Intent newIntent(Context context) {
        return new Intent(context, AboutActivity.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        JockeyApplication.getComponent(this).inject(this);

        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar toolbar = getSupportActionBar();
        toolbar.setDisplayShowHomeEnabled(true);
        toolbar.setDisplayHomeAsUpEnabled(true);
        toolbar.setHomeButtonEnabled(true);

        ((TextView) findViewById(R.id.aboutVersion)).setText(BuildConfig.VERSION_NAME);
        ((ImageView) findViewById(R.id.aboutAppIcon)).setImageDrawable(mThemeStore.getLargeAppIcon());

        ((ImageView) findViewById(R.id.aboutAppIcon)).setImageDrawable(mThemeStore.getLargeAppIcon());
        findViewById(R.id.aboutMarverenicLogo).setOnClickListener(v -> {
            Intent webIntent = new Intent(Intent.ACTION_VIEW);
            webIntent.setData(Uri.parse("http://marverenic.github.io/Jockey/"));
            startActivity(webIntent);
        });

        findViewById(R.id.aboutPrivacyPolicy).setOnClickListener(v -> {
            Intent webIntent = new Intent(Intent.ACTION_VIEW);
            webIntent.setData(Uri.parse(getString(R.string.url_privacy_policy)));
            startActivity(webIntent);
        });
    }
}
