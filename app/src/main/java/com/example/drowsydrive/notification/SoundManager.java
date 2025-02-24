package com.example.drowsydrive.notification;

import android.media.AudioAttributes;
import android.media.SoundPool;
import android.content.Context;
import android.util.Log;

/**
 * SoundManager 클래스
 * 알람 소리를 재생하고, 알림 다이얼로그를 표시하며, TTS를 활용한 음성 알림 기능을 제공.
 */
public class SoundManager {
    private static final String TAG = "SoundManager"; // 디버깅용 태그

    private final SoundPool soundPool; // SoundPool 객체, 알람 소리 재생을 위한 오디오 스트림 관리
    private final int alarmSound; // 알람 사운드 리소스 ID
    private boolean isDialogShowing = false; // 현재 알림 다이얼로그가 표시 중인지 여부
    private final TTSManager ttsManager; // TTSManager 객체, 음성 알림 기능 제공

    /**
     * 생성자
     *
     * @param context          Context 객체
     * @param alarmSoundResId  알람 사운드 리소스 ID
     * @param ttsManager       TTSManager 객체
     */
    public SoundManager(Context context, int alarmSoundResId, TTSManager ttsManager) {
        this.ttsManager = ttsManager;

        // 오디오 속성을 설정 (미디어 재생용)
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA) // 미디어 재생 용도 설정
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION) // 효과음 컨텐츠 타입 설정
                .build();

        // SoundPool 초기화 (최대 스트림 1개)
        soundPool = new SoundPool.Builder()
                .setMaxStreams(1) // 최대 동시 재생 가능한 스트림 수 설정
                .setAudioAttributes(audioAttributes) // 오디오 속성 연결
                .build();

        // 알람 사운드 로드
        alarmSound = soundPool.load(context, alarmSoundResId, 1);
    }

    /**
     * 알람 소리 재생 및 알림 다이얼로그 표시
     *
     * @param context    Context 객체
     * @param callback   AlarmDialog 종료 시 호출할 콜백
     * @param alertType  알림 유형
     */
    public void playAlarm(Context context, AlarmDialog.AlarmDialogCallback callback, AlarmDialog.AlertType alertType) {
        // 다이얼로그가 표시 중이 아니면 실행
        if (!isDialogShowing) {
            isDialogShowing = true; // 다이얼로그 상태 설정

            // 알람 소리 재생
            int result = soundPool.play(alarmSound, 1, 1, 1, 0, 1);
            if (result == 0) {
                Log.d(TAG, "알람이 재생되고 있지 않음"); // 알람 재생 실패 로그
            } else {
                Log.d(TAG, "알람이 재생되고 있음"); // 알람 재생 성공 로그

                // 알림 다이얼로그 생성 및 표시
                AlarmDialog dialog = new AlarmDialog(context, new AlarmDialog.AlarmDialogCallback() {
                    @Override
                    public void onStop() {
                        stopAlarm(); // 알람 정지
                        callback.onStop(); // 외부 콜백 호출
                    }

                    @Override
                    public void onCancel() {
                        callback.onCancel(); // 외부 콜백 호출
                    }
                }, alertType, ttsManager); // TTSManager 전달
                dialog.show(); // 다이얼로그 표시
            }
        }
    }

    /**
     * 알람 정지
     */
    public void stopAlarm() {
        soundPool.stop(alarmSound); // 알람 사운드 정지
        Log.d(TAG, "알람이 정지됨"); // 디버그 로그 출력
        isDialogShowing = false; // 다이얼로그 상태 초기화
    }

    /**
     * SoundPool 리소스 해제
     */
    public void release() {
        soundPool.release(); // SoundPool 리소스 해제
    }
}
