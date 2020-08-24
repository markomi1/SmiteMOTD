package com.marko.smitemotd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class MotdDetails extends Activity{
    private final static String TAG = "MotdDetails";
    private TextView motdNameTextView, motdDetailsTextView,motdDescriptionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fadein,R.anim.fadeout);
        setContentView(R.layout.activity_motd_details);
        motdNameTextView = findViewById( R.id.motdName);
        motdDescriptionTextView = findViewById(R.id.motdDescription);
        motdDetailsTextView = findViewById( R.id.multilineTextArea);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // If we've received a touch notification that the user has touched
        // outside the app, finish the activity.
        if (MotionEvent.ACTION_UP == event.getAction()) {
            Log.d(TAG,"Touch event UP");
            finish();
            return true;
        }

        // Delegate everything else to Activity.
        return super.onTouchEvent(event);
    }
    @Override
    protected void onResume() {
        Intent intent = getIntent();
        String details = intent.getStringExtra("motdDetails");
        Log.d(TAG, "Got: " + details);
        Gson gson = new Gson();
        JsonObject arr = gson.fromJson(details, JsonObject.class);
        JsonArray rulesArray = arr.get("rules").getAsJsonArray();
        StringBuilder textToAdd = new StringBuilder();
        for (int i = 0; i < rulesArray.size(); i++){
            Log.d(TAG,"Arr :" + i);
            textToAdd.append(rulesArray.get(i).getAsString()).append("\n");
        }
        motdNameTextView.setText(arr.get("name").getAsString());
        motdDescriptionTextView.setText(arr.get("description").getAsString());
        motdDetailsTextView.setText(textToAdd);
        super.onResume();
    }
}
