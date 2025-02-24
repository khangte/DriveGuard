package com.example.drowsydrive.util;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.drowsydrive.ui.MainActivity;

public class LocationPermissionManager {
    private final MainActivity mainActivity;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private final String[] locationPermissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };

    public LocationPermissionManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocationPermissions() {
        ActivityCompat.requestPermissions(mainActivity, locationPermissions, LOCATION_PERMISSION_REQUEST_CODE);
    }

    public void showPermissionDeniedDialog() {
        new AlertDialog.Builder(mainActivity)
                .setMessage("위치 권한이 필요합니다. 설정으로 이동하시겠습니까?")
                .setPositiveButton("설정", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", mainActivity.getPackageName(), null);
                    intent.setData(uri);
                    mainActivity.startActivity(intent);
                })
                .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }
}
