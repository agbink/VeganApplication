package com.vegan.main;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vegan.R;
import com.vegan.api.TokenManager;
import com.vegan.auth.ChangeActivity;
import com.vegan.auth.LoginActivity;
import com.vegan.auth.WithdrawActivity;
import com.vegan.cart.CartActivity;
import com.vegan.category.CategoryActivity;
import com.vegan.order.OrderHistoryActivity;
import com.vegan.review.ReviewHistoryActivity;

public class MyPageActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView Tv_my_name;
    Dialog dialog;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);

        dialog = new Dialog(MyPageActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.customdialog);

        Tv_my_name = findViewById(R.id.my_name);
        String username = TokenManager.getInstance().getUsername();
        if (username != null) {
            Tv_my_name.setText(username + "님");
        }

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.tab_mypage);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.tab_home) {
                startActivity(new Intent(this, MainActivity.class)); finish(); return true;
            } else if (item.getItemId() == R.id.tab_category) {
                startActivity(new Intent(this, CategoryActivity.class)); finish(); return true;
            } else if (item.getItemId() == R.id.tab_cart) {
                startActivity(new Intent(this, CartActivity.class)); finish(); return true;
            } else if (item.getItemId() == R.id.tab_mypage) {
                startActivity(new Intent(this, MyPageActivity.class)); finish(); return true;
            }
            return false;
        });

        findViewById(R.id.orderhistory_move).setOnClickListener(this);
        findViewById(R.id.rv_move).setOnClickListener(this);
        findViewById(R.id.mod_move).setOnClickListener(this);
        findViewById(R.id.wdl_move).setOnClickListener(this);
        findViewById(R.id.order_move).setOnClickListener(this);
        findViewById(R.id.review_move).setOnClickListener(this);
        findViewById(R.id.modify_move).setOnClickListener(this);
        findViewById(R.id.wd_move).setOnClickListener(this);
        findViewById(R.id.logout_move).setOnClickListener(v -> showLogoutConfirmationDialog());
        findViewById(R.id.out_move).setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.orderhistory_move || id == R.id.order_move) {
            startActivity(new Intent(this, OrderHistoryActivity.class));
        } else if (id == R.id.rv_move || id == R.id.review_move) {
            startActivity(new Intent(this, ReviewHistoryActivity.class));
        } else if (id == R.id.mod_move || id == R.id.modify_move) {
            startActivity(new Intent(this, ChangeActivity.class));
        } else if (id == R.id.wd_move || id == R.id.wdl_move) {
            startActivity(new Intent(this, WithdrawActivity.class));
        }
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
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
