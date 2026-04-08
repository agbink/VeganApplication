package com.vegan.Manage;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.vegan.AddProductActivity;
import com.vegan.LoginActivity;
import com.vegan.MainActivity;
import com.vegan.ProductListActivity;
import com.vegan.R;

public class ManageMainActivity extends AppCompatActivity {
    Dialog dialog;
    private FirebaseAuth mAuth;
    TextView LogoutTxt;
    Button Btn_ManageProduct, Btn_AddProduct, Btn_ManageUser, Btn_ManageOrder, Btn_ManageReview, Btn_GoToShoppingMalls;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_main);

        mAuth = FirebaseAuth.getInstance();
        dialog = new Dialog(ManageMainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.customdialog);


        //상품 관리
        Btn_ManageProduct = (Button) findViewById(R.id.Btn_ManageProduct);
        Btn_ManageProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ManageMainActivity.this, ProductListActivity.class);
                startActivity(intent);
            }
        });

        //상품 추가
        Btn_AddProduct = (Button) findViewById(R.id.Btn_AddProduct);
        Btn_AddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ManageMainActivity.this, AddProductActivity.class);
                startActivity(intent);
            }
        });

        //홈으로 이동
        Btn_GoToShoppingMalls = (Button) findViewById(R.id.Btn_GoToShoppingMalls);
        Btn_GoToShoppingMalls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ManageMainActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        //회원 주문 관리
        Btn_ManageOrder = (Button) findViewById(R.id.Btn_ManageOrder);
        Btn_ManageOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(ManageMainActivity.this, ManageUserOrderActivity.class);
                startActivity(intent);

            }
        });

        // 회원 관리
        Btn_ManageUser = (Button) findViewById(R.id.Btn_ManageUser);
        Btn_ManageUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ManageMainActivity.this, ManageUserActivity.class);
                startActivity(intent);
            }
        });

        //후기 관리
        Btn_ManageReview = (Button) findViewById(R.id.Btn_ManageReview);
        Btn_ManageReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ManageMainActivity.this, ManageReviewActivity.class);
                startActivity(intent);
            }
        });


        LogoutTxt = (TextView) findViewById(R.id.LogoutTxt);

        String mystring = LogoutTxt.getText().toString();
        SpannableString content = new SpannableString(mystring);
        content.setSpan(new UnderlineSpan(), 0, mystring.length(), 0);
        LogoutTxt.setText(content);

        LogoutTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutConfirmationDialog();
            }
        });

    }
    public void showLogoutConfirmationDialog() {

        dialog.show();

        TextView confirmTextView = dialog.findViewById(R.id.say);
        confirmTextView.setText("로그아웃하시겠습니까?");

        Button btnno = dialog.findViewById(R.id.btnNo);
        Button btnok = dialog.findViewById(R.id.btnOk);
        btnno.setText("아니요");
        btnok.setText("예");

        btnok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
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

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(ManageMainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}