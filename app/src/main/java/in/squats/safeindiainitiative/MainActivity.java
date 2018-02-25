package in.squats.safeindiainitiative;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends Activity implements LocationListener {
    private static final int REQUEST_LOCATION_PERMISSION_ID = 99;
    private static final int REQUEST_AUDIO_PERMISSIONS_ID = 33;
    private static final String TAG = MainActivity.class.getSimpleName();

    LocationManager lm;
    String provider;
    Location l;
    double lng, lat;
    String deviceId;

    private Intent listenToUserServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listen_to_user);

        // Initialize Text To Speech engine
        TTS.init(getApplicationContext());

        // Get location permission and send user location
        requestLocationAndAudioPermission();

        deviceId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        sendUserLocationEveryFewSeconds();
    }

    private void sendUserLocationEveryFewSeconds() {
        final Handler handler = new Handler();
        final int delay = R.string.location_update_frequency_millisec;
        handler.postDelayed(new Runnable() {
            public void run() {
                Log.d(TAG, "Calling service to update user location");
                UpdateLocationService.startActionUpdateLocation(getApplicationContext(), lat, lng, deviceId);
                handler.postDelayed(this, delay);
            }
        }, delay);
    }


    private void requestLocationAndAudioPermission() {
        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Criteria c = new Criteria();
        provider = lm.getBestProvider(c, false);
        ActivityCompat.requestPermissions(this,
                new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.RECORD_AUDIO},
                REQUEST_LOCATION_PERMISSION_ID);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        l = lm.getLastKnownLocation(provider);
                        if (l != null) {
                            //get latitude and longitude of the location
                            lng = l.getLongitude();
                            lat = l.getLatitude();

                            ((SafeIndiaApplication) this.getApplication()).userLat = lat;
                            ((SafeIndiaApplication) this.getApplication()).userLong = lng;
                        }
                    }

                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED) {
                        new ListenToUser(getApplicationContext(), this).start();
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "This app cannot work without knowing your location and recording audio",
                            Toast.LENGTH_LONG).show();
                    this.requestLocationAndAudioPermission();
                }
                return;
            }
            case REQUEST_AUDIO_PERMISSIONS_ID: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                } else {
                    Toast.makeText(getApplicationContext(), "This app cannot work without permission to record audio",
                            Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        lng = l.getLongitude();
        lat = l.getLatitude();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }


    public void requestAudioRecordPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.RECORD_AUDIO)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.RECORD_AUDIO},
                        REQUEST_AUDIO_PERMISSIONS_ID);

            }
        }
    }


}
