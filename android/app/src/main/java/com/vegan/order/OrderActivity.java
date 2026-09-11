package com.vegan.order;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vegan.R;
import com.vegan.api.ApiCartItem;
import com.vegan.api.ApiOrder;
import com.vegan.api.ApiUser;
import com.vegan.api.OrderCreateRequestBody;
import com.vegan.api.OrderItemRequestBody;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<ApiCartItem> cartList;
    private TextView overTotalAmount;
    private TextView orderName, orderPhone, orderAddress;
    private String strOrderName, strOrderPhone, strOrderAddress;
    private Button btnPayment;
    private DecimalFormat decimalFormat = new DecimalFormat("###,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        recyclerView = findViewById(R.id.recyclerView_order);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        overTotalAmount = findViewById(R.id.order_totalPrice);
        orderName = findViewById(R.id.order_name);
        orderPhone = findViewById(R.id.order_phone);
        orderAddress = findViewById(R.id.order_address);
        btnPayment = findViewById(R.id.btnPayment);

        cartList = (ArrayList<ApiCartItem>) getIntent().getSerializableExtra("itemList");
        if (cartList == null) cartList = new ArrayList<>();

        recyclerView.setAdapter(new OrderAdapter(this, cartList));

        int total = 0;
        for (ApiCartItem item : cartList) {
            total += item.getProductPrice() * item.getSelectedQuantity();
        }
        overTotalAmount.setText(decimalFormat.format(total) + "원");

        String token = TokenManager.getInstance().getToken();
        RetrofitClient.getUserApi().getMe(token).enqueue(new Callback<ApiUser>() {
            @Override
            public void onResponse(Call<ApiUser> call, Response<ApiUser> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiUser user = response.body();
                    orderName.setText(user.getUsername());
                    orderPhone.setText(user.getPhone() != null ? user.getPhone() : "");
                    orderAddress.setText(user.getAddress() != null ? user.getAddress() : "");
                    strOrderName = user.getUsername();
                    strOrderPhone = user.getPhone();
                    strOrderAddress = user.getAddress();
                }
            }
            @Override
            public void onFailure(Call<ApiUser> call, Throwable t) {
                Log.e("OrderActivity", "사용자 정보 로드 실패", t);
            }
        });

        btnPayment.setOnClickListener(v -> {
            if (cartList.isEmpty()) return;
            placeOrder(token);
        });
    }

    private void placeOrder(String token) {
        List<OrderItemRequestBody> items = new ArrayList<>();
        for (ApiCartItem item : cartList) {
            items.add(new OrderItemRequestBody(item.getProductId(), item.getSelectedQuantity()));
        }
        OrderCreateRequestBody body = new OrderCreateRequestBody(
                strOrderName, strOrderPhone, strOrderAddress, items);

        RetrofitClient.getOrderApi().createOrder(token, body).enqueue(new Callback<ApiOrder>() {
            @Override
            public void onResponse(Call<ApiOrder> call, Response<ApiOrder> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e("OrderActivity", "주문 실패: " + response.code());
                    return;
                }
                RetrofitClient.getCartApi().clearCart(token).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> r) {}
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });
                Intent intent = new Intent(OrderActivity.this, OrderCompleteActivity.class);
                intent.putExtra("orderId", String.valueOf(response.body().getId()));
                startActivity(intent);
                finish();
            }
            @Override
            public void onFailure(Call<ApiOrder> call, Throwable t) {
                Log.e("OrderActivity", "서버 연결 실패", t);
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
