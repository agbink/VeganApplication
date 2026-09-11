package com.vegan;

import android.app.Application;

import com.vegan.api.TokenManager;

// 앱이 켜질 때 딱 한 번 TokenManager를 초기화합니다.
public class VeganApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        TokenManager.init(this);
    }
}
