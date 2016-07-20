package com.marverenic.music.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.R;

public class AboutActivity extends BaseActivity implements View.OnClickListener {

    public static Intent newIntent(Context context) {
        return new Intent(context, AboutActivity.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ((TextView) findViewById(R.id.aboutVersion)).setText(BuildConfig.VERSION_NAME);

        ((ImageView) findViewById(R.id.aboutAppIcon)).setImageBitmap(mThemeStore.getLargeAppIcon());
        findViewById(R.id.aboutMarverenicLogo).setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        Intent webIntent = new Intent(Intent.ACTION_VIEW);
        webIntent.setData(Uri.parse("http://marverenic.github.io/Jockey/"));
        startActivity(webIntent);
    }
}
