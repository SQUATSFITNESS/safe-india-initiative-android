package in.squats.safeindiainitiative;

import android.*;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class UpdateLocationService extends IntentService {
    private static final String ACTION_UPDATE_LOCATION = "in.squats.safeindiainitiative.action.UPDATE_LOCATION";
    LocationManager lm;
    String provider;
    Location l;
    double lng, lat;
    String deviceId;

    public UpdateLocationService() {
        super("UpdateLocationService");
    }

    /**
     * Starts this service to perform action with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
     public static void startActionUpdateLocation(Context context) {
        Intent intent = new Intent(context, UpdateLocationService.class);
        intent.setAction(ACTION_UPDATE_LOCATION);
        context.startService(intent);
     }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPDATE_LOCATION.equals(action)) {
                handleActionUpdateLocation();
            }
        }
    }

    /**
     * update user location to server.
     */
    private void handleActionUpdateLocation() {
        Log.d("UpdateLocationService", "Updating user location");    }

}
