package in.squats.safeindiainitiative;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

public class MainActivity extends Activity {
    public static final int LOCATION_REQUEST_CODE = 99;

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

        requestLocationPermission();
        UpdateLocationService.startActionUpdateLocation(getApplicationContext());
        ListenToUserService.startActionListenToUser(getApplicationContext(), null);
    }

    private void requestLocationPermission() {
        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Criteria c = new Criteria();
        provider = lm.getBestProvider(c, false);
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
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
                } else {
                    Toast.makeText(getApplicationContext(), "This app cannot work without knowing your location",
                            Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }
}
