package com.vegan.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vegan.CartActivity;
import com.vegan.CategoryActivity;
import com.vegan.ItemsDomain;
import com.vegan.MainActivity;
import com.vegan.MyPageActivity;
import com.vegan.R;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private EditText searchEditText;
    private RecyclerView resultsListView;
    private SearchAdapter adapter;
    private List<ItemsDomain> searchResults = new ArrayList<>(); // 검색 결과를 저장할 리스트

    Toolbar toolbar;
    private BottomNavigationView bottomNavigationView;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);//기본 제목 삭제.
        actionBar.setDisplayHomeAsUpEnabled(true);


        searchEditText = findViewById(R.id.searchEditText);
        resultsListView = findViewById(R.id.searchRecyclerView);
        resultsListView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SearchAdapter(searchResults);
        resultsListView.setAdapter(adapter);

        // // RecyclerView 터치 이벤트 처리
        resultsListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideKeyboard();
                return false;
            }
        });
        //키보드 활성화
        searchEditText.setEnabled(true);
        searchEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        // Firebase Realtime Database에서 데이터를 가져와서 검색 결과를 업데이트
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String searchText = charSequence.toString().toLowerCase();

                // Firebase 데이터베이스에서 검색어와 일치하는 데이터를 찾음
                searchFirebaseDatabase(searchText);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    //스크롤 시 키보드 숨기기
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    private void searchFirebaseDatabase(final String searchText) {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = firebaseDatabase.getReference("Product");

        // 검색어가 비어 있으면 전체 리스트를 보여줍니다.
        if (searchText.isEmpty()) {
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    searchResults.clear(); // 검색 결과 초기화

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        ItemsDomain item = snapshot.getValue(ItemsDomain.class);

                        if (item != null) {
                            searchResults.add(item); // 전체 리스트를 추가
                        }
                    }
                    adapter.notifyDataSetChanged(); // 검색 결과를 RecyclerView에 업데이트
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // 검색 중 오류가 발생했을 때의 처리 코드
                }
            });
        } else {
            // 검색어가 있는 경우에만 쿼리를 수행합니다.
            databaseReference.orderByChild("pname").startAt(searchText).endAt(searchText + "\uf8ff")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            searchResults.clear(); // 검색 결과 초기화

                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                ItemsDomain item = snapshot.getValue(ItemsDomain.class);

                                if (item != null) {
                                    searchResults.add(item); // 검색어와 일치하는 결과를 추가
                                }
                            }
                            adapter.notifyDataSetChanged(); // 검색 결과를 RecyclerView에 업데이트
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // 검색 중 오류가 발생했을 때의 처리 코드
                        }
                    });
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.searchmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) { //뒤로가기
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_search) {
            searchEditText.requestFocus(); // EditText에 포커스를 주어 클릭한 것처럼 만듭니다.
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT); // 키보드를 나타나게 합니다.
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}