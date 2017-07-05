package com.marverenic.music.ui.library.playlist.edit;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;

import com.marverenic.music.BR;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.model.AutoPlaylist;
import com.marverenic.music.model.playlistrules.AutoPlaylistRule;

import javax.inject.Inject;

public class RuleHeaderViewModel extends BaseObservable {

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

    private AutoPlaylist mOriginalReference;
    private AutoPlaylist.Builder mBuilder;

    private boolean mIgnoreFirstNameError;

    public RuleHeaderViewModel(Context context) {
        JockeyApplication.getComponent(context).inject(this);
    }

    public void setOriginalReference(AutoPlaylist playlist) {
        mOriginalReference = playlist;
        mIgnoreFirstNameError = true;
        notifyPropertyChanged(BR.playlistNameError);
    }

    public void setBuilder(AutoPlaylist.Builder builder) {
        mBuilder = builder;
        notifyPropertyChanged(BR.playlistName);
        notifyPropertyChanged(BR.playlistNameError);
        notifyPropertyChanged(BR.matchAllRules);
        notifyPropertyChanged(BR.matchAllRules);
        notifyPropertyChanged(BR.songCountCapped);
        notifyPropertyChanged(BR.chosenBySelection);
        notifyPropertyChanged(BR.songCap);
    }

    @Bindable
    public String getPlaylistName() {
        return mBuilder.getName();
    }

    public TextWatcher onPlaylistNameChanged() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mBuilder.setName(charSequence.toString());
                notifyPropertyChanged(BR.playlistName);
                notifyPropertyChanged(BR.playlistNameError);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        };
    }

    @Bindable
    public String getPlaylistNameError() {
        if (mIgnoreFirstNameError) {
            // Don't show initial errors
            mIgnoreFirstNameError = false;
            return null;
        }

        String initialName = mOriginalReference.getPlaylistName().trim();
        String currentName = mBuilder.getName().trim();

        if (!initialName.isEmpty() && initialName.equalsIgnoreCase(currentName)) {
            // Don't show errors if the playlist name wasn't changed (unless the initial name was
            // blank)
            return null;
        } else {
            return mPlaylistStore.verifyPlaylistName(getPlaylistName());
        }
    }

    @Bindable
    public boolean isMatchAllRules() {
        return mBuilder.isMatchAllRules();
    }

    public View.OnClickListener onMatchAllContainerClick() {
        return v -> {
            mBuilder.setMatchAllRules(!isMatchAllRules());
            notifyPropertyChanged(BR.matchAllRules);
        };
    }

    public CompoundButton.OnCheckedChangeListener onMatchAllToggle() {
        return (checkBox, enabled) -> mBuilder.setMatchAllRules(enabled);
    }

    @Bindable
    public boolean isSongCountCapped() {
        return mBuilder.getMaximumEntries() >= 0;
    }

    public View.OnClickListener onSongCapContainerClick() {
        return v -> {
            mBuilder.setMaximumEntries(-1 * mBuilder.getMaximumEntries());
            notifyPropertyChanged(BR.songCountCapped);
        };
    }

    @Bindable
    public String getSongCap() {
        return Integer.toString(Math.abs(mBuilder.getMaximumEntries()));
    }

    public TextWatcher onSongCapChanged() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!isSongCountCapped()) {
                    return;
                }

                String value = charSequence.toString();
                if (value.isEmpty()) {
                    mBuilder.setMaximumEntries(0);
                } else {
                    try {
                        mBuilder.setMaximumEntries(Integer.parseInt(charSequence.toString()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        };
    }

    @Bindable
    public int getChosenBySelection() {
        int i = 0;
        while (TRUNCATE_CHOICES[i] != mBuilder.getTruncateMethod()) {
            i++;
        }
        while (TRUNCATE_ORDER_ASCENDING[i] != mBuilder.isTruncateAscending()) {
            i++;
        }
        return i;
    }

    public AdapterView.OnItemSelectedListener onTruncateMethodSelected() {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                mBuilder.setTruncateMethod(TRUNCATE_CHOICES[pos]);
                mBuilder.setTruncateAscending(TRUNCATE_ORDER_ASCENDING[pos]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        };
    }

    public CompoundButton.OnCheckedChangeListener onTruncateToggle() {
        return (checkBox, enabled) -> {
            if (enabled) {
                mBuilder.setMaximumEntries(Math.abs(mBuilder.getMaximumEntries()));
            } else {
                mBuilder.setMaximumEntries(-1 * Math.abs(mBuilder.getMaximumEntries()));
            }

            notifyPropertyChanged(BR.songCountCapped);
        };
    }
}
