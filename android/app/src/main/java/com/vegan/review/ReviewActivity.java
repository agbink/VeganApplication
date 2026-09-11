package com.vegan.review;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vegan.R;
import com.vegan.api.ApiReview;
import com.vegan.api.RetrofitClient;
import com.vegan.cart.CartActivity;
import com.vegan.category.CategoryActivity;
import com.vegan.main.MainActivity;
import com.vegan.main.MyPageActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewActivity extends AppCompatActivity {

    private RecyclerView fullreviewrecyclerView;
    private ReviewAdapter reviewAdapter;
    private ArrayList<ApiReview> dataList;
    private long pid;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        fullreviewrecyclerView = findViewById(R.id.fullrecyclerView);
        fullreviewrecyclerView.setHasFixedSize(true);
        fullreviewrecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dataList = new ArrayList<>();

        pid = getIntent().getLongExtra("pid", 0);

        reviewAdapter = new ReviewAdapter(dataList);
        fullreviewrecyclerView.setAdapter(reviewAdapter);

        loadReviews();

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.tab_home);
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

    private void loadReviews() {
        RetrofitClient.getReviewApi().getReviews(pid).enqueue(new Callback<List<ApiReview>>() {
            @Override
            public void onResponse(Call<List<ApiReview>> call, Response<List<ApiReview>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    dataList.clear();
                    dataList.addAll(response.body());
                    reviewAdapter.notifyDataSetChanged();

                    float total = 0;
                    for (ApiReview r : dataList) total += r.getRating();
                    int count = dataList.size();
                    float avg = count > 0 ? total / count : 0;
                    String formatted = String.format("%.1f (%d)", avg, count);

                    TextView reviewRating = findViewById(R.id.value);
                    reviewRating.setText(formatted);
                    RatingBar ratingBar = findViewById(R.id.reviewRating);
                    ratingBar.setRating(avg);
                }
            }
            @Override
            public void onFailure(Call<List<ApiReview>> call, Throwable t) {
                Log.e("ReviewActivity", "리뷰 로드 실패", t);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
