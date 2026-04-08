package com.vegan.Manage;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vegan.R;
import com.vegan.User;

public class ManageUserDetailActivity extends AppCompatActivity {
    TextView UserEmail, UserName, Password, UserPhone, UserToken,UserAdderess, UserRegdate;

    private User user = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_user_detail);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 뒤로가기 버튼, 디폴트로 true만 해도 백버튼이 생김


        UserEmail = (TextView) findViewById(R.id.UserEmail);
        UserName = (TextView) findViewById(R.id.UserName);
        Password = (TextView) findViewById(R.id.UserPassword);
        UserPhone = (TextView) findViewById(R.id.UserPhone);
        UserToken = (TextView) findViewById(R.id.UserToken);
        UserAdderess = (TextView) findViewById(R.id.UserAderess);

        final Object object = getIntent().getSerializableExtra("ManageUserDetail");
        if(object instanceof User){
            user = (User) object;
        }

        UserEmail.setText(user.getEmailId());
        UserName.setText(user.getUsername());
        Password.setText(user.getPassword());
        UserPhone.setText(user.getPhone());
        UserToken.setText(user.getIdToken());
        UserAdderess.setText(user.getAddress());

    }

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