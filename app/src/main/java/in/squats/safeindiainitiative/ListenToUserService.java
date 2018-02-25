package in.squats.safeindiainitiative;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ListenToUserService extends IntentService {
    private static final String ACTION_LISTEN_TO_USER = "in.squats.safeindiainitiative.action.LISTEN_TO_USER";

    private static final String EXTRA_PARAM1 = "in.squats.safeindiainitiative.extra.PARAM1";

    public ListenToUserService() {
        super("ListenToUserService");
    }

    /**
     * Starts this service to perform action with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionListenToUser(Context context, String param1) {
        Intent intent = new Intent(context, ListenToUserService.class);
        intent.setAction(ACTION_LISTEN_TO_USER);
        intent.putExtra(EXTRA_PARAM1, param1);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_LISTEN_TO_USER.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                handleActionFoo(param1);
            }
        }
    }

    /**
     * Handle action ListenToUser in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1) {
        Log.d("ListenToUserService", "Listening to user now");
    }
}
