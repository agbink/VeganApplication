package com.vegan.auth;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vegan.R;
import com.vegan.api.ApiAuthResponse;
import com.vegan.api.RegisterRequestBody;
import com.vegan.api.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText mEtEmail, mEtPwd, mEtchPwd;
    private EditText mEtName, mEtPhone, mEtAddress;
    private Button mBtnRegister;
    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dialog = new Dialog(RegisterActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm);

        mEtEmail = findViewById(R.id.et_email);
        mEtPwd = findViewById(R.id.et_pwd);
        mEtchPwd = findViewById(R.id.et_ckpwd);
        mBtnRegister = findViewById(R.id.btn_login);
        mEtName = findViewById(R.id.et_name);
        mEtPhone = findViewById(R.id.et_phone);
        mEtAddress = findViewById(R.id.et_address);

        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(v -> onBackPressed());

        mBtnRegister.setOnClickListener(v -> {
            String strName = mEtName.getText().toString();
            String strEmail = mEtEmail.getText().toString();
            String strPwd = mEtPwd.getText().toString();
            String strchPwd = mEtchPwd.getText().toString();
            String strPhone = mEtPhone.getText().toString();
            String strAddress = mEtAddress.getText().toString();

            if (strEmail.isEmpty() || strPwd.isEmpty() || strName.isEmpty() || strPhone.isEmpty() || strAddress.isEmpty()) {
                showDialog();
                return;
            }

            if (!strPwd.equals(strchPwd)) {
                Toast.makeText(getBaseContext(), "비밀번호와 비밀번호 확인이 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                mEtPwd.requestFocus();
                return;
            }

            RegisterRequestBody body = new RegisterRequestBody(strEmail, strPwd, strName, strPhone, strAddress);
            RetrofitClient.getAuthApi().register(body).enqueue(new Callback<ApiAuthResponse>() {
                @Override
                public void onResponse(Call<ApiAuthResponse> call, Response<ApiAuthResponse> response) {
                    if (!response.isSuccessful()) {
                        // 409면 이미 가입된 이메일 (백엔드 ResponseStatusException 그대로 전달됨)
                        showDialog2();
                        return;
                    }

                    // 가입 후 로그인 화면으로 이동 (가입된 아이디 자동 적용) - 기존 동작과 동일
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.putExtra("userEmail", strEmail);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(Call<ApiAuthResponse> call, Throwable t) {
                    Log.e("RegisterActivity", "회원가입 요청 실패", t);
                    showDialog2();
                }
            });
        });
    }

    public void showDialog() {
        dialog.show();
        TextView confirmTextView = dialog.findViewById(R.id.confirmTextView);
        confirmTextView.setText("정보를 모두 입력해주세요.");

        Button btnOk = dialog.findViewById(R.id.btn_ok);
        btnOk.setText("확인");
        btnOk.setOnClickListener(v -> dialog.dismiss());
    }

    public void showDialog2() {
        dialog.show();
        TextView confirmTextView = dialog.findViewById(R.id.confirmTextView);
        confirmTextView.setText("회원가입에 실패했습니다. (이미 가입된 이메일일 수 있어요)");

        Button btnOk = dialog.findViewById(R.id.btn_ok);
        btnOk.setText("확인");
        btnOk.setOnClickListener(v -> dialog.dismiss());
    }
}
