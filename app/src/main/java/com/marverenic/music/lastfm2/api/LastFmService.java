package com.marverenic.music.lastfm2.api;

import com.marverenic.music.lastfm2.model.LfmArtist;

import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

public interface LastFmService {

    String CONSTANT_ARGS = "&api_key=" + LastFmApi.API_KEY + "&format=json";

    @GET("?method=artist.getinfo" + CONSTANT_ARGS)
    Observable<Response<LfmArtist>> getArtistInfo(@Path("artist") String artistName);

}