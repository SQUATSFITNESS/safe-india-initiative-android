package in.squats.safeindiainitiative;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ListenToUserActivity extends Activity {

    private Intent mServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listen_to_user);

        mServiceIntent = new Intent(this.getApplicationContext(), ListenToUserService.class);
        mServiceIntent.setAction("in.squats.safeindiainitiative.action.FOO");
        startService(mServiceIntent);
        finish();
    }
}
