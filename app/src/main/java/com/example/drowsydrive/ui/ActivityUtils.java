package com.example.drowsydrive.ui;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.appcompat.app.AlertDialog;

import android.app.Dialog;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.example.drowsydrive.R;
import com.example.drowsydrive.kakaomap.KakaoMapSearchManager;
import com.kakao.vectormap.LatLng;

/**
 * ActivityUtils
 * MainActivity와 관련된 유틸리티 메서드를 제공하는 클래스입니다.
 */
public class ActivityUtils {
    private final MainActivity mainActivity; // MainActivity 참조
    LatLng currentPosition; // 현재 위치 정보 저장
    private Dialog menuDialog; // 다이얼로그 상태 저장

    public ActivityUtils(MainActivity activity) {
        this.mainActivity = activity;
    }

    /**
     * 뒤로가기 버튼 이벤트 핸들러 설정
     * 뒤로가기 버튼을 눌렀을 때 앱 종료 여부를 묻는 다이얼로그를 표시합니다.
     */
    public void setupBackButtonHandler() {
        OnBackPressedDispatcher onBackPressedDispatcher = mainActivity.getOnBackPressedDispatcher();
        onBackPressedDispatcher.addCallback(mainActivity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog(); // 앱 종료 여부 확인 다이얼로그 표시
            }
        });
    }

    /**
     * 앱 종료 여부를 확인하는 다이얼로그 표시
     * 사용자가 "예"를 선택하면 앱이 종료됩니다.
     */
    public void showExitDialog() {
        new AlertDialog.Builder(mainActivity)
                .setMessage("앱을 종료하시겠습니까?") // 다이얼로그 메시지 설정
                .setPositiveButton("예", (dialog, which) -> mainActivity.finishAffinity()) // "예" 버튼 클릭 시 앱 종료
                .setNegativeButton("아니요", (dialog, which) -> dialog.dismiss()) // "아니요" 버튼 클릭 시 다이얼로그 닫기
                .setCancelable(false) // 다이얼로그를 백 버튼으로 닫을 수 없도록 설정
                .show(); // 다이얼로그 표시
    }

    /**
     * 안내 요청 다이얼로그 표시
     * 사용자에게 안내 요청을 위한 옵션(휴게소, 졸음쉼터)을 제공합니다.
     */
    public void showGuideDialog() {
        currentPosition = mainActivity.locationManager.getCurrentLocation(); // 현재 위치 정보 가져오기

        // 커스텀 다이얼로그 생성
        Dialog dialog = new Dialog(mainActivity);
        dialog.setContentView(R.layout.notification_dialog); // 다이얼로그 레이아웃 설정
        dialog.setTitle("안내 요청"); // 다이얼로그 제목 설정

        // 버튼 참조
        Button btnNo = dialog.findViewById(R.id.btn_no); // "아니요" 버튼
        Button btnRestArea = dialog.findViewById(R.id.btn_rest_area); // "휴게소" 버튼
        Button btnRestStop = dialog.findViewById(R.id.btn_rest_stop); // "졸음쉼터" 버튼

        // "아니요" 버튼 클릭 이벤트
        btnNo.setOnClickListener(view -> dialog.dismiss()); // 다이얼로그 닫기

        // "휴게소" 버튼 클릭 이벤트
        btnRestArea.setOnClickListener(view -> {
            KakaoMapSearchManager.openKakaoMapWithKeyword(mainActivity, "휴게소", currentPosition); // 현재 위치 포함 휴게소 검색
            dialog.dismiss(); // 다이얼로그 닫기
        });

        // "졸음쉼터" 버튼 클릭 이벤트
        btnRestStop.setOnClickListener(view -> {
            KakaoMapSearchManager.openKakaoMapWithKeyword(mainActivity, "졸음쉼터", currentPosition); // 현재 위치 포함 졸음쉼터 검색
            dialog.dismiss(); // 다이얼로그 닫기
        });

        // 다이얼로그 표시
        dialog.show();
    }

    // 메뉴 다이얼로그 토글
    public void toggleMenuDialog(View anchorView) {
        if (menuDialog != null && menuDialog.isShowing()) {
            // 다이얼로그가 이미 표시 중이면 닫기
            menuDialog.dismiss();
            menuDialog = null;
        } else {
            // 다이얼로그가 표시 중이 아니면 열기
            showMenuDialog(anchorView);
        }
    }

    // 메뉴 다이얼로그 표시
    private void showMenuDialog(View anchorView) {
        // 커스텀 다이얼로그 생성
        menuDialog = new Dialog(mainActivity);
        menuDialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // 타이틀 숨기기
        menuDialog.setContentView(R.layout.menu_dialog); // 다이얼로그 레이아웃 설정
        menuDialog.setCanceledOnTouchOutside(true); // 다이얼로그 외부 터치 시 닫기 설정

        // 다이얼로그 위치 설정
        Window window = menuDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();

            int[] location = new int[2];
            anchorView.getLocationOnScreen(location); // FAB 버튼의 위치 가져오기

            params.gravity = Gravity.TOP | Gravity.START;
            params.x = location[0]; // FAB의 X 좌표
            params.y = location[1] - anchorView.getHeight() - 300; // FAB 바로 위로 다이얼로그 위치 조정 (20은 간격)
            window.setAttributes(params);
        }

        // 다이얼로그 버튼 설정
        Button btnOption1 = menuDialog.findViewById(R.id.map_button);

        btnOption1.setOnClickListener(view -> {
            // 다이얼로그는 맨 마지막에 닫기
            LatLng currentLocation = mainActivity.locationManager.getCurrentLocation();

            if (currentLocation != null) {
                // 위치 데이터 로그로 확인
                Log.d("MenuDialog", "Latitude: " + currentLocation.getLatitude() + ", Longitude: " + currentLocation.getLongitude());

                // 인텐트 생성 및 데이터 전달
                Intent intent = new Intent(mainActivity, MapActivity.class);
                // 데이터를 인텐트에 전달
                intent.putExtra("latitude", currentLocation.getLatitude());
                intent.putExtra("longitude", currentLocation.getLongitude());

                // MapActivity 실행
                mainActivity.startActivity(intent);

                // 다이얼로그 닫기
                menuDialog.dismiss();
            } else {
                // 위치 불러오기 실패 시 사용자에게 알림
                Toast.makeText(mainActivity, "위치를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            }
        });


        // 다이얼로그 표시
        menuDialog.show();
    }
}
