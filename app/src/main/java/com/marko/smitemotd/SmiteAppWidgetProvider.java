package com.marko.smitemotd;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
    private SharedPreferences widgetConfig;


    public static final String ACTION_AUTO_UPDATE = "AUTO_UPDATE";
    public static final String ACTION_FORWARD_CLICKED = "FORWARD_CLICKED";
    public static final String ACTION_BACK_CLICKED = "BACK_CLICKED";
    public static final String ACTION_CENTER_CLICKED = "CENTER_CLICKED";


    @Override
    public void onUpdate( Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        api = API.APICall(context).create(APICalls.class);
        //Toast.makeText(context, "OnUpdateCalled", Toast.LENGTH_SHORT).show();
        widgetConfig = context.getSharedPreferences("WidgetConfig", Context.MODE_PRIVATE);
        Gson gson = new Gson(); //Might or might not user it, but declaring it here because it's "cleaner"

        api.getMOTD().enqueue(new Callback<JsonObject>(){
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                JsonArray motdArr = response.body().get("motds").getAsJsonArray();
                JsonArray motdArrayShorted = new JsonArray();
                for (int i = 0; i < 20; i++){
                    motdArrayShorted.add(motdArr.get(i));
                }

                //If the network call was successful we store the array and the position in the shared pref so when
                //next time onUpdate is called in case there's no internet connection it'll still be able to make a
                //switch to newer MOTD if it exists
                widgetConfig.edit().putString("MOTDs",motdArrayShorted.toString()).apply();
                widgetConfig.edit().putInt("MOTDPosition",currentMOTDArrayPosition).apply();

                Log.d("onUpdate","MOTD position in array: " + currentMOTDArrayPosition);
                //Extracted method
                UpdateWidgets(context, appWidgetManager, appWidgetIds, motdArrayShorted);

            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                //Toast.makeText(context, "Failed to fetch new MOTDs, check internet connection.", Toast.LENGTH_LONG).show();
                Log.d(TAG,"There's no internet connection");
                Toast.makeText(context, "Can't get new MOTDs", Toast.LENGTH_SHORT).show();
                int widgetConfigMOTDPossition = widgetConfig.getInt("MOTDPosition",-1);
                if(widgetConfigMOTDPossition > 0){

                    JsonArray widgetConfigMOTDArray = gson.fromJson(widgetConfig.getString("MOTDs",""),JsonArray.class);
                    UpdateWidgets(context,appWidgetManager,appWidgetIds,widgetConfigMOTDArray);
                    Log.d(TAG,"Updated offline");
                    Toast.makeText(context, "Updated MOTDs offline.", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(context, "Failed to update offline.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void UpdateWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, JsonArray motdArrayShorted) {
        long currentTime = System.currentTimeMillis() / 1000L; //Grabs the current system time and divides it by 1000
        currentMOTDArrayPosition =  getCurrentMOTD(currentTime,motdArrayShorted); //Current system time

        for (int appWidgetId : appWidgetIds) {


            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            views.setCharSequence(R.id.motdTitle,"setText", motdArrayShorted.get(currentMOTDArrayPosition).getAsJsonObject().get("name").getAsString());
            long unix_seconds = motdArrayShorted.get(currentMOTDArrayPosition).getAsJsonObject().get("startTime").getAsInt();

            views.setCharSequence(R.id.lastUpdate,"setText","Last updated on: " + formateDate(currentTime,"dd-MM HH:mm"));
            views.setCharSequence(R.id.motdDate,"setText",formateDate(unix_seconds,"dd-MMM-yyy"));
            views.setOnClickPendingIntent(R.id.forwardButton, getPendingIntent(context,ACTION_FORWARD_CLICKED,appWidgetId,currentMOTDArrayPosition - 1, motdArrayShorted));
            views.setOnClickPendingIntent(R.id.backwardButton, getPendingIntent(context,ACTION_BACK_CLICKED,appWidgetId,currentMOTDArrayPosition + 1, motdArrayShorted));
            views.setOnClickPendingIntent(R.id.linearlayout_content,getPendingIntent(context,ACTION_CENTER_CLICKED,appWidgetId,currentMOTDArrayPosition, motdArrayShorted));
            //Don't really need to set all of those intent data but it's better than remaking the whole thing.
            views.setOnClickPendingIntent(R.id.refreshButton,getPendingIntent(context,ACTION_AUTO_UPDATE,appWidgetId,currentMOTDArrayPosition,motdArrayShorted));

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }


    private String formateDate(long unix_seconds,String dateForm){
        Date date = new Date(unix_seconds*1000L);
        SimpleDateFormat dateFormat = new SimpleDateFormat(dateForm);
        dateFormat.setTimeZone(TimeZone.getDefault());
        String java_date = dateFormat.format(date);
        return java_date;
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
        appWidgetAlarm.startAlarm(); //Sets up the widget alarm
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d("Received stuff", intent.toString());
        AppWidgetManager appWidgetManager =  AppWidgetManager.getInstance(context);
        Gson gson = new Gson();
        if(intent.getAction().contains(ACTION_AUTO_UPDATE)) {
            Toast.makeText(context, "Updating MOTDs...", Toast.LENGTH_SHORT).show();
            int[] WidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, SmiteAppWidgetProvider.class)); //Gets all the widget IDs

            if(WidgetIds != null){ //Sanity check in case it might be null (probably gonna crash here if it is)

                onUpdate(context,AppWidgetManager.getInstance(context),WidgetIds);

            }

        }else if(intent.getAction().equals(ACTION_FORWARD_CLICKED)){//If the forward button is clicked
            //NOTE FORWARD BUTTON SECTION
            JsonArray intentMOTDArray = gson.fromJson(intent.getStringExtra("motdArray"),JsonArray.class); //Get the motd Array from the intent
            int position = intent.getIntExtra("currentMOTDPosition", -1); //get the current position from the intent

            if(position >= 0 ){ //If greater or equal to 0 it'll update the widget, if not it'll display the toast bellow
                updateSingleWidgetGivenWidgetID(context,appWidgetManager,intent.getIntExtra("appWidgetId",-1),position,intentMOTDArray);
            }else{
                Toast.makeText(context, "No more future MOTDs", Toast.LENGTH_SHORT).show();
            }

        }else if(intent.getAction().equals(ACTION_BACK_CLICKED)){
            //NOTE BACK BUTTON SECTION

            JsonArray intentMOTDArray = gson.fromJson(intent.getStringExtra("motdArray"),JsonArray.class);
            int position = intent.getIntExtra("currentMOTDPosition", -1);
            if(position <= 19 ){ ///Array limit is 19 (20 including 0)

                updateSingleWidgetGivenWidgetID(context,appWidgetManager,intent.getIntExtra("appWidgetId",-1),position,intentMOTDArray);

            }else{

                Toast.makeText(context, "No more past MOTDs ", Toast.LENGTH_SHORT).show();

            }

        }else if(intent.getAction().equals(ACTION_CENTER_CLICKED)){
            //NOTE CENTER PORTION CLICKED

            Intent motdDetailsIntent = new Intent(context, MotdDetails.class);
            JsonArray intentMOTDArray = gson.fromJson(intent.getStringExtra("motdArray"),JsonArray.class);
            int position = intent.getIntExtra("currentMOTDPosition", -1);

            motdDetailsIntent.putExtra("motdDetails", intentMOTDArray.get(position).toString());
            motdDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //Must set this flag because i'm not starting it from an activity
            context.startActivity(motdDetailsIntent);


        }

    }

    //Responsible for updating one single widget given widget ID.
    private void updateSingleWidgetGivenWidgetID(Context context, AppWidgetManager appWidgetManager, int widgetId,int position, JsonArray motdArray){
        if(widgetId != -1){
            if(motdArray == null || position > 19 || position < 0){ //Checking if the given vars are okay
                Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show();
                return;
            }

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            views.setCharSequence(R.id.motdTitle,"setText",motdArray.get(position).getAsJsonObject().get("name").getAsString());
            long unix_seconds = motdArray.get(position).getAsJsonObject().get("startTime").getAsInt();
            Date date = new Date(unix_seconds*1000L);
            SimpleDateFormat jdf = new SimpleDateFormat("dd-MMM-yyy");
            jdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));
            String java_date = jdf.format(date);

            views.setCharSequence(R.id.motdDate,"setText",java_date ); //Set the center portion date
            views.setOnClickPendingIntent(R.id.forwardButton, getPendingIntent(context,ACTION_FORWARD_CLICKED,widgetId,position - 1,motdArray)); //Setting the button intents
            views.setOnClickPendingIntent(R.id.backwardButton, getPendingIntent(context,ACTION_BACK_CLICKED,widgetId,position + 1,motdArray));
            views.setOnClickPendingIntent(R.id.linearlayout_content,getPendingIntent(context,ACTION_CENTER_CLICKED,widgetId,position,motdArray));

            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }

    private int getCurrentMOTD(Long currentTime, JsonArray motdList){
        //I don't need to search the whole array ( which is quite large), i only need to search the first 20 or so,
        // and if i don't find it there then there's some kind of an error and i'll return 0 just so code doesn't break.
        for (int i = 0; i < motdList.size(); i++){
            int motdTime = motdList.get(i).getAsJsonObject().get("startTime").getAsInt();
            int isToday = currentTime.intValue() - motdTime;
            if(isToday < 86400 && isToday >= 0){
                //Log.d(TAG,"Current time diff is: " + isToday +" Current MOTD is: " + motdList.get(i).getAsJsonObject());
                return i;
            }
        }
        return 0;
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
        Log.d(TAG,"Widget disabled");
    }
}
