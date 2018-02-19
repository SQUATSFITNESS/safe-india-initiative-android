package squats.safeindiainitiative;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by souvik on 12/17/2017.
 */

public class SafeIndiaMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCM Service";
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO: Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated.
        String message = remoteMessage.getNotification().getBody();
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Notification Message Body: " + message);

        Double lat = Double.parseDouble(remoteMessage.getData().get("lat"));
        Log.d(TAG, "Notification Message lat: " + lat);
        Double lng = Double.parseDouble(remoteMessage.getData().get("long"));
        Log.d(TAG, "Notification Message lat: " + lng);
        String helpSeekerFcm = remoteMessage.getData().get("helpSeekerFcm");
        Log.d(TAG, "Notification Message helpSeekerFcm: " + helpSeekerFcm);

        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("lat", lat);
        intent.putExtra("long", lng);
        intent.putExtra("helpSeekerFcm", helpSeekerFcm);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}