package com.vegan.auth;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vegan.R;
import com.vegan.api.ApiUser;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;
import com.vegan.main.MyPageActivity;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangeActivity extends AppCompatActivity {

    private EditText mEtName, mEtAddress, mEtEmail, mEtPhone;
    private Button mBtnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change);

        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(v -> onBackPressed());

        mEtName = findViewById(R.id.et_name);
        mEtAddress = findViewById(R.id.et_address);
        mEtEmail = findViewById(R.id.et_email);
        mEtPhone = findViewById(R.id.et_phone);
        mBtnSave = findViewById(R.id.save_btn);

        String token = TokenManager.getInstance().getToken();
        RetrofitClient.getUserApi().getMe(token).enqueue(new Callback<ApiUser>() {
            @Override
            public void onResponse(Call<ApiUser> call, Response<ApiUser> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiUser user = response.body();
                    mEtName.setText(user.getUsername());
                    mEtPhone.setText(user.getPhone() != null ? user.getPhone() : "");
                    mEtEmail.setText(user.getEmail());
                    mEtAddress.setText(user.getAddress() != null ? user.getAddress() : "");
                }
            }
            @Override
            public void onFailure(Call<ApiUser> call, Throwable t) {
                Toast.makeText(ChangeActivity.this, "회원정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        Button resetPwButton = findViewById(R.id.btnPassword);
        resetPwButton.setOnClickListener(v -> showPasswordInputDialog(token));

        mBtnSave.setOnClickListener(v -> saveChanges(token));
    }

    private void saveChanges(String token) {
        String name = mEtName.getText().toString().trim();
        String address = mEtAddress.getText().toString().trim();
        String phone = mEtPhone.getText().toString().trim();

        Map<String, String> body = new HashMap<>();
        body.put("username", name);
        body.put("phone", phone);
        body.put("address", address);

        RetrofitClient.getUserApi().updateProfile(token, body).enqueue(new Callback<ApiUser>() {
            @Override
            public void onResponse(Call<ApiUser> call, Response<ApiUser> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiUser user = response.body();
                    TokenManager.getInstance().saveSession(token, user.getId(), user.getUsername(), user.getEmail());
                    new AlertDialog.Builder(ChangeActivity.this)
                            .setTitle("회원정보 수정완료")
                            .setMessage("회원정보가 성공적으로 수정되었습니다.")
                            .setPositiveButton("확인", (d, i) -> {
                                startActivity(new Intent(ChangeActivity.this, MyPageActivity.class));
                                finish();
                            }).show();
                } else {
                    Toast.makeText(ChangeActivity.this, "수정 실패", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ApiUser> call, Throwable t) {
                Toast.makeText(ChangeActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPasswordInputDialog(String token) {
        EditText currentPw = new EditText(this);
        currentPw.setHint("현재 비밀번호");
        currentPw.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText newPw = new EditText(this);
        newPw.setHint("새 비밀번호");
        newPw.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        layout.addView(currentPw);
        layout.addView(newPw);

        new AlertDialog.Builder(this)
                .setTitle("비밀번호 변경")
                .setView(layout)
                .setPositiveButton("변경", (d, i) -> {
                    String cur = currentPw.getText().toString().trim();
                    String nw = newPw.getText().toString().trim();
                    if (cur.isEmpty() || nw.isEmpty()) {
                        Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, String> body = new HashMap<>();
                    body.put("currentPassword", cur);
                    body.put("newPassword", nw);
                    RetrofitClient.getUserApi().changePassword(token, body).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(ChangeActivity.this, "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ChangeActivity.this, "현재 비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(ChangeActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("취소", null)
                .show();
    }
}
