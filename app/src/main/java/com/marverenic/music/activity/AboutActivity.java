package com.marverenic.music.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.R;
import com.marverenic.music.utils.Themes;

public class AboutActivity extends BaseActivity implements View.OnClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ((TextView) findViewById(R.id.aboutVersion)).setText(BuildConfig.VERSION_NAME);
    }

    @Override
    public void themeActivity() {
        super.themeActivity();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getSupportActionBar() != null)
            getSupportActionBar().setElevation(0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            ((ImageView) findViewById(R.id.aboutAppIcon)).setImageBitmap(Themes.getLargeIcon(this, DisplayMetrics.DENSITY_XXXHIGH));
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ((ImageView) findViewById(R.id.aboutAppIcon)).setImageBitmap(Themes.getLargeIcon(this, DisplayMetrics.DENSITY_XXHIGH));
            }
            else{
                ((ImageView) findViewById(R.id.aboutAppIcon)).setImageBitmap(Themes.getLargeIcon(this, DisplayMetrics.DENSITY_XHIGH));
            }
        }

        findViewById(R.id.aboutMarverenicLogo).setOnClickListener(this);
    }


    @Override
    public void onClick(View v){
        Intent webIntent = new Intent(Intent.ACTION_VIEW);
        webIntent.setData(Uri.parse("http://marverenic.github.io/Jockey/"));
        startActivity(webIntent);
    }

    @Override
    public void update(){}

    @Override
    public void updateMiniplayer(){}
}