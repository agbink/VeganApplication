package com.vegan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.vegan.search.SearchActivity;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import me.relex.circleindicator.CircleIndicator3;

public class MainActivity extends FragmentActivity {


    private ViewPager2 mPager;
    private ViewPager2 mPager01;
    private int currentPage = 0;
    private int currentPage01 = 0;
    private final long DELAY_MS = 3000;
    private final long PERIOD_MS = 3000;
    private FragmentStateAdapter pagerAdapter;
    private final int num_page = 3;
    private final int num_page01 = 3;
    private CircleIndicator3 mIndicator;
    private CircleIndicator3 mIndicator01;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<ItemsDomain> arrayList;
    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    int pid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mPager = findViewById(R.id.viewpager);
        mPager01 = findViewById(R.id.viewpager01);

        FragmentStateAdapter pagerAdapter = new MainAdapter(this, num_page);
        mPager.setAdapter(pagerAdapter);

        FragmentStateAdapter pagerAdapter01 = new MainAdapter2(this, num_page01);
        mPager01.setAdapter(pagerAdapter01);

        mIndicator = findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
        mIndicator.createIndicators(num_page, 0);

        mIndicator01 = findViewById(R.id.indicator01);
        mIndicator01.setViewPager(mPager01);
        mIndicator01.createIndicators(num_page01, 0);

        mPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        mPager.setCurrentItem(1000);
        mPager.setOffscreenPageLimit(2);

        mPager01.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        mPager01.setCurrentItem(1000);
        mPager01.setOffscreenPageLimit(2);
        final Handler handler = new Handler(Looper.getMainLooper());

        //큰광고 타이머
        final Runnable update = new Runnable() {
            public void run() {
                if (currentPage == pagerAdapter.getItemCount()) {
                    currentPage = 0;
                }
                mPager.setCurrentItem(currentPage++);
            }
        };
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(update);
            }
        }, DELAY_MS, PERIOD_MS);

        recyclerView = findViewById(R.id.newarrival);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        arrayList = new ArrayList<>();
        databaseReference = FirebaseDatabase.getInstance().getReference("Product");

        Query query = databaseReference.orderByChild("timestamp").limitToLast(6);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                arrayList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ItemsDomain item = snapshot.getValue(ItemsDomain.class);
                    arrayList.add(0, item);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MainActivity", String.valueOf(databaseError.toException()));
            }
        });


        adapter = new NewArrivalAdapter(arrayList, this);
        recyclerView.setAdapter(adapter);



        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavigation_main);

        bottomNavigationView.setSelectedItemId(R.id.tab_home);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.tab_home) {
                    startActivity(new Intent(MainActivity.this, MainActivity.class));
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.tab_category) {
                    startActivity(new Intent(MainActivity.this, CategoryActivity.class));
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.tab_cart) {
                    startActivity(new Intent(MainActivity.this, CartActivity.class));
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.tab_mypage) {
                    startActivity(new Intent(MainActivity.this, MyPageActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });

        TextView morebtn = findViewById(R.id.more_btn);
        morebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CategoryActivity.class);
                startActivity(intent);
            }
        });

        ImageView imageView6 = findViewById(R.id.imageView6);
        imageView6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch SearchActivity
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });
    }
}
