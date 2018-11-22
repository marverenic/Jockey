package com.marverenic.music.utils;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.marverenic.music.R;

/**
 * Helper APIs for constructing MediaStyle notifications
 *
 * Modified from https://gist.github.com/ianhanniballake/47617ec3488e0257325c
 * @author Ian Lake
 */
public class MediaStyleHelper {
    /**
     * Build a notification using the information from the given media session.
     * @param context Context used to construct the notification.
     * @param mediaSession Media session to get information.
     * @return A pre-built notification with information from the given media session.
     */
    public static NotificationCompat.Builder from(Context context,
                                                  MediaSessionCompat mediaSession,
                                                  String channel) {

        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel);

        builder
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setContentIntent(controller.getSessionActivity())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setWhen(0)
                .setShowWhen(false);

        if (description.getIconBitmap() == null || description.getIconBitmap().isRecycled()) {
            builder.setLargeIcon(
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.art_default));
        } else {
            builder.setLargeIcon(description.getIconBitmap());
        }

        return builder;
    }
}
