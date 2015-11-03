package com.marverenic.music.activity;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;

import com.marverenic.music.R;
import com.marverenic.music.fragments.EqualizerFragment;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Prefs;
import com.marverenic.music.utils.Themes;

public class SettingsActivity extends BaseActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSupportActionBar().setElevation(getResources().getDimension(R.dimen.header_elevation));
        }

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.prefFrame, new PrefFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!getFragmentManager().popBackStackImmediate()) {
                    Navigate.home(this);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (!getFragmentManager().popBackStackImmediate()) {
            super.onBackPressed();
        }
    }

    @Override
    public void updateMiniplayer() {}

    public static class PrefFragment extends PreferenceFragment {

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            if ("com.marverenic.music.fragments.EqualizerFragment".equals(preference.getFragment())) {
                // TODO animate this
                getFragmentManager().beginTransaction()
                        .replace(R.id.prefFrame, new EqualizerFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            } else if (preference.getKey().equals(Prefs.ADD_SHORTCUT)) {
                AlertDialog prompt = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.add_shortcut)
                        .setMessage(R.string.add_shortcut_description)
                        .setPositiveButton(R.string.action_add, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Themes.updateLauncherIcon(getActivity());
                            }
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();

                Themes.themeAlertDialog(prompt);
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }
}
