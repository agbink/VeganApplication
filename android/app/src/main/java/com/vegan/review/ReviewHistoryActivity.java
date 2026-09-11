package com.vegan.review;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vegan.R;
import com.vegan.api.ApiReview;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;
import com.vegan.cart.CartActivity;
import com.vegan.category.CategoryActivity;
import com.vegan.main.MainActivity;
import com.vegan.main.MyPageActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArrayList<ApiReview> reviewList;
    private ReviewHistoryAdapter adapter;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_history);

        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.reviewHisyoryRecycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewList = new ArrayList<>();
        adapter = new ReviewHistoryAdapter(reviewList, this);
        recyclerView.setAdapter(adapter);

        loadMyReviews();

        bottomNavigationView = findViewById(R.id.bottomNavigation_reviewhistory);
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
    }

    private void loadMyReviews() {
        String token = TokenManager.getInstance().getToken();
        RetrofitClient.getReviewApi().getMyReviews(token).enqueue(new Callback<List<ApiReview>>() {
            @Override
            public void onResponse(Call<List<ApiReview>> call, Response<List<ApiReview>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    reviewList.clear();
                    reviewList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<ApiReview>> call, Throwable t) {
                Log.e("ReviewHistoryActivity", "리뷰 로드 실패", t);
            }
        });
    }
}
