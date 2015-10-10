package com.marverenic.music.instances.viewholder;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.marverenic.music.Library;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AutoPlaylistEditActivity;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Themes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class RuleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private AutoPlaylistEditActivity.Adapter parent;
    private View itemView;
    private AutoPlaylist.Rule reference;

    private AppCompatSpinner typeDropDown;
    private AppCompatSpinner fieldDropDown;
    private FrameLayout valueTextWrapper;
    private TextView valueText;
    private AppCompatSpinner valueSpinner;

    private final DateFormat dateFormat;

    public RuleViewHolder(View itemView, AutoPlaylistEditActivity.Adapter adapter) {
        super(itemView);
        this.itemView = itemView;
        parent = adapter;

        ImageView removeButton = (ImageView) itemView.findViewById(R.id.instanceRemove);
        removeButton.setOnClickListener(this);

        typeDropDown = (AppCompatSpinner) itemView.findViewById(R.id.typeSelector);
        fieldDropDown = (AppCompatSpinner) itemView.findViewById(R.id.fieldSelector);

        valueText = (TextView) itemView.findViewById(R.id.valueText);
        valueTextWrapper = (FrameLayout) itemView.findViewById(R.id.valueTextWrapper);
        valueSpinner = (AppCompatSpinner) itemView.findViewById(R.id.valueSpinner);

        fieldDropDown.setAdapter(new FieldAdapter(itemView.getContext()));
        valueSpinner.setAdapter(new InstanceAdapter());

        typeDropDown.setOnItemSelectedListener(this);
        fieldDropDown.setOnItemSelectedListener(this);
        valueSpinner.setOnItemSelectedListener(this);

        valueTextWrapper.setOnClickListener(this);

        // Set up a SimpleDate format
        dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);
    }

    public void update(AutoPlaylist.Rule rule) {
        reference = rule;
        update();
    }

    public void update() {
        typeDropDown.setSelection(reference.type);

        FieldAdapter fieldAdapter = (FieldAdapter) fieldDropDown.getAdapter();
        fieldDropDown.setSelection(fieldAdapter.lookupIndex(reference.field, reference.match));

        if (reference.field == AutoPlaylist.Rule.Field.DATE_PLAYED
                || reference.field == AutoPlaylist.Rule.Field.DATE_ADDED) {

            Calendar c = Calendar.getInstance();
            try {
                c.setTimeInMillis(Long.parseLong(reference.value) * 1000);
            } catch (NumberFormatException e) {
                c.setTimeInMillis(System.currentTimeMillis());
                reference.value = Long.toString(c.getTimeInMillis() / 1000);
            }

            valueText.setText(dateFormat.format(c.getTime()));

        } else if (reference.field == AutoPlaylist.Rule.Field.ID) {
            InstanceAdapter valueAdapter = ((InstanceAdapter) valueSpinner.getAdapter());
            valueAdapter.setType(reference.type);
            valueSpinner.setSelection(
                    valueAdapter.lookupIndexForId(Long.parseLong(reference.value)));
        } else {
            valueText.setText(reference.value);
        }

        if (reference.field == AutoPlaylist.Rule.Field.ID) {
            valueTextWrapper.setVisibility(View.GONE);
            valueSpinner.setVisibility(View.VISIBLE);
        } else {
            valueTextWrapper.setVisibility(View.VISIBLE);
            valueSpinner.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.instanceRemove) {
            final int index = getAdapterPosition();

            parent.removeRule(index - 1);
            parent.notifyItemRemoved(index);

            Snackbar.make(v, "Removed rule", Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            parent.addRule(index - 1, reference);
                            parent.notifyItemInserted(index);
                        }
                    })
                    .show();
        } else if (v.getId() == R.id.valueTextWrapper) {
            // Show a date picker if relevant, otherwise use a regular AlertDialog to get user input
            if (reference.field == AutoPlaylist.Rule.Field.DATE_ADDED
                    || reference.field == AutoPlaylist.Rule.Field.DATE_PLAYED) {
                // Calculate the date stored in the reference
                Calendar calendar = Calendar.getInstance();
                try {
                    long timestamp = Long.parseLong(reference.value);
                    calendar.setTimeInMillis(timestamp * 1000l);
                } catch (NumberFormatException ignored) {
                    // If the reference's value isn't valid, just use the current time as the
                    // selected date
                    calendar.setTimeInMillis(System.currentTimeMillis());
                }

                DatePickerDialog dateDialog = new DatePickerDialog (
                        itemView.getContext(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                Calendar c = Calendar.getInstance();
                                c.set(year, monthOfYear, dayOfMonth);
                                reference.value = Long.toString(c.getTimeInMillis() / 1000l);
                                update();
                            }
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH));
                dateDialog.show();

            } else {
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

                final TextInputLayout inputLayout = new TextInputLayout(itemView.getContext());
                final AppCompatEditText editText = new AppCompatEditText(itemView.getContext());

                int type = ((FieldAdapter) fieldDropDown.getAdapter())
                        .getInputType(fieldDropDown.getSelectedItemPosition());

                editText.setInputType(type);
                inputLayout.addView(editText);

                final AlertDialog valueDialog = new AlertDialog.Builder(itemView.getContext())
                        .setMessage(
                                typeDropDown.getSelectedItem() + " "
                                        + fieldDropDown.getSelectedItem().toString().toLowerCase())
                        .setView(inputLayout)
                        .setNegativeButton(R.string.action_cancel, null)
                        .setPositiveButton(R.string.action_done, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (editText.getInputType() == InputType.TYPE_CLASS_NUMBER) {
                                    try {
                                        // Verify the input if this rule needs a numeric value
                                        reference.value = Integer.toString(Integer.parseInt(
                                                editText.getText().toString().trim()));

                                    } catch (NumberFormatException e) {
                                        // If the user inputted something that's not a number,
                                        // reset it to 0
                                        reference.value = "0";
                                    }
                                } else {
                                    reference.value = editText.getText().toString().trim();
                                }
                                update();
                            }
                        })
                        .create();

                valueDialog.getWindow()
                        .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

                valueDialog.show();

                int padding = (int) itemView.getResources().getDimension(R.dimen.alert_padding);
                ((View) inputLayout.getParent()).setPadding(
                        padding - inputLayout.getPaddingLeft(),
                        0,
                        padding - inputLayout.getPaddingRight(),
                        0);

                Themes.themeAlertDialog(valueDialog);

                editText.setText(reference.value);
                editText.setSelection(reference.value.length());
                editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == KeyEvent.KEYCODE_ENDCALL) {
                            valueDialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();
                        }
                        return false;
                    }
                });
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (view == null) return;

        // When the type is selected, update the available options in the fieldDropDown and update
        // the rule this viewHolder refers to
        if (view.getParent().equals(typeDropDown)) {
            ((FieldAdapter) fieldDropDown.getAdapter()).setType(position);

            if (reference.type != position) {
                ((InstanceAdapter) valueSpinner.getAdapter()).setType(position);
                valueSpinner.setSelection(0);
            }

            reference.type = position;
        }
        // When a field and match are chosen, update the rule that this viewholder refers to
        if (view.getParent().equals(fieldDropDown)) {
            FieldAdapter fieldAdapter = (FieldAdapter) fieldDropDown.getAdapter();
            final int originalField = reference.field;
            reference.field = fieldAdapter.getRuleField(position);
            reference.match = fieldAdapter.getRuleMatch(position);

            // If the field was switched from or to an ID match, reset the value
            if (originalField == AutoPlaylist.Rule.Field.ID
                    && reference.field != AutoPlaylist.Rule.Field.ID) {
                reference.value = "";
            } else if (originalField != AutoPlaylist.Rule.Field.ID
                    && reference.field == AutoPlaylist.Rule.Field.ID) {
                reference.value = "0";
            }
        }
        if (view.getParent().equals(valueSpinner)) {
            if (reference.field == AutoPlaylist.Rule.Field.ID) {
                reference.value = Long.toString(id);
            }
        }

        update();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    private static class FieldAdapter extends BaseAdapter {

        // These two int arrays correspond to which fields and match types apply to which index
        // int the field string array. Since fields and matches are interwoven in this array, they
        // are used to separate or merge these fields to bridge the gap between the options
        // available in the spinner and the format that the autoplaylist looks at field and match
        // options. Duplicates are intentional. Any changes to R.array.auto_plist_fields must
        // be updated here.
        private static final int[] fields = new int[]{
                AutoPlaylist.Rule.Field.ID,
                AutoPlaylist.Rule.Field.ID,
                AutoPlaylist.Rule.Field.NAME,
                AutoPlaylist.Rule.Field.NAME,
                AutoPlaylist.Rule.Field.NAME,
                AutoPlaylist.Rule.Field.NAME,
                AutoPlaylist.Rule.Field.PLAY_COUNT,
                AutoPlaylist.Rule.Field.PLAY_COUNT,
                AutoPlaylist.Rule.Field.PLAY_COUNT,
                AutoPlaylist.Rule.Field.SKIP_COUNT,
                AutoPlaylist.Rule.Field.SKIP_COUNT,
                AutoPlaylist.Rule.Field.SKIP_COUNT,
                AutoPlaylist.Rule.Field.DATE_ADDED,
                AutoPlaylist.Rule.Field.DATE_ADDED,
                AutoPlaylist.Rule.Field.DATE_ADDED,
                AutoPlaylist.Rule.Field.DATE_PLAYED,
                AutoPlaylist.Rule.Field.DATE_PLAYED,
                AutoPlaylist.Rule.Field.DATE_PLAYED
        };

        private static final int[] matches = new int[]{
                AutoPlaylist.Rule.Match.EQUALS,
                AutoPlaylist.Rule.Match.NOT_EQUALS,
                AutoPlaylist.Rule.Match.EQUALS,
                AutoPlaylist.Rule.Match.NOT_EQUALS,
                AutoPlaylist.Rule.Match.CONTAINS,
                AutoPlaylist.Rule.Match.NOT_CONTAINS,
                AutoPlaylist.Rule.Match.LESS_THAN,
                AutoPlaylist.Rule.Match.EQUALS,
                AutoPlaylist.Rule.Match.GREATER_THAN,
                AutoPlaylist.Rule.Match.LESS_THAN,
                AutoPlaylist.Rule.Match.EQUALS,
                AutoPlaylist.Rule.Match.GREATER_THAN,
                AutoPlaylist.Rule.Match.LESS_THAN,
                AutoPlaylist.Rule.Match.EQUALS,
                AutoPlaylist.Rule.Match.GREATER_THAN,
                AutoPlaylist.Rule.Match.LESS_THAN,
                AutoPlaylist.Rule.Match.EQUALS,
                AutoPlaylist.Rule.Match.GREATER_THAN
        };

        private static final int[] inputType = new int[] {
                InputType.TYPE_NULL,
                InputType.TYPE_NULL,
                InputType.TYPE_CLASS_TEXT,
                InputType.TYPE_CLASS_TEXT,
                InputType.TYPE_CLASS_TEXT,
                InputType.TYPE_CLASS_TEXT,
                InputType.TYPE_CLASS_NUMBER,
                InputType.TYPE_CLASS_NUMBER,
                InputType.TYPE_CLASS_NUMBER,
                InputType.TYPE_CLASS_NUMBER,
                InputType.TYPE_CLASS_NUMBER,
                InputType.TYPE_CLASS_NUMBER,
                InputType.TYPE_NULL,
                InputType.TYPE_NULL,
                InputType.TYPE_NULL,
                InputType.TYPE_NULL,
                InputType.TYPE_NULL,
                InputType.TYPE_NULL
        };

        private Context context;
        private int type;
        private final String[] songChoices;

        public FieldAdapter(Context context) {
            this.context = context;
            songChoices = context.getResources().getStringArray(R.array.auto_plist_fields);
        }

        public void setType(int type) {
            this.type = type;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (type == AutoPlaylist.Rule.Type.SONG) {
                return songChoices.length;
            }
            return 6; // Only the first 6 elements in the array are applicable to all data types
                      // the rest only apply to songs. Thus, use the full list if we have a song,
                      // otherwise, only show the first 6 options
        }

        @Override
        public Object getItem(int position) {
            return songChoices[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public int lookupIndex(int field, int match) {
            int index = 0;
            for (int i = 0; i < fields.length; i++) {
                if (fields[i] == field) {
                    index = i;
                    break;
                }
            }
            for (int i = index; i < matches.length; i++) {
                if (matches[i] == match) {
                    index = i;
                    break;
                }
            }
            return index;
        }

        public int getRuleField(int position) {
            return fields[position];
        }

        public int getRuleMatch(int position) {
            return matches[position];
        }

        public int getInputType(int position) {
            return inputType[position];
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater
                        .from(context)
                        .inflate(android.R.layout.simple_spinner_item, parent, false);
            }

            TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
            textView.setText(songChoices[position]);

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater
                        .from(context)
                        .inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            }

            TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
            textView.setText(songChoices[position]);

            return convertView;
        }
    }

    private static class InstanceAdapter extends BaseAdapter {

        private int type;

        public InstanceAdapter() {
            type = AutoPlaylist.Rule.Type.SONG;
        }

        @Override
        public int getCount() {
            switch (type) {
                case AutoPlaylist.Rule.Type.PLAYLIST:
                    return Library.getPlaylists().size();
                case AutoPlaylist.Rule.Type.SONG:
                    return Library.getSongs().size();
                case AutoPlaylist.Rule.Type.ARTIST:
                    return Library.getArtists().size();
                case AutoPlaylist.Rule.Type.ALBUM:
                    return Library.getAlbums().size();
                case AutoPlaylist.Rule.Type.GENRE:
                    return Library.getGenres().size();
            }
            return 0;
        }

        public void setType(int type) {
            this.type = type;
            notifyDataSetChanged();
        }

        @Override
        public Object getItem(int position) {
            switch (type) {
                case AutoPlaylist.Rule.Type.PLAYLIST:
                    return Library.getPlaylists().get(position);
                case AutoPlaylist.Rule.Type.SONG:
                    return Library.getSongs().get(position);
                case AutoPlaylist.Rule.Type.ARTIST:
                    return Library.getArtists().get(position);
                case AutoPlaylist.Rule.Type.ALBUM:
                    return Library.getAlbums().get(position);
                case AutoPlaylist.Rule.Type.GENRE:
                    return Library.getGenres().get(position);
            }
            return null;
        }

        public int lookupIndexForId(long id) {
            final int count = getCount();
            for (int i = 0; i < count; i++) {
                if (getItemId(i) == id) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public long getItemId(int position) {
            switch (type) {
                case AutoPlaylist.Rule.Type.PLAYLIST:
                    return Library.getPlaylists().get(position).playlistId;
                case AutoPlaylist.Rule.Type.SONG:
                    return Library.getSongs().get(position).songId;
                case AutoPlaylist.Rule.Type.ARTIST:
                    return Library.getArtists().get(position).artistId;
                case AutoPlaylist.Rule.Type.ALBUM:
                    return Library.getAlbums().get(position).albumId;
                case AutoPlaylist.Rule.Type.GENRE:
                    return Library.getGenres().get(position).genreId;
            }
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater
                        .from(parent.getContext())
                        .inflate(R.layout.instance_spinner, parent, false);
            }

            Object reference = getItem(position);

            TextView titleText = (TextView) convertView.findViewById(android.R.id.text1);
            TextView detailText = (TextView) convertView.findViewById(android.R.id.text2);

            titleText.setText(reference.toString());

            if (reference instanceof Song) {
                detailText.setText(((Song) reference).artistName);
            } else if (reference instanceof Album) {
                detailText.setText(((Album) reference).artistName);
            } else {
                detailText.setVisibility(View.GONE);
            }

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater
                        .from(parent.getContext())
                        .inflate(R.layout.instance_spinner_dropdown, parent, false);
            }

            Object reference = getItem(position);

            TextView titleText = (TextView) convertView.findViewById(android.R.id.text1);
            TextView detailText = (TextView) convertView.findViewById(android.R.id.text2);

            titleText.setText(reference.toString());

            if (reference instanceof Song) {
                detailText.setText(((Song) reference).artistName);
            } else if (reference instanceof Album) {
                detailText.setText(((Album) reference).artistName);
            } else {
                detailText.setVisibility(View.GONE);
            }

            return convertView;
        }
    }
}