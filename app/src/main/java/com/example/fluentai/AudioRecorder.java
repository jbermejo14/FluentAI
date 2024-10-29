package com.example.fluentai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AudioRecorder extends AppCompatActivity {

    private static final String TAG = "AudioRecorder";
    private ImageButton recordButton;
    private TextView text;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;

    Boolean get_message = false;
    private AmazonS3Client s3Client;
    private File audioFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_recorder);

        recordButton = findViewById(R.id.btnRecord);
        text = findViewById(R.id.textView);
        initializeS3Client();

        // Check and request audio permissions
        if (!checkPermission()) {
            getMicrophonePermission();
        }

        // Start or stop recording on button click
        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecordingAndUpload();
            } else {
                startRecording();
            }
        });
    }

    private void initializeS3Client() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        s3Client = new AmazonS3Client(awsCredentials);
    }

    private void waitForText() {
      String apiUrl = "https://trk7vouppc.execute-api.us-east-1.amazonaws.com/dev/getTranscription";
        StringBuilder response = new StringBuilder();

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");  // Set the request to GET

            // Check if the request was successful
            if (connection.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                text.setText(response.toString());
                reader.close();
                get_message= true;
            } else {
                get_message = false;
                System.out.println("Failed to call API: " + connection.getResponseCode());
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void startRecording() {
        // Set up the MediaRecorder for audio recording
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioSamplingRate(16000); // Sample rate for better quality
        recordButton.setImageResource(R.drawable.stop_white);

        // File to save audio
        audioFile = new File(getExternalFilesDir(null), "recorded_audio.mp3");
        mediaRecorder.setOutputFile(audioFile.getAbsolutePath());

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            Log.d(TAG, "Recording started");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Recording failed to start", e);
        }
    }



    private void stopRecordingAndUpload() {
        if (mediaRecorder != null && isRecording) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            Log.d(TAG, "Recording stopped");
            recordButton.setImageResource(R.drawable.play_white);
            // Upload the completed file to S3
            uploadToS3(audioFile);
            text.setText("Waiting for text");
        }

//        TODO
//          DOESN'T WORK BECAUSE WHILES DON'T WORK WELL IN ANDROID, USE ASYNC USE (CHATGPT EXPLANATION)
//        while (get_message == false) {
//            waitForText();
//        }
    }

    private void uploadToS3(File file) {
        new Thread(() -> {
            try {
                String fileName = "streamed_audio_" + UUID.randomUUID().toString() + ".mp3"; // Define S3 path
                s3Client.putObject(bucketName, fileName, file);
                Log.d(TAG, "File uploaded to S3: " + fileName);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to upload file", e);
            }
        }).start();
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void getMicrophonePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 200);
    }
}
