package com.marverenic.music.ui;

import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.app.NightMode;
import android.view.View;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.annotations.AccentTheme;
import com.marverenic.music.data.annotations.PrimaryTheme;
import com.marverenic.music.data.manager.PrivacyPolicyManager;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.player.PlayerController;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import java.util.Collections;
import java.util.List;

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
    @Inject PrivacyPolicyManager _mPrivacyPolicyManager;

    private PlayerController.Binding mPlayerControllerBinding;

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

        if (_mPreferenceStore.isFirstStart()) {
            _mPreferenceStore.setIsFirstStart(false);
            _mPrivacyPolicyManager.onLatestPrivacyPolicyConfirmed();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPlayerControllerBinding = _mPlayerController.bind();
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
            return;
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

        _mPrivacyPolicyManager.isPrivacyPolicyUpdated()
                .compose(bindToLifecycle())
                .subscribe(privacyPolicyUpdated -> {
                    if (privacyPolicyUpdated) showPrivacyPolicySnackbar();
                }, throwable -> Timber.e(throwable, "Failed to check for privacy policy updates"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        _mPlayerController.unbind(mPlayerControllerBinding);
        mPlayerControllerBinding = null;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onBackPressed() {
        Timber.v("onBackPressed");

        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments == null) {
            fragments = Collections.emptyList();
        }

        for (Fragment fragment : fragments) {
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

    private void showPrivacyPolicySnackbar() {
        if (_mPreferenceStore.isFirstStart()) {
            return;
        }

        Snackbar snackbar = Snackbar.make(
                findViewById(getSnackbarContainerViewId()),
                R.string.alert_privacy_policy_updated,
                Snackbar.LENGTH_INDEFINITE
        );

        snackbar.setAction(R.string.action_read_privacy_policy, v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(getString(R.string.url_privacy_policy)));
            startActivity(intent);
        });

        snackbar.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                switch (event) {
                    case DISMISS_EVENT_ACTION:
                    case DISMISS_EVENT_MANUAL:
                    case DISMISS_EVENT_SWIPE:
                        _mPrivacyPolicyManager.onLatestPrivacyPolicyConfirmed();
                }
            }
        });

        _mPrivacyPolicyManager.onPrivacyPolicyUpdateNotified();
        snackbar.show();
    }
}
