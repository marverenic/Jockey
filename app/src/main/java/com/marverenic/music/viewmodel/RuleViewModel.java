package com.marverenic.music.viewmodel;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.databinding.library.baseAdapters.BR;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.playlistrules.AutoPlaylistRule;
import com.marverenic.music.instances.playlistrules.RuleEnumeration;

import java.util.List;

import javax.inject.Inject;

import rx.Subscription;

public class RuleViewModel extends BaseObservable {

    private static final String TAG = "RuleViewModel";

    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;

    private AutoPlaylistRule.Factory mFactory;
    private RuleEnumeration mEnumeratedRule;

    private FilterAdapter mFilterAdapter;
    private ValueAdapter<?> mValueAdapter;
    private Subscription mValueSubscription;

    private OnRemovalListener mRemovalListener;
    private List<AutoPlaylistRule> mRules;
    private int mIndex;

    public RuleViewModel(Context context) {
        JockeyApplication.getComponent(context).inject(this);
        mEnumeratedRule = RuleEnumeration.IS;
    }

    public void setRule(List<AutoPlaylistRule> rules, int index) {
        mRules = rules;
        mIndex = index;
        mFactory = new AutoPlaylistRule.Factory(rules.get(index));
        mEnumeratedRule = RuleEnumeration.from(mFactory.getField(), mFactory.getMatch());

        if (mFilterAdapter != null) {
            mFilterAdapter.onTypeChanged();
        }

        setupValueAdapter();

        // Setup everything but the value spinner (since the adapter has to be made asynchronously)
        notifyPropertyChanged(BR.selectedType);
        notifyPropertyChanged(BR.selectedFilter);
        notifyPropertyChanged(BR.valueSpinnerVisibility);
        notifyPropertyChanged(BR.valueTextVisibility);
        notifyPropertyChanged(BR.valueText);
    }

    public void setOnRemovalListener(OnRemovalListener listener) {
        mRemovalListener = listener;
    }

    private void apply() {
        mRules.set(mIndex, mFactory.build());
    }

    @Bindable
    public int getSelectedType() {
        return mFactory.getType();
    }

    public Spinner.OnItemSelectedListener getTypeSelectedListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                mFactory.setType(pos);

                apply();
                mEnumeratedRule = RuleEnumeration.from(mFactory.getField(), mFactory.getMatch());
                mFilterAdapter.onTypeChanged();

                setupValueAdapter();

