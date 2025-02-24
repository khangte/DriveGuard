package com.example.drowsydrive.kakaomap;

import android.app.Application;

import com.example.drowsydrive.BuildConfig;
import com.kakao.vectormap.KakaoMapSdk;

public class MapApplication extends Application {
    static String nativeAppKey = BuildConfig.NATIVE_APP_KEY;

    @Override
    public void onCreate() {
        super.onCreate();
        KakaoMapSdk.init(this, nativeAppKey); // 카카오 SDK 초기화
    }
}
