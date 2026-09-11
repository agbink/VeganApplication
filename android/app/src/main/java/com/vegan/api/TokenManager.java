package com.vegan.api;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Firebase Auth가 알아서 해주던 "로그인 세션 유지"를 직접 구현한 버전.
 * 로그인 성공하면 받은 JWT를 여기 저장해두고, 이후 모든 API 호출에 같이 보냅니다.
 */
public class TokenManager {

    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";

    private static TokenManager instance;
    private final SharedPreferences prefs;

    private TokenManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Application Context로 한 번만 초기화해서 어디서든 TokenManager.getInstance()로 쓰면 됨
    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new TokenManager(context);
        }
    }

    public static TokenManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TokenManager.init(context)을 Application onCreate에서 먼저 호출하세요.");
        }
        return instance;
    }

    public void saveSession(String token, long userId, String username, String email) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    // 로그아웃/회원탈퇴 시 호출
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
