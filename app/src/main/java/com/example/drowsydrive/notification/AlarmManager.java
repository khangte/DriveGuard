package com.example.drowsydrive.notification;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.drowsydrive.ui.ActivityUtils;

public class AlarmManager {

    private final SoundManager soundManager;
    private final ActivityUtils activityUtils;

    public AlarmManager(SoundManager soundManager, ActivityUtils activityUtils) {
        this.soundManager = soundManager;
        this.activityUtils = activityUtils;
    }

    public void playWarningAlarm(Context context, AlarmDialog.AlertType alertType) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        soundManager.playAlarm(context, new AlarmDialog.AlarmDialogCallback() {
            @Override
            public void onStop() {
                mainHandler.post(() -> {
                    soundManager.stopAlarm();
                    if (alertType == AlarmDialog.AlertType.DROWSINESS || alertType == AlarmDialog.AlertType.YAWN) {
                        activityUtils.showGuideDialog(); // UI 관련 작업
                    }
                });
            }

            @Override
            public void onCancel() {
                mainHandler.post(soundManager::stopAlarm); // UI 안전 보장
            }
        }, alertType);
    }

}
