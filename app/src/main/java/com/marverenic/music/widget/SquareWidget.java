package com.marverenic.music.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.MusicPlayer;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.MediaStyleHelper;

import javax.inject.Inject;

import rx.Observable;
import timber.log.Timber;

import static android.view.KeyEvent.KEYCODE_MEDIA_NEXT;
import static android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
import static android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS;

public class SquareWidget extends AppWidgetProvider {

    @Inject PlayerController mPlayerController;

    @Override
    public void onReceive(Context context, Intent intent) {
        JockeyApplication.getComponent(context).inject(this);

        String action = intent.getAction();
        if (MusicPlayer.UPDATE_BROADCAST.equals(action)) {
            updateAllInstances(context);
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_square);

        Intent launcherIntent = NowPlayingActivity.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launcherIntent, 0);
        views.setOnClickPendingIntent(R.id.widget_square_artwork, pendingIntent);

        views.setOnClickPendingIntent(R.id.widget_next,
                MediaStyleHelper.getActionIntent(context, KEYCODE_MEDIA_NEXT));

        views.setOnClickPendingIntent(R.id.widget_play_pause,
                MediaStyleHelper.getActionIntent(context, KEYCODE_MEDIA_PLAY_PAUSE));

        views.setOnClickPendingIntent(R.id.widget_previous,
                MediaStyleHelper.getActionIntent(context, KEYCODE_MEDIA_PREVIOUS));

        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        updateAllInstances(context);
    }

    private ComponentName getComponentName(Context context) {
        return new ComponentName(context.getPackageName(), SquareWidget.class.getCanonicalName());
    }

    private boolean isEnabled(Context context) {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        int[] ids = widgetManager.getAppWidgetIds(getComponentName(context));

        return ids != null && ids.length > 0;
    }

    private void updateAllInstances(Context context) {
        if (!isEnabled(context)) {
            return;
        }

        Observable.just(new RemoteViews(context.getPackageName(), R.layout.widget_square))
                .flatMap(views -> mPlayerController.getNowPlaying().take(1)
                        .map(song -> setSong(context, views, song)))
                .flatMap(views -> mPlayerController.isPlaying().take(1)
                        .map(isPlaying -> setPlaying(views, isPlaying)))
                .flatMap(views -> mPlayerController.getArtwork().take(1)
                        .map(artwork -> setArtwork(views, artwork)))
                .subscribe(views -> partialUpdateAllInstances(context, views),
                        throwable -> Timber.e(throwable, "Failed to update widget"));
    }

    private void partialUpdateAllInstances(Context context, RemoteViews delta) {
        AppWidgetManager wm = AppWidgetManager.getInstance(context);

        int[] widgetIds = wm.getAppWidgetIds(getComponentName(context));
        wm.partiallyUpdateAppWidget(widgetIds, delta);
    }

    private static RemoteViews setSong(Context context, RemoteViews views, @Nullable Song song) {
        if (song == null) {
            String defaultSongName = context.getString(R.string.nothing_playing);

            views.setTextViewText(R.id.widget_square_title, defaultSongName);
            views.setTextViewText(R.id.widget_square_artist, "");
            views.setTextViewText(R.id.widget_square_album, "");
        } else {
            views.setTextViewText(R.id.widget_square_title, song.getSongName());
            views.setTextViewText(R.id.widget_square_artist, song.getArtistName());
            views.setTextViewText(R.id.widget_square_album, song.getAlbumName());
        }

        return views;
    }

    private static RemoteViews setPlaying(RemoteViews views, boolean isPlaying) {
        views.setImageViewResource(R.id.widget_play_pause,
                (isPlaying)
                        ? R.drawable.ic_pause_36dp
                        : R.drawable.ic_play_arrow_36dp);

        return views;
    }

    private static RemoteViews setArtwork(RemoteViews views, @Nullable Bitmap artwork) {
        if (artwork == null) {
            views.setImageViewResource(R.id.widget_square_artwork, R.drawable.art_default_xl);
        } else {
            views.setImageViewBitmap(R.id.widget_square_artwork, artwork);
        }

        return views;
    }
}
