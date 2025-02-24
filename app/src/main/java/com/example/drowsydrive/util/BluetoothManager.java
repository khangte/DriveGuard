package com.example.drowsydrive.util;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.drowsydrive.arduino.ArduinoManager;
import com.example.drowsydrive.ui.MainActivity;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothManager {
    private static final String TAG = "BluetoothManager";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_BLUETOOTH_PERMISSION = 100;

    private final ArduinoManager arduinoManager;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private final Context context;

    public BluetoothManager(Context context, ArduinoManager arduinoManager) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.arduinoManager = arduinoManager;
    }

    // 블루투스 권한 요청 메서드 (Android 12 이상)
    @RequiresApi(api = Build.VERSION_CODES.S)
    public void requestBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            if (context instanceof MainActivity) {
                ActivityCompat.requestPermissions(
                        (MainActivity) context,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_BLUETOOTH_PERMISSION
                );
            } else {
                Log.e(TAG, "Context가 MainActivity의 인스턴스가 아닙니다.");
            }
        }
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void connectToDevice() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
                PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_CONNECT 권한이 없습니다.");
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.isEmpty()) {
            Log.e(TAG, "페어링된 블루투스 장치가 없습니다.");
            return;
        }

        BluetoothDevice device = pairedDevices.iterator().next();
        Log.d(TAG, "연결 시도 중: " + device.getName());
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            inputStream = bluetoothSocket.getInputStream();
            Log.d(TAG, "블루투스 연결 성공: " + device.getName());
            listenForData();
        } catch (Exception e) {
            Log.e(TAG, "블루투스 연결 실패", e);
        }
    }

    private void listenForData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    if (inputStream != null && inputStream.available() > 0) {
                        bytes = inputStream.read(buffer);
                        String data = new String(buffer, 0, bytes).trim();
                        Log.d(TAG, "수신된 데이터: " + data);
                        arduinoManager.processData(data); // 데이터 처리 위임
                    }
                } catch (Exception e) {
                    Log.e(TAG, "데이터 수신 중 오류 발생", e);
                    break;
                }
            }
        }).start();
    }

    public void closeConnection() {
        try {
            if (inputStream != null) inputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
            Log.d(TAG, "블루투스 연결 종료");
        } catch (Exception e) {
            Log.e(TAG, "연결 해제 중 오류 발생", e);
        }
    }
}
