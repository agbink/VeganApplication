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
import com.vegan.api.ApiOrder;
import com.vegan.api.ApiOrderItem;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;
import com.vegan.order.MyOrder;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageUserOrderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ManageUserOrderAdapter adapter;
    private ArrayList<MyOrder> arrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_user_order);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        arrayList = new ArrayList<>();
        adapter = new ManageUserOrderAdapter(arrayList, this);
        recyclerView.setAdapter(adapter);

        loadAllOrders();

        Button btnManageMain = (Button) findViewById(R.id.btnManageMain_Order);
        btnManageMain.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageMainActivity.class));
            finish();
        });
    }

    private void loadAllOrders() {
        String token = TokenManager.getInstance().getToken();
        RetrofitClient.getAdminApi().getAllOrders(token).enqueue(new Callback<List<ApiOrder>>() {
            @Override
            public void onResponse(Call<List<ApiOrder>> call, Response<List<ApiOrder>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e("ManageUserOrderActivity", "응답 실패: " + response.code());
                    return;
                }
                arrayList.clear();
                // ApiOrder 목록을 MyOrder(아이템별) 플랫 리스트로 변환
                for (ApiOrder order : response.body()) {
                    if (order.getItems() == null) continue;
                    for (ApiOrderItem item : order.getItems()) {
                        MyOrder row = new MyOrder();
                        row.setOrderId(String.valueOf(order.getId()));
                        row.setEachOrderedId(String.valueOf(item.getId()));
                        row.setOrderDate(formatDate(order.getOrderDate()));
                        row.setProductName(item.getProductNameSnapshot());
                        row.setProductPrice(String.valueOf(item.getPriceAtOrder()));
                        row.setTotalQuantity(item.getQuantity());
                        row.setTotalPrice(item.getPriceAtOrder() * item.getQuantity());
                        row.setOrderImg(item.getImageUrlSnapshot());
                        row.setOrderstate(order.getOrderState());
                        row.setUserName(order.getUserName());
                        row.setUseridtoken(String.valueOf(order.getUserId()));
                        row.setPhone(order.getPhone());
                        row.setAddress(order.getAddress());
                        row.setOverTotalPrice(order.getTotalPrice());
                        row.setDoReview(item.isReviewed() ? "Yes" : "No");
                        arrayList.add(row);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onFailure(Call<List<ApiOrder>> call, Throwable t) {
                Log.e("ManageUserOrderActivity", "서버 연결 실패", t);
            }
        });
    }

    private String formatDate(String isoDateTime) {
        if (isoDateTime == null) return "";
        String replaced = isoDateTime.replace("T", " ");
        return replaced.length() >= 19 ? replaced.substring(0, 19) : replaced;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
