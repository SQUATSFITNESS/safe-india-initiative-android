package in.squats.safeindiainitiative;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class UpdateLocationService extends IntentService {
    private static final String TAG = UpdateLocationService.class.getSimpleName();
    private static final String ACTION_UPDATE_LOCATION = "in.squats.safeindiainitiative.action.UPDATE_LOCATION";
    private static final String EXTRA_LAT = "in.squats.safeindiainitiative.extra.LAT";
    private static final String EXTRA_LNG = "in.squats.safeindiainitiative.extra.LNG";
    private static final String EXTRA_DEVICE_ID = "in.squats.safeindiainitiative.extra.DEVICE_ID";

    public UpdateLocationService() {
        super("UpdateLocationService");
    }

    /**
     * Starts this service to perform action with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionUpdateLocation(Context context, double lat, double lng, String deviceId) {
        Intent intent = new Intent(context, UpdateLocationService.class);
        intent.setAction(ACTION_UPDATE_LOCATION);
        intent.putExtra(EXTRA_LAT, lat);
        intent.putExtra(EXTRA_LNG, lng);
        intent.putExtra(EXTRA_DEVICE_ID, deviceId);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPDATE_LOCATION.equals(action)) {
                final double lat = intent.getDoubleExtra(EXTRA_LAT, 0.0);
                final double lng = intent.getDoubleExtra(EXTRA_LNG, 0.0);
                final String deviceId = intent.getStringExtra(EXTRA_DEVICE_ID);
                handleActionUpdateLocation(lat, lng, deviceId);
            }
        }
    }

    /**
     * update user location to server.
     */
    private void handleActionUpdateLocation(double lat, double lng, String deviceId) {
        Log.d(TAG, "Updating user location: " + lat + ", " + lng + ", " + deviceId);
        //do something
        String fcm = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "FCM: " + fcm);
        String locationUrl = "https://safe-india-initiative-api.herokuapp.com/api/user-location";
        String locationPosData = "{\"userDetails\": {\"userId\": \"" + deviceId + "\",\"lat\":" + lat + ",\"long\":" + lng + ", \"fcm\":\"" + fcm + "\" }}";

        if (lat != 0 && lng != 0) {
            new SendPostRequest(getApplicationContext()).execute(locationUrl, locationPosData);
        } else {
            Toast.makeText(getApplicationContext(), "Please provide permission to access user location",
                    Toast.LENGTH_LONG).show();
        }
    }
}
