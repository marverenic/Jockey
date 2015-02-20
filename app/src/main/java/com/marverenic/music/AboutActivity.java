package com.marverenic.music;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.marverenic.music.utils.Navigate;
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Navigate.home(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Navigate.back(this);
    }

    @Override
    public void onResume() {
        Themes.setApplicationIcon(this);
        super.onResume();
    }
}
