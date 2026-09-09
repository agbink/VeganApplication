package com.vegan.order;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vegan.R;
import com.vegan.api.TokenManager;
import com.vegan.api.ApiOrder;
import com.vegan.api.ApiOrderItem;
import com.vegan.api.RetrofitClient;
import com.vegan.cart.CartActivity;
import com.vegan.category.CategoryActivity;
import com.vegan.main.MainActivity;
import com.vegan.main.MyPageActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderCompleteActivity extends AppCompatActivity {
    private long orderId;

    private TextView cmpOrderId, cmpOrderDate, cmp_totalPrice, cmp_name, cmp_phone, cmp_address;

    private RecyclerView.LayoutManager layoutManager;
    private RecyclerView recyclerView;

    private OrderCompleteAdapter orderCompleteAdapter;
    private List<MyOrder> myOrderList;

    private BottomNavigationView bottomNavigationView;

    DecimalFormat decimalFormat = new DecimalFormat("###,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_complete);
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // OrderActivity에서 보낸 새 주문 id (MySQL PK) 가져오기
        Intent intent = getIntent();
        orderId = Long.parseLong(intent.getStringExtra("orderId"));
        Log.d("OrderCompleteActivity", String.valueOf(orderId));

        recyclerView = findViewById(R.id.recyclerView_orderComplete);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        cmpOrderId = findViewById(R.id.cmpOrderId);
        cmpOrderDate = findViewById(R.id.cmpOrderDate);
        cmp_totalPrice = findViewById(R.id.cmp_totalPrice);
        cmp_name = findViewById(R.id.cmp_name);
        cmp_phone = findViewById(R.id.cmp_phone);
        cmp_address = findViewById(R.id.cmp_address);

        myOrderList = new ArrayList<>();
        orderCompleteAdapter = new OrderCompleteAdapter(this, myOrderList);
        recyclerView.setAdapter(orderCompleteAdapter);

        loadOrderDetail();

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.tab_cart);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.tab_home) {
                startActivity(new Intent(OrderCompleteActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.tab_category) {
                startActivity(new Intent(OrderCompleteActivity.this, CategoryActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.tab_cart) {
                startActivity(new Intent(OrderCompleteActivity.this, CartActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.tab_mypage) {
                startActivity(new Intent(OrderCompleteActivity.this, MyPageActivity.class));
                finish();
                return true;
            }
            return false;
        });

        Button gotohomeButton = findViewById(R.id.gotohome);
        gotohomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(OrderCompleteActivity.this, MainActivity.class);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(homeIntent);
                finish();
            }
        });
    }

    // 핵심 변경 부분: GET /api/orders/{id} 한 번으로 헤더 + 상품목록 전체를 받아옴
    private void loadOrderDetail() {
        String token = TokenManager.getInstance().getToken();
        RetrofitClient.getOrderApi().getOrder(token, orderId).enqueue(new Callback<ApiOrder>() {
            @Override
            public void onResponse(@NonNull Call<ApiOrder> call, @NonNull Response<ApiOrder> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e("OrderCompleteActivity", "응답 실패: " + response.code());
                    return;
                }

                ApiOrder order = response.body();

                cmpOrderId.setText(String.valueOf(order.getId()));
                cmpOrderDate.setText(order.getOrderDate() == null ? "" : order.getOrderDate().replace("T", " "));
                cmp_totalPrice.setText(decimalFormat.format(order.getTotalPrice()) + "원");
                cmp_name.setText(order.getUserName());
                cmp_phone.setText(order.getPhone());
                cmp_address.setText(order.getAddress());

                myOrderList.clear();
                if (order.getItems() != null) {
                    for (ApiOrderItem item : order.getItems()) {
                        MyOrder line = new MyOrder();
                        line.setOrderImg(item.getImageUrlSnapshot());
                        line.setProductName(item.getProductNameSnapshot());
                        line.setTotalPrice(item.getPriceAtOrder() * item.getQuantity());
                        line.setTotalQuantity(item.getQuantity());
                        myOrderList.add(line);
                    }
                }
                orderCompleteAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(@NonNull Call<ApiOrder> call, @NonNull Throwable t) {
                Log.e("OrderCompleteActivity", "서버 연결 실패", t);
            }
        });
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
