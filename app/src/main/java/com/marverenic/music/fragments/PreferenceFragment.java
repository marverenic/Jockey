package com.marverenic.music.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.marverenic.music.R;
import com.marverenic.music.utils.Prefs;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.utils.Util;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

public class PreferenceFragment extends PreferenceFragmentCompat
        implements View.OnLongClickListener {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

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
        view.setBackgroundColor(Themes.getBackground());

        int padding = (int) getResources().getDimension(R.dimen.global_padding);
        view.setPadding(padding, 0, padding, 0);

        view.addItemDecoration(new BackgroundDecoration(
                Themes.getBackgroundElevated(), android.R.id.title));
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
                if ("com.marverenic.music.fragments.EqualizerFragment".equals(fragment)) {

                    ViewGroup itemView = (ViewGroup) holder.itemView;
                    TextView title = (TextView) itemView.findViewById(android.R.id.title);
                    TextView detail = (TextView) itemView.findViewById(android.R.id.summary);

                    boolean hasSystemEq = Util.getSystemEqIntent(getContext()) != null;

                    if (hasSystemEq && Util.hasEqualizer()) {
                        // If we have Jockey's Equalizer and another Equalizer
                        itemView.setOnLongClickListener(PreferenceFragment.this);
                        detail.setText(R.string.equalizer_more_options_detail);
                        detail.setVisibility(View.VISIBLE);

                    } else if (hasSystemEq && !Util.hasEqualizer()) {
                        // If we don't have any equalizers
                        detail.setText(R.string.equalizerUnsupported);
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
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof ListPreference) {
            final ListPreference listPref = (ListPreference) preference;

            new AlertDialog.Builder(getContext())
                    .setSingleChoiceItems(
                            listPref.getEntries(),
                            listPref.findIndexOfValue(listPref.getValue()),
                            (dialog, which) -> {
                                listPref.setValueIndex(which);
                                dialog.dismiss();
                            }
                    )
                    .setTitle(preference.getTitle())
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.header_settings);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if ("com.marverenic.music.fragments.EqualizerFragment"
                .equals(preference.getFragment())) {
            Intent eqIntent = Util.getSystemEqIntent(getActivity());

            if (eqIntent != null
                    && !Prefs.getPrefs(getActivity()).getBoolean(Prefs.EQ_ENABLED, false)) {
                // If the system has an equalizer implementation already in place, use it
                // to avoid weird problems and conflicts that can cause unexpected behavior

                // for example, on Motorola devices, attaching an Equalizer can cause the
                // MediaPlayer's volume to briefly become very loud -- even when the phone
                // is muted
                startActivity(eqIntent);
            } else if (Util.hasEqualizer()) {
                // If there isn't a global equalizer or the user has already enabled our
                // equalizer, navigate to the built-in implementation
                getFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                R.anim.slide_in_left, R.anim.slide_out_right)
                        .replace(R.id.prefFrame, new EqualizerFragment())
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(getActivity(), R.string.equalizerUnsupported, Toast.LENGTH_LONG)
                        .show();
            }
            return true;
        } else if (preference.getKey().equals(Prefs.ADD_SHORTCUT)) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.add_shortcut)
                    .setMessage(R.string.add_shortcut_description)
                    .setPositiveButton(R.string.action_add,
                            (dialog, which) -> {
                                Themes.updateLauncherIcon(getActivity());
                            })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onLongClick(View v) {
        if (Util.hasEqualizer()) {
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                            R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.prefFrame, new EqualizerFragment())
                    .addToBackStack(null)
                    .commit();
        } else {
            Toast
                    .makeText(
                            getActivity(),
                            R.string.equalizerUnsupported,
                            Toast.LENGTH_LONG)
                    .show();
        }
        return true;
    }
}