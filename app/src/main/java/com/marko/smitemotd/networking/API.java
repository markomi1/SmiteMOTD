package com.marko.smitemotd.networking;

import android.content.Context;
import android.util.Log;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class API{
    public static Retrofit retrofit = null;
    private static final String BASE_URL = "https://motd.today"; //https://192.168.4.110:8080



    public static Retrofit APICall(Context context) {
        Log.d("Retrofit", "Before builder");

        Retrofit.Builder builder = new Retrofit.Builder().baseUrl(BASE_URL);
        Log.d("Retrofit", builder.toString());

        if (retrofit == null) {
            retrofit = builder.addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }
}
