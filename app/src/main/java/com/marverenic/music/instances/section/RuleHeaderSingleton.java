package com.marverenic.music.instances.section;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;

import com.marverenic.heterogeneousadapter.EnhancedViewHolder;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter.SingletonSection;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.databinding.InstanceRulesHeaderBinding;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.playlistrules.AutoPlaylistRule;

import javax.inject.Inject;

public class RuleHeaderSingleton extends SingletonSection<AutoPlaylist.Builder> {

    public RuleHeaderSingleton(AutoPlaylist.Builder editor) {
        super(editor);
    }

    @Override
    public EnhancedViewHolder<AutoPlaylist.Builder> createViewHolder(HeterogeneousAdapter adapter,
                                                                     ViewGroup parent) {
        InstanceRulesHeaderBinding binding = InstanceRulesHeaderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding, get(0));
    }

    public static class ViewHolder extends EnhancedViewHolder<AutoPlaylist.Builder>
            implements View.OnClickListener, CompoundButton.OnCheckedChangeListener,
            AdapterView.OnItemSelectedListener {

        private static final int[] TRUNCATE_CHOICES = new int[] {
                AutoPlaylistRule.ID,
                AutoPlaylistRule.NAME,
                AutoPlaylistRule.PLAY_COUNT,
                AutoPlaylistRule.PLAY_COUNT,
                AutoPlaylistRule.SKIP_COUNT,
                AutoPlaylistRule.SKIP_COUNT,
                AutoPlaylistRule.DATE_ADDED,
                AutoPlaylistRule.DATE_ADDED,
                AutoPlaylistRule.DATE_PLAYED,
                AutoPlaylistRule.DATE_PLAYED
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

        private AutoPlaylist.Builder reference;
        private final String originalName;

        private InstanceRulesHeaderBinding mBinding;

        public ViewHolder(InstanceRulesHeaderBinding binding, AutoPlaylist.Builder reference) {
            super(binding.getRoot());
            JockeyApplication.getComponent(binding.getRoot().getContext()).inject(this);

            mBinding = binding;

            this.reference = reference;
            this.originalName = reference.getName();

            init();
        }

        private void init() {
            // Update View contents to match those provided in the current reference
            mBinding.playlistNameInputText.setText(reference.getName());
            mBinding.playlistMatchAll.setChecked(reference.isMatchAllRules());
            if (reference.getMaximumEntries() > 0) {
                mBinding.playlistMaximumInputText.setText(Integer.toString(reference.getMaximumEntries()));
            }

            mBinding.playlistChosenBy.setSelection(lookupTruncateMethod(
                    reference.getTruncateMethod(), reference.isTruncateAscending()));
            mBinding.playlistSongCapCheck.setChecked(reference.getMaximumEntries() > 0);
            onCheckedChanged(mBinding.playlistSongCapCheck, reference.getMaximumEntries() > 0);

            // These view groups allow the entire description text to be clickable to toggle
            // the setting
            ((ViewGroup) mBinding.playlistMatchAll.getParent()).setOnClickListener(this);
            ((ViewGroup) mBinding.playlistSongCapCheck.getParent()).setOnClickListener(this);
            mBinding.playlistSongCapCheck.setOnCheckedChangeListener(this);
            mBinding.playlistMatchAll.setOnCheckedChangeListener(this);

            // Add listeners to modify the reference when values are changed
            mBinding.playlistChosenBy.setOnItemSelectedListener(this);

            mBinding.playlistNameInputText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Validate playlist names to avoid collisions
                    if (originalName.isEmpty()
                            || !originalName.equalsIgnoreCase(s.toString().trim())) {
                        String error = mPlaylistStore.verifyPlaylistName(s.toString());
                        mBinding.playlistNameInputText.setError(error);
                    } else {
                        mBinding.playlistNameInput.setError(null);
                        mBinding.playlistNameInput.setErrorEnabled(false);
                    }
                    reference.setName(s.toString().trim());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            mBinding.playlistMaximumInputText.addTextChangedListener(new TextWatcher() {
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
        public void onUpdate(AutoPlaylist.Builder item, int sectionPosition) {
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
            if (v == mBinding.playlistSongCapCheck.getParent()) {
                mBinding.playlistSongCapCheck.toggle();
            }
            if (v == mBinding.playlistMatchAllContainer) {
                mBinding.playlistMatchAll.toggle();
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (buttonView == mBinding.playlistSongCapCheck) {
                mBinding.playlistMaximumInputText.setEnabled(isChecked);
                mBinding.playlistChosenBy.setEnabled(isChecked);
                mBinding.playlistChosenByPrefix.setEnabled(isChecked);
                if (!isChecked) {
                    reference.setMaximumEntries(AutoPlaylist.UNLIMITED_ENTRIES);
                } else {
                    if (mBinding.playlistMaximumInputText.getText().length() > 0) {
                        try {
                            reference.setMaximumEntries(
                                    Integer.parseInt(mBinding.playlistMaximumInputText.getText().toString().trim()));
                        } catch (NumberFormatException e) {
                            reference.setMaximumEntries(0);
                        }
                    }
                }
            }
            if (buttonView == mBinding.playlistMatchAll) {
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
