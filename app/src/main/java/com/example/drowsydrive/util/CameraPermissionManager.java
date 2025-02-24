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

public class CameraPermissionManager {
    private final MainActivity mainActivity;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1002;

    private final String cameraPermission = Manifest.permission.CAMERA;

    public CameraPermissionManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(mainActivity, cameraPermission) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestCameraPermission() {
        ActivityCompat.requestPermissions(mainActivity, new String[]{cameraPermission}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    public void showCameraPermissionDeniedDialog() {
        new AlertDialog.Builder(mainActivity)
                .setMessage("카메라 권한이 필요합니다. 설정으로 이동하시겠습니까?")
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
