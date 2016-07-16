package com.marverenic.music.viewmodel;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.android.databinding.library.baseAdapters.BR;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.playlistrules.AutoPlaylistRule;
import com.marverenic.music.instances.playlistrules.RuleEnumeration;

import java.util.Calendar;
import java.util.Formatter;
import java.util.List;

import javax.inject.Inject;

import rx.Subscription;
import timber.log.Timber;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

public class RuleViewModel extends BaseObservable {

    private Context mContext;

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
        mContext = context;
        JockeyApplication.getComponent(context).inject(this);
        mEnumeratedRule = RuleEnumeration.IS;
    }

    public void setRule(List<AutoPlaylistRule> rules, int index) {
        if (rules.equals(mRules) && index == mIndex) {
            return;
        }

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
                if (mFactory.getType() == pos) {
                    return;
                }

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
                RuleEnumeration nextRule = mFilterAdapter.mFilters.get(pos);

                if (nextRule.getField() == mEnumeratedRule.getField()
                        && nextRule.getMatch() == mEnumeratedRule.getMatch()) {
                    return;
                }

                if (nextRule.getInputType() != mEnumeratedRule.getInputType()) {
                    mFactory.setValue("");
                }

                mEnumeratedRule = nextRule;
                mFactory.setField(mEnumeratedRule.getField());
                mFactory.setMatch(mEnumeratedRule.getMatch());
                apply();

                setupValueAdapter();

                notifyPropertyChanged(BR.valueText);
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
            return;
        }

        mValueAdapter = null;
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
                    Timber.e(throwable, "setupSongAdapter: Failed to setup song adapter");
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
                    Timber.e(throwable, "setupArtistAdapter: Failed to setup artist adapter");
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
                    Timber.e(throwable, "setupAlbumAdapter: Failed to setup album adapter");
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
                    Timber.e(throwable, "setupGenreAdapter: Failed to setup genre adapter");
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
                    Timber.e(throwable, "setupPlaylistAdapter: Failed to setup playlist adapter");
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
            if ((mEnumeratedRule.getInputType() & InputType.TYPE_CLASS_DATETIME) != 0) {
                showDateValueDialog();
            } else {
                showValueDialog();
            }
        };
    }

    private void showValueDialog() {
        /*
             Ideally, the View that this ViewHolder wraps would have the EditText directly
             in it without doing the trickery below where it disguises a TextView as an EditText
             and opens an AlertDialog, but there are severe penalties with nesting EditTexts in
             a RecyclerView with a LinearLayoutManager. With no code in the ReyclerView
             Adapter's .onBindViewHolder() method, GC will kick in frequently when scrolling
             to free ~2MB from the heap while pausing for around 60ms (which may also be
             complimented by extra layout calls with the EditText). This has been previously
             reported to Google's AOSP bug tracker which provides more insight into this problem
             https://code.google.com/p/android/issues/detail?id=82586 (closed Feb '15)
             There are some workarounds to this issue, but the most practical suggestions that
             keep the previously mentioned layout are to use a ListView or to extend EditText
             or LinearLayout Manager (which either cause problems in themselves, don't work,
             or both).
             The solution used here simply avoids the problem all together by not nesting an
             EditText in a RecyclerView. When an EditText is needed, the user is prompted with
             an AlertDialog. It's not the best UX, but it's the most practical one for now.
             10/8/15
         */

        TextInputLayout inputLayout = new TextInputLayout(mContext);
        AppCompatEditText editText = new AppCompatEditText(mContext);

        editText.setInputType(mEnumeratedRule.getInputType());
        inputLayout.addView(editText);

        Resources res = mContext.getResources();

        String type = res.getStringArray(R.array.auto_plist_types)[getSelectedType()];
        String match = res.getString(mEnumeratedRule.getNameRes()).toLowerCase();

        AlertDialog valueDialog = new AlertDialog.Builder(mContext)
                .setMessage(type + " " + match)
                .setView(inputLayout)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_done,
                        (dialog, which) -> {
                            String value = editText.getText().toString().trim();
                            if (editText.getInputType() == InputType.TYPE_CLASS_NUMBER) {
                                // Verify the input if this rule needs a numeric value
                                if (TextUtils.isDigitsOnly(value)) {
                                    mFactory.setValue(value);
                                } else {
                                    // If the user inputted something that's not a number, reset it
                                    mFactory.setValue("0");
                                }
                            } else {
                                mFactory.setValue(value);
                            }
                            apply();
                            notifyPropertyChanged(BR.valueText);
                        })
                .create();

        valueDialog.getWindow().setSoftInputMode(SOFT_INPUT_STATE_VISIBLE);

        valueDialog.show();

        int padding = (int) mContext.getResources().getDimension(R.dimen.alert_padding);
        ((View) inputLayout.getParent()).setPadding(
                padding - inputLayout.getPaddingLeft(), 0,
                padding - inputLayout.getPaddingRight(), 0);

        editText.setText(mFactory.getValue());
        editText.setSelection(mFactory.getValue().length());
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == KeyEvent.KEYCODE_ENDCALL) {
                valueDialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();
            }
            return false;
        });
    }

    private void showDateValueDialog() {
        // Calculate the date stored in the reference
        Calendar currentDate = Calendar.getInstance();
        try {
            long timestamp = Long.parseLong(mFactory.getValue());
            currentDate.setTimeInMillis(timestamp * 1000L);
        } catch (NumberFormatException ignored) {
            // If the reference's value isn't valid, just use the current time as the
            // selected date
            currentDate.setTimeInMillis(System.currentTimeMillis());
        }

        DatePickerDialog dateDialog = new DatePickerDialog(mContext,
                (view, year, monthOfYear, dayOfMonth) -> {
                    Calendar nextDate = Calendar.getInstance();
                    nextDate.set(year, monthOfYear, dayOfMonth);
                    mFactory.setValue(Long.toString(nextDate.getTimeInMillis() / 1000L));

                    apply();
                    notifyPropertyChanged(BR.valueText);
                },
                currentDate.get(Calendar.YEAR),
                currentDate.get(Calendar.MONTH),
                currentDate.get(Calendar.DAY_OF_MONTH));

        dateDialog.show();
    }

    public View.OnClickListener onRemoveClick() {
        return v -> mRemovalListener.onRuleRemoved(mIndex);
    }

    @Bindable
    public String getValueText() {
        if ((mEnumeratedRule.getInputType() & InputType.TYPE_CLASS_DATETIME) != 0) {
            long dateAsUnixTimestamp;
            try {
                dateAsUnixTimestamp = Long.parseLong(mFactory.getValue()) * 1000;
            } catch (NumberFormatException e) {
                dateAsUnixTimestamp = System.currentTimeMillis();
            }

            int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
            Formatter date = DateUtils.formatDateRange(mContext, new Formatter(),
                    dateAsUnixTimestamp, dateAsUnixTimestamp, flags, "UTC");

            return date.toString();
        } else {
            return mFactory.getValue();
        }
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
        void onRuleRemoved(int removedIndex);
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
