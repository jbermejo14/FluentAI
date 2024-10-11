package com.example.fluentai;

import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

import java.io.IOException;

public class AudioRecorder extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 200;
    private MediaRecorder mediaRecorder;
    private ImageButton recordButton;
    private boolean isRecording = false;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                Log.d("PermissionResult", "Permission: " + permissions[i] + ", Granted: " + (grantResults[i] == PackageManager.PERMISSION_GRANTED));
            }
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "Permissions are required to record audio.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_recorder);

        recordButton = findViewById(R.id.btnRecord);
        checkAndRequestPermissions();

        // Handle button click to start/stop recording
        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                if (checkPermissions()) { // Check permissions before starting recording
                    startRecording();
                } else {
                    Toast.makeText(this, "Permissions are required to record audio.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void startRecording() {
        String audioFilePath = getExternalFilesDir(null).getAbsolutePath() + "/audio_record.3gp";
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(audioFilePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            recordButton.setImageResource(R.drawable.stop_recording);  // Change to stop icon
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private boolean checkPermissions() {
        boolean audioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        Log.d("Permissions", "Audio Permission: " + audioPermission);
        Log.d("Permissions", "Storage Permission: " + storagePermission);

        return audioPermission && storagePermission;
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSIONS);
        } else {
            // Permissions are already granted; you can start recording.
            Toast.makeText(this, "Permissions already granted.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
        isRecording = false;
        recordButton.setImageResource(R.drawable.play);  // Change back to record icon
    }
}
