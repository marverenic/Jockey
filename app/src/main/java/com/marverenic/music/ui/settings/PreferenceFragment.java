package com.marverenic.music.ui.settings;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.settings.sls.SlsPreferenceFragment;
import com.marverenic.music.utils.Util;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import javax.inject.Inject;

public class PreferenceFragment extends PreferenceFragmentCompat
        implements View.OnLongClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String DIRECTORY_FRAGMENT =
            "com.marverenic.music.ui.settings.DirectoryListFragment";
    private static final String EQUALIZER_FRAGMENT =
            "com.marverenic.music.ui.settings.EqualizerFragment";
    private static final String SLS_BROADCAST_FRAGMENT =
            "com.marverenic.music.ui.settings.sls.SlsPreferenceFragment";

    @Inject PreferenceStore mPrefStore;
    @Inject ThemeStore mThemeStore;
    @Inject PlayerController mPlayerController;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        addPreferencesFromResource(R.xml.prefs);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = super.onCreateView(inflater, container, savedInstanceState);

        setDivider(null);
        setDividerHeight(0);

        return view;
    }

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
                                             Bundle savedInstanceState) {
        RecyclerView view = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
        view.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background));

        int padding = (int) getResources().getDimension(R.dimen.global_padding);
        view.setPadding(padding, 0, padding, 0);

        view.addItemDecoration(new BackgroundDecoration(android.R.id.title));
        view.addItemDecoration(new DividerDecoration(getContext(), android.R.id.title));

        return view;
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        return new PreferenceGroupAdapter(preferenceScreen) {
            @Override
            public void onBindViewHolder(PreferenceViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);

                // Override Equalizer preference attachment to add a long click listener
                // and to change the detail text at runtime
                String fragment = getItem(position).getFragment();
                if ("com.marverenic.music.ui.settings.EqualizerFragment".equals(fragment)) {

                    ViewGroup itemView = (ViewGroup) holder.itemView;
                    TextView title = itemView.findViewById(android.R.id.title);
                    TextView detail = itemView.findViewById(android.R.id.summary);

                    boolean hasSystemEq = Util.getSystemEqIntent(getContext()) != null;

                    if (hasSystemEq && Util.hasEqualizer()) {
                        // If we have Jockey's Equalizer and another Equalizer
                        itemView.setOnLongClickListener(PreferenceFragment.this);
                        detail.setText(R.string.equalizer_more_options_detail);
                        detail.setVisibility(View.VISIBLE);

                    } else if (hasSystemEq && !Util.hasEqualizer()) {
                        // If we don't have any equalizers
                        detail.setText(R.string.equalizer_unsupported);
                        detail.setVisibility(View.VISIBLE);
                        itemView.setEnabled(false);
                        title.setEnabled(false);
                        detail.setEnabled(false);
                    }
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.header_settings);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (EQUALIZER_FRAGMENT.equals(preference.getFragment())) {
            Intent eqIntent = Util.getSystemEqIntent(getActivity());

            if (eqIntent != null && !mPrefStore.getEqualizerEnabled()) {
                // If the system has an equalizer implementation already in place, use it
                // to avoid weird problems and conflicts that can cause unexpected behavior

                // for example, on Motorola devices, attaching an Equalizer can cause the
                // MediaPlayer's volume to briefly become very loud -- even when the phone
                // is muted
                startActivity(eqIntent);
            } else if (Util.hasEqualizer()) {
                // If there isn't a global equalizer or the user has already enabled our
                // equalizer, navigate to the built-in implementation
                showEqualizerFragment();
            } else {
                Toast.makeText(getActivity(), R.string.equalizer_unsupported, Toast.LENGTH_LONG)
                        .show();
            }
            return true;
        } else if (DIRECTORY_FRAGMENT.equals(preference.getFragment())) {
            String prefKey = preference.getKey();
            boolean exclude = getString(R.string.pref_key_excluded_dirs).equals(prefKey);

            showDirectoryInclusionExclusionFragment(exclude);
            return true;
        } else if (SLS_BROADCAST_FRAGMENT.equals(preference.getFragment())) {
            replaceFragment(new SlsPreferenceFragment());
            return true;
        } else if (getString(R.string.pref_key_create_launcher_icon).equals(preference.getKey())) {
            if (mPrefStore.getIconColor() != mPrefStore.getPrimaryColor()) {
                showNewShortcutDialog();
            } else {
                View root = getView();
                Snackbar.make(root, R.string.add_shortcut_already_matched, Snackbar.LENGTH_LONG)
                        .show();
            }
            return true;
        } else if (getString(R.string.pref_key_resume_with_headphones).equals(preference.getKey())) {
            mPlayerController.updatePlayerPreferences(mPrefStore);
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void showNewShortcutDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.add_shortcut)
                .setMessage(R.string.add_shortcut_description)
                .setPositiveButton(R.string.action_add,
                        (dialog, which) -> {
                            mThemeStore.createThemedLauncherIcon();
                        })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void replaceFragment(Fragment next) {
        getFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.pref_fragment_container, next)
                .addToBackStack(null)
                .commit();
    }

    private void showEqualizerFragment() {
        replaceFragment(new EqualizerFragment());
    }

    private void showDirectoryInclusionExclusionFragment(boolean exclude) {
        replaceFragment(DirectoryListFragment.newInstance(exclude));
    }

    @Override
    public boolean onLongClick(View v) {
        if (Util.hasEqualizer()) {
            showEqualizerFragment();
        } else {
            Toast
                    .makeText(
                            getActivity(),
                            R.string.equalizer_unsupported,
                            Toast.LENGTH_LONG)
                    .show();
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getString(R.string.pref_key_color_base).equals(key)
                || getString(R.string.pref_key_color_accent).equals(key)
                || getString(R.string.pref_key_color_primary).equals(key)) {
            getActivity().recreate();
        }
    }
}
