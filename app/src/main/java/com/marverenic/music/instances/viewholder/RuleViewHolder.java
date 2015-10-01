package com.marverenic.music.instances.viewholder;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AutoPlaylistEditor;
import com.marverenic.music.instances.AutoPlaylist;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class RuleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, AdapterView.OnItemSelectedListener, TextWatcher {

    private AutoPlaylistEditor.Adapter parent;
    private View itemView;
    private AutoPlaylist.Rule reference;

    private AppCompatSpinner typeDropDown;
    private AppCompatSpinner fieldDropDown;
    private AppCompatEditText editText;
    private TextInputLayout textInputLayout;

    private final DateFormat dateFormat;
    private final String datePattern;

    public RuleViewHolder(View itemView, AutoPlaylistEditor.Adapter adapter) {
        super(itemView);
        this.itemView = itemView;
        parent = adapter;

        ImageView menuButton = (ImageView) itemView.findViewById(R.id.instanceRemove);
        menuButton.setOnClickListener(this);

        typeDropDown = (AppCompatSpinner) itemView.findViewById(R.id.typeSelector);
        fieldDropDown = (AppCompatSpinner) itemView.findViewById(R.id.fieldSelector);

        editText = (AppCompatEditText) itemView.findViewById(R.id.valueEditText);
        textInputLayout = (TextInputLayout) itemView.findViewById(R.id.valueInputLayout);

        fieldDropDown.setAdapter(new FieldAdapter(itemView.getContext()));

        typeDropDown.setOnItemSelectedListener(this);
        fieldDropDown.setOnItemSelectedListener(this);

        editText.addTextChangedListener(this);

        // Set up a SimpleDate format
        dateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT);

        // Figure out the pattern of the dateFormat by parsing January 2, 1970...
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(24 * 60 * 60 * 1000 - c.getTimeZone().getRawOffset() + 1);
        // ... And converting the result into a pattern
        datePattern = dateFormat.format(c.getTime())
                .replace("19", "YY").replace("70", "YY").replace("1", "MM").replace("2", "DD");
    }

    public void update(AutoPlaylist.Rule rule) {
        reference = rule;

        typeDropDown.setSelection(rule.type);

        FieldAdapter fieldAdapter = (FieldAdapter) fieldDropDown.getAdapter();
        fieldDropDown.setSelection(fieldAdapter.lookupIndex(rule.field, rule.match));

        if (rule.field == AutoPlaylist.Rule.Field.DATE_PLAYED || rule.field == AutoPlaylist.Rule.Field.DATE_ADDED) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(Integer.parseInt(rule.value) * 1000);
            editText.setText(dateFormat.format(c.getTime()));
        } else {
            editText.setText(rule.value);
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
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (view == null) return;

        // When the type is selected, update the available options in the fieldDropDown and update
        // the rule this viewHolder refers to
        if (view.getParent().equals(typeDropDown)) {
            ((FieldAdapter) fieldDropDown.getAdapter()).setType(position);
            reference.type = position;
        }
        // When a field and match are chosen, update the rule that this viewholder refers to
        if (view.getParent().equals(fieldDropDown)) {
            FieldAdapter fieldAdapter = (FieldAdapter) fieldDropDown.getAdapter();
            reference.field = fieldAdapter.getRuleField(position);
            reference.match = fieldAdapter.getRuleMatch(position);

            editText.setInputType(fieldAdapter.getInputType(position));
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if ((editText.getInputType() & InputType.TYPE_CLASS_DATETIME) == InputType.TYPE_CLASS_DATETIME) {
            // Handle calendar conversions
            try {
                Calendar c = Calendar.getInstance();
                c.setTime(dateFormat.parse(s.toString()));
                // Take year shorthand into account
                final int year = c.get(Calendar.YEAR);
                if (year < 100) {
                    int currentYear = Calendar.getInstance().get(Calendar.YEAR) % 100;
                    int century = currentYear / 100; // The one century offset doesn't matter here
                    int lowYear = year + (century - 1) * 100;
                    int highYear = year + century * 100;

                    // Whichever year is closer to the current date is more likely, favoring
                    // dates in the past
                    if (year - lowYear <= highYear - year) {
                        c.set(Calendar.YEAR, lowYear);
                    } else {
                        c.set(Calendar.YEAR, highYear);
                    }
                }

                reference.value = Long.toString(c.getTimeInMillis() / 1000);
                textInputLayout.setError(null);
            } catch (ParseException e) {
                if (!datePattern.equals(textInputLayout.getError())) {
                    textInputLayout.setError(datePattern);
                }
                Crashlytics.logException(e);
                e.printStackTrace();
            }
        } else {
            // Handle all other conversions
            reference.value = s.toString();
            textInputLayout.setError(null); // TODO show more errors
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

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
                InputType.TYPE_CLASS_NUMBER, // TODO implement ids
                InputType.TYPE_CLASS_NUMBER, // TODO implement ids
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
                InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE,
                InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE,
                InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE,
                InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE,
                InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE,
                InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE
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
}