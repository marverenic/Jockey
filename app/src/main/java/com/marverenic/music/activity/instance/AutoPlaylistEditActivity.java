package com.marverenic.music.activity.instance;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.marverenic.music.Library;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.viewholder.RuleHeaderViewHolder;
import com.marverenic.music.instances.viewholder.RuleViewHolder;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.ArrayList;
import java.util.Collections;

public class AutoPlaylistEditActivity extends BaseActivity {

    public static final String PLAYLIST_EXTRA = "auto-playlist";

    private AutoPlaylist reference;
    private AutoPlaylist editedReference;
    private ArrayList<AutoPlaylist.Rule> editedRules;
    private Adapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance);

        reference = getIntent().getParcelableExtra(PLAYLIST_EXTRA);

        if (getSupportActionBar() != null){
            if (reference == null){
                getSupportActionBar().setTitle(R.string.playlist_auto_new);
            } else {
                getSupportActionBar().setTitle(reference.playlistName);
            }
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_done_24dp);
        }

        if (reference == null) {
            reference = AutoPlaylist.EMPTY;
        }
        if (editedReference == null || editedRules == null) {
            editedReference = new AutoPlaylist(reference);
            editedRules = new ArrayList<>(reference.getRules().length);
            Collections.addAll(editedRules, editedReference.getRules());
        }
        adapter = new Adapter(editedReference, editedRules);

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        list.addItemDecoration(new DividerDecoration(this));
        list.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_auto_playlist_editor, menu);
        return true;
    }

    public void saveChanges() {
        AutoPlaylist.Rule[] modifiedRules = new AutoPlaylist.Rule[editedRules.size()];
        editedReference.rules = editedRules.toArray(modifiedRules);
        if (reference.playlistId == AutoPlaylist.EMPTY.playlistId) {
            Library.makeAutoPlaylist(this, editedReference);
        } else {
            Library.editAutoPlaylist(this, editedReference);
        }
    }

    private boolean rulesChanged() {
        if (editedRules.size() != reference.getRules().length){
            return true;
        }

        for (int i = 0; i < editedRules.size(); i++) {
            if (!reference.getRules()[i].equals(editedRules.get(i))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.add:
                adapter.rules.add(AutoPlaylist.Rule.EMPTY);
                adapter.notifyItemInserted(adapter.rules.size());
                return true;
            case R.id.discard:
                if (rulesChanged()) {
                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setMessage("Discard changes?")
                            .setPositiveButton("Discard", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setNegativeButton(R.string.action_cancel, null)
                            .show();

                    Themes.themeAlertDialog(dialog);
                }
                return true;
            case android.R.id.home:
                if (rulesChanged()) {
                    saveChanges();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (rulesChanged()) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage("Save changes?")
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            saveChanges();
                            Navigate.back(AutoPlaylistEditActivity.this);
                        }
                    })
                    .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Navigate.back(AutoPlaylistEditActivity.this);
                        }
                    })
                    .setNeutralButton("Cancel", null)
                    .show();

            Themes.themeAlertDialog(dialog);
        } else {
            super.onBackPressed();
        }
    }

    public static class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int HEADER_VIEW = 0;
        private static final int RULE_VIEW = 1;

        private final ArrayList<AutoPlaylist.Rule> rules;
        private AutoPlaylist reference;

        public Adapter(AutoPlaylist playlist, ArrayList<AutoPlaylist.Rule> editedRules) {
            reference = playlist;
            rules = editedRules;
        }

        public void removeRule(int index) {
            rules.remove(index);
        }

        public void addRule(AutoPlaylist.Rule r) {
            rules.add(r);
        }

        public void addRule(int index, AutoPlaylist.Rule r) {
            rules.add(index, r);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case HEADER_VIEW:
                    return new RuleHeaderViewHolder(
                            LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.instance_rules_header, parent, false),
                            reference);
                case RULE_VIEW:
                default:
                    return new RuleViewHolder(
                            LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.instance_rule, parent, false),
                            this);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof RuleViewHolder) {
                ((RuleViewHolder) holder).update(rules.get(position - 1));
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) return HEADER_VIEW;
            return RULE_VIEW;
        }

        @Override
        public int getItemCount() {
            return rules.size() + 1;
        }

    }
}