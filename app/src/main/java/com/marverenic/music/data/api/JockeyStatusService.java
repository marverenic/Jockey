package com.marverenic.music.data.api;

import retrofit2.Response;
import retrofit2.http.GET;
import rx.Observable;

public interface JockeyStatusService {

    @GET("Jockey/privacy-policy-metadata.json")
    Observable<Response<PrivacyPolicyMetadata>> getPrivacyPolicyMetadata();

}
