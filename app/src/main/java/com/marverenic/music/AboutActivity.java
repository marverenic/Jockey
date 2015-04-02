package com.marverenic.music;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Themes.setTheme(this);

        setContentView(R.layout.about);

        ((TextView) findViewById(R.id.aboutVersion)).setText(BuildConfig.VERSION_NAME);

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
    public void onPause() {
        super.onPause();
        LibraryScanner.saveLibrary(this);
    }

    @Override
    public void onResume() {
        Themes.setApplicationIcon(this);
        super.onResume();
    }
}
