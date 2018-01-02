package com.marverenic.music.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.marverenic.music.R;
import com.marverenic.music.ui.BaseActivity;

public class SettingsActivity extends BaseActivity {

    public static Intent newIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar toolbar = getSupportActionBar();
        toolbar.setDisplayShowHomeEnabled(true);
        toolbar.setDisplayHomeAsUpEnabled(true);
        toolbar.setHomeButtonEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(getResources().getDimension(R.dimen.header_elevation));
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.pref_fragment_container, new PreferenceFragment())
                    .commit();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (!getSupportFragmentManager().popBackStackImmediate()) {
            finish();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (!getSupportFragmentManager().popBackStackImmediate()) {
            super.onBackPressed();
        }
    }
}
