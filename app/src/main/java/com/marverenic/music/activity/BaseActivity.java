package com.marverenic.music.activity;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
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

public abstract class BaseActivity extends RxAppCompatActivity
        implements PlayerController.UpdateListener, PlayerController.InfoListener,
        PlayerController.ErrorListener {

    // Used when resuming the Activity to respond to a potential theme change
    @PrimaryTheme
    private int mPrimaryColor;
    @AccentTheme
    private int mAccentColor;

    @Inject PreferenceStore mPreferenceStore;
    @Inject ThemeStore mThemeStore;

    /**
     * @inheritDoc
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        JockeyApplication.getComponent(this).injectBaseActivity(this);

        mThemeStore.setTheme(this);
        mPrimaryColor = mPreferenceStore.getPrimaryColor();
        mAccentColor = mPreferenceStore.getAccentColor();

        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (mPreferenceStore.showFirstStart()) {
            showFirstRunDialog();
        }
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
                            mPreferenceStore.setAllowLogging(pref.isChecked());
                            mPreferenceStore.setShowFirstStart(false);
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

        /*  Start the player service. If it's already running, this does nothing.
            This call is placed here because the service is dependent on having permission to
            storage. Permission results will always trigger onResume (even when requested with
            RxPermissions), so we can use this to promptly start the service.
         */
        PlayerController.startService(getApplicationContext());

        // If the theme was changed since this Activity was created, or the automatic day/night
        // theme has changed state, recreate this activity
        mThemeStore.setTheme(this);
        boolean primaryDiff = mPrimaryColor != mPreferenceStore.getPrimaryColor();
        boolean accentDiff = mAccentColor != mPreferenceStore.getAccentColor();

        if (primaryDiff || accentDiff) {
            recreate();
        } else {
            PlayerController.registerUpdateListener(this);
            PlayerController.registerInfoListener(this);
            PlayerController.registerErrorListener(this);
            onUpdate();
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onUpdate() {

    }

    /**
     * @inheritDoc
     */
    @Override
    public void onPause() {
        super.onPause();
        PlayerController.unregisterUpdateListener(this);
        PlayerController.unregisterInfoListener(this);
        PlayerController.unregisterErrorListener(this);
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
        super.onBackPressed();
        finish();
    }

    @Override
    public void onInfo(String message) {
        showSnackbar(message);
    }

    @Override
    public void onError(String message) {
        showSnackbar(message);
    }

    protected void showSnackbar(String message) {
        View content = findViewById(R.id.list);
        if (content == null) {
            content = findViewById(android.R.id.content);
        }
        Snackbar.make(content, message, Snackbar.LENGTH_LONG).show();
    }
}
