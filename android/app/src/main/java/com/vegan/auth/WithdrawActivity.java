package com.vegan.auth;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.vegan.R;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WithdrawActivity extends AppCompatActivity {

    private RadioButton radioButton;
    private Button cancelButton, withdrawButton;
    private Dialog dialog, dialog2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);

        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.customdialog);

        dialog2 = new Dialog(this);
        dialog2.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog2.setContentView(R.layout.dialog_confirm);

        radioButton = findViewById(R.id.radioButton);
        cancelButton = findViewById(R.id.cancle);
        withdrawButton = findViewById(R.id.withdraw);

        cancelButton.setOnClickListener(v -> finish());
        radioButton.setOnClickListener(v -> withdrawButton.setEnabled(radioButton.isChecked()));
        withdrawButton.setOnClickListener(v -> {
            if (radioButton.isChecked()) showConfirmationDialog();
            else showFailDialog();
        });
    }

    private void showFailDialog() {
        dialog2.show();
        ((TextView) dialog2.findViewById(R.id.confirmTextView))
                .setText("유의사항 확인 후 동의하셔야 회원탈퇴가 가능합니다.");
        Button ok = dialog2.findViewById(R.id.btn_ok);
        ok.setText("확인");
        ok.setOnClickListener(v -> dialog2.dismiss());
    }

    private void showConfirmationDialog() {
        dialog.show();
        ((TextView) dialog.findViewById(R.id.say)).setText("앱을 탈퇴하시겠습니까?");
        Button yes = dialog.findViewById(R.id.btnOk);
        Button no = dialog.findViewById(R.id.btnNo);
        yes.setText("예");
        no.setText("아니요");
        yes.setOnClickListener(v -> { dialog.dismiss(); deleteAccount(); });
        no.setOnClickListener(v -> dialog.dismiss());
    }

    private void deleteAccount() {
        String token = TokenManager.getInstance().getToken();
        RetrofitClient.getUserApi().deleteAccount(token).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                TokenManager.getInstance().clearSession();
                dialog2.show();
                ((TextView) dialog2.findViewById(R.id.confirmTextView)).setText("탈퇴되었습니다.");
                Button ok = dialog2.findViewById(R.id.btn_ok);
                ok.setText("확인");
                ok.setOnClickListener(v -> {
                    dialog2.dismiss();
                    Intent intent = new Intent(WithdrawActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                });
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(WithdrawActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
