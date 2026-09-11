package com.vegan.cart;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vegan.api.ApiCartItem;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;
import com.vegan.main.MyPageActivity;
import com.vegan.R;
import com.vegan.category.CategoryActivity;
import com.vegan.main.MainActivity;
import com.vegan.order.OrderActivity;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private RecyclerView recyclerView;
    private CartAdapter cartAdapter;
    private List<ApiCartItem> cartList;
    private TextView overTotalAmount;
    private Button buyBtn;
    private DecimalFormat decimalFormat = new DecimalFormat("###,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.cartView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        overTotalAmount = findViewById(R.id.txt_totalPrice);
        buyBtn = findViewById(R.id.buy_now);
        cartList = new ArrayList<>();
        cartAdapter = new CartAdapter(this, cartList, overTotalAmount);
        recyclerView.setAdapter(cartAdapter);

        loadCart();

        buyBtn.setOnClickListener(v -> {
            if (cartList.isEmpty()) {
                Toast.makeText(this, "장바구니가 비어있습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(CartActivity.this, OrderActivity.class);
            intent.putExtra("itemList", (Serializable) new ArrayList<>(cartList));
            startActivity(intent);
            finish();
        });

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.tab_cart);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.tab_home) {
                startActivity(new Intent(CartActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.tab_category) {
                startActivity(new Intent(CartActivity.this, CategoryActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.tab_cart) {
                return true;
            } else if (item.getItemId() == R.id.tab_mypage) {
                startActivity(new Intent(CartActivity.this, MyPageActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadCart() {
        String token = TokenManager.getInstance().getToken();
        RetrofitClient.getCartApi().getCart(token).enqueue(new Callback<List<ApiCartItem>>() {
            @Override
            public void onResponse(Call<List<ApiCartItem>> call, Response<List<ApiCartItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cartList.clear();
                    cartList.addAll(response.body());
                    cartAdapter.notifyDataSetChanged();
                    updateTotal();
                }
            }

            @Override
            public void onFailure(Call<List<ApiCartItem>> call, Throwable t) {
                Log.e("CartActivity", "장바구니 로드 실패", t);
                Toast.makeText(CartActivity.this, "장바구니를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updateTotal() {
        int total = 0;
        for (ApiCartItem item : cartList) {
            total += item.getProductPrice() * item.getSelectedQuantity();
        }
        overTotalAmount.setText(decimalFormat.format(total) + "원");
    }
}
