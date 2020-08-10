package com.marko.smitemotd;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.marko.smitemotd.networking.API;
import com.marko.smitemotd.networking.APICalls;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SmiteAppWidgetProvider extends AppWidgetProvider{
    private APICalls api;
    private final static String TAG = "SmiteMOTDWidgetTag";
    private int currentMOTD;
    @Override
    public void onUpdate( Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        api = API.APICall(context).create(APICalls.class);

        api.getMOTD().enqueue(new Callback<JsonObject>(){
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                JsonArray motdArr = response.body().get("motds").getAsJsonArray();
                long currentTime = System.currentTimeMillis() / 1000L; //Grabs the current system time and divides it by 1000
                currentMOTD =  getCurrentMOTD(currentTime,motdArr); //Current system time
                for (int appWidgetId : appWidgetIds) {
                    Intent intent = new Intent(context, MainActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
                    views.setCharSequence(R.id.motdTitle,"setText",motdArr.get(currentMOTD).getAsJsonObject().get("name").getAsString());
                    long unix_seconds = motdArr.get(currentMOTD).getAsJsonObject().get("startTime").getAsInt();
                    Date date = new Date(unix_seconds*1000L);
                    SimpleDateFormat jdf = new SimpleDateFormat("dd-MMM-yyy");
                    jdf.setTimeZone(TimeZone.getTimeZone("GMT+1"));
                    String java_date = jdf.format(date);
                    views.setCharSequence(R.id.motdDate,"setText",java_date);
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(context, "Failed to fetch MOTDs", Toast.LENGTH_LONG).show();
                t.printStackTrace();
            }
        });
    }

    private int getCurrentMOTD(Long currentTime, JsonArray motdList){
        //I don't need to search the whole array ( which is quite large), i only need to search the first 20 or so, and if i don't find it there then there's some kind of an error and i'll return 0 just so code doesn't break.
        for (int i = 0; i < 20; i++){
            int motdTime = motdList.get(i).getAsJsonObject().get("startTime").getAsInt();
            int isToday = currentTime.intValue() - motdTime;
            if(isToday < 86400 && isToday >= 0){
                Log.d(TAG,"Current MOTD is: " + motdList.get(i).getAsJsonObject());
                return i;
            }
        }
        return 0;
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

        Toast.makeText(context, "minWidth: " + minWidth + "\nmaxWidth: " + maxWidth + "\nminHeight: " + minHeight + "\nmaxHeight: " +  maxHeight, Toast.LENGTH_LONG).show();

    }
}
