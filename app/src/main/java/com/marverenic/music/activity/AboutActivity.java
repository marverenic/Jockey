package com.marverenic.music.activity;

import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.ImageView;
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
        super.themeActivity();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) getSupportActionBar().setElevation(0);

        findViewById(R.id.aboutScroll).setBackgroundColor(Themes.getPrimary());

        int[] primaryText = {R.id.aboutAppName, R.id.lastFmHeader, R.id.aboutAOSPHeader, R.id.aboutAOSPTabsHeader,
                R.id.aboutPicassoHeader, R.id.aboutDSLVHeader, R.id.aboutApolloHeader, R.id.aboutStackOverflowHeader};
        int[] detailText = {R.id.aboutDescription, R.id.aboutLicense, R.id.aboutUsesHeader,
                R.id.aboutVersion, R.id.aboutAOSPDetail, R.id.aboutAOSPTabsDetail, R.id.aboutUILDetail, R.id.aboutDSLVDetail};

        for (int aPrimaryText : primaryText)
            ((TextView) findViewById(aPrimaryText)).setTextColor(Themes.getUiText());
        for (int aDetailText : detailText)
            ((TextView) findViewById(aDetailText)).setTextColor(Themes.getUiDetailText());

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
    }

    @Override
    public void update() {}

    @Override
    public void updateMiniplayer(){}
}
