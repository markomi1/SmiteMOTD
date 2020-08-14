package com.marko.smitemotd;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;
import java.util.TimeZone;

public class AppWidgetAlarm{
    private final int ALARM_ID = 0;
    private final int INTERVAL_MILLIS = 60000;

    private Context mContext;
    private int MOTDTime;

    public AppWidgetAlarm(Context context)
    {

        mContext = context;
    }


    public void startAlarm()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        //calendar.add(Calendar.MILLISECOND, INTERVAL_MILLIS);
        calendar.set(Calendar.HOUR_OF_DAY,9);
        calendar.set(Calendar.MINUTE,30);
        calendar.set(Calendar.SECOND,0);

        Intent alarmIntent=new Intent(mContext, SmiteAppWidgetProvider.class);
        alarmIntent.setAction(SmiteAppWidgetProvider.ACTION_AUTO_UPDATE);
        alarmIntent.putExtra("AlarmName",(int) calendar.getTimeInMillis());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, ALARM_ID, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        // RTC does not wake the device up
        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), AlarmManager.INTERVAL_HALF_DAY, pendingIntent);
        Log.d("MarkoAlarmClass","Alarm has been started for " + calendar.getTime());
    }


    public void stopAlarm()
    {
        Intent alarmIntent = new Intent(SmiteAppWidgetProvider.ACTION_AUTO_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, ALARM_ID, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        Log.d("AlarmClass","Alarm has been cancelled");
    }
}
