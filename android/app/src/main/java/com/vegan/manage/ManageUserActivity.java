package com.vegan.manage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vegan.R;
import com.vegan.api.AdminApiService;
import com.vegan.api.ApiUser;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageUserActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ManageUserAdapter adapter;
    private ArrayList<ApiUser> arrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_user);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        arrayList = new ArrayList<>();
        adapter = new ManageUserAdapter(arrayList, this);
        recyclerView.setAdapter(adapter);

        loadUsers();

        Button btnManageMain = (Button) findViewById(R.id.btnManageMain_User);
        btnManageMain.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageMainActivity.class));
            finish();
        });
    }

    private void loadUsers() {
        String token = TokenManager.getInstance().getToken();
        RetrofitClient.getAdminApi().getUsers(token).enqueue(new Callback<List<ApiUser>>() {
            @Override
            public void onResponse(Call<List<ApiUser>> call, Response<List<ApiUser>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    arrayList.clear();
                    arrayList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Log.e("ManageUserActivity", "응답 실패: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<List<ApiUser>> call, Throwable t) {
                Log.e("ManageUserActivity", "서버 연결 실패", t);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
