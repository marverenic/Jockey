package com.marverenic.music.instances;


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

import com.marverenic.music.R;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDialog {

    public static final class MakeNormal implements DialogInterface.OnClickListener, TextWatcher {

        private Context mContext;
        private View mReturnView;
        private List<Song> mData;
        private TextInputLayout mInputLayout;
        private AppCompatEditText mEditText;
        private AlertDialog mDialog;

        public static void alert(View view) {
            alert(view, null);
        }

        public static void alert(View view, List<Song> songs) {
            new MakeNormal(view, songs).prompt();
        }

        private MakeNormal(View view, List<Song> songs) {
            mContext = view.getContext();
            mReturnView = view;
            mData = songs;
        }

        private void buildLayout() {
            mInputLayout = new TextInputLayout(mContext);
            mEditText = new AppCompatEditText(mContext);
            mEditText.setInputType(InputType.TYPE_CLASS_TEXT);
            mEditText.setHint(R.string.hint_playlist_name);
            mInputLayout.addView(mEditText);
            mInputLayout.setErrorEnabled(true);
            mEditText.addTextChangedListener(this);
        }

        private void prompt() {
            buildLayout();

            mDialog = new AlertDialog.Builder(mContext)
                    .setTitle(R.string.header_create_playlist)
                    .setView(mInputLayout)
                    .setPositiveButton(R.string.action_create, this)
                    .setNegativeButton(R.string.action_cancel, this)
                    .show();

            mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            //noinspection deprecation
            mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    mContext.getResources().getColor(R.color.secondary_text_disabled));

            int padding = (int) mContext.getResources().getDimension(R.dimen.alert_padding);
            ((View) mInputLayout.getParent()).setPadding(
                    padding - mInputLayout.getPaddingLeft(),
                    padding,
                    padding - mInputLayout.getPaddingRight(),
                    mInputLayout.getPaddingBottom());
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                Library.createPlaylist(mReturnView, mEditText.getText().toString(), mData);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String error = Library.verifyPlaylistName(mContext, s.toString());
            mInputLayout.setError(error);
            mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
                    error == null && s.length() > 0);
            if (error == null && s.length() > 0) {
                mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Themes.getAccent());
            } else {
                //noinspection deprecation
                mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                        mContext.getResources().getColor(R.color.secondary_text_disabled));
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    public static final class AddToNormal implements DialogInterface.OnClickListener {

        private Context mContext;
        private View mReturnView;
        private List<Song> mData;
        private Song mSingleData;
        private List<Playlist> mChoices;

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

        public static void alert(View view, List<Song> songs, @StringRes int header) {
            alert(view, songs, view.getContext().getString(header));
        }

        public static void alert(View view, List<Song> songs, String header) {
            new AddToNormal(view, songs).prompt(header);
        }

        private AddToNormal(View view) {
            this.mContext = view.getContext();
            this.mReturnView = view;

            getChoices();
        }

        private AddToNormal(View view, List<Song> data) {
            this(view);
            this.mData = data;
        }

        private AddToNormal(View view, Song data) {
            this(view);
            this.mSingleData = data;
        }

        private void getChoices() {
            mChoices = new ArrayList<>();
            mChoices.add(new Playlist(-1,
                    mContext.getResources().getString(R.string.action_make_new_playlist)));

            for (Playlist p : Library.getPlaylists()) {
                if (!(p instanceof AutoPlaylist)) {
                    mChoices.add(p);
                }
            }
        }

        public void prompt(String header) {
            String[] playlistNames = new String[mChoices.size()];

            for (int i = 0; i < mChoices.size(); i++) {
                playlistNames[i] = mChoices.get(i).toString();
            }
            new AlertDialog.Builder(mContext)
                    .setTitle(header)
                    .setItems(playlistNames, this)
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == 0) {
                if (mSingleData != null) {
                    List<Song> wrappedSong = new ArrayList<>();
                    wrappedSong.add(mSingleData);

                    new MakeNormal(mReturnView, wrappedSong).prompt();
                } else {
                    new MakeNormal(mReturnView, mData).prompt();
                }
            } else {
                if (mSingleData != null) {
                    Library.addPlaylistEntry(
                            mContext,
                            mChoices.get(which),
                            mSingleData);
                } else {
                    Library.addPlaylistEntries(
                            mReturnView,
                            mChoices.get(which),
                            mData);
                }
            }
        }
    }

}
