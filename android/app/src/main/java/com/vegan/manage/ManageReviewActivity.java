package com.vegan.manage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vegan.R;
import com.vegan.api.ApiReview;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageReviewActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ManageReviewAdapter adapter;
    private ArrayList<ApiReview> arrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_review);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        arrayList = new ArrayList<>();
        adapter = new ManageReviewAdapter(arrayList, this);
        recyclerView.setAdapter(adapter);

        loadAllReviews();

        Button btnManageMain = (Button) findViewById(R.id.btnManageMain_Review);
        btnManageMain.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageMainActivity.class));
            finish();
        });
    }

    public void loadAllReviews() {
        String token = TokenManager.getInstance().getToken();
        RetrofitClient.getAdminApi().getAllReviews(token).enqueue(new Callback<List<ApiReview>>() {
            @Override
            public void onResponse(Call<List<ApiReview>> call, Response<List<ApiReview>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    arrayList.clear();
                    arrayList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Log.e("ManageReviewActivity", "응답 실패: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<List<ApiReview>> call, Throwable t) {
                Log.e("ManageReviewActivity", "서버 연결 실패", t);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
