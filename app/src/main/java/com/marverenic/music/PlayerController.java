package com.marverenic.music;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.BackgroundTask;

import java.util.ArrayList;
import java.util.List;

public class PlayerController {

    private static IPlayerService playerService = null;
    private static ServiceConnection binder = null;

    private static final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            PlayerCache.invalidate();

            /*
             * It's convenient to update the play counts here because we want to make sure
             * it only runs on the main thread, and there's already a broadcast receiver
             * set up like this, so don't bother to make another one.
             */
            new BackgroundTask(new BackgroundTask.BackgroundAction() {
                @Override
                public void doInBackground() {
                    /*
                     * Because this receiver has high priority, do this in the background so that
                     * any other views can update without waiting for this to finish (Views updated
                     * with UPDATE broadcasts aren't affected by play counts)
                     */
                    LibraryScanner.updateCounts(context);
                }
            }, null);
        }
    };

    public static void bind(final Context context) {
        if (binder == null) {
            final ContextWrapper contextWrapper = new ContextWrapper(context);
            binder = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    playerService = IPlayerService.Stub.asInterface(service);
                    contextWrapper.sendOrderedBroadcast(new Intent(Player.UPDATE_BROADCAST), null);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    playerService = null;
                    try {
                        contextWrapper.unregisterReceiver(updateReceiver);
                    } catch (Exception ignored) {
                    }
                }
            };
            Intent serviceIntent = new Intent(contextWrapper, PlayerService.class);
            contextWrapper.startService(serviceIntent);
            contextWrapper.bindService(serviceIntent, binder, 0);

            // Register a receiver to invalidate "cached" data when an UPDATE broadcast is sent
            // It has to have high priority because it MUST execute before other BroadcastReceivers
            // to ensure that they don't receive old data
            IntentFilter filter = new IntentFilter(Player.UPDATE_BROADCAST);
            filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
            contextWrapper.registerReceiver(updateReceiver, filter);

            if (BuildConfig.DEBUG) Log.i("PlayerController", "Bound Player service");
        }
    }

    public static void unbind(Context context) {
        if (binder != null) {
            context.unbindService(binder);
            binder = null;
            playerService = null;
            if (BuildConfig.DEBUG) Log.i("PlayerController", "Unbound Player service");
        }
    }

    public static void begin() {
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.begin();
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
        PlayerCache.invalidate();
    }

    public static void stop() {
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.stop();
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
    }

    public static void skip() {
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.skip();
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
    }

    public static void previous() {
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.previous();
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
    }

    public static void play() {
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.play();
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
    }

    public static void pause() {
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.pause();
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
    }

    public static void togglePlay() {
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.togglePlay();
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
    }

    public static void toggleRepeat() {
        try {
            playerService.toggleRepeat();
        } catch (final RemoteException ignored) {
        }
    }

    public static void toggleShuffle() {
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.toggleShuffle();
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
    }

    public static boolean isPlaying() {
        if (playerService != null) {
            try {
                return playerService.isPlaying();
            } catch (final RemoteException ignored) {
            }
        }
        return false;
    }

    public static boolean isPreparing() {
        if (playerService != null) {
            try {
                return playerService.isPreparing();
            } catch (final RemoteException ignored) {
            }
        }
        return false;
    }

    public static boolean isShuffle() {
        try {
            if (playerService != null) return playerService.isShuffle();
        }
        catch (RemoteException ignored){}
        return false;
    }

    public static boolean isRepeat() {
        try {
            if (playerService != null) return playerService.isRepeat();
        }
        catch (RemoteException ignored){}
        return false;
    }

    public static boolean isRepeatOne() {
        try {
            if (playerService != null) return playerService.isRepeatOne();
        }
        catch (RemoteException ignored){}
        return false;
    }

    public static Song getNowPlaying() {
        if (PlayerCache.nowPlaying != null) return PlayerCache.nowPlaying;
        try {
            if (playerService != null){
                PlayerCache.nowPlaying = playerService.getNowPlaying();
                return PlayerCache.nowPlaying;
            }
        }
        catch (RemoteException ignored){}
        return null;
    }

    public static Bitmap getArt() {
        if (PlayerCache.art != null) return PlayerCache.art;
        try {
            if (playerService != null){
                PlayerCache.art = playerService.getArt();
                return PlayerCache.art;
            }
        }
        catch (RemoteException ignored){}
        return null;
    }

    public static Bitmap getFullArt() {
        if (PlayerCache.artFullRes != null) return PlayerCache.artFullRes;
        try {
            if (playerService != null){
                PlayerCache.artFullRes = playerService.getFullArt();
                return PlayerCache.artFullRes;
            }
        }
        catch (RemoteException ignored){}
        return null;
    }

    public static ArrayList<Song> getQueue() {
        try {
            if (playerService != null) return new ArrayList<>(playerService.getQueue());
        }
        catch (RemoteException ignored){}
        return new ArrayList<>();
    }

    public static void setQueue(final List<Song> newQueue, final int newPosition) {
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.setQueue(newQueue, newPosition);
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
    }

    public static void changeQueue(final List<Song> newQueue, final int newPosition) {
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.changeQueue(newQueue, newPosition);
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
    }

    public static int getPosition() {
        try {
            if (playerService != null) {
                return playerService.getPosition();
            }
        } catch (final RemoteException ignored) {
        }
        return 0;
    }

    public static void queueNext(final Song song) {
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.queueNext(song);
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
    }

    public static void queueNext(final ArrayList<Song> songs) {
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.queueNextList(songs);
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
    }

    public static void queueLast(final Song song) {
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.queueLast(song);
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
    }

    public static void queueLast(final ArrayList<Song> songs) {
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.queueLastList(songs);
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
    }

    public static void seek(final int position) {
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.seek(position);
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
    }

    public static int getCurrentPosition() {
        if (playerService != null) {
            try {
                return playerService.getCurrentPosition();
            } catch (final RemoteException ignored) {
            }
        }
        return 0;
    }

    public static int getDuration() {
        if (playerService != null){
            try{
                return playerService.getDuration();
            }
            catch (RemoteException ignored){}
        }
        return Integer.MAX_VALUE;
    }

    public static void changeSong(final int newPosition){
        if (playerService != null) {
            new BackgroundTask(
                    new BackgroundTask.BackgroundAction() {
                        @Override
                        public void doInBackground() {
                            try {
                                playerService.changeSong(newPosition);
                            } catch (RemoteException ignored){}
                        }
                    }, null);
        }
        PlayerCache.invalidate();
    }

    public static final class PlayerCache{
        /*
         * A simple class to store data related to the player's status.
         * The idea is to "cache" data on the main process after it's
         * first requested so that only 1 AIDL call has to be made between
         * the main process and service process for each field
         *
         * These fields are reset whenever an UPDATE broadcast is received
         * in order to ensure these values are up-to-date
         */

        // All of these "cached" objects were chosen because they are more
        // complex as parcelables, and always have an associated UPDATE broadcast
        public static Song nowPlaying;
        public static Bitmap art;
        public static Bitmap artFullRes;

        public static void invalidate(){
            // Reset all fields -- called when UPDATE broadcast is received
            nowPlaying = null;
            art = null;
            artFullRes = null;
        }
    }
}
