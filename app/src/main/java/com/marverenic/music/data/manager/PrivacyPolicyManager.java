package com.marverenic.music.data.manager;

import android.content.Context;
import android.content.SharedPreferences;

import com.marverenic.music.data.api.JockeyStatusService;
import com.marverenic.music.data.api.PrivacyPolicyMetadata;
import com.marverenic.music.utils.Util;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.exceptions.Exceptions;
import timber.log.Timber;

/**
 * This class is responsible for checking for changes to the privacy policy. It does this by
 * reaching out to a website and looking at the timestamp of the latest policy. If it is newer
 * than the one accepted on-record, then a notification will be requested.
 *
 * The polling logic is as follows
 *  - Only poll once per session
 *  - Only poll once per interval (currently 3 days -- see {@link #POLL_INTERVAL_MILLIS})
 *
 * The notification logic is as follows
 *  - Only show the notification once per session (by calling {@link #onPrivacyPolicyUpdateNotified()})
 *  - Mark the latest policy as seen when {@link #onLatestPrivacyPolicyConfirmed()} is called
 *  - The same method should be used on first app start to ensure the latest policy is accepted
 *  - If the user does not explicitly accept the policy, nag them on the next app start
 *      - Regardless of whether or not they explicitly accept, mark the privacy policy as read
 *        the next time the alert is shown
 *
 * The accepted privacy policy version is stored in shared preferences as the timestamp that the
 * policy was written. If the policy revision timestamp can't be fetched, then
 * {@link System#currentTimeMillis()} is used as a fallback.
 */
public class PrivacyPolicyManager {

    private static final String PREF_AGREED_REVISION = "PrivacyPolicy.agreedRevision";
    private static final String PREF_LAST_UNREVISED_PRIVACY_CHECK = "PrivacyPolicy.lastCheck";
    private static final String PREF_REVISION_NAG = "PrivacyPolicy.nagRevision";
    private static final long POLL_INTERVAL_MILLIS = TimeUnit.DAYS.toMillis(3);

    private Context context;
    private JockeyStatusService service;
    private Observable<PrivacyPolicyMetadata> privacyPolicyMetadata;
    private SharedPreferences sharedPreferences;
    private boolean updateShownInThisSession = false;

    public PrivacyPolicyManager(Context context,
                                JockeyStatusService service,
                                SharedPreferences sharedPreferences) {
        this.context = context;
        this.service = service;
        this.sharedPreferences = sharedPreferences;
    }

    public Observable<Boolean> isPrivacyPolicyUpdated() {
        if (shouldNagPrivacyPolicyUpdate()) {
            return Observable.just(true);
        } else if (!shouldCheckForPrivacyPolicyUpdate()) {
            return Observable.just(false);
        }

        return getPrivacyPolicyMetadata()
                .map(metadata -> metadata.getLastUpdatedTimestamp() > getLastAgreedRevisionDate())
                .doOnNext(updateAvailable -> {
                    if (!updateAvailable) markNoRevisionAvailable();
                });
    }

    public void onPrivacyPolicyUpdateNotified() {
        updateShownInThisSession = true;
        if (sharedPreferences.getBoolean(PREF_REVISION_NAG, false)) {
            // Confirm the privacy policy if the user was alerted twice
            onLatestPrivacyPolicyConfirmed();
        } else {
            sharedPreferences.edit()
                    .putBoolean(PREF_REVISION_NAG, true)
                    .apply();
        }
    }

    public void onLatestPrivacyPolicyConfirmed() {
        getPrivacyPolicyMetadata()
                .first()
                .map(PrivacyPolicyMetadata::getLastUpdatedTimestamp)
                .onErrorReturn(throwable -> {
                    Timber.w(throwable, "Failed to check timestamp of latest privacy policy. "
                            + "Falling back to system time.");
                    return System.currentTimeMillis();
                })
                .subscribe(
                        this::setLastAgreedRevisionDate,
                        e -> Timber.e(e, "Failed to mark latest privacy policy as seen")
                );
    }

    private Observable<PrivacyPolicyMetadata> getPrivacyPolicyMetadata() {
        if (privacyPolicyMetadata == null) {
            privacyPolicyMetadata = service.getPrivacyPolicyMetadata()
                    .map(response -> {
                        if (!response.isSuccessful()) {
                            throw Exceptions.propagate(
                                    new IOException("Failed to fetch privacy policy metadata: "
                                            + response.code() + ", " + response.message())
                            );
                        } else {
                            return response.body();
                        }
                    })
                    .doOnError(throwable -> privacyPolicyMetadata = null)
                    .cache();
        }

        return privacyPolicyMetadata;
    }

    private long getLastAgreedRevisionDate() {
        return sharedPreferences.getLong(PREF_AGREED_REVISION, 0);
    }

    private void setLastAgreedRevisionDate(long revisionDate) {
        sharedPreferences.edit()
                .putLong(PREF_AGREED_REVISION, revisionDate)
                .remove(PREF_REVISION_NAG)
                .apply();
    }

    private boolean shouldCheckForPrivacyPolicyUpdate() {
        long lastCheck = sharedPreferences.getLong(PREF_LAST_UNREVISED_PRIVACY_CHECK, 0);
        return lastCheck + POLL_INTERVAL_MILLIS < System.currentTimeMillis()
                && !updateShownInThisSession && Util.canAccessInternet(context, true);
    }

    private boolean shouldNagPrivacyPolicyUpdate() {
        return sharedPreferences.getBoolean(PREF_REVISION_NAG, false);
    }

    private void markNoRevisionAvailable() {
        sharedPreferences.edit()
                .putLong(PREF_LAST_UNREVISED_PRIVACY_CHECK, System.currentTimeMillis())
                .remove(PREF_REVISION_NAG)
                .apply();
    }
}
