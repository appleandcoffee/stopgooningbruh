package com.avi.stopgooning;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
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

    private PreviewView previewView;
    private TextView counterText;
    private int pushupCount = 0;
    private boolean isDown = false;
    private PoseDetector poseDetector;
    private static final int CAMERA_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Window Flags (Must be before setContentView)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_pushup);

        // 2. Initialize Views IMMEDIATELY
        previewView = findViewById(R.id.previewView);
        counterText = findViewById(R.id.counterText);

        // 3. Setup Pose Detector
        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();
        poseDetector = PoseDetection.getClient(options);

        // 4. Start permission flow
        checkPermissionAndStart();
    }

    private void checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            // Wait for previewView to be measured before starting camera
            if (previewView != null) {
                previewView.post(this::startCamera);
            }
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

                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("CameraX", "Binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImage(ImageProxy imageProxy) {
        if (imageProxy == null || imageProxy.getImage() == null) {
            if (imageProxy != null) imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        poseDetector.process(image)
                .addOnSuccessListener(pose -> {
                    // Try Left landmarks first
                    PoseLandmark shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
                    PoseLandmark elbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
                    PoseLandmark wrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);

                    // If left isn't visible, try right
                    if (shoulder == null) {
                        shoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
                        elbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
                        wrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
                    }

                    if (shoulder != null && elbow != null && wrist != null) {
                        double angle = getAngle(shoulder, elbow, wrist);
                        checkPushup(angle);
                    }
                })
                .addOnFailureListener(e -> Log.e("MLKit", "Detection failed", e))
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
        } else if (isDown && angle > 150) {
            pushupCount++;
            isDown = false;
            runOnUiThread(() -> {
                counterText.setText("Pushups: " + pushupCount + " / 10");
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
        }
    }
}