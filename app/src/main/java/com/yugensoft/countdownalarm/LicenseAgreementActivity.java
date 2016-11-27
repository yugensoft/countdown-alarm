package com.yugensoft.countdownalarm;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.w3c.dom.Text;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class LicenseAgreementActivity extends AppCompatActivity {

    private TextView textView;

    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_license_agreement);

        textView = (TextView)findViewById(R.id.text_agree_note);
        String agreeNote =getString(R.string.agree_note) + " ";
        String licenseAgreementStr = getString(R.string.agree_note_2);

        SpannableStringBuilder ssb = new SpannableStringBuilder(agreeNote);
        ssb.append(licenseAgreementStr);
        ClickableSpan licenseAgreementLink = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(LicenseAgreementActivity.this,EulaActivity.class));
            }
        };

        ssb.setSpan(licenseAgreementLink,agreeNote.length(),agreeNote.length()+licenseAgreementStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(ssb);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        // Obtain the shared Tracker instance.
        mTracker = ((CountdownAlarmApplication)getApplication()).getDefaultTracker();
    }


    @Override
    public void onBackPressed() {
        finish();
    }

    /**
     * Agree to terms and go to main
     * @param view
     */
    public void continueToMain(View view) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(MainActivity.KEY_HAS_AGREED,true).apply();
        startActivity(new Intent(this,MainActivity.class));
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
