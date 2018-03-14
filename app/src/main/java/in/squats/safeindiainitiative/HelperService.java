package in.squats.safeindiainitiative;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class HelperService extends IntentService {
    private static final String TAG = HelperService.class.getSimpleName();
    private static final String ACTION_RECORD_HELPER = "in.squats.safeindiainitiative.actions.RECORD_HELPER";
    private static final String ACTION_NO_HELP = "in.squats.safeindiainitiative.actions.NO_HELP";

    // TODO: Rename parameters
    private static final String EXTRA_LAT = "lat";
    private static final String EXTRA_LNG = "lng";
    private static final String EXTRA_HELP_SEEKER_FCM = "helpSeekerFcm";

    public HelperService() {
        super("HelperService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionWillHelp(Context context, String lat, String lng, String helpSeekerFcm) {
        Intent intent = new Intent(context, HelperService.class);
        intent.setAction(ACTION_RECORD_HELPER);
        intent.putExtra(EXTRA_LAT, lat);
        intent.putExtra(EXTRA_LNG, lng);
        intent.putExtra(EXTRA_HELP_SEEKER_FCM, helpSeekerFcm);
        context.startService(intent);
    }

    public static void startActionNoHelp(Context context, String helpSeekerFcm) {
        Intent intent = new Intent(context, HelperService.class);
        intent.setAction(ACTION_NO_HELP);
        intent.putExtra(EXTRA_HELP_SEEKER_FCM, helpSeekerFcm);
        context.startService(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent");
        if (intent != null) {
            final String action = intent.getAction();
            Log.v(TAG, "onHandleIntent: " + action);
            if (ACTION_RECORD_HELPER.equals(action)) {
                final String lat = intent.getStringExtra(EXTRA_LAT);
                final String lng = intent.getStringExtra(EXTRA_LNG);
                final String helpSeekerFcm = intent.getStringExtra(EXTRA_HELP_SEEKER_FCM);
                handleActionWillHelp(lat, lng, helpSeekerFcm);
            } else if (ACTION_NO_HELP.equals(action)) {
                final String helpSeekerFcm = intent.getStringExtra(EXTRA_HELP_SEEKER_FCM);
                handleActionNoHelp(helpSeekerFcm);
            }

        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void handleActionWillHelp(String lat, String lng, String helpSeekerFcm) {
        Log.v(TAG, "Recording helper details and opening google map to navigate to victim");
        recordHelperDetails(lat, lng, helpSeekerFcm);
        removeAllNotifications();
        navigateToHelpSeeker(lat, lng);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void handleActionNoHelp(String helpSeekerFcm) {
        Log.v(TAG, "Remove notifications as helper decided not to help");
        removeAllNotifications();
    }

    private void removeAllNotifications() {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(ns);
        notificationManager.cancelAll();
    }

    private void recordHelperDetails(String lat, String lng, String helpSeekerFcm) {
        String deviceId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);;

        String fcm = FirebaseInstanceId.getInstance().getToken();
        String helperUrl = "https://safe-india-initiative-api.herokuapp.com/api/helpers";
        String helperPosData = "{\"userId\": \"" + deviceId  + "\",\"lat\":" + lat + ",\"long\":" + lng + ", \"fcm\":\"" + fcm + "\", \"helpSeekerFcm\" : \"" + helpSeekerFcm + "\"}";
        new SendPostRequest(getApplicationContext()).execute(helperUrl, helperPosData);
    }

    private void navigateToHelpSeeker(String lat, String lng) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng + "&mode=w");
        Intent navigateIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        navigateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        navigateIntent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        startActivity(navigateIntent);
    }
}
