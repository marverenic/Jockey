package com.marverenic.music.lastfm2.api;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.schedulers.Schedulers;

public final class LastFmApi {

    protected static final String BASE_URL = "http://ws.audioscrobbler.com/2.0/";
    protected static final String API_KEY = "a9fc65293034b84b83d20c6e2ecda4b5";

    /**
     * This class is never instantiated
     */
    private LastFmApi() {

    }

    public static LastFmService getService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(
                        RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build();

        return retrofit.create(LastFmService.class);
    }

}
