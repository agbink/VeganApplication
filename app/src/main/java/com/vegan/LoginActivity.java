package com.vegan;

import static android.app.ProgressDialog.show;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.navercorp.nid.NaverIdLoginSDK;
import com.vegan.Manage.ManageMainActivity;


import com.navercorp.nid.oauth.OAuthLoginCallback;
import com.navercorp.nid.oauth.NidOAuthLogin;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import de.hdodenhof.circleimageview.CircleImageView;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseRef;
    private SharedPreferences sharedPreferences;
    private EditText mEtEmail, mEtPwd;
    private CheckBox mCheckBoxSaveId;
    Dialog dialog;
    String strEmail;

    // 추가된 변수
    private GoogleSignInClient googleSignInClient;
    private final int RC_SIGN_IN = 1004;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        NaverIdLoginSDK.INSTANCE.initialize(this, getString(R.string.naver_client_id),
                getString(R.string.naver_client_secret), getString(R.string.app_name));

        mAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("User");

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

        // 기존 이메일 로그인 처리
        Button loginBtn = findViewById(R.id.btn_login);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                strEmail = mEtEmail.getText().toString();
                String strPwd = mEtPwd.getText().toString();

                mAuth.signInWithEmailAndPassword(strEmail, strPwd)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    if (mCheckBoxSaveId.isChecked()) {
                                        String uid = mAuth.getCurrentUser().getUid();
                                        mDatabaseRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if (snapshot.exists()) {
                                                    User user = snapshot.getValue(User.class);
                                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                                    editor.putString("email", user.getEmailId());
                                                    editor.putBoolean("save_id", true);
                                                    editor.apply();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.e("LoginActivity, 로그인 오류", String.valueOf(error.toException()));
                                            }
                                        });
                                    } else {
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.remove("email");
                                        editor.remove("save_id");
                                        editor.apply();
                                    }
                                    if ("test@test.com".equals(strEmail) && "123456".equals(strPwd)) {
                                        Intent intent = new Intent(LoginActivity.this, ManageMainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }

                                } else {
                                    showDialog();
                                }
                            }
                        });
            }
        });

        TextView emailLogin = findViewById(R.id.email_login);
        emailLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // Google 로그인 버튼 처리
        ImageView googleLogin = findViewById(R.id.google_login);
        googleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 로그인 버튼 클릭 시 기존 로그인된 세션을 로그아웃
                googleSignInClient.signOut().addOnCompleteListener(LoginActivity.this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // 로그아웃이 완료되면 Google 로그인 시작
                        googleLogin();
                    }
                });
            }
        });


        // Google 로그인 옵션 구성
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        NaverIdLoginSDK.INSTANCE.initialize(this, getString(R.string.naver_client_id),
                getString(R.string.naver_client_secret), getString(R.string.app_name));

        CircleImageView naverLogin = findViewById(R.id.naver_login);
        naverLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateNaverLogin();
            }
        });
    }

    private void initiateNaverLogin() {
        // 현재 로그인된 사용자 로그아웃
        NaverIdLoginSDK.INSTANCE.logout();

        // 새로운 로그인 요청
        NaverIdLoginSDK.INSTANCE.authenticate(this, new OAuthLoginCallback() {
            @Override
            public void onSuccess() {
                String accessToken = NaverIdLoginSDK.INSTANCE.getAccessToken();
                // Access Token을 사용하여 사용자 정보 요청
                fetchUserInfo(accessToken);
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


    private void fetchUserInfo(String accessToken) {
        new Thread(() -> {
            try {
                String url = "https://openapi.naver.com/v1/nid/me";
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                // 응답 처리
                InputStream inputStream = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                // JSON 응답 파싱
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONObject responseObject = jsonResponse.getJSONObject("response");
                String name = responseObject.getString("name");
                String email = responseObject.getString("email");
                String phone = responseObject.has("mobile") ? responseObject.getString("mobile") : "";

                // Firebase에 사용자 정보 저장
                registerUserWithNaver(email, name, phone);

            } catch (Exception e) {
                Log.e("NaverLogin", "Error fetching user info: " + e.getMessage());
            }
        }).start();
    }


    private void registerUserWithNaver(String email, String name, String phone) {
        mAuth.signInWithEmailAndPassword(email, "randomPassword123")
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        handleSuccessfulLogin(email);
                    } else {
                        createUserInFirebase(email, name, phone);  // 전화번호 추가
                    }
                });
    }


    private void createUserInFirebase(String email, String name, String phone) {
        mAuth.createUserWithEmailAndPassword(email, "randomPassword123")
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();

                            // User 객체에 전화번호 추가
                            User user = new User();
                            user.setIdToken(uid);
                            user.setEmailId(email);
                            user.setUsername(name);
                            user.setPhone(phone);  // 전화번호 저장

                            // Firebase Database에 사용자 정보 저장
                            mDatabaseRef.child(uid).setValue(user)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            handleSuccessfulLogin(email);
                                        } else {
                                            Log.e("Firebase", "Failed to save user: " + task1.getException().getMessage());
                                        }
                                    });
                        }
                    } else {
                        Log.e("NaverLogin", "회원가입 실패: " + task.getException().getMessage());
                    }
                });
    }


    private void handleSuccessfulLogin(String email) {
        // SharedPreferences에 사용자 이메일 저장
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (mCheckBoxSaveId.isChecked()) {
            editor.putString("email", email);
            editor.putBoolean("save_id", true);
        } else {
            editor.remove("email");
            editor.remove("save_id");
        }
        editor.apply();

        // MainActivity로 이동
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }



    // Google 로그인 메서드
    private void googleLogin() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // Activity 결과 처리
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account);
                } catch (ApiException e) {
                    Log.w("LoginActivity", "Google sign in failed", e);
                }
            }
        }
    }

    // Google 계정으로 Firebase 인증
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        String idToken = account.getIdToken();
        if (idToken != null) {
            mAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                if (firebaseUser != null) {
                                    String uid = firebaseUser.getUid();
                                    String name = firebaseUser.getDisplayName();
                                    String email = firebaseUser.getEmail();

                                    // 기존 방식에 맞게 User 객체 생성
                                    User userInfo = new User();
                                    userInfo.setIdToken(uid);
                                    userInfo.setEmailId(email);
                                    userInfo.setUsername(name);

                                    mDatabaseRef.child(uid).setValue(userInfo)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        // 사용자 정보를 SharedPreferences에 저장
                                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                                        editor.putString("email", email);
                                                        editor.putBoolean("save_id", true);
                                                        editor.apply();

                                                        goMainActivity();
                                                    } else {
                                                        Toast.makeText(LoginActivity.this, "정보 저장 실패", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "Firebase Authentication failed.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    // 메인 액티비티로 이동
    private void goMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    // 로그인 실패 시 다이얼로그 표시
    public void showDialog() {
        dialog.show();

        TextView confirmTextView = dialog.findViewById(R.id.confirmTextView);
        confirmTextView.setText("아이디 또는 비밀번호가 일치하지 않습니다.");

        Button btnOk = dialog.findViewById(R.id.btn_ok);
        btnOk.setText("확인");
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }
}
