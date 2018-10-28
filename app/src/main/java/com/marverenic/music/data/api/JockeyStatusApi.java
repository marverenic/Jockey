package com.marverenic.music.data.api;

import com.marverenic.music.utils.OkHttpUtils;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.schedulers.Schedulers;

public class JockeyStatusApi {

    private static final String BASE_URL = "https://www.marverenic.com/";

    public static JockeyStatusService getService() {
        OkHttpClient client = OkHttpUtils.enableTls12OnPreLollipop(new OkHttpClient.Builder())
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(
                        RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build();

        return retrofit.create(JockeyStatusService.class);
    }

}
