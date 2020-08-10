package com.marko.smitemotd.networking;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;

public interface APICalls{

    @GET("data.json")
    Call<JsonObject> getMOTD();

}
