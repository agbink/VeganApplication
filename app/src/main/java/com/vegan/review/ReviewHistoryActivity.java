package com.vegan.review;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.vegan.CartActivity;
import com.vegan.CategoryActivity;
import com.vegan.MainActivity;
import com.vegan.MyPageActivity;
import com.vegan.R;
import com.vegan.User;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class ReviewHistoryActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    FirebaseDatabase database;
    private RecyclerView recyclerView;
    private ArrayList<Review> reviewhistoryList;
    private ReviewHistoryAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private DatabaseReference databaseReference2;

    private String username;

    private String idToken; //idToken으로 변경중

    private BottomNavigationView bottomNavigationView;


    private ImageButton navMain, navCategory, navDonation, navMypage;
    Toolbar toolbar;
    DecimalFormat decimalFormat = new DecimalFormat("###,###");



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_history);

        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.reviewHisyoryRecycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        layoutManager = new LinearLayoutManager(this);
        reviewhistoryList = new ArrayList<>();

        database = FirebaseDatabase.getInstance(); //파이어베이스 연동
        //firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = database.getReference("Review"); // Firebase Realtime Database에서 "Review" 항목을 가져옵니다.

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        databaseReference2 = FirebaseDatabase.getInstance().getReference("User");

        databaseReference2.child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    idToken = user.getIdToken();

                    Query reviewhistoryQuery = databaseReference.orderByChild("idToken").equalTo(idToken);
                    reviewhistoryQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            reviewhistoryList.clear();
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Review review = snapshot.getValue(Review.class);
                                reviewhistoryList.add(review);
                                Log.d("useridtoken", review.getIdToken() + "가져왔음");
                            }
                            adapter.notifyDataSetChanged();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e("ReviewHistoryActivity", String.valueOf(databaseError.toException()));
                        }
                    });
                    adapter = new ReviewHistoryAdapter(reviewhistoryList, ReviewHistoryActivity.this);
                    recyclerView.setAdapter(adapter);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        // 하단바 구현
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavigation_reviewhistory);

        bottomNavigationView.setSelectedItemId(R.id.tab_mypage);
                bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.tab_home) {
                    startActivity(new Intent(ReviewHistoryActivity.this, MainActivity.class));
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.tab_category) {
                    startActivity(new Intent(ReviewHistoryActivity.this, CategoryActivity.class));
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.tab_cart) {
                    startActivity(new Intent(ReviewHistoryActivity.this, CartActivity.class));
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.tab_mypage) {
                    startActivity(new Intent(ReviewHistoryActivity.this, MyPageActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });

    }


}