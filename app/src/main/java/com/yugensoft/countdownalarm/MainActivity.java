package com.yugensoft.countdownalarm;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.analytics.Tracker;

public class MainActivity extends AppCompatActivity {

    private Tracker mTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the shared tracker instance
        mTracker = ((CountdownAlarmApplication)getApplication()).getDefaultTracker();

        // todo, development, jump straight to the message activity
        startActivity(new Intent(this,MessageActivity.class));
    }

    public void startMessageActivity(View view) {
        startActivity(new Intent(getApplicationContext(),MessageActivity.class));
    }
}
