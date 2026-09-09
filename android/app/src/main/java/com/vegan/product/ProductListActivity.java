package com.vegan.product;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.vegan.manage.ManageMainActivity;
import com.vegan.R;
import com.vegan.api.ApiProduct;
import com.vegan.api.PageResponse;
import com.vegan.api.RetrofitClient;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductListActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 20;

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private ArrayList<ApiProduct> arrayList;
    private LinearLayoutManager layoutManager;

    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        arrayList = new ArrayList<>();
        adapter = new ProductAdapter(arrayList, this);
        recyclerView.setAdapter(adapter);

        // 무한 스크롤: 목록 끝 5개 이내로 스크롤되면 다음 페이지 로드
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || isLoading || isLastPage) return;
                if (layoutManager.findLastVisibleItemPosition() >= arrayList.size() - 5) {
                    loadNextPage();
                }
            }
        });

        loadNextPage();

        Button btnManageMain = findViewById(R.id.btnManageMain_shop);
        btnManageMain.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageMainActivity.class));
            finish();
        });
    }

    private void loadNextPage() {
        isLoading = true;
        RetrofitClient.getProductApi().getProductsPaged(null, currentPage, PAGE_SIZE)
                .enqueue(new Callback<PageResponse<ApiProduct>>() {
                    @Override
                    public void onResponse(Call<PageResponse<ApiProduct>> call,
                                           Response<PageResponse<ApiProduct>> response) {
                        isLoading = false;
                        if (!response.isSuccessful() || response.body() == null) {
                            Log.e("ProductListActivity", "응답 실패: " + response.code());
                            return;
                        }
                        int start = arrayList.size();
                        arrayList.addAll(response.body().getContent());
                        adapter.notifyItemRangeInserted(start, response.body().getContent().size());
                        isLastPage = response.body().isLast();
                        currentPage++;
                    }

                    @Override
                    public void onFailure(Call<PageResponse<ApiProduct>> call, Throwable t) {
                        isLoading = false;
                        Log.e("ProductListActivity", "서버 연결 실패", t);
                    }
                });
    }
}
