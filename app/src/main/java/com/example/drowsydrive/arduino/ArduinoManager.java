package com.example.drowsydrive.arduino;

import android.util.Log;

public class ArduinoManager {
    private static final String TAG = "ArduinoManager";
    private CO2AlertCallback callback;
    private long lastAlarmTime = 0; // 마지막 알람 시간 기록
    private static final int CO2_WARNING_THRESHOLD = 2000; // CO2 농도 경고 수준 (ppm)

    public interface CO2AlertCallback {
        void onHighCO2Detected(int co2Level);
    }

    public void setCO2AlertCallback(CO2AlertCallback callback) {
        this.callback = callback;
    }

    public void processData(String data) {
        // 수신된 데이터를 기반으로 처리 로직 작성
        try {
            int co2Level = Integer.parseInt(data); // 데이터가 CO2 농도 ppm으로 가정
            Log.d(TAG, "--------------------------");
            Log.d(TAG, "수신된 CO2 농도: " + co2Level + " ppm");
            handleCO2Level(co2Level);
        } catch (NumberFormatException e) {
            Log.e(TAG, "잘못된 데이터 형식: " + data, e);
        }
    }

    private void handleCO2Level(int co2Level) {
        // CO2 농도에 따른 경고 처리
        if (co2Level > CO2_WARNING_THRESHOLD) {
            Log.w(TAG, "CO2 농도 위험 수준: " + co2Level + " ppm");

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAlarmTime >= 60000) { // 1분(60,000ms) 경과 확인
                lastAlarmTime = currentTime; // 마지막 알람 시간 갱신
                if (callback != null) {
                    callback.onHighCO2Detected(co2Level); // 알람 실행
                }
            } else {
                Log.d(TAG, "1분 이내에 알람 발생 제한: " + (60000 - (currentTime - lastAlarmTime)) + "ms 남음");
            }
        } else {
            Log.d(TAG, "CO2 농도 정상: " + co2Level + " ppm");
        }
    }
}
