package com.vegan.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vegan.R;
import com.vegan.api.ApiProduct;
import com.vegan.api.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

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
        resultsListView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchAdapter(searchResults);
        resultsListView.setAdapter(adapter);

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
                String keyword = s.toString().trim();
                if (keyword.isEmpty()) {
                    // 빈 검색어면 전체 상품 표시
                    RetrofitClient.getProductApi().getProducts(null).enqueue(new Callback<List<ApiProduct>>() {
                        @Override
                        public void onResponse(Call<List<ApiProduct>> call, Response<List<ApiProduct>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                searchResults.clear();
                                searchResults.addAll(response.body());
                                adapter.notifyDataSetChanged();
                            }
                        }
                        @Override public void onFailure(Call<List<ApiProduct>> call, Throwable t) {}
                    });
                } else {
                    RetrofitClient.getProductApi().searchProducts(keyword).enqueue(new Callback<List<ApiProduct>>() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void onResponse(Call<List<ApiProduct>> call, Response<List<ApiProduct>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                searchResults.clear();
                                searchResults.addAll(response.body());
                                adapter.notifyDataSetChanged();
                            }
                        }
                        @Override
                        public void onFailure(Call<List<ApiProduct>> call, Throwable t) {
                            Log.e("SearchActivity", "검색 실패", t);
                        }
                    });
                }
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
