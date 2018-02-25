package in.squats.safeindiainitiative;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;

public class NotifyUserActivity extends Activity {
    private static final String TAG = NotifyUserActivity.class.getSimpleName();

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notify_user);

        Bundle b = getIntent().getExtras();
        String lat = b.getString("lat");
        String lng = b.getString("long");
        String helpSeekerFcm = b.getString("helpSeekerFcm");

        Log.v(TAG, "lat, long: " + lat + ", " + lng);
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng + "&mode=w");
        Intent helpIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        helpIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        helpIntent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(helpIntent);
        PendingIntent willHelpPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent noHelpIntent = new Intent("REMOVE_NOTIFICATION");
        noHelpIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        TaskStackBuilder stackBuilder2 = TaskStackBuilder.create(this);
        stackBuilder2.addNextIntentWithParentStack(noHelpIntent);
        PendingIntent willNotHelpPendingIntent =
                stackBuilder2.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(ns);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Can you please help?")
                .setContentIntent(willHelpPendingIntent)
                .setContentText("Select Yes below to navigate to victim").setSmallIcon(R.drawable.ic_launcher_foreground)
                .addAction(R.drawable.ic_launcher_foreground, "Yes", willHelpPendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "No", willNotHelpPendingIntent).build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(1, notification);
    }
}