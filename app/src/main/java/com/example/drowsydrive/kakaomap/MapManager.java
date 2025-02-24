package com.example.drowsydrive.kakaomap;

import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.drowsydrive.R;
import com.example.drowsydrive.ui.MapActivity;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelManager;
import com.kakao.vectormap.label.TrackingManager;

public class MapManager {
    private static final String TAG = "MapManager";

    private final MapActivity mapActivity; // MainActivity 인스턴스
    protected final MapView mapView; // MapView 인스턴스
    private final ProgressBar progressBar; // 로딩 상태를 보여주는 ProgressBar 인스턴스
    private KakaoMap kakaoMap; // KakaoMap 인스턴스
    private Label centerLabel; // 현재 위치를 표시하는 레이블

    // 생성자: MainActivity, MapView, ProgressBar를 인자로 받아 초기화
    public MapManager(MapActivity mapActivity, MapView mapView, ProgressBar progressBar) {
        this.mapActivity = mapActivity;
        this.mapView = mapView;
        this.progressBar = progressBar;
    }

    // 지도를 초기화하는 메서드
    public void initializeMap(LatLng startPosition, int startZoomLevel) {
        mapView.start(new MapLifeCycleCallback() { // 지도 생명주기 콜백 설정
            @Override
            public void onMapDestroy() {
                Log.d(TAG, "onMapDestroy"); // 지도 파괴 로그
                Toast.makeText(mapActivity, "Map Destroyed", Toast.LENGTH_SHORT).show(); // 지도 파괴 메시지
            }

            @Override
            public void onMapError(Exception error) {
                Log.e(TAG, "onMapError", error); // 오류 로그
                Toast.makeText(mapActivity, error.getMessage(), Toast.LENGTH_SHORT).show(); // 오류 메시지 표시
            }
        }, new KakaoMapReadyCallback() { // 지도 준비 완료 콜백 설정
            @Override
            public void onMapReady(@NonNull KakaoMap map) {
                kakaoMap = map; // KakaoMap 인스턴스 저장
                progressBar.setVisibility(View.GONE); // ProgressBar 숨김

                // 지도 초기화 후 레이블 추가
                if (kakaoMap != null) {
                    LabelManager labelManager = kakaoMap.getLabelManager(); // 레이블 매니저 가져오기
                    if (labelManager != null) {
                        LabelLayer layer = labelManager.getLayer(); // 레이블 레이어 가져오기
                        if (layer != null) {
                            // 중심 레이블 추가
                            centerLabel = layer.addLabel(LabelOptions.from("centerLabel", startPosition)
                                    .setStyles(LabelStyle.from(R.drawable.current_location).setAnchorPoint(0.5f, 0.5f))
                                    .setRank(1));
                            if (centerLabel != null) {
                                // 레이블 추적 시작
                                TrackingManager trackingManager = kakaoMap.getTrackingManager();
                                if (trackingManager != null) {
                                    trackingManager.startTracking(centerLabel);
                                }
                            }
                            // 카메라 이동 시작 시 추적 중지
                            kakaoMap.setOnCameraMoveStartListener((kakaoMap, gestureType) -> {
                                TrackingManager trackingManager = kakaoMap.getTrackingManager();
                                if (trackingManager != null) {
                                    trackingManager.stopTracking(); // 카메라 이동 중 추적 중지
                                }
                            });
                        }
                    }
                }
                mapActivity.locationManager.startLocationUpdates(); // 위치 업데이트 시작
            }

            @NonNull
            @Override
            public LatLng getPosition() {
                return startPosition; // 시작 위치 반환
            }

            @Override
            public int getZoomLevel() {
                return startZoomLevel; // 시작 줌 레벨 반환
            }
        });
    }

    public void onResume() {
        mapView.resume();     // MapView 의 resume 호출
    }

    public void onPause() {
        mapView.pause();    // MapView 의 pause 호출
    }

    public void onDestroy() {
        if (mapView != null) {
            mapView.destroyDrawingCache(); // MapView 자원 해제
        }
    }

    // 레이블의 위치를 업데이트하는 메서드
    public void updateLabelPosition(LatLng newPosition) {
        if (centerLabel != null) {
            centerLabel.moveTo(newPosition); // 레이블을 새로운 위치로 이동
        }
    }

    // 현재 위치로 카메라 이동하는 메서드
    public void moveCameraToCurrentLocation(LatLng currentPosition) {
        if (kakaoMap != null) {
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(currentPosition)); // 현재 위치로 카메라 이동
        } else {
            Log.e(TAG, "KakaoMap is null"); // KakaoMap이 null인 경우 로그
        }
    }
}
