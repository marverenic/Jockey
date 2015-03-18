package com.marverenic.music.fragments;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Build;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.marverenic.music.NowPlayingActivity;
import com.marverenic.music.PlayerService;
import com.marverenic.music.R;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

// A static class that manipulates the Miniplayer view
public class MiniplayerManager {

    public static void show(Activity activity, int contentViewId){
        RelativeLayout.LayoutParams contentLayoutParams = (RelativeLayout.LayoutParams) (activity.findViewById(contentViewId)).getLayoutParams();
        contentLayoutParams.bottomMargin = activity.getResources().getDimensionPixelSize(R.dimen.now_playing_ticker_height);
        (activity.findViewById(contentViewId)).setLayoutParams(contentLayoutParams);

        FrameLayout.LayoutParams playerLayoutParams = (FrameLayout.LayoutParams) (activity.findViewById(R.id.miniplayer)).getLayoutParams();
        playerLayoutParams.height = activity.getResources().getDimensionPixelSize(R.dimen.now_playing_ticker_height);
        (activity.findViewById(R.id.miniplayer)).setLayoutParams(playerLayoutParams);
    }

    public static void hide(Activity activity, int contentViewId){
        RelativeLayout.LayoutParams contentLayoutParams = (RelativeLayout.LayoutParams) (activity.findViewById(contentViewId)).getLayoutParams();
        contentLayoutParams.bottomMargin = 0;
        (activity.findViewById(contentViewId)).setLayoutParams(contentLayoutParams);

        FrameLayout.LayoutParams playerLayoutParams = (FrameLayout.LayoutParams) (activity.findViewById(R.id.miniplayer)).getLayoutParams();
        playerLayoutParams.height = 0;
        (activity.findViewById(R.id.miniplayer)).setLayoutParams(playerLayoutParams);
    }

    public static void update(Activity activity, int contentViewId){
        if (PlayerService.isInitialized() && PlayerService.getNowPlaying() != null) {
            final TextView songTitle = (TextView) activity.findViewById(R.id.textNowPlayingTitle);
            final TextView artistName = (TextView) activity.findViewById(R.id.textNowPlayingDetail);

            songTitle.setText(PlayerService.getNowPlaying().songName);
            artistName.setText(PlayerService.getNowPlaying().artistName);

            if (!(PlayerService.isPlaying() || PlayerService.isPreparing())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ((ImageButton) activity.findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_play);
                    ((ImageButton) activity.findViewById(R.id.playButton)).setImageTintList(ColorStateList.valueOf(Themes.getListText()));
                } else {
                    if (Themes.isLight(activity)) {
                        ((ImageButton) activity.findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_miniplayer_light);
                    } else {
                        ((ImageButton) activity.findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_miniplayer);
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ((ImageButton) activity.findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_pause);
                    ((ImageButton) activity.findViewById(R.id.playButton)).setImageTintList(ColorStateList.valueOf(Themes.getListText()));
                } else {
                    if (Themes.isLight(activity)) {
                        ((ImageButton) activity.findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause_miniplayer_light);
                    } else {
                        ((ImageButton) activity.findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause_miniplayer);
                    }
                }
            }

            if (PlayerService.getArt() != null) {
                ((ImageView) activity.findViewById(R.id.imageArtwork)).setImageBitmap(PlayerService.getArt());
            } else {
                ((ImageView) activity.findViewById(R.id.imageArtwork)).setImageResource(R.drawable.art_default);
            }

            show(activity, contentViewId);
        } else {
            hide(activity, contentViewId);
        }
    }

    public static void onClick(int viewId, Activity activity, int contentViewId){
        switch (viewId) {
            case R.id.miniplayer:
                Navigate.to(activity, NowPlayingActivity.class);
                break;
            case R.id.playButton:
                PlayerService.togglePlay();
                break;
            case R.id.skipButton:
                PlayerService.skip();
                break;
        }
        update(activity, contentViewId);
    }

}
