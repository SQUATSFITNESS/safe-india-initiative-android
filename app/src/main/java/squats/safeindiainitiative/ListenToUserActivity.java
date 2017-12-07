package squats.safeindiainitiative;

import android.content.Intent;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        implements AIListener, AdapterView.OnItemSelectedListener {

    public static final String TAG = ListenToUserActivity.class.getName();


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

        recog = SpeechRecognizer.createSpeechRecognizer(this);
        recog.setRecognitionListener(new RecogListener(this));
        Intent speechintent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechintent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechintent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS , 100);
        speechintent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,  RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechintent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());


        readyRecognizeSpeech = new Runnable() {
            @Override public void run() {
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

//        final Handler handler = new Handler();
//        final int delay = 2000; //milliseconds
//
//        handler.postDelayed(new Runnable(){
//            public void run(){
//                //do something
//                aiService.startListening();
//
//                handler.postDelayed(this, delay);
//            }
//        }, delay);
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

    private static class RecogListener implements RecognitionListener {
        private ListenToUserActivity caller;
        private TextView status;
        private TextView subStatus;
        private ProgressBar progressBar;
        private AIService aiService;

        RecogListener(ListenToUserActivity a) {
            caller = a;
            status = (TextView)a.findViewById(R.id.status);
            subStatus = (TextView)a.findViewById(R.id.sub_status);
            progressBar = (ProgressBar) a.findViewById(R.id.progressBar);
            aiService = (AIService) a.aiService;
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            status.setText("ready for speech");
            Log.v(TAG,"ready for speech");
        }

        @Override
        public void onBeginningOfSpeech() {
            status.setText("beginning of speech");
            Log.v(TAG,"beginning of speech");
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            //status.setText("onBufferReceived");
            //Log.v(TAG,"onBufferReceived");
        }

        float maxRms = -10;
        @Override
        public void onRmsChanged(float rmsdB) {
            if(rmsdB > maxRms) {
                maxRms = rmsdB;
            }

            String s = String.format("Max volume : % 2.2f[dB]", maxRms);
            subStatus.setText(s);
            progressBar.setProgress((int) rmsdB);

            //Log.v(TAG,"recieve : " + rmsdB + "dB");
        }

        @Override
        public void onEndOfSpeech() {
            status.setText("end of speech");
            Log.v(TAG,"end of speech");
            caller.handler.postDelayed(caller.readyRecognizeSpeech, 500);
        }

        @Override
        public void onError(int error) {
            status.setText("on error");
            Log.v(TAG,"on error");
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    subStatus.setText("ERROR_AUDIO");
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    subStatus.setText("ERROR_CLIENT");
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    subStatus.setText("ERROR_INSUFFICIENT_PERMISSIONS");
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    subStatus.setText("ERROR_NETWORK");
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    subStatus.setText("ERROR_NETWORK_TIMEOUT");
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    subStatus.setText("ERROR_NO_MATCH");
                    caller.handler.postDelayed(caller.readyRecognizeSpeech,1000);
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    subStatus.setText("ERROR_RECOGNIZER_BUSY");
                    caller.handler.postDelayed(caller.readyRecognizeSpeech,1000);
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    subStatus.setText("ERROR_SERVER");
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    subStatus.setText("ERROR_SPEECH_TIMEOUT");
                    caller.handler.postDelayed(caller.readyRecognizeSpeech,1000);
                    break;
                default:
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            status.setText("on event");
            Log.v(TAG,"on event");
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            status.setText("on partial results");
            Log.v(TAG,"on results");
        }

        @Override
        public void onResults(Bundle data) {
            status.setText("on results");
            Log.v(TAG,"on results");

            ArrayList<String> results = data.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            TextView t = (TextView)caller.findViewById(R.id.resultTextView);
            t.setText("");
            for (String s : results) {
                t.append(s + "\n");
            }

            maxRms = -10;
            boolean end=false;
            for (String s : results) {
                if (s.equals("Help"))
                    end=true;
                if (s.equals("Bachao"))
                    end=true;
            }

            //caller.startRecognizeSpeech();
        }
    }
}
