package in.squats.safeindiainitiative;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;

public class NotifyVictimActivity extends Activity {
    private static final String TAG = NotifyVictimActivity.class.getSimpleName();

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notify_user);

        Bundle b = getIntent().getExtras();
        String lat = b.getString("lat");
        String lng = b.getString("long");

        Log.v(TAG, "lat, long: " + lat + ", " + lng);
        TTS.speak("People arriving soon to you");
    }
}