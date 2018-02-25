package in.squats.safeindiainitiative;

import android.app.Application;

public class SafeIndiaApplication extends Application {

    public Double userLat, userLong;
    public String deviceId;

    private static final String TAG = SafeIndiaApplication.class.getSimpleName();
}
