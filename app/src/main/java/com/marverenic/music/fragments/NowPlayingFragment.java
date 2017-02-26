package com.marverenic.music.fragments;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.databinding.ActivityNowPlayingBinding;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.view.TimeView;
import com.marverenic.music.viewmodel.NowPlayingArtworkViewModel;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class NowPlayingFragment extends BaseFragment {

    @Inject PlayerController mPlayerController;

    private ActivityNowPlayingBinding mBinding;
    private NowPlayingArtworkViewModel mArtworkViewModel;

    private Subscription mSleepTimerSubscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        JockeyApplication.getComponent(this).inject(this);

        mPlayerController.getSleepTimerEndTime()
                .compose(bindToLifecycle())
                .subscribe(this::updateSleepTimerCounter, throwable -> {
                    Timber.e(throwable, "Failed to update sleep timer end timestamp");
                });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.activity_now_playing,
                container, false);

        mArtworkViewModel = new NowPlayingArtworkViewModel(this);
        mBinding.setArtworkViewModel(mArtworkViewModel);

        boolean landscape = getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE;

        Toolbar actionBar = (Toolbar) mBinding.getRoot().findViewById(R.id.toolbar);
        if (actionBar != null) {
            if (!landscape) {
                actionBar.setBackground(new ColorDrawable(Color.TRANSPARENT));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                actionBar.setElevation(getResources().getDimension(R.dimen.header_elevation));
            }
            actionBar.setTitle("");
            actionBar.setNavigationIcon(R.drawable.ic_clear_24dp);
        }

        return mBinding.getRoot();
    }

    private void updateSleepTimerCounter(long endTimestamp) {
        TimeView timer = (TimeView) getView().findViewById(R.id.now_playing_sleep_timer);
        long sleepTimerValue = endTimestamp - System.currentTimeMillis();

        if (mSleepTimerSubscription != null) {
            mSleepTimerSubscription.unsubscribe();
        }

        if (sleepTimerValue <= 0) {
            timer.setVisibility(View.GONE);
        } else {
            timer.setVisibility(View.VISIBLE);
            timer.setTime((int) sleepTimerValue);

            mSleepTimerSubscription = Observable.interval(500, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.computation())
                    .map(tick -> (int) (sleepTimerValue - 500 * tick))
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(bindToLifecycle())
                    .subscribe(time -> {
                        timer.setTime(time);
                        if (time <= 0) {
                            mSleepTimerSubscription.unsubscribe();
                            animateOutSleepTimerCounter();
                        }
                    }, throwable -> {
                        Timber.e(throwable, "Failed to update sleep timer value");
                    });
        }
    }

    private void animateOutSleepTimerCounter() {
        TimeView timer = (TimeView) getView().findViewById(R.id.now_playing_sleep_timer);

        Animation transition = AnimationUtils.loadAnimation(getContext(), R.anim.tooltip_out_down);
        transition.setStartOffset(250);
        transition.setDuration(300);
        transition.setInterpolator(getContext(), android.R.interpolator.accelerate_quint);

        timer.startAnimation(transition);

        new Handler().postDelayed(() -> timer.setVisibility(View.GONE), 550);
    }

}
