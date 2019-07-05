package com.marverenic.music.ui.settings.sls;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.databinding.FragmentSlsPreferenceBinding;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseFragment;

import javax.inject.Inject;

public class SlsPreferenceFragment extends BaseFragment {

    @Inject PreferenceStore preferenceStore;
    @Inject PlayerController playerController;

    private FragmentSlsPreferenceBinding binding;
    private SlsPreferenceViewModel viewModel;

    private SlsInstallReceiver installReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
        installReceiver = new SlsInstallReceiver();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_sls_preference,
                container, false);

        viewModel = new SlsPreferenceViewModel(getContext(), preferenceStore, playerController);
        binding.setViewModel(viewModel);

        binding.readmeMessage.setMovementMethod(new LinkMovementMethod());

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.updatesSlsInstallationState();
        binding.executePendingBindings();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        requireContext().registerReceiver(installReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(installReceiver);
    }

    private class SlsInstallReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            viewModel.updatesSlsInstallationState();
        }
    }

}
