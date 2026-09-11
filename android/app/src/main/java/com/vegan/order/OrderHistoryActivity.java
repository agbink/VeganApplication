package com.vegan.order;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vegan.api.TokenManager;
import com.vegan.R;
import com.vegan.api.ApiOrder;
import com.vegan.api.ApiOrderItem;
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

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView parentRecyclerView;
    private ArrayList<MyOrder> parentModelArrayList;
    private RecyclerView.Adapter parentAdapter;
    private RecyclerView.LayoutManager parentLayoutManager;

    private BottomNavigationView bottomNavigationView;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        parentRecyclerView = findViewById(R.id.Parent_recyclerView);
        parentRecyclerView.setHasFixedSize(true);
        parentLayoutManager = new LinearLayoutManager(this);
        parentRecyclerView.setLayoutManager(parentLayoutManager);

        toolbar = findViewById(R.id.ordh_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        loadOrderHistory();

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.tab_mypage);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.tab_home) {
                startActivity(new Intent(OrderHistoryActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.tab_category) {
                startActivity(new Intent(OrderHistoryActivity.this, CategoryActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.tab_cart) {
                startActivity(new Intent(OrderHistoryActivity.this, CartActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.tab_mypage) {
                startActivity(new Intent(OrderHistoryActivity.this, MyPageActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    // 핵심 변경 부분: Firebase 대신 GET /api/orders 호출 (JWT로 본인 주문만 조회)
    // 서버가 이미 최신순(orderDate desc)으로 정렬해서 보내주기 때문에,
    // 기존에 있던 Collections.sort() 코드는 더 이상 필요 없습니다.
    private void loadOrderHistory() {
        String token = TokenManager.getInstance().getToken();
        RetrofitClient.getOrderApi().getOrders(token).enqueue(new Callback<List<ApiOrder>>() {
            @Override
            public void onResponse(Call<List<ApiOrder>> call, Response<List<ApiOrder>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e("OrderHistoryActivity", "응답 실패: " + response.code());
                    return;
                }

                parentModelArrayList = new ArrayList<>();

                for (ApiOrder apiOrder : response.body()) {
                    if (apiOrder.getItems() == null || apiOrder.getItems().isEmpty()) {
                        continue;
                    }

                    ArrayList<MyOrder> childModelArrayList = new ArrayList<>();
                    for (ApiOrderItem item : apiOrder.getItems()) {
                        MyOrder child = new MyOrder();
                        child.setOrderId(String.valueOf(apiOrder.getId()));
                        child.setProductName(item.getProductNameSnapshot());
                        child.setProductPrice(String.valueOf(item.getPriceAtOrder()));
                        child.setTotalQuantity(item.getQuantity());
                        child.setOrderImg(item.getImageUrlSnapshot());
                        child.setOrderstate(apiOrder.getOrderState());
                        child.setDoReview(item.isReviewed() ? "Yes" : "No");
                        childModelArrayList.add(child);
                    }

                    MyOrder parent = new MyOrder();
                    parent.setOrderId(String.valueOf(apiOrder.getId()));
                    parent.setOrderDate(formatDate(apiOrder.getOrderDate()));
                    parent.setChildModelArrayList(childModelArrayList);
                    parentModelArrayList.add(parent);
                }

                parentAdapter = new OrderHistoryParentRcyAdapter(parentModelArrayList, OrderHistoryActivity.this);
                parentRecyclerView.setAdapter(parentAdapter);
            }

            @Override
            public void onFailure(Call<List<ApiOrder>> call, Throwable t) {
                Log.e("OrderHistoryActivity", "서버 연결 실패", t);
            }
        });
    }

    // 서버가 보내는 "2024-12-04T06:08:46.123" 형태를 "2024-12-04 06:08:46"로 보기 좋게 변환
    private String formatDate(String isoDateTime) {
        if (isoDateTime == null) {
            return "";
        }
        String replaced = isoDateTime.replace("T", " ");
        return replaced.length() >= 19 ? replaced.substring(0, 19) : replaced;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}