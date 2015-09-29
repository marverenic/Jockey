package com.marverenic.music.instances.viewholder;

import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.marverenic.music.Library;
import com.marverenic.music.R;
import com.marverenic.music.instances.AutoPlaylist;

public class RuleHeaderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, AdapterView.OnItemSelectedListener{

    private static final int[] TRUNCATE_CHOICES = new int[] {
            AutoPlaylist.Rule.Field.ID,
            AutoPlaylist.Rule.Field.NAME,
            AutoPlaylist.Rule.Field.PLAY_COUNT,
            AutoPlaylist.Rule.Field.PLAY_COUNT,
            AutoPlaylist.Rule.Field.SKIP_COUNT,
            AutoPlaylist.Rule.Field.SKIP_COUNT,
            AutoPlaylist.Rule.Field.DATE_ADDED,
            AutoPlaylist.Rule.Field.DATE_ADDED,
            AutoPlaylist.Rule.Field.DATE_PLAYED,
            AutoPlaylist.Rule.Field.DATE_PLAYED
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

    private View itemView;
    private AutoPlaylist reference;

    private AppCompatEditText nameEditText;
    private TextInputLayout nameEditLayout;

    private SwitchCompat matchAllRulesSwitch;

    private RelativeLayout songCapContainer;
    private AppCompatCheckBox songCapCheckBox;
    private AppCompatEditText maximumEditText;
    private AppCompatSpinner truncateMethodSpinner;
    private TextView truncateMethodPrefix;

    public RuleHeaderViewHolder(final View itemView, final AutoPlaylist reference) {
        super(itemView);

        this.itemView = itemView;
        this.reference = reference;

        // Initialize View references
        nameEditText = (AppCompatEditText) itemView.findViewById(R.id.playlist_name_input_text);
        nameEditLayout = (TextInputLayout) itemView.findViewById(R.id.playlist_name_input);

        matchAllRulesSwitch = (SwitchCompat) itemView.findViewById(R.id.playlist_match_all);

        songCapCheckBox = (AppCompatCheckBox) itemView.findViewById(R.id.playlist_song_cap_check);
        songCapContainer = (RelativeLayout) itemView.findViewById(R.id.playlist_maximum);
        maximumEditText = (AppCompatEditText) itemView.findViewById(R.id.playlist_maximum_input_text);
        truncateMethodSpinner = (AppCompatSpinner) itemView.findViewById(R.id.playlist_chosen_by);
        truncateMethodPrefix = (TextView) itemView.findViewById(R.id.playlist_chosen_by_prefix);

        // Update View contents to match those provided in the current reference
        nameEditText.setText(reference.playlistName);
        matchAllRulesSwitch.setChecked(reference.matchAllRules);
        if (reference.maximumEntries > 0) {
            maximumEditText.setText(Integer.toString(reference.maximumEntries));
        }

        truncateMethodSpinner.setSelection(lookupTruncateMethod(reference.truncateMethod, reference.truncateAscending));
        songCapCheckBox.setChecked(reference.maximumEntries > 0);
        onCheckedChanged(songCapCheckBox, reference.maximumEntries > 0);

        // These view groups allow the entire description text to be clickable to toggle
        // the setting
        ((ViewGroup) matchAllRulesSwitch.getParent()).setOnClickListener(this);
        songCapContainer.setOnClickListener(this);
        songCapCheckBox.setOnCheckedChangeListener(this);

        // Add listeners to modify the reference when values are changed
        truncateMethodSpinner.setOnItemSelectedListener(this);

        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Validate playlist names to avoid collisions
                String error = Library.verifyPlaylistName(itemView.getContext(), s.toString());
                nameEditLayout.setError(error);

                reference.playlistName = s.toString();
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
                    reference.maximumEntries = Integer.parseInt(s.toString().trim());
                    nameEditLayout.setError(null);
                } catch (NumberFormatException e) {
                    nameEditLayout.setError("Please enter a number"); // TODO String resource
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
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
        }
        if (buttonView == matchAllRulesSwitch) {
            reference.matchAllRules = isChecked;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        reference.truncateMethod = TRUNCATE_CHOICES[(int) id];
        reference.truncateAscending = TRUNCATE_ORDER_ASCENDING[(int) id];
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}