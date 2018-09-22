package com.marverenic.music.data.manager;

import android.content.SharedPreferences;

import com.marverenic.music.data.api.JockeyStatusService;
import com.marverenic.music.data.api.PrivacyPolicyMetadata;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.exceptions.Exceptions;
import timber.log.Timber;

public class PrivacyPolicyManager {

    private static final String PREF_AGREED_REVISION = "PrivacyPolicy.agreedRevision";
    private static final String PREF_LAST_UNREVISED_PRIVACY_CHECK = "PrivacyPolicy.lastCheck";
    private static final String PREF_REVISION_NAG = "PrivacyPolicy.nagRevision";
    private static final long POLL_INTERVAL_MILLIS = TimeUnit.DAYS.toMillis(3);

    private JockeyStatusService service;
    private Observable<PrivacyPolicyMetadata> privacyPolicyMetadata;
    private SharedPreferences sharedPreferences;
    private boolean updateShownInThisSession = false;

    public PrivacyPolicyManager(JockeyStatusService service, SharedPreferences sharedPreferences) {
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
                && !updateShownInThisSession;
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
