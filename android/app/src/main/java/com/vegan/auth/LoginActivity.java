package com.vegan.auth;

// ===========================================================================
// 기존 LoginActivity.java를 Firebase Auth 없이 MySQL + JWT 기반으로 바꾼 버전입니다.
// - 이메일/비밀번호 로그인: Spring Boot의 POST /api/auth/login 호출
// - 구글 로그인: GoogleSignInClient로 idToken을 받는 부분은 그대로 두고,
//               그 idToken을 Firebase 대신 POST /api/auth/google 으로 보냄
// - 네이버 로그인: NaverIdLoginSDK로 accessToken을 받는 부분은 그대로 두고,
//               (기존에 안드로이드가 직접 하던 네이버 프로필 조회는 서버로 옮김)
//               accessToken을 POST /api/auth/naver 로 보냄
// ===========================================================================

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.navercorp.nid.NaverIdLoginSDK;
import com.navercorp.nid.oauth.OAuthLoginCallback;
import com.vegan.R;
import com.vegan.api.ApiAuthResponse;
import com.vegan.api.LoginRequestBody;
import com.vegan.api.RetrofitClient;
import com.vegan.manage.ManageMainActivity;
import com.vegan.api.SocialLoginRequestBody;
import com.vegan.api.TokenManager;
import com.vegan.main.MainActivity;

