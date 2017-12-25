package squats.safeindiainitiative;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import ai.api.AIListener;
import ai.api.RequestExtras;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.android.GsonFactory;
import ai.api.model.AIContext;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;

public class ListenToUserActivity extends BaseActivity
        implements AIListener, AdapterView.OnItemSelectedListener, LocationListener {

    public static final String TAG = ListenToUserActivity.class.getName();
    public static final int LOCATION_REQUEST_CODE = 99;

    LocationManager lm;
    String provider;
    Location l;
    double lng, lat;
    String deviceId;

    private AIService aiService;
    private ProgressBar progressBar;
    private TextView resultTextView;
    private EditText contextEditText;

    private Gson gson = GsonFactory.getGson();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listen_to_user);

        TTS.init(getApplicationContext());

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        resultTextView = (TextView) findViewById(R.id.resultTextView);

        if(recog != null) {
            recog.destroy();
        }

        recog = SpeechRecognizer.createSpeechRecognizer(this);
        recog.setRecognitionListener(new RecogListener(this));
        Intent speechintent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechintent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechintent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 100);
        speechintent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechintent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        deviceId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);;

        //get location service
        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Criteria c = new Criteria();
        provider = lm.getBestProvider(c, false);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);

        readyRecognizeSpeech = new Runnable() {
            @Override
            public void run() {
                startRecognizeSpeech();
            }
        };

        startRecognizeSpeech();

    }

    private void initService(final LanguageConfig selectedLanguage) {
        final AIConfiguration.SupportedLanguages lang = AIConfiguration.SupportedLanguages.fromLanguageTag(selectedLanguage.getLanguageCode());
        final AIConfiguration config = new AIConfiguration(selectedLanguage.getAccessToken(),
                lang,
                AIConfiguration.RecognitionEngine.System);

        if (aiService != null) {
            aiService.pause();
        }

        aiService = AIService.getService(this, config);
        aiService.setListener(this);
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
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        l = lm.getLastKnownLocation(provider);
                        if (l != null) {
                            //get latitude and longitude of the location
                            lng = l.getLongitude();
                            lat = l.getLatitude();
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


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_aiservice_sample, menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        // use this method to disconnect from speech recognition service
        // Not destroying the SpeechRecognition object in onPause method would block other apps from using SpeechRecognition service
        if (aiService != null) {
            aiService.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // use this method to reinit connection to recognition service
        if (aiService != null) {
            aiService.resume();
        }
    }

    public void startRecognition(final View view) {
        aiService.startListening();
    }

    public void stopRecognition(final View view) {
        aiService.stopListening();
    }

    public void cancelRecognition(final View view) {
        aiService.cancel();
    }

    @Override
    public void onResult(final AIResponse response) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onResult");

                resultTextView.setText(gson.toJson(response));

                Log.i(TAG, "Received success response");

                // this is example how to get different parts of result object
                final Status status = response.getStatus();
                Log.i(TAG, "Status code: " + status.getCode());
                Log.i(TAG, "Status type: " + status.getErrorType());

                final Result result = response.getResult();
                Log.i(TAG, "Resolved query: " + result.getResolvedQuery());

                Log.i(TAG, "Action: " + result.getAction());

                final String speech = result.getFulfillment().getSpeech();
                Log.i(TAG, "Speech: " + speech);
                TTS.speak(speech);

                final Metadata metadata = result.getMetadata();
                if (metadata != null) {
                    Log.i(TAG, "Intent id: " + metadata.getIntentId());
                    Log.i(TAG, "Intent name: " + metadata.getIntentName());
                }

                final HashMap<String, JsonElement> params = result.getParameters();
                if (params != null && !params.isEmpty()) {
                    Log.i(TAG, "Parameters: ");
                    for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
                        Log.i(TAG, String.format("%s: %s", entry.getKey(), entry.getValue().toString()));
                    }
                }
            }

        });
    }

    @Override
    public void onError(final AIError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultTextView.setText(error.toString());
            }
        });
    }

    @Override
    public void onAudioLevel(final float level) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                float positiveLevel = Math.abs(level);

                if (positiveLevel > 100) {
                    positiveLevel = 100;
                }
                progressBar.setProgress((int) positiveLevel);
            }
        });
    }

    @Override
    public void onListeningStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    @Override
    public void onListeningCanceled() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultTextView.setText("");
            }
        });
    }

    @Override
    public void onListeningFinished() {
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected void onStart() {
        super.onStart();

        checkAudioRecordPermission();

        final LanguageConfig selectedLanguage = Config.language;
        initService(selectedLanguage);

        final Handler handler = new Handler();
        final int delay = 10000; //milliseconds

        handler.postDelayed(new Runnable() {
            public void run() {
                //do something
                String fcm = FirebaseInstanceId.getInstance().getToken();
                String locationUrl = "https://safe-india-initiative-api.herokuapp.com/api/user-location";
                String locationPosData = "{\"userDetails\": {\"userId\": \"" + deviceId  + "\",\"lat\":" + lat + ",\"long\":" + lng + ", \"fcm\":\"" + fcm + "\" }}";

                if(lat != 0 && lng != 0){
                    new SendPostRequest().execute(locationUrl, locationPosData);
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide permission to access user location",
                            Toast.LENGTH_LONG).show();
                }

                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    private void startActivity(Class<?> cls) {
        final Intent intent = new Intent(this, cls);
        startActivity(intent);
    }


    // TRY USING ANDROID SPEACHRECOGNIZR
    private SpeechRecognizer recog;
    private Runnable readyRecognizeSpeech;
    private Handler handler = new Handler();


    private void startRecognizeSpeech() {
        handler.removeCallbacks(readyRecognizeSpeech);

        Intent intent = RecognizerIntent.getVoiceDetailsIntent(getApplicationContext());
        recog.startListening(intent);
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

    private class RecogListener implements RecognitionListener {
        private ListenToUserActivity caller;
        private TextView status;
        private TextView subStatus;
        private ProgressBar progressBar;
        private AIService aiService;

        RecogListener(ListenToUserActivity a) {
            caller = a;
            status = (TextView) a.findViewById(R.id.status);
            subStatus = (TextView) a.findViewById(R.id.sub_status);
            progressBar = (ProgressBar) a.findViewById(R.id.progressBar);
            aiService = (AIService) a.aiService;
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            status.setText("Speak now...");
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            //status.setText("onBufferReceived");
            //Log.v(TAG,"onBufferReceived");
        }

        float maxRms = -10;

        @Override
        public void onRmsChanged(float rmsdB) {
            if (rmsdB > maxRms) {
                maxRms = rmsdB;
            }

            String s = String.format("Max volume : % 2.2f[dB]", maxRms);
            subStatus.setText(s);
            progressBar.setProgress((int) rmsdB);

            //Log.v(TAG,"recieve : " + rmsdB + "dB");
        }

        @Override
        public void onEndOfSpeech() {
            status.setText("");
            caller.handler.postDelayed(caller.readyRecognizeSpeech, 500);
        }

        @Override
        public void onError(int error) {
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    subStatus.setText("ERROR_AUDIO");
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    subStatus.setText("ERROR_CLIENT");
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    subStatus.setText("ERROR_INSUFFICIENT_PERMISSIONS");
                    checkAudioRecordPermission();
                    caller.handler.postDelayed(caller.readyRecognizeSpeech, 1000);
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    subStatus.setText("ERROR_NETWORK");
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    subStatus.setText("ERROR_NETWORK_TIMEOUT");
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    subStatus.setText("ERROR_NO_MATCH");
                    caller.handler.postDelayed(caller.readyRecognizeSpeech, 1000);
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    subStatus.setText("ERROR_RECOGNIZER_BUSY");
                    caller.handler.postDelayed(caller.readyRecognizeSpeech, 1000);
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    subStatus.setText("ERROR_SERVER");
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    subStatus.setText("ERROR_SPEECH_TIMEOUT");
                    caller.handler.postDelayed(caller.readyRecognizeSpeech, 1000);
                    break;
                default:
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onResults(Bundle data) {
            ArrayList<String> results = data.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            for (String s : results) {

                String triggerWord = "hello";
                if (s.equals(triggerWord) || s.startsWith(triggerWord + " ") || s.endsWith(" " + triggerWord) || s.indexOf(" " + triggerWord + " ") > -1) {
                    String fcm = FirebaseInstanceId.getInstance().getToken();
                    String helpUrl = "https://safe-india-initiative-api.herokuapp.com/api/help";
                    String helpPosData = "{\"userDetails\": {\"userId\": \"" + deviceId  + "\",\"lat\":" + lat + ",\"long\":" + lng + ", \"fcm\":\"" + fcm + "\" }}";

                    if(lat != 0 && lng != 0){
                        Log.d("API call", "POST /help " + helpPosData);
                        new SendPostRequest().execute(helpUrl, helpPosData);

                        Intent intent = new Intent(this.caller, HelpArrivingMapsActivity.class);
                        startActivity(intent);

                    } else {
                        Toast.makeText(getApplicationContext(), "Please provide permission to access user location",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }

            maxRms = -10;
            boolean end = false;
            for (String s : results) {
                if (s.equals("Help"))
                    end = true;
                if (s.equals("Bachao"))
                    end = true;
            }

            //caller.startRecognizeSpeech();
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
