package com.marverenic.music.instances.viewholder;

import android.content.Context;
import android.support.design.widget.Snackbar;
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

import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AutoPlaylistEditor;
import com.marverenic.music.instances.AutoPlaylist;

public class RuleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, AdapterView.OnItemSelectedListener, TextWatcher {

    AutoPlaylistEditor.Adapter parent;
    View itemView;
    AutoPlaylist.Rule reference;

    AppCompatSpinner typeDropDown;
    AppCompatSpinner fieldDropDown;
    AppCompatEditText editText;

    public RuleViewHolder(View itemView, AutoPlaylistEditor.Adapter adapter) {
        super(itemView);
        this.itemView = itemView;
        parent = adapter;

        ImageView menuButton = (ImageView) itemView.findViewById(R.id.instanceRemove);
        menuButton.setOnClickListener(this);

        typeDropDown = (AppCompatSpinner) itemView.findViewById(R.id.typeSelector);
        fieldDropDown = (AppCompatSpinner) itemView.findViewById(R.id.fieldSelector);
        editText = (AppCompatEditText) itemView.findViewById(R.id.valueEditText);

        fieldDropDown.setAdapter(new FieldAdapter(itemView.getContext()));

        typeDropDown.setOnItemSelectedListener(this);
        fieldDropDown.setOnItemSelectedListener(this);

        editText.addTextChangedListener(this);
    }

    public void update(AutoPlaylist.Rule rule) {
        reference = rule;

        typeDropDown.setSelection(rule.type);

        FieldAdapter fieldAdapter = (FieldAdapter) fieldDropDown.getAdapter();
        fieldDropDown.setSelection(fieldAdapter.lookupIndex(rule.field, rule.match));

        editText.setText(rule.value);
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

            if (editText.getInputType() != fieldAdapter.getInputType(position)) {
                // If the input type changes, clear the text that the user has entered and change
                // the input type
                editText.setText("");
                editText.setInputType(fieldAdapter.getInputType(position));
            }
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
        reference.value = s.toString();
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
                InputType.TYPE_CLASS_NUMBER, // TODO implement dates
                InputType.TYPE_CLASS_NUMBER,
                InputType.TYPE_CLASS_NUMBER,
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