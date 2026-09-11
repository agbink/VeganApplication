package com.vegan.category;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vegan.R;
import com.vegan.api.ApiProduct;
import com.vegan.api.PageResponse;
import com.vegan.api.RetrofitClient;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Category2Fragment extends Fragment {

    private static final int PAGE_SIZE = 20;
    private static final int CATEGORY = 101;

    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private ArrayList<ApiProduct> arrayList;
    private LinearLayoutManager layoutManager;

    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category2, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        arrayList = new ArrayList<>();
        adapter = new CategoryAdapter(arrayList, getActivity());
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
        return view;
    }

    private void loadNextPage() {
        isLoading = true;
        RetrofitClient.getProductApi().getProductsPaged(CATEGORY, currentPage, PAGE_SIZE)
                .enqueue(new Callback<PageResponse<ApiProduct>>() {
                    @Override
                    public void onResponse(Call<PageResponse<ApiProduct>> call,
                                           Response<PageResponse<ApiProduct>> response) {
                        isLoading = false;
                        if (response.isSuccessful() && response.body() != null) {
                            int start = arrayList.size();
                            arrayList.addAll(response.body().getContent());
                            adapter.notifyItemRangeInserted(start, response.body().getContent().size());
                            isLastPage = response.body().isLast();
                            currentPage++;
                        }
                    }

                    @Override
                    public void onFailure(Call<PageResponse<ApiProduct>> call, Throwable t) {
                        isLoading = false;
                        Log.e("Category2Fragment", "상품 로드 실패", t);
                    }
                });
    }
}
