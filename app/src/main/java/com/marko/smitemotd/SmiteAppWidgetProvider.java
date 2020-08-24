package com.marko.smitemotd;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.google.gson.Gson;
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
    private int currentMOTDArrayPosition;



    public JsonArray motdArray;
    public static final String ACTION_AUTO_UPDATE = "AUTO_UPDATE";
    public static final String ACTION_FORWARD_CLICKED = "FORWARD_CLICKED";
    public static final String ACTION_BACK_CLICKED = "BACK_CLICKED";
    public static final String ACTION_CENTER_CLICKED = "CENTER_CLICKED";


    @Override
    public void onUpdate( Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        api = API.APICall(context).create(APICalls.class);
        Toast.makeText(context, "OnUpdateCalled", Toast.LENGTH_SHORT).show();

        api.getMOTD().enqueue(new Callback<JsonObject>(){
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                JsonArray motdArr = response.body().get("motds").getAsJsonArray();
                JsonArray motdArrayShorted = new JsonArray();
                for (int i = 0; i < 20; i++){
                    motdArrayShorted.add(motdArr.get(i));
                }

                long currentTime = System.currentTimeMillis() / 1000L; //Grabs the current system time and divides it by 1000
                currentMOTDArrayPosition =  getCurrentMOTD(currentTime,motdArrayShorted); //Current system time


                Log.d(TAG,"Size of array: " + motdArrayShorted.size());
                Log.d(TAG,"Content of array: " + motdArrayShorted);
                Log.d("onUpdate","MOTD position in array: " + currentMOTDArrayPosition);
                for (int appWidgetId : appWidgetIds) {


                    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
                    views.setCharSequence(R.id.motdTitle,"setText",motdArrayShorted.get(currentMOTDArrayPosition).getAsJsonObject().get("name").getAsString());
                    long unix_seconds = motdArrayShorted.get(currentMOTDArrayPosition).getAsJsonObject().get("startTime").getAsInt();
                    Date date = new Date(unix_seconds*1000L);
                    SimpleDateFormat jdf = new SimpleDateFormat("dd-MMM-yyy");
                    jdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));
                    String java_date = jdf.format(date);
                    views.setCharSequence(R.id.motdDate,"setText",java_date);

                    views.setOnClickPendingIntent(R.id.forwardButton, getPendingIntent(context,ACTION_FORWARD_CLICKED,appWidgetId,currentMOTDArrayPosition - 1,motdArrayShorted));
                    views.setOnClickPendingIntent(R.id.backwardButton, getPendingIntent(context,ACTION_BACK_CLICKED,appWidgetId,currentMOTDArrayPosition + 1,motdArrayShorted));
                    views.setOnClickPendingIntent(R.id.linearlayout_content,getPendingIntent(context,ACTION_CENTER_CLICKED,appWidgetId,currentMOTDArrayPosition,motdArrayShorted));


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



    private PendingIntent getPendingIntent(Context context, String action, int widgetID, int currentMOTDArrayPosition, JsonArray motdArray) {
        // An explicit intent directed at the current class (the "self").
        Intent intent = new Intent(context, getClass());
        intent.putExtra("appWidgetId", widgetID);
        intent.putExtra("currentMOTDPosition",currentMOTDArrayPosition);
        intent.putExtra("motdArray", String.valueOf(motdArray));
        intent.setAction(action);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }


    @Override
    public void onEnabled(Context context) {
        // start alarm
        AppWidgetAlarm appWidgetAlarm = new AppWidgetAlarm(context.getApplicationContext());
        appWidgetAlarm.startAlarm();
//        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        Toast.makeText(context, "OnEnableCalled", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d("Received stuff", intent.toString());
        AppWidgetManager appWidgetManager =  AppWidgetManager.getInstance(context);
        Gson gson = new Gson();
        if(intent.getAction().contains(ACTION_AUTO_UPDATE)) {

            int[] WidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, SmiteAppWidgetProvider.class));

            if(WidgetIds != null){
                Log.d("UpdateCalled","Update called: " + intent.getAction());
                Toast.makeText(context.getApplicationContext(), "Update called", Toast.LENGTH_SHORT).show();
                onUpdate(context,AppWidgetManager.getInstance(context),WidgetIds);
            }
        }else if(intent.getAction().equals(ACTION_FORWARD_CLICKED)){
            Log.d("UpdateCalled","Forward clicked");

            JsonArray intentMOTDArray = gson.fromJson(intent.getStringExtra("motdArray"),JsonArray.class);
            int position = intent.getIntExtra("currentMOTDPosition", -1);
            Log.d("UpdateCalled","Forward pressed, position is: " + position);

            if(position >= 0 ){
                updateSingleWidgetGivenWidgetID(context,appWidgetManager,intent.getIntExtra("appWidgetId",-1),position,intentMOTDArray);
            }else{

                Toast.makeText(context, "No more future MOTDs", Toast.LENGTH_SHORT).show();
                //updateSingleWidgetGivenWidgetID(context,appWidgetManager,intent.getIntExtra("appWidgetId",-1),);
            }


        }else if(intent.getAction().equals(ACTION_BACK_CLICKED)){
            Log.d("UpdateCalled","Back button pressed");
            JsonArray intentMOTDArray = gson.fromJson(intent.getStringExtra("motdArray"),JsonArray.class);
            int position = intent.getIntExtra("currentMOTDPosition", -1);
            Log.d("UpdateCalled","Back pressed, position is: " + position);
            if(position <= 19 ){

                updateSingleWidgetGivenWidgetID(context,appWidgetManager,intent.getIntExtra("appWidgetId",-1),position,intentMOTDArray);
            }else{

                Toast.makeText(context, "No more past MOTDs ", Toast.LENGTH_SHORT).show();
                //updateSingleWidgetGivenWidgetID(context,appWidgetManager,intent.getIntExtra("appWidgetId",-1),);
            }
        }else if(intent.getAction().equals(ACTION_CENTER_CLICKED)){
            Log.d("UpdateCalled","Center pressed");

            Intent motdDetailsIntent = new Intent(context, MotdDetails.class);
            JsonArray intentMOTDArray = gson.fromJson(intent.getStringExtra("motdArray"),JsonArray.class);
            int position = intent.getIntExtra("currentMOTDPosition", -1);
            Log.d("UpdateCalled","Back pressed, position is: " + position);

            motdDetailsIntent.putExtra("motdDetails", intentMOTDArray.get(position).toString());
            motdDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(motdDetailsIntent);


        }

    }

    private void updateSingleWidgetGivenWidgetID(Context context, AppWidgetManager appWidgetManager, int widgetId,int position, JsonArray motdArray){
        if(widgetId != -1){
            if(motdArray == null || position > 19 || position < 0){
                Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d("updateSingleWidget","MOTD Array size: " + motdArray.size());
            Log.d("updateSingleWidget","MOTD position:  " + position);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            views.setCharSequence(R.id.motdTitle,"setText",motdArray.get(position).getAsJsonObject().get("name").getAsString());
            long unix_seconds = motdArray.get(position).getAsJsonObject().get("startTime").getAsInt();
            Date date = new Date(unix_seconds*1000L);
            SimpleDateFormat jdf = new SimpleDateFormat("dd-MMM-yyy");
            jdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));
            String java_date = jdf.format(date);
            views.setCharSequence(R.id.motdDate,"setText",java_date );

            views.setOnClickPendingIntent(R.id.forwardButton, getPendingIntent(context,ACTION_FORWARD_CLICKED,widgetId,position - 1,motdArray));
            views.setOnClickPendingIntent(R.id.backwardButton, getPendingIntent(context,ACTION_BACK_CLICKED,widgetId,position + 1,motdArray));
            views.setOnClickPendingIntent(R.id.linearlayout_content,getPendingIntent(context,ACTION_CENTER_CLICKED,widgetId,position,motdArray));

            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }

    private int getCurrentMOTD(Long currentTime, JsonArray motdList){
        //I don't need to search the whole array ( which is quite large), i only need to search the first 20 or so, and if i don't find it there then there's some kind of an error and i'll return 0 just so code doesn't break.
        for (int i = 0; i < motdList.size(); i++){
            int motdTime = motdList.get(i).getAsJsonObject().get("startTime").getAsInt();
            int isToday = currentTime.intValue() - motdTime;
            if(isToday < 86400 && isToday >= 0){
                Log.d(TAG,"Current time diff is: " + isToday +" Current MOTD is: " + motdList.get(i).getAsJsonObject());
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

    @Override
    public void onDisabled(Context context) {
        // stop alarm only if all widgets have been disabled
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(),getClass().getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
        if (appWidgetIds.length == 0) {
            // stop alarm
            AppWidgetAlarm appWidgetAlarm = new AppWidgetAlarm(context.getApplicationContext());
            appWidgetAlarm.stopAlarm();
        }

    }
}
