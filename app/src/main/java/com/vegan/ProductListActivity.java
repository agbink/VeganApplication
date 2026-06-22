package com.vegan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.vegan.Manage.ManageMainActivity;
import com.vegan.api.ApiProduct;
import com.vegan.api.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private ArrayList<ItemsDomain> arrayList;

    private Button btnManageMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        arrayList = new ArrayList<>();
        adapter = new ProductAdapter(arrayList, this);
        recyclerView.setAdapter(adapter);

        loadProducts();

        btnManageMain = findViewById(R.id.btnManageMain_shop);
        btnManageMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProductListActivity.this, ManageMainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void loadProducts() {
        // category에 null을 넘기면 서버에서 전체 상품을 최신순으로 반환합니다.
        RetrofitClient.getProductApi().getProducts(null).enqueue(new Callback<List<ApiProduct>>() {
            @Override
            public void onResponse(Call<List<ApiProduct>> call, Response<List<ApiProduct>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e("ProductListActivity", "응답 실패: " + response.code());
                    return;
                }

                arrayList.clear();
                for (ApiProduct p : response.body()) {
                    ItemsDomain item = new ItemsDomain();
                    item.setPid((int) p.getId());
                    item.setPname(p.getName());
                    item.setPbname(p.getBrandName());
                    item.setPprice(p.getPrice());
                    item.setPimg(p.getImageUrl());
                    item.setPdetailimg(p.getDetailImageUrl());
                    item.setStock(p.getStock());
                    item.setCategory(p.getCategory());
                    item.setPsay(p.getDescription());
                    item.setSalescount(p.getSalesCount());
                    arrayList.add(item);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<List<ApiProduct>> call, Throwable t) {
                Log.e("ProductListActivity", "서버 연결 실패", t);
            }
        });
    }
}
