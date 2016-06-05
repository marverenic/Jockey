package com.marverenic.music.lastfm.api;

import android.content.Context;

import com.marverenic.music.BuildConfig;

import java.io.File;
import java.io.IOException;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.schedulers.Schedulers;

public final class LastFmApi {

    protected static final String BASE_URL = "http://ws.audioscrobbler.com/2.0/";

    /**
     * API key used to access data on Last.fm. If forking Jockey, please use your own token
     */
    protected static final String API_KEY = "a9fc65293034b84b83d20c6e2ecda4b5";

    /**
     * The directory name to place cache files from OkHttp. This directory will be placed in the
     * app's internal cache directory (or external if this is a debug build)
     */
    private static final String CACHE_DIR = "lastfm";

    /**
     * The overridden cache duration to keep data from GET requests. By default, Last.fm's API
     * returns 1 day, but its API policy requires that items be cached for a week.
     */
    private static final long CACHE_DURATION_SEC = 7 * 24 * 60 * 60;

    /**
     * The maximum size of the cache. This is currently set at 10 MiB
     */
    private static final int CACHE_SIZE = 10 * 1024 * 1024;

    /**
     * This class is never instantiated
     */
    private LastFmApi() {

    }

    private static File getCacheDir(Context context) {
        if (BuildConfig.DEBUG) {
            return new File(context.getExternalCacheDir(), CACHE_DIR);
        } else {
            return new File(context.getCacheDir(), CACHE_DIR);
        }
    }

    private static OkHttpClient.Builder getOkHttpClientBuilder(Context context) {
        Cache cache = new Cache(getCacheDir(context), CACHE_SIZE);
        return new OkHttpClient.Builder().cache(cache);
    }

    public static LastFmService getService(Context context) {
        OkHttpClient client = getOkHttpClientBuilder(context)
                .addNetworkInterceptor(new CacheInterceptor())
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(
                        RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build();

        return retrofit.create(LastFmService.class);
    }

    private static class CacheInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            if (!chain.request().method().equals("GET")) {
                return chain.proceed(chain.request());
            }

            Request originalRequest = chain.request();
            String cacheHeaderValue = "public, max-age=" + CACHE_DURATION_SEC;
            Request request = originalRequest.newBuilder().build();
            Response response = chain.proceed(request);

            return response.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", cacheHeaderValue)
                    .build();
        }
    }

}
