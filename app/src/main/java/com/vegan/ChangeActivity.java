package com.vegan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ChangeActivity extends AppCompatActivity {

    private FirebaseAuth mFirebaseAuth; // 파이어베이스 인증 처리
    private DatabaseReference mDatabaseRef; // 실시간 데이터베이스
    private EditText mEtName, mEtPostcode, mEtAddress, mEtEmail, mEtPhone;
    private Button mBtnSave;
    Toolbar toolbar;
    Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change);
        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(v -> onBackPressed());

        dialog = new Dialog(ChangeActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.customdialog);

        Button resetPwButton = findViewById(R.id.btnPassword);
        resetPwButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();

                TextView confirmTextView = dialog.findViewById(R.id.say);
                confirmTextView.setText("비밀번호 재설정 이메일을 전송하시겠습니까?");

                Button btnno = dialog.findViewById(R.id.btnNo);
                Button btnok = dialog.findViewById(R.id.btnOk);
                btnno.setText("아니요");
                btnok.setText("예");
                btnok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendEmailForPasswordUpdate();
                        dialog.dismiss();
                    }
                });

                btnno.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
            }
        });


        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("User");

        mEtName = findViewById(R.id.et_name);
        mEtAddress = findViewById(R.id.et_address);
        mEtEmail = findViewById(R.id.et_email);
        mEtPhone = findViewById(R.id.et_phone);
        mBtnSave = findViewById(R.id.save_btn);

        // 사용자 정보 가져오기
        FirebaseUser user = mFirebaseAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            DatabaseReference userRef = mDatabaseRef.child(uid);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String name = dataSnapshot.child("username").getValue(String.class);
                        String phone = dataSnapshot.child("phone").getValue(String.class);
                        String email = dataSnapshot.child("emailId").getValue(String.class);
                        String address = dataSnapshot.child("address").getValue(String.class);

                        mEtName.setText(name);
                        mEtPhone.setText(phone);
                        mEtEmail.setText(email);
                        mEtAddress.setText(address);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(ChangeActivity.this, "회원정보를 불러오는데에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        }


        // 저장 버튼 클릭 이벤트 처리
        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChanges();
            }
        });
    }

    private void saveChanges() {
        // 변경된 정보 가져오기
        String name = mEtName.getText().toString().trim();
        String address = mEtAddress.getText().toString().trim();
        String email = mEtEmail.getText().toString().trim();
        String phone = mEtPhone.getText().toString().trim();

        // 변경된 정보 업데이트
        FirebaseUser user = mFirebaseAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            DatabaseReference userRef = mDatabaseRef.child(uid);

            // 사용자 정보 업데이트
            userRef.child("username").setValue(name);
            userRef.child("address").setValue(address);
            userRef.child("emailId").setValue(email);  // 이메일 업데이트 추가
            userRef.child("phone").setValue(phone);    // 전화번호 업데이트 추가

            // 사용자 정보 수정 완료 메시지
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("회원정보 수정완료");
            builder.setMessage("회원정보가 성공적으로 수정되었습니다");

            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(ChangeActivity.this, MyPageActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
            builder.show();
        }
    }



    // 비밀번호 재설정 이메일 보내기
    private void sendEmailForPasswordUpdate() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String email = getEmail();

        if (email != null) {
            auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Snackbar.make(getWindow().getDecorView().getRootView(), "이메일이 전송되었습니다", Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(getWindow().getDecorView().getRootView(), "이메일 전송 실패", Snackbar.LENGTH_LONG).show();
                }
            });
        } else {
            Snackbar.make(getWindow().getDecorView().getRootView(), "해당 이메일이 존재하지 않습니다", Snackbar.LENGTH_LONG).show();
        }
    }

    // 사용자 이메일 가져오기
    private String getEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String email = user.getEmail();
            return email != null ? email.toString() : null;
        } else {
            // 사용자가 로그인하지 않음
            return null;
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) { //뒤로가기
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}