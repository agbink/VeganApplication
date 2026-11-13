package com.vegan.category;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

public class Category1Fragment extends Fragment {

    private static final int PAGE_SIZE = 20;

    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private ArrayList<ApiProduct> arrayList;
    private LinearLayoutManager layoutManager;

    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private Call<PageResponse<ApiProduct>> inFlight;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category1, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        arrayList = new ArrayList<>();
        // FragmentPagerAdapter는 탭을 벗어나면 View만 파괴하고 Fragment 객체는 재사용한다.
        // View를 다시 만들 때 페이지 상태도 초기화해야 0페이지부터 다시 불러온다.
        currentPage = 0;
        isLoading = false;
        isLastPage = false;
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

    @Override
    public void onDestroyView() {
        // 탭을 벗어나 View가 파괴되면 진행 중인 요청을 취소한다.
        if (inFlight != null) {
            inFlight.cancel();
            inFlight = null;
        }
        super.onDestroyView();
    }

    // 전체 상품 (카테고리 필터 없음)
    private void loadNextPage() {
        isLoading = true;
        Call<PageResponse<ApiProduct>> request =
                RetrofitClient.getProductApi().getProductsPaged(null, currentPage, PAGE_SIZE);
        inFlight = request;
        request.enqueue(new Callback<PageResponse<ApiProduct>>() {
                    @Override
                    public void onResponse(Call<PageResponse<ApiProduct>> call,
                                           Response<PageResponse<ApiProduct>> response) {
                        // 이전 View에서 보낸 요청의 늦은 응답은 새 목록에 섞이지 않게 무시한다.
                        if (call != inFlight) return;
                        inFlight = null;
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
                        if (call != inFlight) return; // 취소됐거나 이전 View의 요청
                        inFlight = null;
                        isLoading = false;
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "상품을 불러오지 못했습니다", Toast.LENGTH_SHORT).show();
                        }
                        Log.e("Category1Fragment", "상품 로드 실패", t);
                    }
                });
    }
}
