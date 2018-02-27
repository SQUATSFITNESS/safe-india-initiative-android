package in.squats.safeindiainitiative;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by souvik on 2/25/2018.
 */

public class SendGetRequest extends AsyncTask<String, Void, String> {
    private static final String TAG = SendGetRequest.class.getSimpleName();

    private Context appContext;

    public SendGetRequest(Context applicationContext) {
        this.appContext = applicationContext;
    }

    protected void onPreExecute() {
    }

    protected String doInBackground(String... arg0) {

        try {

            URL url = new URL(arg0[0]);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
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
                return new String("API call failed. URL: " + arg0[0] + " Response code: " + responseCode);
            }
        } catch (Exception e) {
            return new String("Exception: " + e.getMessage());
        }
    }

    @Override
    protected void onPostExecute(String result) {
        Log.v(TAG, result);
        JSONArray helpers = null;
        try {
            helpers = new JSONArray(result);
            Log.d("result", result);

            for (int i = 0; i < helpers.length(); i++) {
                JSONObject helper = (JSONObject) helpers.get(i);
                String lat = helper.getString("lat");
                String lng = helper.getString("lng");
                if (lat != "0" && lng != "0") {
                    Log.v(TAG, "Help arriving soon...");
                }
            }
            Log.v(TAG, "Helpers count: " + helpers.length());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
