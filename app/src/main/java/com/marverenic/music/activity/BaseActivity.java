package com.marverenic.music.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
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

public abstract class BaseActivity extends AppCompatActivity implements View.OnClickListener {

    @LayoutRes private int layoutResID = -1;
    @IdRes private int contentResId = -1;

    private static final boolean debug = BuildConfig.DEBUG;
    private boolean createdView = false;
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
        if (layoutResID != -1) super.setContentView(layoutResID);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        }

        themeActivity();
        ((JockeyApplication) getApplication()).activityCreated();

        if (findViewById(R.id.miniplayer) != null) {
            findViewById(R.id.miniplayer).setOnClickListener(this);
            findViewById(R.id.playButton).setOnClickListener(this);
            findViewById(R.id.skipButton).setOnClickListener(this);
        }
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
        createdView = true;
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
     * Called when the @link PlayerService sends an UPDATE broadcast.
     */
    public abstract void update();

    /**
     * Update the miniplayer to reflect the most recent @link PlayerService status. If no miniplayer
     * exists in the view, override this method with an empty code block.
     */
    @SuppressWarnings("ResourceType")
    public void updateMiniplayer() {
        if (debug) Log.i(getClass().toString(), "Called updateMiniplayer");
        if (contentResId != -1) {
            Song nowPlaying = PlayerController.getNowPlaying();
            // If there's music playing, update the Miniplayer's view
            if (nowPlaying != null) {

                // update the text and images inside the miniplayer
                updateMiniplayerContents(true, PlayerController.getFullArt(), PlayerController.getNowPlaying(), PlayerController.isPlaying() || PlayerController.isPreparing());

                final View miniplayerView = findViewById(R.id.miniplayer_holder);
                final int miniplayerHeight = getResources().getDimensionPixelSize(R.dimen.now_playing_ticker_height);

                if(miniplayerView != null && miniplayerView.getLayoutParams().height != miniplayerHeight && miniplayerView.getAnimation() == null) {
                    // If the view isn't being created for the first time, animate it in
                    if (createdView) {
                        Animation miniplayerHeightAnim = new Animation() {
                            @Override
                            protected void applyTransformation(float interpolatedTime, Transformation t) {
                                RelativeLayout.LayoutParams miniplayerLayoutParams = (RelativeLayout.LayoutParams) miniplayerView.getLayoutParams();
                                miniplayerLayoutParams.height = (int) (miniplayerHeight * interpolatedTime);
                                miniplayerView.setLayoutParams(miniplayerLayoutParams);
                            }
                        };
                        miniplayerHeightAnim.setDuration(300);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            miniplayerHeightAnim.setInterpolator(this, android.R.interpolator.linear_out_slow_in);
                        miniplayerView.startAnimation(miniplayerHeightAnim);

                        if (contentResId != -1) {
                            // If the id of the main content has been specified, update its margin
                            final View contentView = findViewById(contentResId);
                            if (contentView != null) {

                                Animation contentViewMarginAnim = null;

                                if (contentView.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                                    contentViewMarginAnim = new Animation() {
                                        @Override
                                        protected void applyTransformation(float interpolatedTime, Transformation t) {
                                            RelativeLayout.LayoutParams contentLayoutParams = (RelativeLayout.LayoutParams) contentView.getLayoutParams();
                                            contentLayoutParams.bottomMargin = (int) (miniplayerHeight * interpolatedTime);
                                            contentView.setLayoutParams(contentLayoutParams);
                                        }
                                    };
                                }
                                else if (contentView.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                                    contentViewMarginAnim = new Animation() {
                                        @Override
                                        protected void applyTransformation(float interpolatedTime, Transformation t) {
                                            FrameLayout.LayoutParams contentLayoutParams = (FrameLayout.LayoutParams) contentView.getLayoutParams();
                                            contentLayoutParams.bottomMargin = (int) (miniplayerHeight * interpolatedTime);
                                            contentView.setLayoutParams(contentLayoutParams);
                                        }
                                    };
                                }
                                else {
                                    Log.w(getClass().toString(), "Couldn't animate content layout margin because it isn't in a frame layout or relative layout");
                                }

                                if (contentViewMarginAnim != null) {
                                    contentViewMarginAnim.setDuration(300);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                        contentViewMarginAnim.setInterpolator(this, android.R.interpolator.linear_out_slow_in);
                                    contentView.startAnimation(contentViewMarginAnim);
                                }
                            }
                        }
                    }
                    // If this is being called while initializing the activity's view, don't animate it in
                    else{
                        RelativeLayout.LayoutParams miniplayerLayoutParams = (RelativeLayout.LayoutParams) miniplayerView.getLayoutParams();
                        miniplayerLayoutParams.height = miniplayerHeight;
                        miniplayerView.setLayoutParams(miniplayerLayoutParams);

                        if (contentResId != -1) {
                            // If the id of the main content has been specified, update its margin
                            final View contentView = findViewById(contentResId);

                            if (contentView.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                                RelativeLayout.LayoutParams contentLayoutParams = (RelativeLayout.LayoutParams) contentView.getLayoutParams();
                                contentLayoutParams.bottomMargin = miniplayerHeight;
                                contentView.setLayoutParams(contentLayoutParams);
                            }
                            else if (contentView.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                                FrameLayout.LayoutParams contentLayoutParams = (FrameLayout.LayoutParams) contentView.getLayoutParams();
                                contentLayoutParams.bottomMargin = miniplayerHeight;
                                contentView.setLayoutParams(contentLayoutParams);
                            }
                            else {
                                Log.w(getClass().toString(), "Couldn't set content layout margin because it isn't in a frame layout or relative layout");
                            }
                        }
                    }
                }
            }
            else {
                final View contentView = findViewById(contentResId);

                if (contentView.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                    RelativeLayout.LayoutParams contentLayoutParams = (RelativeLayout.LayoutParams) contentView.getLayoutParams();
                    contentLayoutParams.bottomMargin = 0;
                    contentView.setLayoutParams(contentLayoutParams);
                }
                else if (contentView.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams contentLayoutParams = (FrameLayout.LayoutParams) contentView.getLayoutParams();
                    contentLayoutParams.bottomMargin = 0;
                    contentView.setLayoutParams(contentLayoutParams);
                }
                else {
                    Log.w(getClass().toString(), "Couldn't set content layout margin because it isn't in a frame layout or relative layout");
                }
            }
        }
    }

    /**
     * Update the views inside the miniplayer
     * @param hasArt Whether or not to update the artwork in the view
     * @param art The new album artwork to display (null for default image)
     * @param nowPlaying The currently playing song (null to keep old song)
     * @param isPlaying The current playing status
     */
    public void updateMiniplayerContents(boolean hasArt, Bitmap art, Song nowPlaying, boolean isPlaying){
        if (hasArt) {
            if (art != null)
                ((ImageView) findViewById(R.id.imageArtwork)).setImageBitmap(art);
            else
                ((ImageView) findViewById(R.id.imageArtwork)).setImageResource(R.drawable.art_default);
        }

        if (nowPlaying != null) {
            final TextView songTitle = (TextView) findViewById(R.id.textNowPlayingTitle);
            final TextView artistName = (TextView) findViewById(R.id.textNowPlayingDetail);

            songTitle.setText(nowPlaying.songName);
            artistName.setText(nowPlaying.artistName);
            ((ImageButton) findViewById(R.id.skipButton)).setColorFilter(Themes.getListText());
        }

        if (!isPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_play);
                ((ImageButton) findViewById(R.id.playButton)).setImageTintList(ColorStateList.valueOf(Themes.getListText()));
            } else {
                ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_miniplayer);
                ((ImageButton) findViewById(R.id.playButton)).setColorFilter(Themes.getListText());
            }
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_pause);
                ((ImageButton) findViewById(R.id.playButton)).setImageTintList(ColorStateList.valueOf(Themes.getListText()));
            } else {
                ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause_miniplayer);
                ((ImageButton) findViewById(R.id.playButton)).setColorFilter(Themes.getListText());
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
                break;
            case R.id.skipButton:
                PlayerController.skip();
                break;
        }
    }
}
