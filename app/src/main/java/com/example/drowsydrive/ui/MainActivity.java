package com.example.drowsydrive.ui;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import android.graphics.Canvas;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.drowsydrive.R;
import com.example.drowsydrive.arduino.ArduinoManager;
import com.example.drowsydrive.notification.AlarmManager;
import com.example.drowsydrive.notification.TTSManager;
import com.example.drowsydrive.util.BluetoothManager;
import com.example.drowsydrive.kakaomap.LocationManager;
import com.example.drowsydrive.mlkit.CameraController;
import com.example.drowsydrive.mlkit.FaceDetectionManager;
import com.example.drowsydrive.notification.SoundManager;
import com.example.drowsydrive.notification.AlarmDialog;
import com.example.drowsydrive.util.PermissionManager;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;

import java.io.InputStream;
import java.util.Objects;

public class MainActivity extends AppCompatActivity{
    public LocationManager locationManager; // 위치 관리를 위한 LocationManager 인스턴스
    protected ActivityUtils activityUtils;
    private FaceDetectionManager faceDetectionManager;
    private SoundManager soundManager;
    private PermissionManager permissionManager;
    private BluetoothManager bluetoothManager;
    private CameraController cameraController;
    private TTSManager ttsManager;
    private ArduinoManager arduinoManager;
    private AlarmManager alarmManager;

    protected InputStream inputStream; // Bluetooth로 받은 데이터 스트림
    private boolean isListening = true; // 데이터 수신 상태

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 레이아웃 설정

        // 셀프 카메라를 위한 PreviewView
        PreviewView cameraPreviewView = findViewById(R.id.camera_preview); // PreviewView 초기화

        initializeManagers();
        permissionManager.checkAndRequestPermissions(locationManager);
        locationManager.startLocationUpdates();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothManager.requestBluetoothPermission();
        }
        if (bluetoothManager.isBluetoothEnabled()) {
            bluetoothManager.connectToDevice(); // 블루투스 연결 시도
        } else {
            Log.e("MainActivity", "블루투스가 비활성화되어 있습니다.");
        }

        cameraController = new CameraController(this, cameraPreviewView, this::analyzeImage);
        cameraController.startCamera();

        // CO2 데이터 수신 시작
        listenForData();
        // CO2 경고 콜백 설정
        arduinoManager.setCO2AlertCallback(co2Level -> runOnUiThread(this::playCO2Alarm));

        activityUtils.setupBackButtonHandler();

    }

    private void initializeManagers() {
        locationManager = new LocationManager(this); // LocationManager 초기화
        activityUtils = new ActivityUtils(this);
        faceDetectionManager = new FaceDetectionManager();
        ttsManager = new TTSManager(this);
        soundManager = new SoundManager(this, R.raw.alarm_sound, ttsManager);
        permissionManager = new PermissionManager(this);
        arduinoManager = new ArduinoManager();
        bluetoothManager = new BluetoothManager(this, arduinoManager);
        alarmManager = new AlarmManager(soundManager, activityUtils);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraController.startCamera();
        if (locationManager != null && !locationManager.isRequestingLocationUpdates()) {
            locationManager.startLocationUpdates(); // 위치 업데이트 시작
        }
    }

    @Override
    protected void onPause() {
        if (locationManager != null && locationManager.isRequestingLocationUpdates()) {
            locationManager.stopLocationUpdates(); // 위치 업데이트 중지
        }
        cameraController.stopCamera();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (locationManager != null) {
            locationManager.shutdown(); // LocationManager 종료
        }
        soundManager.release();
        if (bluetoothManager != null) {
            bluetoothManager.closeConnection();
        }
        cameraController.stopCamera();
        ttsManager.shutdown();
        super.onDestroy();
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeImage(ImageProxy imageProxy) {
        faceDetectionManager.analyzeImage(
                InputImage.fromMediaImage(
                        Objects.requireNonNull(imageProxy.getImage()),
                        imageProxy.getImageInfo().getRotationDegrees()
                ),
                imageProxy,
                new FaceDetectionManager.FaceDetectionCallback() {
                    @Override
                    public void onEyesClosed() {
                        playDrowsinessAlarmForClosedEyes();
                    }

                    @Override
                    public void onYawnDetected() {
                        playDrowsinessAlarmForYawning();
                    }

                    @Override
                    public void onFaceDetected(Canvas canvas, Face face) {
                        // 얼굴이 감지되면 Canvas에 프레임을 그리도록 함
                        if (canvas != null) {
                            faceDetectionManager.drawFaceFrame(canvas, face.getBoundingBox()); // 프레임 그리기
                        } else {
                            Log.e("FaceDetection", "Canvas is null when drawing face frame.");
                        }
                    }
                }
        );
    }

    // 눈을 감고 있을 때 알람 처리
    private void playDrowsinessAlarmForClosedEyes() {
        alarmManager.playWarningAlarm(this, AlarmDialog.AlertType.DROWSINESS); // 졸음 알림
    }

    // 하품할 때 알람 처리
    private void playDrowsinessAlarmForYawning() {
       alarmManager.playWarningAlarm(this, AlarmDialog.AlertType.YAWN); // 하품 알림
    }

    private void playCO2Alarm() {
       alarmManager.playWarningAlarm(this, AlarmDialog.AlertType.CO2); // 하품 알림
    }

    @SuppressLint("SetTextI18n")
    private void listenForData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (isListening) {
                try {
                    if (inputStream != null && inputStream.available() > 0) {
                        bytes = inputStream.read(buffer);
                        String data = new String(buffer, 0, bytes).trim();

                        // 데이터를 수신한 후 UI 스레드에서 CO2 값을 업데이트
                        runOnUiThread(() -> {
                            TextView co2TextView = findViewById(R.id.co2_text);
                            co2TextView.setText("CO2: " + data + " ppm"); // 수신된 CO2 농도를 표시
                        });

                        Log.d("CO2", "수신된 데이터: " + data + " ppm");
                    }
                } catch (Exception e) {
                    Log.e("CO2", "데이터 수신 중 오류 발생", e);
                    isListening = false;
                    break;
                }
            }
        }).start();
    }
}
