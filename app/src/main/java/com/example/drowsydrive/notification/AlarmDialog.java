package com.example.drowsydrive.notification;

import android.app.AlertDialog;
import android.content.Context;

public class AlarmDialog {

    public interface AlarmDialogCallback {
        void onStop();
        void onCancel();
    }

    private final AlarmDialogCallback callback;
    private final Context context;
    private final AlertType alertType;
    private final TTSManager ttsManager;

    public enum AlertType {
        YAWN,
        DROWSINESS,
        CO2
    }

    public AlarmDialog(Context context, AlarmDialogCallback callback, AlertType alertType, TTSManager ttsManager) {
        this.context = context;
        this.callback = callback;
        this.alertType = alertType;
        this.ttsManager = ttsManager;
    }

    public void show() {
        String title = "";
        String message = "";

        if (alertType == AlertType.YAWN) {
            title = "하품 경고";
            message = "하품이 감지되었습니다.\n졸음에 주의하세요!";
        } else if (alertType == AlertType.DROWSINESS){
            title = "졸음 경고";
            message = "졸음이 감지되었습니다.\n휴식을 취하세요!";
        } else if (alertType == AlertType.CO2){
            title = "CO₂ 경고";
            message = "CO₂ 농도가 위험 수준입니다.\n환기하세요!";
        } else {  }

        if(ttsManager != null) {
            ttsManager.speak(message);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("알람 끄기", (dialog, which) -> callback.onStop())
                .setCancelable(false);
        builder.show();
    }
}