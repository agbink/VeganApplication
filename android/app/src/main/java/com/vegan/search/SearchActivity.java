package com.vegan.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vegan.R;
import com.vegan.api.ApiProduct;
import com.vegan.api.PageResponse;
import com.vegan.api.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private static final int SEARCH_PAGE_SIZE = 20;
    private static final long SEARCH_DEBOUNCE_MS = 300;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch = () -> {};
    private Call<PageResponse<ApiProduct>> inFlight;

    // 검색 결과 무한 스크롤 상태 (검색어가 바뀌면 초기화)
    private String currentKeyword = "";
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean isLastPage = true;

    private EditText searchEditText;
    private RecyclerView resultsListView;
    private SearchAdapter adapter;
    private List<ApiProduct> searchResults = new ArrayList<>();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        searchEditText = findViewById(R.id.searchEditText);
        resultsListView = findViewById(R.id.searchRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        resultsListView.setLayoutManager(layoutManager);
        adapter = new SearchAdapter(searchResults);
        resultsListView.setAdapter(adapter);

        // 무한 스크롤: 목록 끝 5개 이내로 스크롤되면 같은 검색어의 다음 페이지 로드
        resultsListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || isLoading || isLastPage) return;
                if (layoutManager.findLastVisibleItemPosition() >= searchResults.size() - 5) {
                    loadPage(false);
                }
            }
        });

        resultsListView.setOnTouchListener((v, event) -> { hideKeyboard(); return false; });

        searchEditText.setEnabled(true);
        searchEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                // 디바운스: 입력이 SEARCH_DEBOUNCE_MS 동안 멈췄을 때 한 번만 요청한다.
                String keyword = s.toString().trim();
                handler.removeCallbacks(pendingSearch);
                pendingSearch = () -> search(keyword);
                handler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
            }
        });
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(pendingSearch);
        if (inFlight != null) inFlight.cancel();
        super.onDestroy();
    }

    private void search(String keyword) {
        // 이전 검색어의 요청은 취소하고 첫 페이지부터 다시 불러온다.
        if (inFlight != null) inFlight.cancel();
        currentKeyword = keyword;
        currentPage = 0;
        isLastPage = false;
        loadPage(true);
    }

    private void loadPage(boolean reset) {
        Call<PageResponse<ApiProduct>> call = currentKeyword.isEmpty()
                // 빈 검색어면 최신 상품 목록을 보여준다
                ? RetrofitClient.getProductApi().getProductsPaged(null, currentPage, SEARCH_PAGE_SIZE)
                : RetrofitClient.getProductApi().searchProductsPaged(currentKeyword, currentPage, SEARCH_PAGE_SIZE);
        inFlight = call;
        isLoading = true;

        call.enqueue(new Callback<PageResponse<ApiProduct>>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(Call<PageResponse<ApiProduct>> call, Response<PageResponse<ApiProduct>> response) {
                // 이전 검색어(또는 취소된 요청)의 늦은 응답이 최신 결과를 덮어쓰지 않게 무시한다.
                if (call != inFlight) return;
                inFlight = null;
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    List<ApiProduct> content = response.body().getContent();
                    if (reset) {
                        searchResults.clear();
                        searchResults.addAll(content);
                        adapter.notifyDataSetChanged();
                        resultsListView.scrollToPosition(0);
                    } else {
                        int start = searchResults.size();
                        searchResults.addAll(content);
                        adapter.notifyItemRangeInserted(start, content.size());
                    }
                    isLastPage = response.body().isLast();
                    currentPage++;
                }
            }

            @Override
            public void onFailure(Call<PageResponse<ApiProduct>> call, Throwable t) {
                if (call != inFlight) return; // 취소됐거나 이전 검색어의 요청
                inFlight = null;
                isLoading = false;
                Log.e("SearchActivity", "검색 실패", t);
            }
        });
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.searchmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_search) {
            searchEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
