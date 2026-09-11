package com.vegan.manage;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vegan.R;

public class ManageUserDetailActivity extends AppCompatActivity {
    TextView UserEmail, UserName, UserPhone, UserToken, UserAddress, UserProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_user_detail);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        UserEmail    = (TextView) findViewById(R.id.UserEmail);
        UserName     = (TextView) findViewById(R.id.UserName);
        UserPhone    = (TextView) findViewById(R.id.UserPhone);
        UserToken    = (TextView) findViewById(R.id.UserToken);    // 재사용: 회원 ID 표시
        UserAddress  = (TextView) findViewById(R.id.UserAderess);

        // UserPassword TextView는 provider 표시에 재사용
        UserProvider = (TextView) findViewById(R.id.UserPassword);

        long userId      = getIntent().getLongExtra("userId", 0);
        String email     = getIntent().getStringExtra("userEmail");
        String name      = getIntent().getStringExtra("userName");
        String phone     = getIntent().getStringExtra("userPhone");
        String address   = getIntent().getStringExtra("userAddress");
        String provider  = getIntent().getStringExtra("userProvider");

        UserEmail.setText(email != null ? email : "");
        UserName.setText(name != null ? name : "");
        UserPhone.setText(phone != null ? phone : "");
        UserToken.setText("ID: " + userId);
        UserAddress.setText(address != null ? address : "");
        UserProvider.setText("가입방법: " + (provider != null ? provider : ""));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
