package in.squats.safeindiainitiative;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Objects;

/**
 * Created by souvik on 12/17/2017.
 */

public class SafeIndiaMessagingService extends FirebaseMessagingService {
    private static final String TAG = SafeIndiaMessagingService.class.getSimpleName();
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO: Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated.

        String msgType = remoteMessage.getData().get("action");
        Log.v(TAG, "Msg Type: '" + msgType + "'");


        if(msgType.equals("NOTIFY_USER")) {
            Log.v(TAG, "Notifying nearby user of help needed");
            String message = remoteMessage.getNotification().getBody();
            Log.d(TAG, "From: " + remoteMessage.getFrom());
            Log.d(TAG, "Notification Message Body: " + message);

            Double lat = Double.parseDouble(remoteMessage.getData().get("lat"));
            Log.d(TAG, "Notification Message lat: " + lat);
            Double lng = Double.parseDouble(remoteMessage.getData().get("long"));
            Log.d(TAG, "Notification Message lat: " + lng);
            String helpSeekerFcm = remoteMessage.getData().get("helpSeekerFcm");
            Log.d(TAG, "Notification Message helpSeekerFcm: " + helpSeekerFcm);

            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng + "&mode=w");
            Intent helpIntent = new Intent("in.squats.safeindiainitiative.actions.RECORD_HELPER");
            helpIntent.putExtra("lat", lat);
            helpIntent.putExtra("lng", lng);
            helpIntent.putExtra("helpSeekerFcm", helpSeekerFcm);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addNextIntentWithParentStack(helpIntent);
            PendingIntent willHelpPendingIntent =
                    stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent noHelpIntent = new Intent("in.squats.safeindiainitiative.actions.NO_HELP");
            noHelpIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            TaskStackBuilder stackBuilder2 = TaskStackBuilder.create(this);
            stackBuilder2.addNextIntentWithParentStack(noHelpIntent);
            PendingIntent willNotHelpPendingIntent =
                    stackBuilder2.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(ns);

            Notification notification = new Notification.Builder(this)
                    .setContentTitle(message)
                    .setContentIntent(willHelpPendingIntent)
                    .setContentText("Select Yes below to navigate to victim").setSmallIcon(R.drawable.ic_launcher_foreground)
                    .addAction(R.drawable.ic_launcher_foreground, "Yes", willHelpPendingIntent)
                    .addAction(R.drawable.ic_launcher_foreground, "No", willNotHelpPendingIntent).build();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;

            notificationManager.notify(1, notification);
        } else if (msgType.equals("NOTIFY_VICTIM")) {
            Log.v(TAG, "Notifying victim of help arriving");
            Double lat = Double.parseDouble(remoteMessage.getData().get("lat"));
            Log.d(TAG, "Notification Message lat: " + lat);
            Double lng = Double.parseDouble(remoteMessage.getData().get("long"));
            Log.d(TAG, "Notification Message lat: " + lng);
            Log.v(TAG, "Notifying victim of help arriving: " + lat + ", " + lng);

            Intent notifyVictimIntent = new Intent("in.squats.safeindiainitiative.actions.NOTIFY_VICTIM");
            notifyVictimIntent.putExtra("lat", lat);
            notifyVictimIntent.putExtra("lng", lng);
            notifyVictimIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(notifyVictimIntent);
        }
    }
}