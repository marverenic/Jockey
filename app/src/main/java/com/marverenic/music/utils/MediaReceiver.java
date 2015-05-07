package com.marverenic.music.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;

import com.marverenic.music.PlayerController;
import com.marverenic.music.PlayerService;
import com.marverenic.music.activity.NowPlayingActivity;

public class MediaReceiver extends BroadcastReceiver {

    //
    //      Most of this class is from Apollo
    //      https://github.com/CyanogenMod/android_packages_apps_Apollo/blob/master/src/com/andrew/apollo/MediaButtonIntentReceiver.java
    //

    private static final int MSG_LONGPRESS_TIMEOUT = 1;
    private static final int LONG_PRESS_DELAY = 1000;
    private static final int DOUBLE_CLICK = 800;
    private static long mLastClickTime = 0;
    private static boolean mDown = false;
    private static boolean mLaunched = false;

    private MediaHandler mHandler = new MediaHandler();

    static class MediaHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_LONGPRESS_TIMEOUT:
                    if (!mLaunched) {
                        final Context context = (Context)msg.obj;
                        final Intent i = new Intent();
                        i.setClass(context, NowPlayingActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(i);
                        mLaunched = true;
                    }
                    break;
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (PlayerService.getInstance() == null) return;

        if (PlayerController.isPlaying()) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                if (intent.getIntExtra("state", -1) == 0) {
                    try {
                        ((PlayerService.Stub) PlayerService.getInstance().getBinder()).pause();
                    }
                    catch (Exception ignored){}
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event == null) {
                    return;
                }

                final int keycode = event.getKeyCode();
                final int action = event.getAction();
                final long eventtime = event.getEventTime();

                String command = null;
                switch (keycode) {
                    case KeyEvent.KEYCODE_MEDIA_STOP:
                        command = PlayerService.ACTION_STOP;
                        break;
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        command = PlayerService.ACTION_TOGGLE_PLAY;
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        command = PlayerService.ACTION_NEXT;
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        command = PlayerService.ACTION_PREV;
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        command = PlayerService.ACTION_PAUSE;
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        command = PlayerService.ACTION_PLAY;
                        break;
                }
                if (command != null) {
                    if (action == KeyEvent.ACTION_DOWN) {
                        if (mDown) {
                            if ((PlayerService.ACTION_TOGGLE_PLAY.equals(command) || PlayerService.ACTION_PLAY.equals(command))
                                    && mLastClickTime != 0 && eventtime - mLastClickTime > LONG_PRESS_DELAY) {
                                mHandler.sendMessage(mHandler.obtainMessage(MSG_LONGPRESS_TIMEOUT, context));
                            }
                        } else if (event.getRepeatCount() == 0) {
                            // Only consider the first event in a sequence, not the
                            // repeat events,
                            // so that we don't trigger in cases where the first
                            // event went to
                            // a different app (e.g. when the user ends a phone call
                            // by
                            // long pressing the headset button)

                            // The service may or may not be running, but we need to
                            // send it
                            // a command.
                            final Intent i = new Intent(context, PlayerService.class);
                            if (keycode == KeyEvent.KEYCODE_HEADSETHOOK && eventtime - mLastClickTime < DOUBLE_CLICK) {
                                i.setAction(PlayerService.ACTION_NEXT);
                                context.startService(i);
                                mLastClickTime = 0;
                            } else {
                                i.setAction(command);
                                context.startService(i);
                                mLastClickTime = eventtime;
                            }
                            mLaunched = false;
                            mDown = true;
                        }
                    } else {
                        mHandler.removeMessages(MSG_LONGPRESS_TIMEOUT);
                        mDown = false;
                    }
                    if (isOrderedBroadcast()) {
                        abortBroadcast();
                    }
                }
            }
        }
    }
}