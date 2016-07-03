package com.marverenic.music.instances.section;

import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.heterogeneousadapter.EnhancedViewHolder;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.instances.playlistrules.AutoPlaylistRule;

import javax.inject.Inject;

public class RuleHeaderSingleton extends HeterogeneousAdapter.SingletonSection<AutoPlaylist> {

    public RuleHeaderSingleton(AutoPlaylist data) {
        super(data);
    }

    @Override
    public EnhancedViewHolder<AutoPlaylist> createViewHolder(
            HeterogeneousAdapter adapter, ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.instance_rules_header, parent, false),
                get(0));
    }

    public static class ViewHolder extends EnhancedViewHolder<AutoPlaylist>
            implements View.OnClickListener, CompoundButton.OnCheckedChangeListener,
            AdapterView.OnItemSelectedListener {

        private static final int[] TRUNCATE_CHOICES = new int[] {
                AutoPlaylistRule.Field.ID,
                AutoPlaylistRule.Field.NAME,
                AutoPlaylistRule.Field.PLAY_COUNT,
                AutoPlaylistRule.Field.PLAY_COUNT,
                AutoPlaylistRule.Field.SKIP_COUNT,
                AutoPlaylistRule.Field.SKIP_COUNT,
                AutoPlaylistRule.Field.DATE_ADDED,
                AutoPlaylistRule.Field.DATE_ADDED,
                AutoPlaylistRule.Field.DATE_PLAYED,
                AutoPlaylistRule.Field.DATE_PLAYED
        };

        private static final boolean[] TRUNCATE_ORDER_ASCENDING = new boolean[] {
                true,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                false,
                true
        };

        @Inject PlaylistStore mPlaylistStore;

        private AutoPlaylist reference;
        private final String originalName;

        private TextInputLayout nameEditLayout;

        private SwitchCompat matchAllRulesSwitch;

        private RelativeLayout songCapContainer;
        private AppCompatCheckBox songCapCheckBox;
        private AppCompatEditText maximumEditText;

        private AppCompatSpinner truncateMethodSpinner;
        private TextView truncateMethodPrefix;

        public ViewHolder(View itemView, AutoPlaylist reference) {
            super(itemView);
            JockeyApplication.getComponent(itemView.getContext()).inject(this);

            this.reference = reference;
            this.originalName = reference.getPlaylistName();

            // Initialize View references
            nameEditLayout = (TextInputLayout) itemView.findViewById(R.id.playlist_name_input);

            matchAllRulesSwitch = (SwitchCompat) itemView.findViewById(R.id.playlist_match_all);

            songCapCheckBox =
                    (AppCompatCheckBox) itemView.findViewById(R.id.playlist_song_cap_check);
            songCapContainer =
                    (RelativeLayout) itemView.findViewById(R.id.playlist_maximum);
            maximumEditText =
                    (AppCompatEditText) itemView.findViewById(R.id.playlist_maximum_input_text);
            truncateMethodSpinner =
                    (AppCompatSpinner) itemView.findViewById(R.id.playlist_chosen_by);
            truncateMethodPrefix =
                    (TextView) itemView.findViewById(R.id.playlist_chosen_by_prefix);

            init();
        }

        private void init() {
            AppCompatEditText nameEditText =
                    (AppCompatEditText) itemView.findViewById(R.id.playlist_name_input_text);

            // Update View contents to match those provided in the current reference
            nameEditText.setText(reference.getPlaylistName());
            matchAllRulesSwitch.setChecked(reference.isMatchAllRules());
            if (reference.getMaximumEntries() > 0) {
                maximumEditText.setText(Integer.toString(reference.getMaximumEntries()));
            }

            truncateMethodSpinner.setSelection(lookupTruncateMethod(
                    reference.getTruncateMethod(), reference.isTruncateAscending()));
            songCapCheckBox.setChecked(reference.getMaximumEntries() > 0);
            onCheckedChanged(songCapCheckBox, reference.getMaximumEntries() > 0);

            // These view groups allow the entire description text to be clickable to toggle
            // the setting
            ((ViewGroup) matchAllRulesSwitch.getParent()).setOnClickListener(this);
            songCapContainer.setOnClickListener(this);
            songCapCheckBox.setOnCheckedChangeListener(this);
            matchAllRulesSwitch.setOnCheckedChangeListener(this);

            // Add listeners to modify the reference when values are changed
            truncateMethodSpinner.setOnItemSelectedListener(this);

            nameEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Validate playlist names to avoid collisions
                    if (originalName.isEmpty()
                            || !originalName.equalsIgnoreCase(s.toString().trim())) {
                        String error = mPlaylistStore.verifyPlaylistName(s.toString());
                        nameEditLayout.setError(error);
                    } else {
                        nameEditLayout.setError(null);
                        nameEditLayout.setErrorEnabled(false);
                    }
                    reference.setPlaylistName(s.toString().trim());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            maximumEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        reference.setMaximumEntries(Integer.parseInt(s.toString().trim()));
                    } catch (NumberFormatException e) {
                        reference.setMaximumEntries(0);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
        }

        @Override
        public void onUpdate(AutoPlaylist item, int sectionPosition) {
            reference = item;
        }

        private int lookupTruncateMethod(int field, boolean ascending) {
            int i = 0;
            while (TRUNCATE_CHOICES[i] != field) {
                i++;
            }
            while (TRUNCATE_ORDER_ASCENDING[i] != ascending) {
                i++;
            }
            return i;
        }

        @Override
        public void onClick(View v) {
            if (v == songCapContainer) {
                songCapCheckBox.toggle();
            }
            if (v == matchAllRulesSwitch.getParent()) {
                matchAllRulesSwitch.toggle();
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (buttonView == songCapCheckBox) {
                maximumEditText.setEnabled(isChecked);
                truncateMethodSpinner.setEnabled(isChecked);
                truncateMethodPrefix.setEnabled(isChecked);
                if (!isChecked) {
                    reference.setMaximumEntries(AutoPlaylist.UNLIMITED_ENTRIES);
                } else {
                    if (maximumEditText.getText().length() > 0) {
                        try {
                            reference.setMaximumEntries(
                                    Integer.parseInt(maximumEditText.getText().toString().trim()));
                        } catch (NumberFormatException e) {
                            reference.setMaximumEntries(0);
                        }
                    }
                }
            }
            if (buttonView == matchAllRulesSwitch) {
                reference.setMatchAllRules(isChecked);
            }
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            reference.setTruncateMethod(TRUNCATE_CHOICES[(int) id]);
            reference.setTruncateAscending(TRUNCATE_ORDER_ASCENDING[(int) id]);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }
}
