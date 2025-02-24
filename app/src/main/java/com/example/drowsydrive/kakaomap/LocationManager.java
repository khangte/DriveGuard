package com.example.drowsydrive.kakaomap;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.drowsydrive.ui.MainActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.kakao.vectormap.LatLng;

public class LocationManager {
    private static final String TAG = "LocationManager";

    private final FusedLocationProviderClient fusedLocationClient; // 위치 제공 클라이언트
    private final LocationRequest locationRequest; // 위치 요청 설정
    private final MainActivity mainActivity; // MainActivity 참조
    private boolean requestingLocationUpdates = false; // 위치 업데이트 요청 상태
    private final LocationCallback locationCallback; // 위치 콜백
    private Location lastKnownLocation;
    LatLng currentPosition;

    public LocationManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(mainActivity);
        this.locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L) // 업데이트 간격 5초
                .setMinUpdateIntervalMillis(2000L) // 최소 업데이트 간격 2초
                .setMaxUpdateDelayMillis(10000L)  // 최대 지연 시간 10초
                .build();

        // 위치 콜백 정의
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        long locationAge = System.currentTimeMillis() - location.getTime();
                        if (locationAge < 5000) { // 5초 이내 위치 정보만 사용
                            Log.d(TAG, "onLocationResult: 새로운 위치 수신 - 위도: "
                                    + location.getLatitude() + ", 경도: " + location.getLongitude());
                            lastKnownLocation = location;
                            //activity.mapManager.updateLabelPosition(LatLng.from(location.getLatitude(), location.getLongitude()));
                        } else {
                            Log.w(TAG, "오래된 위치 정보 수신, 무시됨.");
                        }
                    }
                }
            }
        };
    }

    // 현재 위치 가져오기 및 맵 초기화
    @SuppressLint("MissingPermission")
    public void getStartLocation(MapManager mapManager) {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(mainActivity, location -> {
                    if (location != null) {
                        LatLng startPosition = LatLng.from(location.getLatitude(), location.getLongitude());
                        mapManager.initializeMap(startPosition, 17); // 줌 레벨 17로 맵 초기화
                        Log.d(TAG, "초기 위치 설정 완료: " + startPosition);
                    } else {
                        Log.w(TAG, "현재 위치 정보를 가져오지 못했습니다.");
                        Toast.makeText(mainActivity, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "현재 위치 요청 중 오류 발생", e);
                    Toast.makeText(mainActivity, "위치 요청 실패", Toast.LENGTH_SHORT).show();
                });
    }


    // 위치 업데이트 시작
    @SuppressLint("MissingPermission")
    public void startLocationUpdates() {
        try {
            requestingLocationUpdates = true;
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            Log.d(TAG, "위치 업데이트 요청 성공");
        } catch (SecurityException e) {
            Log.e(TAG, "위치 업데이트 요청 실패: 권한 없음", e);
            Toast.makeText(mainActivity, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // 위치 업데이트 중지
    public void stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            requestingLocationUpdates = false;
            Log.d(TAG, "위치 업데이트 중지 완료");
        } catch (Exception e) {
            Log.e(TAG, "위치 업데이트 중지 중 오류 발생", e);
        }
    }

    // 종료 메서드 추가
    public void shutdown() {
        if (requestingLocationUpdates) {
            stopLocationUpdates(); // 위치 업데이트 중지
        }
        Log.d(TAG, "LocationManager shutdown 호출 완료.");
    }

    public boolean isRequestingLocationUpdates() {
        return requestingLocationUpdates; // 위치 업데이트 요청 상태 반환
    }

    // 현재 위치 반환
    public LatLng getCurrentLocation() {
        if (lastKnownLocation != null) {
            LatLng position = LatLng.from(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            Log.d(TAG, "현재 위치 반환: 위도 " + position.getLatitude() + ", 경도 " + position.getLongitude());
            return position;
        }
        Log.w(TAG, "위치 정보가 없습니다.");
        return null;
    }

    // 현재 위치로 카메라 이동
    public void returnToMyLocation(MapManager mapManager) {
        currentPosition = getCurrentLocation(); // 현재 위치 정보 가져오기

        if (currentPosition != null) {
            mapManager.moveCameraToCurrentLocation(currentPosition); // 현재 위치로 카메라 이동
            Log.d(TAG, "카메라를 현재 위치로 이동");
        } else {
            Toast.makeText(mainActivity, "위치 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show(); // 위치 정보 없음 오류 메시지
        }
    }
}
