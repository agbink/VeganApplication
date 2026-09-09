package com.vegan.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vegan.R;
import com.vegan.api.ApiProduct;
import com.vegan.api.RetrofitClient;
import com.vegan.cart.CartActivity;
import com.vegan.category.CategoryActivity;
import com.vegan.search.SearchActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.relex.circleindicator.CircleIndicator3;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends FragmentActivity {

    private ViewPager2 mPager, mPager01;
    private int currentPage = 0, currentPage01 = 0;
    private final long DELAY_MS = 3000, PERIOD_MS = 3000;
    private final int num_page = 3, num_page01 = 3;
    private CircleIndicator3 mIndicator, mIndicator01;

    private RecyclerView recyclerView;
    private NewArrivalAdapter adapter;
    private ArrayList<ApiProduct> arrayList;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPager = findViewById(R.id.viewpager);
        mPager01 = findViewById(R.id.viewpager01);
        mPager.setAdapter(new MainAdapter(this, num_page));
        mPager01.setAdapter(new MainAdapter2(this, num_page01));

        mIndicator = findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
        mIndicator.createIndicators(num_page, 0);
        mIndicator01 = findViewById(R.id.indicator01);
        mIndicator01.setViewPager(mPager01);
        mIndicator01.createIndicators(num_page01, 0);

        mPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        mPager.setCurrentItem(1000);
        mPager.setOffscreenPageLimit(2);
        mPager01.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        mPager01.setCurrentItem(1000);
        mPager01.setOffscreenPageLimit(2);

        final Handler handler = new Handler(Looper.getMainLooper());
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override public void run() {
                handler.post(() -> mPager.setCurrentItem(currentPage++));
            }
        }, DELAY_MS, PERIOD_MS);

        recyclerView = findViewById(R.id.newarrival);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        arrayList = new ArrayList<>();
        adapter = new NewArrivalAdapter(arrayList, this);
        recyclerView.setAdapter(adapter);

        // 최신 상품 (전체 조회 후 앞 6개 표시)
        RetrofitClient.getProductApi().getProducts(null).enqueue(new Callback<List<ApiProduct>>() {
            @Override
            public void onResponse(Call<List<ApiProduct>> call, Response<List<ApiProduct>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    arrayList.clear();
                    arrayList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<ApiProduct>> call, Throwable t) {
                Log.e("MainActivity", "상품 로드 실패", t);
            }
        });

        bottomNavigationView = findViewById(R.id.bottomNavigation_main);
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

        TextView morebtn = findViewById(R.id.more_btn);
        morebtn.setOnClickListener(v -> startActivity(new Intent(this, CategoryActivity.class)));

        findViewById(R.id.imageView6).setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
    }
}
