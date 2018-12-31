package com.marverenic.music.ui.settings.sls;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
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
    }

}
