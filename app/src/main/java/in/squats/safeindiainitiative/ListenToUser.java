package in.squats.safeindiainitiative;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;

public class ListenToUser {
    private final String TAG = ListenToUser.class.getSimpleName();

    private SpeechRecognizer recog;
    private Runnable readyRecognizeSpeech;
    private Handler handler = new Handler();
    private Context appContext;
    private MainActivity parentActivity;

    public ListenToUser(Context appContext, MainActivity parentActivity) {
        this.appContext = appContext;
        this.parentActivity = parentActivity;
    }

    public void start() {
        Log.d(TAG, "onStartCommand");

        if (recog != null) {
            recog.destroy();
        }
        recog = SpeechRecognizer.createSpeechRecognizer(appContext);
        recog.setRecognitionListener(new SpeechRecognizrListener(this));

        readyRecognizeSpeech = new Runnable() {
            @Override
            public void run() {
                startRecognizeSpeech();
            }
        };
        startRecognizeSpeech();
    }

    private void startRecognizeSpeech() {
        Log.d(TAG, "startRecognizeSpeech");
        handler.removeCallbacks(readyRecognizeSpeech);
        Intent intent = RecognizerIntent.getVoiceDetailsIntent(appContext);
        recog.startListening(intent);
    }

    public void checkAudioRecordPermission() {
        parentActivity.requestAudioRecordPermission();
    }

    private class SpeechRecognizrListener implements RecognitionListener {
        private final String SRTAG = SpeechRecognizrListener.class.getSimpleName();
        private ListenToUser caller;
        private float maxRms = -10;

        SpeechRecognizrListener(ListenToUser caller) {
            Log.v(SRTAG, "Constructor");
            this.caller = caller;
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.v(SRTAG, "Speak now...");
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }


        @Override
        public void onRmsChanged(float rmsdB) {
            if (rmsdB > maxRms) {
                maxRms = rmsdB;
            }

            Log.v(SRTAG, "onRmsChanged:" + String.format("Max volume : % 2.2f[dB]", maxRms));
            Log.v(SRTAG, "onRmsChanged:" + String.format("Receive : " + rmsdB + "dB"));
        }

        @Override
        public void onEndOfSpeech() {
            Log.v(SRTAG, "End of speech");
            caller.handler.postDelayed(caller.readyRecognizeSpeech, 500);
        }

        @Override
        public void onError(int error) {
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    Log.e(SRTAG, "ERROR_AUDIO");
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    Log.e(SRTAG, "ERROR_CLIENT");
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    Log.e(SRTAG, "ERROR_INSUFFICIENT_PERMISSIONS");

                    // check permission again and call speech recognizr
                    caller.checkAudioRecordPermission();
                    caller.handler.postDelayed(caller.readyRecognizeSpeech, 1000);
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    Log.e(SRTAG, "ERROR_NETWORK");
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    Log.e(SRTAG, "ERROR_NETWORK_TIMEOUT");
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    Log.e(SRTAG, "ERROR_NO_MATCH");

                    caller.handler.postDelayed(caller.readyRecognizeSpeech, 1000);
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    Log.e(SRTAG, "ERROR_RECOGNIZER_BUSY");
                    caller.handler.postDelayed(caller.readyRecognizeSpeech, 1000);
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    Log.e(SRTAG, "ERROR_SERVER");
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    Log.e(SRTAG, "ERROR_SPEECH_TIMEOUT");
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
            SafeIndiaApplication application = (SafeIndiaApplication) caller.parentActivity.getApplication();

            for (String s : results) {

                String[] triggerWords = new String[]{"help", "bachao"};
                for (String triggerWord : triggerWords) {
                    if (s.equals(triggerWord) || s.startsWith(triggerWord + " ") || s.endsWith(" " + triggerWord) || s.indexOf(" " + triggerWord + " ") > -1) {
                        Log.d(SRTAG, "Recognized word: " + triggerWord);

                        String fcm = FirebaseInstanceId.getInstance().getToken();
                        String helpUrl = "https://safe-india-initiative-api.herokuapp.com/api/help";
                        String helpPosData = "{\"userDetails\": {\"userId\": \"" + application.deviceId + "\",\"lat\":" + application.userLat + ",\"long\":" + application.userLat + ", \"fcm\":\"" + fcm + "\" }}";
                        new SendPostRequest(appContext).execute(helpUrl, helpPosData);
                    }
                }
            }

            maxRms = -10;
        }
    }
}
