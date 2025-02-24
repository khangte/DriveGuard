package com.example.drowsydrive.util;

import com.example.drowsydrive.kakaomap.LocationManager;
import com.example.drowsydrive.ui.MainActivity;

public class PermissionManager {
    private final LocationPermissionManager locationPermissionManager;
    private final CameraPermissionManager cameraPermissionManager;

    public PermissionManager(MainActivity mainActivity) {
        this.locationPermissionManager = new LocationPermissionManager(mainActivity);
        this.cameraPermissionManager = new CameraPermissionManager(mainActivity);
    }

    public void checkAndRequestPermissions(LocationManager locationManager) {
        // 위치 권한 확인
        if (locationPermissionManager.checkLocationPermissions()) {
            locationManager.getCurrentLocation();
        } else {
            locationPermissionManager.showPermissionDeniedDialog();
            locationPermissionManager.requestLocationPermissions();
        }

        // 카메라 권한 확인
        if (!cameraPermissionManager.checkCameraPermission()) {
            cameraPermissionManager.showCameraPermissionDeniedDialog();
            cameraPermissionManager.requestCameraPermission();
        }
    }
}
