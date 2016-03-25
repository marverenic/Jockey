package com.marverenic.music.fragments;

import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.marverenic.music.player.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

public class MiniplayerFragment extends Fragment implements PlayerController.UpdateListener,
        View.OnClickListener {

    private ImageView artworkImageView;
    private TextView songTextView;
    private TextView artistTextView;
    private ImageView playButton;
    private ProgressBar songProgress;

    private final MediaObserver observer = new MediaObserver(this);
    private Thread observerThread = new Thread(observer);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_miniplayer, container, false);

        artworkImageView = (ImageView) view.findViewById(R.id.imageArtwork);
        songTextView = (TextView) view.findViewById(R.id.textNowPlayingTitle);
        artistTextView = (TextView) view.findViewById(R.id.textNowPlayingDetail);
        playButton = (ImageView) view.findViewById(R.id.playButton);
        songProgress = (ProgressBar) view.findViewById(R.id.songProgress);

        view.setOnClickListener(this);
        playButton.setOnClickListener(this);
        view.findViewById(R.id.skipButton).setOnClickListener(this);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            LayerDrawable progressDrawable = (LayerDrawable) songProgress.getProgressDrawable();
            progressDrawable.findDrawableByLayerId(android.R.id.progress).setColorFilter(
                    Themes.getAccent(), PorterDuff.Mode.SRC_ATOP);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        PlayerController.registerUpdateListener(this);
        onUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        PlayerController.unregisterUpdateListener(this);
        observer.stop();
        observerThread = null;
    }

    @Override
    public void onUpdate() {
        final Song nowPlaying = PlayerController.getNowPlaying();

        if (nowPlaying != null) {
            if (PlayerController.getArtwork() != null) {
                artworkImageView.setImageBitmap(PlayerController.getArtwork());
            } else {
                artworkImageView.setImageResource(R.drawable.art_default);
            }

            songTextView.setText(nowPlaying.getSongName());
            artistTextView.setText(nowPlaying.getArtistName());

            songProgress.setMax(PlayerController.getDuration());

            if (PlayerController.isPlaying()) {
                playButton.setImageResource(R.drawable.ic_pause_32dp);

                if (!observer.isRunning()) {
                    observerThread = new Thread(observer);
                    observerThread.start();
                }
            } else {
                playButton.setImageResource(R.drawable.ic_play_arrow_32dp);
                songProgress.setProgress(PlayerController.getCurrentPosition());
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.miniplayer:
                Navigate.to(getActivity(), NowPlayingActivity.class);
                break;
            case R.id.playButton:
                PlayerController.togglePlay();
                break;
            case R.id.skipButton:
                PlayerController.skip();
                break;
        }
    }

    private static class MediaObserver implements Runnable, PlayerController.UpdateListener {

        private static final int UPDATE_FREQUENCY_MS = 200;

        private boolean run;
        private MiniplayerFragment parent;
        private final Runnable updater;

        MediaObserver(MiniplayerFragment parent) {
            this.parent = parent;

            updater = new Runnable() {
                @Override
                public void run() {
                    MediaObserver.this.parent.songProgress
                            .setProgress(PlayerController.getCurrentPosition());
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
                    Thread.sleep(UPDATE_FREQUENCY_MS);
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
