package com.example.fluentai;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import io.github.cdimascio.dotenv.Dotenv;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.*;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class AudioRecorder extends AppCompatActivity {

    private ImageButton recordButton;
    private boolean isRecording = false;
    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    private AudioRecord audioRecord;

//    Dotenv dotenv = Dotenv.load();
//
//    private String accessKey = dotenv.get("AWS_ACCESS_KEY_ID");
//    private String secretKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
//    private String bucketName = dotenv.get("AWS_STORAGE_BUCKET_NAME");
//    private String region = dotenv.get("AWS_S3_REGION_NAME");


    AmazonS3Client s3Client;

    private TransferUtility transferUtility;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_recorder);
        recordButton = findViewById(R.id.btnRecord);

        if (checkPermission()) {
            getMicrophonePermission();
            recordButton.setOnClickListener(v -> {
                if (isRecording) {
                    stopRecording();
                } else {
                    initializeS3Client();
                    startRecording();
                }
            });
        }
    }

    private void initializeS3Client() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        s3Client = new AmazonS3Client(awsCredentials);
    }

    private void startRecording() {
        if (checkPermission()) {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
            audioRecord.startRecording();
            isRecording = true;
        }

        new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (isRecording) {
                int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    uploadToS3(buffer, bytesRead);
                }
            }
        }).start();
    }

    private void uploadToS3(byte[] audioData, int length) {
        try {
            InputStream inputStream = new ByteArrayInputStream(audioData, 0, length);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(length);

            s3Client.putObject(bucketName, "audio/streamed_audio.pcm", inputStream, metadata);
            Log.d("AudioStreaming", "Uploaded chunk to S3");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if (audioRecord != null) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    private boolean checkPermission() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }

    private void getMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED) {
            int MICROPHONE_PERMISSION_CODE = 200;
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_CODE);
        }
    }

}
