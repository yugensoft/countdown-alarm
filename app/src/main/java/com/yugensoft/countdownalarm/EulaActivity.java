package com.yugensoft.countdownalarm;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class EulaActivity extends AppCompatActivity {

    private WebView webView;

    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eula);

        webView =(WebView)findViewById(R.id.webview);
        webView.getSettings().setTextZoom(75);
        webView.loadUrl("file:///android_asset/EULA.html");

        // Obtain the shared Tracker instance.
        mTracker = ((CountdownAlarmApplication)getApplication()).getDefaultTracker();

    }

    public void closeEula(View view) {
        finish();
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Tracking
        mTracker.setScreenName("Image~" + this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
}
