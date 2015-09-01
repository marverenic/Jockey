package com.marverenic.music.utils;


import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;

import com.marverenic.music.Library;
import com.marverenic.music.R;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.util.ArrayList;

public class PlaylistDialog {

    public static class MakeNormal implements DialogInterface.OnClickListener, TextWatcher {

        private Context context;
        private View snackbarReturnView;
        private ArrayList<Song> data;
        private TextInputLayout inputLayout;
        private AppCompatEditText editText;
        private AlertDialog dialog;

        public static void alert(View view) {
            alert(view, null);
        }

        public static void alert(View view, ArrayList<Song> songs) {
            new MakeNormal(view, songs).prompt();
        }

        private MakeNormal(View view, ArrayList<Song> songs) {
            context = view.getContext();
            snackbarReturnView = view;
            data = songs;
        }

        private void buildLayout(){
            inputLayout = new TextInputLayout(context);
            editText = new AppCompatEditText(context);
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            editText.setHint(R.string.hint_playlist_name);
            inputLayout.addView(editText);
            inputLayout.setErrorEnabled(true);
            editText.addTextChangedListener(this);
        }

        private void prompt(){
            buildLayout();

            dialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.header_create_playlist)
                    .setView(inputLayout)
                    .setPositiveButton(R.string.action_create, this)
                    .setNegativeButton(R.string.action_cancel, this)
                    .show();

            Themes.themeAlertDialog(dialog);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            //noinspection deprecation
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    context.getResources().getColor((Themes.isLight(context)
                            ? R.color.secondary_text_disabled_material_light
                            : R.color.secondary_text_disabled_material_dark)));

            int padding = (int) context.getResources().getDimension(R.dimen.alert_padding);
            ((View) inputLayout.getParent()).setPadding(
                    padding - inputLayout.getPaddingLeft(),
                    padding,
                    padding - inputLayout.getPaddingRight(),
                    inputLayout.getPaddingBottom());
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    Library.createPlaylist(snackbarReturnView, editText.getText().toString(), data);
                    break;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String error = Library.verifyPlaylistName(context, s.toString());
            inputLayout.setError(error);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(error == null && s.length() > 0);
            if (error == null && s.length() > 0){
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Themes.getAccent());
            }
            else{
                //noinspection deprecation
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                        context.getResources().getColor((Themes.isLight(context)
                                ? R.color.secondary_text_disabled_material_light
                                : R.color.secondary_text_disabled_material_dark)));
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }

    public static class AddToNormal implements DialogInterface.OnClickListener {

        private Context context;
        private View snackbarReturnView;
        private ArrayList<Song> data;
        private Song singleData;
        private ArrayList<Playlist> choices;

        public static void alert(View view, @StringRes int header) {
            alert(view, view.getContext().getString(header));
        }

        public static void alert(View view, String header) {
            new AddToNormal(view).prompt(header);
        }

        public static void alert(View view, Song song, @StringRes int header) {
            alert(view, song, view.getContext().getString(header));
        }

        public static void alert(View view, Song song, String header) {
            new AddToNormal(view, song).prompt(header);
        }

        public static void alert(View view, ArrayList<Song> songs, @StringRes int header) {
            alert(view, songs, view.getContext().getString(header));
        }

        public static void alert(View view, ArrayList<Song> songs, String header) {
            new AddToNormal(view, songs).prompt(header);
        }

        private AddToNormal(View view){
            this.context = view.getContext();
            this.snackbarReturnView = view;

            getChoices();
        }

        private AddToNormal(View view, ArrayList<Song> data){
            this(view);
            this.data = data;
        }

        private AddToNormal(View view, Song data){
            this(view);
            this.singleData = data;
        }

        private void getChoices(){
            choices = new ArrayList<>();
            choices.add(new Playlist(-1,
                    context.getResources().getString(R.string.action_make_new_playlist)));

            for (Playlist p : Library.getPlaylists()){
                if (!(p instanceof AutoPlaylist)) choices.add(p);
            }
        }

        public void prompt(String header){
            String[] playlistNames = new String[choices.size()];

            for (int i = 0; i < choices.size(); i++ ){
                playlistNames[i] = choices.get(i).toString();
            }
            final AlertDialog addToPlaylistDialog = new AlertDialog.Builder(context)
                    .setTitle(header)
                    .setItems(playlistNames, this)
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();

            Themes.themeAlertDialog(addToPlaylistDialog);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == 0){
                if (singleData != null) {
                    ArrayList<Song> wrappedSong = new ArrayList<>();
                    wrappedSong.add(singleData);

                    new MakeNormal(snackbarReturnView, wrappedSong).prompt();
                } else {
                    new MakeNormal(snackbarReturnView, data).prompt();
                }
            } else {
                if (singleData != null){
                    Library.addPlaylistEntry(
                            context,
                            choices.get(which),
                            singleData);
                } else {
                    Library.addPlaylistEntries(
                            snackbarReturnView,
                            choices.get(which),
                            data);
                }
            }
        }
    }

    public static class MakeAuto implements DialogInterface.OnClickListener, TextWatcher {

        private Context context;
        private View snackbarReturnView;
        private AlertDialog dialog;

        public static void alert(View view){
            new MakeAuto(view).prompt();
        }

        private MakeAuto(View view){
            context = view.getContext();
            snackbarReturnView = view;
        }

        private void buildLayout(){

        }

        private void prompt(){
            buildLayout();

            dialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.header_create_playlist)
                            //.setView(view)
                    .setPositiveButton(R.string.action_create, this)
                    .setNegativeButton(R.string.action_cancel, this)
                    .show();

            Themes.themeAlertDialog(dialog);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // TODO
                    break;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String error = Library.verifyPlaylistName(context, s.toString());
            //TODO inputLayout.setError(error);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(error == null && s.length() > 0);
            if (error == null && s.length() > 0){
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Themes.getAccent());
            }
            else{
                //noinspection deprecation
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                        context.getResources().getColor((Themes.isLight(context)
                                ? R.color.secondary_text_disabled_material_light
                                : R.color.secondary_text_disabled_material_dark)));
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }

}
