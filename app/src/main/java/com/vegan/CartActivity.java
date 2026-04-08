package com.vegan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;

    private RecyclerView.LayoutManager layoutManager;

    private DecimalFormat decimalFormat = new DecimalFormat("###,###");

    private RecyclerView recyclerView;
    private CartAdapter cartAdapter;
    private List<Cart> cartList;

    private TextView overTotalAmount;

    private Button buyBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        recyclerView = findViewById(R.id.cartView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        firebaseAuth = FirebaseAuth.getInstance();

        buyBtn = findViewById(R.id.buy_now);

        firebaseDatabase = FirebaseDatabase.getInstance();

        overTotalAmount = findViewById(R.id.txt_totalPrice);
        cartList = new ArrayList<>();

        firebaseDatabase.getReference("CurrentUser").child(firebaseAuth.getCurrentUser().getUid()).child("AddToCart").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DataSnapshot dataSnapshot : task.getResult().getChildren()) {
                    String dataId = dataSnapshot.getKey();

                    Cart cart = dataSnapshot.getValue(Cart.class);
                    cart.setDataId(dataId);

                    cartList.add(cart);
                    cartAdapter.notifyDataSetChanged();

                    int total = 0;
                    for (Cart item : cartList) {
                        total += (item.getSelectedQuantity() * Integer.parseInt(item.getProductPrice()));
                    }
                    overTotalAmount.setText(decimalFormat.format(total) + "원");
                }
            }
        });

        cartAdapter = new CartAdapter(this, cartList, overTotalAmount);
        recyclerView.setAdapter(cartAdapter);

        buyBtn.setOnClickListener(v -> {
            // 최신 데이터 기반으로 결제 화면으로 이동
            Intent intent = new Intent(CartActivity.this, OrderActivity.class);
            intent.putExtra("itemList", (Serializable) cartList);
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
                startActivity(new Intent(CartActivity.this, CartActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.tab_mypage) {
                startActivity(new Intent(CartActivity.this, MyPageActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
