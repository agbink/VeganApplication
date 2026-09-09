package com.vegan.manage;

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

import com.vegan.api.TokenManager;
import com.vegan.product.AddProductActivity;
import com.vegan.auth.LoginActivity;
import com.vegan.main.MainActivity;
import com.vegan.product.ProductListActivity;
import com.vegan.R;

public class ManageMainActivity extends AppCompatActivity {
    Dialog dialog;
    TextView LogoutTxt;
    Button Btn_ManageProduct, Btn_AddProduct, Btn_ManageUser, Btn_ManageOrder, Btn_ManageReview, Btn_GoToShoppingMalls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_main);

        dialog = new Dialog(ManageMainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.customdialog);

        Btn_ManageProduct = (Button) findViewById(R.id.Btn_ManageProduct);
        Btn_ManageProduct.setOnClickListener(v -> startActivity(new Intent(this, ProductListActivity.class)));

        Btn_AddProduct = (Button) findViewById(R.id.Btn_AddProduct);
        Btn_AddProduct.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));

        Btn_GoToShoppingMalls = (Button) findViewById(R.id.Btn_GoToShoppingMalls);
        Btn_GoToShoppingMalls.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));

        Btn_ManageOrder = (Button) findViewById(R.id.Btn_ManageOrder);
        Btn_ManageOrder.setOnClickListener(v -> startActivity(new Intent(this, ManageUserOrderActivity.class)));

        Btn_ManageUser = (Button) findViewById(R.id.Btn_ManageUser);
        Btn_ManageUser.setOnClickListener(v -> startActivity(new Intent(this, ManageUserActivity.class)));

        Btn_ManageReview = (Button) findViewById(R.id.Btn_ManageReview);
        Btn_ManageReview.setOnClickListener(v -> startActivity(new Intent(this, ManageReviewActivity.class)));

        LogoutTxt = (TextView) findViewById(R.id.LogoutTxt);
        String mystring = LogoutTxt.getText().toString();
        SpannableString content = new SpannableString(mystring);
        content.setSpan(new UnderlineSpan(), 0, mystring.length(), 0);
        LogoutTxt.setText(content);
        LogoutTxt.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    public void showLogoutConfirmationDialog() {
        dialog.show();
        ((TextView) dialog.findViewById(R.id.say)).setText("로그아웃하시겠습니까?");
        Button btnno = dialog.findViewById(R.id.btnNo);
        Button btnok = dialog.findViewById(R.id.btnOk);
        btnno.setText("아니요");
        btnok.setText("예");
        btnok.setOnClickListener(v -> { logout(); dialog.dismiss(); });
        btnno.setOnClickListener(v -> dialog.dismiss());
    }

    private void logout() {
        TokenManager.getInstance().clearSession();
        Intent intent = new Intent(ManageMainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
