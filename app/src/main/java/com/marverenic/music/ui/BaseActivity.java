package com.marverenic.music.ui;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.app.NightMode;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.annotations.AccentTheme;
import com.marverenic.music.data.annotations.PrimaryTheme;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.player.PlayerController;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import javax.inject.Inject;

import timber.log.Timber;

public abstract class BaseActivity extends RxAppCompatActivity {

    // Used when resuming the Activity to respond to a potential theme change
    @PrimaryTheme
    private int mPrimaryColor;
    @AccentTheme
    private int mAccentColor;
    @NightMode
    private int mBackgroundColor;

    private boolean mNightMode;

    @Inject PreferenceStore _mPreferenceStore;
    @Inject ThemeStore _mThemeStore;
    @Inject PlayerController _mPlayerController;

    /**
     * @inheritDoc
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        JockeyApplication.getComponent(this).injectBaseActivity(this);

        _mThemeStore.setTheme(this);
        mPrimaryColor = _mPreferenceStore.getPrimaryColor();
        mAccentColor = _mPreferenceStore.getAccentColor();
        mBackgroundColor = _mThemeStore.getNightMode();

        mNightMode = getResources().getBoolean(R.bool.is_night);

        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (_mPreferenceStore.showFirstStart()) {
            showFirstRunDialog();
        }

        _mPlayerController.getInfo()
                .compose(bindToLifecycle())
                .subscribe(this::showSnackbar, throwable -> {
                    Timber.e(throwable, "Failed to show info message");
                });

        _mPlayerController.getError()
                .compose(bindToLifecycle())
                .subscribe(this::showSnackbar, throwable -> {
                    Timber.e(throwable, "Failed to show error message");
                });
    }

    private void showFirstRunDialog() {
        View messageView = getLayoutInflater().inflate(R.layout.alert_pref, null);
        TextView message = (TextView) messageView.findViewById(R.id.pref_alert_content);
        CheckBox pref = (CheckBox) messageView.findViewById(R.id.pref_alert_option);

        message.setText(Html.fromHtml(getString(R.string.first_launch_detail)));
        message.setMovementMethod(LinkMovementMethod.getInstance());

        pref.setChecked(true);
        pref.setText(R.string.enable_additional_logging_detailed);

        new AlertDialog.Builder(this)
                .setTitle(R.string.first_launch_title)
                .setView(messageView)
                .setPositiveButton(R.string.action_agree,
                        (dialog, which) -> {
                            _mPreferenceStore.setAllowLogging(pref.isChecked());
                            _mPreferenceStore.setShowFirstStart(false);
                        })
                .setCancelable(false)
                .show();
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setContentView(@LayoutRes int layoutResId) {
        super.setContentView(layoutResId);
        setupToolbar();
    }

    protected void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onResume() {
        super.onResume();

        // If the theme was changed since this Activity was created, or the automatic day/night
        // theme has changed state, recreate this activity
        _mThemeStore.setTheme(this);
        boolean primaryDiff = mPrimaryColor != _mPreferenceStore.getPrimaryColor();
        boolean accentDiff = mAccentColor != _mPreferenceStore.getAccentColor();
        boolean backgroundDiff = mBackgroundColor != _mThemeStore.getNightMode();

        boolean nightDiff = mNightMode != getResources().getBoolean(R.bool.is_night);

        if (primaryDiff || accentDiff || backgroundDiff
                || (mBackgroundColor == AppCompatDelegate.MODE_NIGHT_AUTO && nightDiff)) {
            recreate();
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onBackPressed() {
        Timber.v("onBackPressed");

        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof BaseFragment) {
                BaseFragment baseFragment = (BaseFragment) fragment;
                if (baseFragment.onBackPressed()) {
                    return;
                }
            }
        }

        super.onBackPressed();
        finish();
    }

    @IdRes
    protected int getSnackbarContainerViewId() {
        return R.id.list;
    }

    protected void showSnackbar(String message) {
        View content = findViewById(getSnackbarContainerViewId());
        if (content == null) {
            content = findViewById(android.R.id.content);
        }
        Snackbar.make(content, message, Snackbar.LENGTH_LONG).show();
    }
}
