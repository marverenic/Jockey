package com.marverenic.music.ui.settings.sls;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.Bindable;
import android.net.Uri;
import android.text.Html;

import com.marverenic.music.BR;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.player.extensions.scrobbler.SlsMessenger;
import com.marverenic.music.ui.BaseViewModel;

public class SlsPreferenceViewModel extends BaseViewModel {

    private static final String SLS_PACKAGE_NAME = "com.adam.aslfms";
    private static final String SLS_STORE_URL = "https://play.google.com/store/apps/details?id=com.adam.aslfms";

    private PreferenceStore mPreferences;
    private PlayerController mPlayerController;
    private boolean mSlsInstalled;

    public SlsPreferenceViewModel(Context context, PreferenceStore preferences,
                                  PlayerController playerController) {
        super(context);
        mPreferences = preferences;
        mPlayerController = playerController;
        updatesSlsInstallationState();
    }

    public void toggleSlsBroadcastsEnabled() {
        setSlsBroadcastsEnabled(!isSlsBroadcastsEnabled());
    }

    @Bindable
    public boolean isSlsBroadcastsEnabled() {
        return mPreferences.isSlsBroadcastingEnabled();
    }

    public void setSlsBroadcastsEnabled(boolean enabled) {
        mPreferences.setSlsBroadcastingEnabled(enabled);
        mPlayerController.updatePlayerPreferences(mPreferences);
        notifyPropertyChanged(BR.slsBroadcastsEnabled);
        notifyPropertyChanged(BR.slsDescription);
        notifyPropertyChanged(BR.slsToggleEnabled);
    }

    @Bindable
    public CharSequence getSlsDescription() {
        if (mSlsInstalled) {
            return getString(R.string.pref_sls_description);
        } else if (isSlsBroadcastsEnabled()) {
            return Html.fromHtml(getString(R.string.pref_sls_description_enabled_but_uninstalled));
        } else {
            return Html.fromHtml(getString(R.string.pref_sls_description_not_installed));
        }
    }

    @Bindable
    public boolean isSlsToggleEnabled() {
        return mSlsInstalled || isSlsBroadcastsEnabled();
    }

    @Bindable
    public int getActionLabel() {
        if (mSlsInstalled) {
            return R.string.pref_sls_action_open;
        } else {
            return R.string.pref_sls_action_install;
        }
    }

    public void onClickActionItem() {
        if (mSlsInstalled) {
            PackageManager pm = getContext().getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(SLS_PACKAGE_NAME);
            if (intent != null) {
                getContext().startActivity(intent);
            } else {
                updatesSlsInstallationState();
            }
        } else {
            getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SLS_STORE_URL)));
        }
    }

    public void updatesSlsInstallationState() {
        mSlsInstalled = SlsMessenger.isSlsInstalled(getContext());
        notifyPropertyChanged(BR.slsToggleEnabled);
        notifyPropertyChanged(BR.slsDescription);
        notifyPropertyChanged(BR.actionLabel);
    }
}
