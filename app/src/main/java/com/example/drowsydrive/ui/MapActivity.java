package com.example.drowsydrive.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.drowsydrive.R;
import com.example.drowsydrive.kakaomap.LocationManager;
import com.example.drowsydrive.kakaomap.MapManager;
import com.kakao.vectormap.MapView;

public class MapActivity extends AppCompatActivity {
    public LocationManager locationManager;
    protected MapManager mapManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map); // activity_map 레이아웃 연결

        MapView mapView = findViewById(R.id.map_view);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        mapManager = new MapManager(this, mapView, progressBar);

        locationManager.getStartLocation(mapManager);
        locationManager.startLocationUpdates(); // 위치 업데이트 시작

        // "내 위치로 돌아가기" 버튼 클릭 이벤트 처리
        Button returnToMyLocationButton = findViewById(R.id.btn_return_to_my_location);
        returnToMyLocationButton.setOnClickListener(v -> locationManager.returnToMyLocation(mapManager));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapManager != null) {
            mapManager.onResume(); // MapManager에서 지도 생명주기 resume 관리
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapManager != null) {
            mapManager.onPause(); // MapManager에서 지도 생명주기 pause 관리
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapManager != null) {
            mapManager.onDestroy(); // MapManager에서 지도 생명주기 destroy 관리
        }
    }
}
