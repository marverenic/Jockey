package com.marverenic.music.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.Player;
import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

public abstract class BaseActivity extends FragmentActivity implements View.OnClickListener {

    @LayoutRes private int layoutResID = -1;
    @IdRes private int contentResId = -1;

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Themes.setTheme(this);
        super.setContentView(layoutResID);
        themeActivity();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        PlayerController.bind(getApplicationContext());
    }

    public void setContentLayout(@LayoutRes int layoutResID){
        this.layoutResID = layoutResID;
    }

    @Override
    public void setContentView(@IdRes int contentResId){
        this.contentResId = contentResId;
    }

    @Override
    public void onResume(){
        super.onResume();
        Themes.setApplicationIcon(this);
        registerReceiver(updateReceiver, new IntentFilter(Player.UPDATE_BROADCAST));
        updateMiniplayer();
        update();
        ((JockeyApplication) getApplication()).activityResumed();
    }

    @Override
    public void onPause(){
        super.onPause();
        try {
            unregisterReceiver(updateReceiver);
        } catch (Exception ignored) {}
        ((JockeyApplication) getApplication()).activityPaused();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        ((JockeyApplication) getApplication()).activityDestroyed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            Navigate.up(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Navigate.back(this);
    }

    public abstract void themeActivity();

    public void update(){
        updateMiniplayer();
    }

    @SuppressWarnings("ResourceType")
    public void updateMiniplayer(){
        if (contentResId != -1) {
            Song nowPlaying = PlayerController.getNowPlaying();
            if (nowPlaying != null) {
                final TextView songTitle = (TextView) findViewById(R.id.textNowPlayingTitle);
                final TextView artistName = (TextView) findViewById(R.id.textNowPlayingDetail);

                songTitle.setText(nowPlaying.songName);
                artistName.setText(nowPlaying.artistName);

                if (!(PlayerController.isPlaying() || PlayerController.isPreparing())) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_play);
                        ((ImageButton) findViewById(R.id.playButton)).setImageTintList(ColorStateList.valueOf(Themes.getListText()));
                    } else {
                        if (Themes.isLight(this)) {
                            ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_miniplayer_light);
                        } else {
                            ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_miniplayer);
                        }
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_pause);
                        ((ImageButton) findViewById(R.id.playButton)).setImageTintList(ColorStateList.valueOf(Themes.getListText()));
                    } else {
                        if (Themes.isLight(this)) {
                            ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause_miniplayer_light);
                        } else {
                            ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause_miniplayer);
                        }
                    }
                }

                if (PlayerController.getArt() != null) {
                    ((ImageView) findViewById(R.id.imageArtwork)).setImageBitmap(PlayerController.getArt());
                } else {
                    ((ImageView) findViewById(R.id.imageArtwork)).setImageResource(R.drawable.art_default);
                }

                RelativeLayout.LayoutParams contentLayoutParams = (RelativeLayout.LayoutParams) findViewById(contentResId).getLayoutParams();
                contentLayoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.now_playing_ticker_height);
                findViewById(contentResId).setLayoutParams(contentLayoutParams);

                FrameLayout.LayoutParams playerLayoutParams = (FrameLayout.LayoutParams) (findViewById(R.id.miniplayer)).getLayoutParams();
                playerLayoutParams.height = getResources().getDimensionPixelSize(R.dimen.now_playing_ticker_height);
                findViewById(R.id.miniplayer).setLayoutParams(playerLayoutParams);
            } else {
                RelativeLayout.LayoutParams contentLayoutParams = (RelativeLayout.LayoutParams) (findViewById(contentResId)).getLayoutParams();
                contentLayoutParams.bottomMargin = 0;
                findViewById(contentResId).setLayoutParams(contentLayoutParams);

                FrameLayout.LayoutParams playerLayoutParams = (FrameLayout.LayoutParams) (findViewById(R.id.miniplayer)).getLayoutParams();
                playerLayoutParams.height = 0;
                findViewById(R.id.miniplayer).setLayoutParams(playerLayoutParams);
            }
        }
    }

    @Override
    public void onClick(View view){
        switch (view.getId()) {
            case R.id.miniplayer:
                Navigate.to(this, NowPlayingActivity.class);
                break;
            case R.id.playButton:
                PlayerController.togglePlay();
                updateMiniplayer();
                break;
            case R.id.skipButton:
                PlayerController.skip();
                break;
        }
    }
}
