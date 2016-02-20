package com.marverenic.music.fragments;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.PlaylistDialog;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.TimeView;

public class PlayerControllerFragment extends Fragment implements PlayerController.UpdateListener,
        View.OnClickListener, PopupMenu.OnMenuItemClickListener, SeekBar.OnSeekBarChangeListener {

    private TextView songTitle;
    private TextView songArtist;
    private TextView songAlbum;

    private ImageButton moreInfoButton;
    private ImageButton skipPrevButton;
    private ImageButton skipNextButton;
    private ImageButton playPauseButton;

    private SeekBar seekBar;
    private TimeView timePosition;
    private TimeView timeDuration;
    private TimeView scrubberThumb;

    private final MediaObserver observer = new MediaObserver(this);
    private Thread observerThread = new Thread(observer);
    private boolean userTouchingProgressBar = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player_controls, container, false);

        songTitle = (TextView) view.findViewById(R.id.textSongTitle);
        songArtist = (TextView) view.findViewById(R.id.textArtistName);
        songAlbum = (TextView) view.findViewById(R.id.textAlbumTitle);

        moreInfoButton = (ImageButton) view.findViewById(R.id.songDetail);
        skipNextButton = (ImageButton) view.findViewById(R.id.nextButton);
        skipPrevButton = (ImageButton) view.findViewById(R.id.previousButton);
        playPauseButton = (ImageButton) view.findViewById(R.id.playButton);

        moreInfoButton.setOnClickListener(this);
        skipNextButton.setOnClickListener(this);
        skipPrevButton.setOnClickListener(this);
        playPauseButton.setOnClickListener(this);

        seekBar = (SeekBar) view.findViewById(R.id.songSeekBar);
        timePosition = (TimeView) view.findViewById(R.id.songTimeCurr);
        timeDuration = (TimeView) view.findViewById(R.id.songTimeMax);
        scrubberThumb = (TimeView) view.findViewById(R.id.seekThumb);

        seekBar.setOnSeekBarChangeListener(this);
        scrubberThumb.getBackground().setColorFilter(Themes.getAccent(), PorterDuff.Mode.SRC_IN);

        ((LayerDrawable) seekBar.getProgressDrawable())
                .findDrawableByLayerId(android.R.id.background)
                .setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC_IN);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        PlayerController.unregisterUpdateListener(this);
        observer.stop();
        observerThread = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        PlayerController.registerUpdateListener(this);
        onUpdate();
    }

    @Override
    public void onUpdate() {
        Song nowPlaying = PlayerController.getNowPlaying();
        if (nowPlaying != null) {
            songTitle.setText(nowPlaying.getSongName());
            songArtist.setText(nowPlaying.getArtistName());
            songAlbum.setText(nowPlaying.getAlbumName());

            int duration = PlayerController.getDuration();
            timeDuration.setTime(duration);
            seekBar.setMax(duration);

            if (!observer.isRunning()) {
                observerThread = new Thread(observer);
                observerThread.start();
            }
        } else {
            songTitle.setText(R.string.nothing_playing);
            songArtist.setText(R.string.unknown_artist);
            songAlbum.setText(R.string.unknown_album);
        }

        seekBar.setEnabled(nowPlaying != null);

        if (PlayerController.isPlaying()) {
            playPauseButton.setImageResource(R.drawable.ic_pause_36dp);
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play_arrow_36dp);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == playPauseButton) {
            PlayerController.togglePlay();
        } else if (v == skipPrevButton) {
            PlayerController.previous();
        } else if (v == skipNextButton) {
            PlayerController.skip();
        } else if (v == moreInfoButton) {
            // Song info
            final Song nowPlaying = PlayerController.getNowPlaying();

            if (nowPlaying != null) {
                final PopupMenu menu = new PopupMenu(getContext(), v, Gravity.END);
                String[] options = getResources().getStringArray(R.array.now_playing_options);

                for (int i = 0; i < options.length;  i++) {
                    menu.getMenu().add(Menu.NONE, i, i, options[i]);
                }
                menu.setOnMenuItemClickListener(this);
                menu.show();
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final Song nowPlaying = PlayerController.getNowPlaying();
        if (nowPlaying == null) {
            return false;
        }

        switch (item.getItemId()) {
            case 0: //Go to artist
                Artist artist = Library.findArtistById(nowPlaying.getArtistId());
                Navigate.to(getContext(), ArtistActivity.class, ArtistActivity.ARTIST_EXTRA, artist);
                return true;
            case 1: //Go to album
                Album album = Library.findAlbumById(nowPlaying.getAlbumId());

                Navigate.to(getContext(), AlbumActivity.class, AlbumActivity.ALBUM_EXTRA, album);
                return true;
            case 2: //Add to playlist
                PlaylistDialog.AddToNormal.alert(
                        getView(),
                        nowPlaying,
                        getString(
                                R.string.header_add_song_name_to_playlist,
                                nowPlaying.getSongName()));
                return true;
        }
        return false;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (!userTouchingProgressBar) {
                // For keyboards and non-touch based things
                onStartTrackingTouch(seekBar);
                onStopTrackingTouch(seekBar);
            } else {
                alignSeekThumb();
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        userTouchingProgressBar = true;
        alignSeekThumb();
        showSeekThumb();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        PlayerController.seek(seekBar.getProgress());
        userTouchingProgressBar = false;
        hideSeekThumb();
    }

    private void showSeekThumb() {
        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.slider_thumb_in);
        anim.setDuration(300);
        anim.setInterpolator(getContext(), android.R.interpolator.decelerate_quint);

        scrubberThumb.startAnimation(anim);
        scrubberThumb.setVisibility(View.VISIBLE);
    }

    private void hideSeekThumb() {
        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.slider_thumb_out);
        anim.setDuration(300);
        anim.setInterpolator(getContext(), android.R.interpolator.accelerate_quint);

        scrubberThumb.startAnimation(anim);

        // Make sure to hide the thumb after the animation finishes
        scrubberThumb.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!userTouchingProgressBar) scrubberThumb.setVisibility(View.INVISIBLE);
            }
        }, 300);
    }

    private void alignSeekThumb() {
        scrubberThumb.setTime(seekBar.getProgress());
        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) scrubberThumb.getLayoutParams();

        double progress = seekBar.getProgress() / (double) seekBar.getMax();
        int leftOffset = (int) (seekBar.getWidth() * progress) - scrubberThumb.getWidth() / 2;
        leftOffset = Math.min(leftOffset, seekBar.getWidth() - scrubberThumb.getWidth());
        leftOffset = Math.max(leftOffset, 0);

        params.setMargins(leftOffset, params.topMargin, params.rightMargin, params.bottomMargin);
        scrubberThumb.setLayoutParams(params);
    }

    private static class MediaObserver implements Runnable, PlayerController.UpdateListener {

        private boolean run;
        private PlayerControllerFragment parent;
        private final Runnable updater;

        MediaObserver(PlayerControllerFragment parent) {
            this.parent = parent;

            updater = new Runnable() {
                @Override
                public void run() {
                    int position = PlayerController.getCurrentPosition();
                    if (!MediaObserver.this.parent.userTouchingProgressBar) {
                        MediaObserver.this.parent.seekBar.setProgress(position);
                    }

                    MediaObserver.this.parent.timePosition.setTime(position);
                }
            };
        }

        public void stop() {
            run = false;
        }

        public boolean isRunning() {
            return run;
        }

        @Override
        public void run() {
            run = true;
            while (run) {
                parent.getActivity().runOnUiThread(updater);
                try {
                    Thread.sleep(200);
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        public void onUpdate() {
            final boolean wasRunning = run;
            run = PlayerController.isPlaying();
            if (!wasRunning && run) {
                parent.observerThread.run();
            }
        }
    }
}
