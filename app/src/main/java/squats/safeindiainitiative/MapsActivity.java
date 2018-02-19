package squats.safeindiainitiative;

import android.content.Intent;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView help_message;
    private Button canHelpButton;
    private Double lat, lng;
    private Double myLat, myLng;
    private String helpSeekerFcm;
    private LatLng helpSeekerLocation, myLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Intent intent = getIntent();
        lat = intent.getDoubleExtra("lat", 0);
        lng = intent.getDoubleExtra("long", 0);
        helpSeekerFcm = intent.getStringExtra("helpSeekerFcm");

        help_message = (TextView) this.findViewById(R.id.help_message);

        canHelpButton = (Button) this.findViewById(R.id.button_can_help);
        canHelpButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String deviceId = Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ANDROID_ID);;

                String fcm = FirebaseInstanceId.getInstance().getToken();
                String helperUrl = "https://safe-india-initiative-api.herokuapp.com/api/helpers";
                String helperPosData = "{\"userId\": \"" + deviceId  + "\",\"lat\":" + lat + ",\"long\":" + lng + ", \"fcm\":\"" + fcm + "\", \"helpSeekerFcm\" : \"" + helpSeekerFcm + "\"}";
                new MapsActivity.SendPostRequest().execute(helperUrl, helperPosData);
            }
        });
    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        helpSeekerLocation = new LatLng(lat, lng);
        AIApplication application = (AIApplication) this.getApplication();
        myLocation = new LatLng(application.userLat, application.userLong);
        Marker helpSeekerMarker = mMap.addMarker(new MarkerOptions().position(helpSeekerLocation).title("Help seeker"));
        helpSeekerMarker.showInfoWindow();
        Marker myMarker = mMap.addMarker(new MarkerOptions().position(myLocation).title("You"));


        mMap.setMinZoomPreference(16);
        help_message.setText("Can you reach the person now?");
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                //set zoom level to show both the markers
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(helpSeekerLocation);
                builder.include(myLocation);
                LatLngBounds bounds = builder.build();
                int padding = 90; // offset from edges of the map in pixels
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                mMap.moveCamera(cu);
            }
        });
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

            help_message.setText("Please reach the marker on map as soon as possible");
            canHelpButton.setVisibility(View.INVISIBLE);
        }
    }
}
