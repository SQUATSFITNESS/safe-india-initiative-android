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
            new SendPostRequest().execute(locationUrl, locationPosData);
        } else {
            Toast.makeText(getApplicationContext(), "Please provide permission to access user location",
                    Toast.LENGTH_LONG).show();
        }
    }


    class SendPostRequest extends AsyncTask<String, Void, String> {

        protected void onPreExecute() {
        }

        protected String doInBackground(String... arg0) {

            try {

                URL url = new URL(arg0[0]);

                JSONObject postDataParams = new JSONObject();

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(arg0[1]);

                writer.flush();
                writer.close();
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {

                    BufferedReader in = new BufferedReader(new
                            InputStreamReader(
                            conn.getInputStream()));

                    StringBuffer sb = new StringBuffer("");
                    String line = "";

                    while ((line = in.readLine()) != null) {

                        sb.append(line);
                        break;
                    }

                    in.close();
                    return sb.toString();

                } else {
                    return new String("API call failed. URL: " + arg0[0] + " param: " + arg0[1] + " Response code: " + responseCode);
                }
            } catch (Exception e) {
                return new String("Exception: " + e.getMessage());
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getApplicationContext(), result,
                    Toast.LENGTH_LONG).show();
        }
    }
}
