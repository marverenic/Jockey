package com.marverenic.music.ui;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.marverenic.colors.Colors;
import com.marverenic.colors.activity.ColorsActivityDelegate;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.player.PlayerController;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public abstract class BaseActivity extends RxAppCompatActivity {

    private ColorsActivityDelegate mColorsDelegate = new ColorsActivityDelegate(this);

    @Inject PreferenceStore _mPreferenceStore;
    @Inject ThemeStore _mThemeStore;
    @Inject PlayerController _mPlayerController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        JockeyApplication.getComponent(this).injectBaseActivity(this);
        Colors.setTheme(_mThemeStore.getPrimaryColor(), _mThemeStore.getAccentColor());
        _mThemeStore.setTheme(this);
        mColorsDelegate.onCreate();

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

    @Override
    public void onResume() {
        super.onResume();
        mColorsDelegate.onResume();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

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
}
