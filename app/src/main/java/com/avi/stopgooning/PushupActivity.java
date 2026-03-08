package com.avi.stopgooning;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

public class PushupActivity extends AppCompatActivity {

    private static final String TAG = "PushupActivity";
    private static final int CAMERA_PERMISSION_CODE = 100;

    private PreviewView previewView;
    private TextView tvCount;
    private int pushupCount = 0;
    private boolean isDown = false;
    private PoseDetector poseDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_pushup);

        previewView = findViewById(R.id.previewView);
        tvCount = findViewById(R.id.tvCount);

        if (previewView == null || tvCount == null) {
            Log.e(TAG, "Views not found – check layout IDs");
            Toast.makeText(this, "Layout error – cannot start", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();
        poseDetector = PoseDetection.getClient(options);

        checkPermissionAndStart();
    }

    private void checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::processImage);

                // Use BACK camera – this should map to your real webcam in emulator
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // If back camera is black → change to front for testing:
                // CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

                Log.i(TAG, "Camera bound successfully – using " +
                        (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA ? "BACK" : "FRONT"));

            } catch (Exception e) {
                Log.e(TAG, "Camera binding failed", e);
                Toast.makeText(this, "Camera failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImage(ImageProxy imageProxy) {
        Log.d(TAG, "New frame received");

        if (imageProxy == null || imageProxy.getImage() == null) {
            Log.w(TAG, "ImageProxy or media image is null");
            if (imageProxy != null) imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        poseDetector.process(image)
                .addOnSuccessListener(pose -> {
                    Log.d(TAG, "Pose processed – landmarks found");

                    PoseLandmark shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
                    PoseLandmark elbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
                    PoseLandmark wrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);

                    if (shoulder == null) {
                        shoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
                        elbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
                        wrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
                    }

                    if (shoulder != null && elbow != null && wrist != null) {
                        double angle = getAngle(shoulder, elbow, wrist);
                        Log.d(TAG, "Elbow angle: " + String.format("%.1f°", angle));
                        checkPushup(angle);
                    } else {
                        Log.d(TAG, "Key landmarks missing – body not fully visible");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Pose detection failed", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private double getAngle(PoseLandmark first, PoseLandmark mid, PoseLandmark last) {
        double result = Math.toDegrees(
                Math.atan2(last.getPosition().y - mid.getPosition().y, last.getPosition().x - mid.getPosition().x) -
                        Math.atan2(first.getPosition().y - mid.getPosition().y, first.getPosition().x - mid.getPosition().x)
        );
        result = Math.abs(result);
        if (result > 180) result = 360.0 - result;
        return result;
    }

    private void checkPushup(double angle) {
        if (angle < 90) {
            isDown = true;
            Log.d(TAG, "Down position detected");
        } else if (isDown && angle > 150) {
            isDown = false;
            pushupCount++;
            Log.i(TAG, "Push-up counted! Total: " + pushupCount);

            runOnUiThread(() -> {
                tvCount.setText("Pushups: " + pushupCount + " / 10");
                if (pushupCount >= 10) {
                    Toast.makeText(this, "RECOVERY COMPLETE", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}