                notifyPropertyChanged(BR.valueTextVisibility);
                notifyPropertyChanged(BR.valueSpinnerVisibility);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
    }

    @Bindable
    public int getSelectedFilter() {
        RuleEnumeration[] values = RuleEnumeration.values();

        for (int i = 0; i < values.length; i++) {
            RuleEnumeration filter = values[i];
            if (filter.equals(mEnumeratedRule)) {
                return i;
            }
        }
        return -1;
    }

    public Spinner.OnItemSelectedListener getFilterSelectedListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                mEnumeratedRule = mFilterAdapter.mFilters.get(pos);

                mFactory.setField(mEnumeratedRule.getField());
                mFactory.setMatch(mEnumeratedRule.getMatch());
                apply();

                setupValueAdapter();

                notifyPropertyChanged(BR.valueTextVisibility);
                notifyPropertyChanged(BR.valueSpinnerVisibility);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
    }

    private void setupValueAdapter() {
        if (mValueSubscription != null) {
            mValueSubscription.unsubscribe();
        }

        if (getValueSpinnerVisibility() != View.VISIBLE) {
            mValueAdapter = null;
        }

        switch (mFactory.getType()) {
            case AutoPlaylistRule.SONG:
                setupSongAdapter();
                break;
            case AutoPlaylistRule.ARTIST:
                setupArtistAdapter();
                break;
            case AutoPlaylistRule.ALBUM:
                setupAlbumAdapter();
                break;
            case AutoPlaylistRule.GENRE:
                setupGenreAdapter();
                break;
            case AutoPlaylistRule.PLAYLIST:
                setupPlaylistAdapter();
                break;
        }

        notifyPropertyChanged(BR.valueAdapter);
    }

    private void setupSongAdapter() {
        mValueSubscription = mMusicStore.getSongs()
                .take(1)
                .subscribe(songs -> {
                    mValueAdapter = new ValueAdapter<Song>(songs) {
                        @Override
                        public long getItemId(Song item) {
                            return item.getSongId();
                        }

                        @Override
                        public String getItemName(Song item) {
                            return item.getSongName();
                        }
                    };
                    notifyPropertyChanged(BR.valueAdapter);
                    notifyPropertyChanged(BR.selectedValue);
                }, throwable -> {
                    Log.e(TAG, "setupSongAdapter: Failed to setup song adapter", throwable);
                });
    }

    private void setupArtistAdapter() {
        mValueSubscription = mMusicStore.getArtists()
                .take(1)
                .subscribe(artists -> {
                    mValueAdapter = new ValueAdapter<Artist>(artists) {
                        @Override
                        public long getItemId(Artist item) {
                            return item.getArtistId();
                        }

                        @Override
                        public String getItemName(Artist item) {
                            return item.getArtistName();
                        }
                    };
                    notifyPropertyChanged(BR.valueAdapter);
                    notifyPropertyChanged(BR.selectedValue);
                }, throwable -> {
                    Log.e(TAG, "setupArtistAdapter: Failed to setup artist adapter", throwable);
                });
    }

    private void setupAlbumAdapter() {
        mValueSubscription = mMusicStore.getAlbums()
                .take(1)
                .subscribe(albums -> {
                    mValueAdapter = new ValueAdapter<Album>(albums) {
                        @Override
                        public long getItemId(Album item) {
                            return item.getAlbumId();
                        }

                        @Override
                        public String getItemName(Album item) {
                            return item.getAlbumName();
                        }
                    };
                    notifyPropertyChanged(BR.valueAdapter);
                    notifyPropertyChanged(BR.selectedValue);
                }, throwable -> {
                    Log.e(TAG, "setupAlbumAdapter: Failed to setup album adapter", throwable);
                });
    }

    private void setupGenreAdapter() {
        mValueSubscription = mMusicStore.getGenres()
                .take(1)
                .subscribe(genres -> {
                    mValueAdapter = new ValueAdapter<Genre>(genres) {
                        @Override
                        public long getItemId(Genre item) {
                            return item.getGenreId();
                        }

                        @Override
                        public String getItemName(Genre item) {
                            return item.getGenreName();
                        }
                    };
                    notifyPropertyChanged(BR.valueAdapter);
                    notifyPropertyChanged(BR.selectedValue);
                }, throwable -> {
                    Log.e(TAG, "setupGenreAdapter: Failed to setup genre adapter", throwable);
                });
    }

    private void setupPlaylistAdapter() {
        mValueSubscription = mPlaylistStore.getPlaylists()
                .take(1)
                .subscribe(playlists -> {
                    mValueAdapter = new ValueAdapter<Playlist>(playlists) {
                        @Override
                        public long getItemId(Playlist item) {
                            return item.getPlaylistId();
                        }

                        @Override
                        public String getItemName(Playlist item) {
                            return item.getPlaylistName();
                        }
                    };
                    notifyPropertyChanged(BR.valueAdapter);
                    notifyPropertyChanged(BR.selectedValue);
                }, throwable -> {
                    Log.e(TAG, "setupPlaylistAdapter: Failed to setup playlist adapter", throwable);
                });
    }

    public SpinnerAdapter getFilterAdapter() {
        if (mFilterAdapter == null) {
            mFilterAdapter = new FilterAdapter();
        } else {
            mFilterAdapter.onTypeChanged();
        }
        return mFilterAdapter;
    }

    @Bindable
    public SpinnerAdapter getValueAdapter() {
        return mValueAdapter;
    }

    @Bindable
    public int getValueSpinnerVisibility() {
        return (mEnumeratedRule.getInputType() == InputType.TYPE_NULL) ? View.VISIBLE : View.GONE;
    }

    @Bindable
    public int getValueTextVisibility() {
        return (mEnumeratedRule.getInputType() != InputType.TYPE_NULL) ? View.VISIBLE : View.GONE;
    }

    public View.OnClickListener onValueTextClick() {
        return v -> {
            // TODO
            Toast.makeText(v.getContext(), "TODO: Implement this", Toast.LENGTH_SHORT).show();
        };
    }

    public View.OnClickListener onRemoveClick() {
        return v -> {
            // TODO show confirmation snackbar
            mRules.remove(mIndex);
            mRemovalListener.onRuleRemoved(mIndex);
        };
    }

    @Bindable
    public String getValueText() {
        return mFactory.getValue();
    }

    @Bindable
    public int getSelectedValue() {
        if (mValueAdapter == null) {
            return 0;
        }

        long value;
        try {
            value = Long.parseLong(mFactory.getValue());
        } catch (NumberFormatException exception) {
            return 0;
        }

        for (int i = 0; i < mValueAdapter.getCount(); i++) {
            if (mValueAdapter.getItemId(i) == value) {
                return i;
            }
        }

        return 0;
    }

    public AdapterView.OnItemSelectedListener getValueSelectedListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (getValueSpinnerVisibility() == View.VISIBLE) {
                    mFactory.setValue(Long.toString(id));
                    apply();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        };
    }

    public interface OnRemovalListener {
        void onRuleRemoved(int index);
    }

    private class FilterAdapter extends BaseAdapter {

        private List<RuleEnumeration> mFilters;

        public FilterAdapter() {
            onTypeChanged();
        }

        public void onTypeChanged() {
            switch (mFactory.getType()) {
                case AutoPlaylistRule.SONG:
                    mFilters = RuleEnumeration.getAllSongRules();
                    break;
                case AutoPlaylistRule.ALBUM:
                    mFilters = RuleEnumeration.getAllAlbumRules();
                    break;
                case AutoPlaylistRule.ARTIST:
                    mFilters = RuleEnumeration.getAllArtistRules();
                    break;
                case AutoPlaylistRule.GENRE:
                    mFilters = RuleEnumeration.getAllGenreRules();
                    break;
                case AutoPlaylistRule.PLAYLIST:
                    mFilters = RuleEnumeration.getAllPlaylistRules();
                    break;
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mFilters.size();
        }

        @Override
        public Object getItem(int pos) {
            return mFilters.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return mFilters.get(pos).getId();
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(
                        android.R.layout.simple_spinner_item, parent, false);
            }

            TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
            textView.setText(mFilters.get(pos).getNameRes());

            return convertView;
        }

        @Override
        public View getDropDownView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(
                        android.R.layout.simple_spinner_dropdown_item, parent, false);
            }

            TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
            textView.setText(mFilters.get(pos).getNameRes());

            return convertView;
        }
    }

    private abstract class ValueAdapter<Type> extends BaseAdapter {

        private List<Type> mValues;

        public ValueAdapter(List<Type> values) {
            mValues = values;
        }

        @Override
        public int getCount() {
            return mValues.size();
        }

        @Override
        public Object getItem(int pos) {
            return mValues.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return getItemId(mValues.get(pos));
        }

        public abstract long getItemId(Type item);

        public abstract String getItemName(Type item);

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(
                        android.R.layout.simple_spinner_dropdown_item, parent, false);
            }

            TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
            textView.setText(getItemName(mValues.get(pos)));

            return convertView;
        }
    }
}
