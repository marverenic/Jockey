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
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.Player;
import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

public abstract class BaseActivity extends ActionBarActivity implements View.OnClickListener {

    @LayoutRes private int layoutResID = -1;
    @IdRes private int contentResId = -1;

    private static final boolean debug = BuildConfig.DEBUG;
    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };

    /**
     * @inheritDoc
     */
    @Override
    public void onCreate(Bundle savedInstanceState){
        if (debug) Log.i(getClass().toString(), "Called onCreate");

        Themes.setTheme(this);
        super.onCreate(savedInstanceState);
        super.setContentView(layoutResID);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        themeActivity();
        ((JockeyApplication) getApplication()).activityCreated();
    }

    /**
     * Set the layout resource for this activity. Required.
     * @param layoutResID The layout to be inflated
     */
    public void setContentLayout(@LayoutRes int layoutResID){
        this.layoutResID = layoutResID;
    }

    /**
     * Set the layout id that contains the main content in this view. Optional if the #update and
     * #updateMiniplayer method is overridden and doesn't call the super method
     * @param contentResId The id of the content container
     */
    @Override
    public void setContentView(@IdRes int contentResId){
        this.contentResId = contentResId;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onResume(){
        if (debug) Log.i(getClass().toString(), "Called onResume");
        super.onResume();
        ((JockeyApplication) getApplication()).activityResumed();
        Themes.setApplicationIcon(this);
        registerReceiver(updateReceiver, new IntentFilter(Player.UPDATE_BROADCAST));
        update();
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onPause(){
        if (debug) Log.i(getClass().toString(), "Called onPause");
        super.onPause();
        try {
            unregisterReceiver(updateReceiver);
        } catch (Exception ignored) {}
        ((JockeyApplication) getApplication()).activityPaused();
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onDestroy(){
        if (debug) Log.i(getClass().toString(), "Called onDestroy");
        super.onDestroy();
        ((JockeyApplication) getApplication()).activityDestroyed();
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            Navigate.up(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onBackPressed() {
        if (debug) Log.i(getClass().toString(), "Called calledOnBackPressed");
        super.onBackPressed();
        Navigate.back(this);
    }

    /**
     * Method to theme elements in the view hierarchy for this activity. By default, this method
     * sets the app's primary color, app icon, and background color. If the miniplayer is in the
     * hierarchy, it is also themed.
     */
    public void themeActivity(){
        Themes.updateColors(this);
        Themes.setApplicationIcon(this);
        getWindow().getDecorView().findViewById(android.R.id.content).setBackgroundColor(Themes.getBackground());

        if (findViewById(R.id.miniplayer) != null) {
            View miniplayer = (View) findViewById(R.id.miniplayer).getParent();

            miniplayer.setBackgroundColor(Themes.getBackgroundMiniplayer());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ((ImageButton) miniplayer.findViewById(R.id.skipButton)).setImageTintList(ColorStateList.valueOf(Themes.getListText()));
                ((ImageButton) miniplayer.findViewById(R.id.playButton)).setImageTintList(ColorStateList.valueOf(Themes.getListText()));
            } else {
                if (!Themes.isLight(this)) {
                    ((ImageButton) miniplayer.findViewById(R.id.skipButton)).setImageResource(R.drawable.ic_skip_next_miniplayer);
                    ((ImageButton) miniplayer.findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_miniplayer);
                } else {
                    ((ImageButton) miniplayer.findViewById(R.id.skipButton)).setImageResource(R.drawable.ic_skip_next_miniplayer_light);
                    ((ImageButton) miniplayer.findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_miniplayer_light);
                }
            }

            ((TextView) miniplayer.findViewById(R.id.textNowPlayingTitle)).setTextColor(Themes.getListText());
            ((TextView) miniplayer.findViewById(R.id.textNowPlayingDetail)).setTextColor(Themes.getDetailText());
        }
    }

    /**
     * Called when the @link PlayerService sends an UPDATE broadcast. The default implementation
     * updates the miniplayer
     */
    public void update(){
        if (debug) Log.i(getClass().toString(), "Called update");
        updateMiniplayer();
    }

    /**
     * Update the miniplayer to reflect the most recent @link PlayerService status. If no miniplayer
     * exists in the view, override this method with an empty code block.
     */
    @SuppressWarnings("ResourceType")
    public void updateMiniplayer(){
        if (debug) Log.i(getClass().toString(), "Called updateMiniplayer");
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

    /**
     * @inheritDoc
     */
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
