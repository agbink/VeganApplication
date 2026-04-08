package com.vegan;

import static android.content.ContentValues.TAG;

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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseRef;
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

        mAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("User");

        mEtEmail = findViewById(R.id.et_email);
        mEtPwd = findViewById(R.id.et_pwd);
        mEtchPwd=findViewById(R.id.et_ckpwd);
        mBtnRegister = findViewById(R.id.btn_login);
        mEtName = findViewById(R.id.et_name);
        mEtPhone = findViewById(R.id.et_phone);
        mEtAddress = findViewById(R.id.et_address);

        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mBtnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strName = mEtName.getText().toString();
                String strEmail = mEtEmail.getText().toString();
                String strPwd = mEtPwd.getText().toString();
                String strchPwd = mEtchPwd.getText().toString();
                String strPhone = mEtPhone.getText().toString();
                String strAddress = mEtAddress.getText().toString();

                // 입력창이 하나라도 비어 있을 경우
                if (strEmail.isEmpty() || strPwd.isEmpty() || strName.isEmpty() || strPhone.isEmpty() || strAddress.isEmpty()) {
                    showDialog();  // 다이얼로그 생성
                    return;
                }
                if (!strPwd.equals(strchPwd)) {
                    // 비밀번호와 비밀번호 확인이 일치하지 않을 경우
                    Toast.makeText(
                            getBaseContext(),
                            "비밀번호와 비밀번호 확인이 일치하지 않습니다.",
                            Toast.LENGTH_SHORT
                    ).show();

                    // 비밀번호 입력란으로 포커스 이동
                    mEtPwd.requestFocus();
                } else {
                    // 비밀번호와 비밀번호 확인이 일치할 경우
                    mAuth.createUserWithEmailAndPassword(strEmail, strPwd)
                            .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()){
                                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                        User user = new User();
                                        user.setIdToken(firebaseUser.getUid());
                                        user.setEmailId(firebaseUser.getEmail());
                                        user.setPassword(strPwd);
                                        user.setUsername(strName);
                                        user.setPhone(strPhone);
                                        user.setAddress(strAddress);

                                        mDatabaseRef.child(firebaseUser.getUid()).setValue(user);

                                                    // 가입 후 로그인 화면으로 이동 (가입된 아이디 자동 적용)
                                                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                                    intent.putExtra("userEmail", firebaseUser.getEmail());
                                                    startActivity(intent);
                                                    finish();
                                                } else{
                                                    showDialog2();
                                                }
                                            }
                                        });
                }
            }
        });
    }

    public void showDialog() {
        dialog.show();

        TextView confirmTextView = dialog.findViewById(R.id.confirmTextView);
        confirmTextView.setText("정보를 모두 입력해주세요.");

        Button btnOk = dialog.findViewById(R.id.btn_ok);
        btnOk.setText("확인");
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    public void showDialog2() {
        dialog.show();

        TextView confirmTextView = dialog.findViewById(R.id.confirmTextView);
        confirmTextView.setText("회원가입에 실패했습니다.");

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
