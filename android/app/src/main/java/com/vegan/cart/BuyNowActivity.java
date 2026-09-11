package com.vegan.cart;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.vegan.R;
import com.vegan.api.ApiOrder;
import com.vegan.api.ApiUser;
import com.vegan.api.OrderCreateRequestBody;
import com.vegan.api.OrderItemRequestBody;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;
import com.vegan.order.OrderCompleteActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyNowActivity extends AppCompatActivity {

    private TextView buynow_pname, buynow_pprice, buynow_totalquantity, overTotalAmount;
    private ImageView buynow_pimg;
    private TextView orderName, orderPhone, orderAddress;
    private Button btnPayment;

    private String productName, productPrice, productImg;
    private int totalPrice, selectedQuantity;
    private long productId;

    private String strOrderName, strOrderPhone, strOrderAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_now);

        buynow_pimg = findViewById(R.id.buynow_pimg);
        buynow_pname = findViewById(R.id.buynow_pname);
        buynow_pprice = findViewById(R.id.buynow_pprice);
        buynow_totalquantity = findViewById(R.id.buynow_totalquantity);
        overTotalAmount = findViewById(R.id.buynow_overtotalPrice);
        orderName = findViewById(R.id.buynow_name);
        orderPhone = findViewById(R.id.buynow_phone);
        orderAddress = findViewById(R.id.buynow_address);
        btnPayment = findViewById(R.id.buynow_btnPayment);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            productName = bundle.getString("productName");
            productPrice = bundle.getString("productPrice");
            selectedQuantity = bundle.getInt("selectedQuantity");
            productImg = bundle.getString("productImg");
            totalPrice = bundle.getInt("totalPrice");
            productId = bundle.getLong("productId");
        }

        DecimalFormat fmt = new DecimalFormat("###,###");
        Glide.with(getApplicationContext()).load(productImg).into(buynow_pimg);
        buynow_pname.setText(productName);
        buynow_pprice.setText(fmt.format(Integer.parseInt(productPrice)) + "원");
        buynow_totalquantity.setText(fmt.format(selectedQuantity) + "개");
        overTotalAmount.setText(fmt.format(totalPrice) + "원");

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
                Log.e("BuyNowActivity", "사용자 정보 로드 실패", t);
            }
        });

        btnPayment.setOnClickListener(v -> placeOrder(token));
    }

    private void placeOrder(String token) {
        List<OrderItemRequestBody> items = new ArrayList<>();
        items.add(new OrderItemRequestBody(productId, selectedQuantity));
        OrderCreateRequestBody body = new OrderCreateRequestBody(
                strOrderName, strOrderPhone, strOrderAddress, items);

        RetrofitClient.getOrderApi().createOrder(token, body).enqueue(new Callback<ApiOrder>() {
            @Override
            public void onResponse(Call<ApiOrder> call, Response<ApiOrder> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e("BuyNowActivity", "주문 실패: " + response.code());
                    return;
                }
                Intent intent = new Intent(BuyNowActivity.this, OrderCompleteActivity.class);
                intent.putExtra("orderId", String.valueOf(response.body().getId()));
                startActivity(intent);
                finish();
            }
            @Override
            public void onFailure(Call<ApiOrder> call, Throwable t) {
                Log.e("BuyNowActivity", "서버 연결 실패", t);
            }
        });
    }
}