import de.hdodenhof.circleimageview.CircleImageView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private EditText mEtEmail, mEtPwd;
    private CheckBox mCheckBoxSaveId;
    Dialog dialog;
    String strEmail;

    private GoogleSignInClient googleSignInClient;
    private final int RC_SIGN_IN = 1004;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // TokenManager는 Application.onCreate()에서 한 번 init 해두면 여기선 안 해도 됩니다.
        // 혹시 아직 안 해놨다면 아래 줄 주석을 풀어서 임시로 여기서 초기화해도 동작합니다.
         TokenManager.init(getApplicationContext());

        NaverIdLoginSDK.INSTANCE.initialize(this, getString(R.string.naver_client_id),
                getString(R.string.naver_client_secret), getString(R.string.app_name));

        mEtEmail = findViewById(R.id.et_email);
        mEtPwd = findViewById(R.id.et_pwd);
        mCheckBoxSaveId = findViewById(R.id.checkbox_saveId);

        ColorStateList colorStateList = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                        getResources().getColor(R.color.dovegray),
                        getResources().getColor(R.color.textColorGray)
                }
        );
        mCheckBoxSaveId.setButtonTintList(colorStateList);

        sharedPreferences = getSharedPreferences("login_prefs", Context.MODE_PRIVATE);

        boolean autoLoginEnabled = sharedPreferences.getBoolean("save_id", false);
        mCheckBoxSaveId.setChecked(autoLoginEnabled);

        dialog = new Dialog(LoginActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm);

        String savedEmail = sharedPreferences.getString("email", "");
        if (!TextUtils.isEmpty(savedEmail)) {
            mEtEmail.setText(savedEmail);
        }

        Intent receivedIntent = getIntent();
        if (receivedIntent != null && receivedIntent.hasExtra("userEmail")) {
            strEmail = receivedIntent.getStringExtra("userEmail");
            mEtEmail.setText(strEmail);
        }

        // ---------------- 이메일/비밀번호 로그인 ----------------
        Button loginBtn = findViewById(R.id.btn_login);
        loginBtn.setOnClickListener(v -> {
            strEmail = mEtEmail.getText().toString();
            String strPwd = mEtPwd.getText().toString();

            LoginRequestBody body = new LoginRequestBody(strEmail, strPwd);
            RetrofitClient.getAuthApi().login(body).enqueue(new Callback<ApiAuthResponse>() {
                @Override
                public void onResponse(Call<ApiAuthResponse> call, Response<ApiAuthResponse> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        showDialog();
                        return;
                    }
                    handleLoginSuccess(response.body(), strEmail, strPwd);
                }

                @Override
                public void onFailure(Call<ApiAuthResponse> call, Throwable t) {
                    Log.e("LoginActivity", "로그인 요청 실패", t);
                    showDialog();
                }
            });
        });

        TextView emailLogin = findViewById(R.id.email_login);
        emailLogin.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // ---------------- 구글 로그인 ----------------
        ImageView googleLogin = findViewById(R.id.google_login);
        googleLogin.setOnClickListener(v ->
                googleSignInClient.signOut().addOnCompleteListener(task -> googleLogin())
        );

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // ---------------- 네이버 로그인 ----------------
        CircleImageView naverLogin = findViewById(R.id.naver_login);
        naverLogin.setOnClickListener(v -> initiateNaverLogin());
    }

    private void handleLoginSuccess(ApiAuthResponse auth, String emailForSave, String pwdForAdminCheck) {
        TokenManager.getInstance().saveSession(
                auth.getToken(), auth.getUserId(), auth.getUsername(), auth.getEmail());

        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (mCheckBoxSaveId.isChecked()) {
            editor.putString("email", emailForSave);
            editor.putBoolean("save_id", true);
        } else {
            editor.remove("email");
            editor.remove("save_id");
        }
        editor.apply();

        // 기존 코드에 있던 관리자 임시 분기 (TODO: 추후 User에 role 컬럼 추가해서 정식으로 처리)
        if ("test@test.com".equals(emailForSave) && "123456".equals(pwdForAdminCheck)) {
            startActivity(new Intent(LoginActivity.this, ManageMainActivity.class));
        } else {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }
        finish();
    }

    private void initiateNaverLogin() {
        NaverIdLoginSDK.INSTANCE.logout();

        NaverIdLoginSDK.INSTANCE.authenticate(this, new OAuthLoginCallback() {
            @Override
            public void onSuccess() {
                String accessToken = NaverIdLoginSDK.INSTANCE.getAccessToken();
                loginWithNaverToken(accessToken);
            }

            @Override
            public void onFailure(int errorCode, String errorMessage) {
                Log.e("NaverLogin", "Login failed: " + errorMessage);
                Toast.makeText(LoginActivity.this, "Naver Login Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int errorCode, String errorMessage) {
                Log.e("NaverLogin", "Error: " + errorMessage);
            }
        });
    }

    // 기존에는 안드로이드가 직접 https://openapi.naver.com/v1/nid/me 를 호출했지만,
    // 이제는 accessToken만 우리 서버로 보내고, 서버가 검증 + 프로필 조회를 대신합니다.
    private void loginWithNaverToken(String accessToken) {
        SocialLoginRequestBody body = new SocialLoginRequestBody(accessToken);
        RetrofitClient.getAuthApi().loginWithNaver(body).enqueue(new Callback<ApiAuthResponse>() {
            @Override
            public void onResponse(Call<ApiAuthResponse> call, Response<ApiAuthResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(LoginActivity.this, "네이버 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                handleLoginSuccess(response.body(), response.body().getEmail(), null);
            }

            @Override
            public void onFailure(Call<ApiAuthResponse> call, Throwable t) {
                Log.e("NaverLogin", "서버 연결 실패", t);
                Toast.makeText(LoginActivity.this, "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void googleLogin() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    loginWithGoogleToken(account.getIdToken());
                } catch (ApiException e) {
                    Log.w("LoginActivity", "Google sign in failed", e);
                }
            }
        }
    }

    // 기존에는 idToken을 Firebase(signInWithCredential)로 보냈지만,
    // 이제는 우리 서버가 직접 구글에 검증을 요청합니다.
    private void loginWithGoogleToken(String idToken) {
        if (idToken == null) {
            Toast.makeText(this, "Google ID 토큰을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        SocialLoginRequestBody body = new SocialLoginRequestBody(idToken);
        RetrofitClient.getAuthApi().loginWithGoogle(body).enqueue(new Callback<ApiAuthResponse>() {
            @Override
            public void onResponse(Call<ApiAuthResponse> call, Response<ApiAuthResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(LoginActivity.this, "Google 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                handleLoginSuccess(response.body(), response.body().getEmail(), null);
            }

            @Override
            public void onFailure(Call<ApiAuthResponse> call, Throwable t) {
                Log.e("LoginActivity", "서버 연결 실패", t);
                Toast.makeText(LoginActivity.this, "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void showDialog() {
        dialog.show();
        TextView confirmTextView = dialog.findViewById(R.id.confirmTextView);
        confirmTextView.setText("아이디 또는 비밀번호가 일치하지 않습니다.");

        Button btnOk = dialog.findViewById(R.id.btn_ok);
        btnOk.setText("확인");
        btnOk.setOnClickListener(v -> dialog.dismiss());
    }
}
