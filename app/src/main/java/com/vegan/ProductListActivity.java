package com.vegan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.vegan.Manage.ManageMainActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ProductListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<ItemsDomain> arrayList;
    private FirebaseDatabase database;
    private DatabaseReference databaseReference;

    private Button btnManageMain;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        arrayList = new ArrayList<>();

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("Product");

        Query query = databaseReference.orderByChild("timestamp");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                arrayList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ItemsDomain item = snapshot.getValue(ItemsDomain.class);
                    arrayList.add(item);
                }

                Collections.sort(arrayList, new Comparator<ItemsDomain>() {
                    @Override
                    public int compare(ItemsDomain item1, ItemsDomain item2) {
                        return Long.compare(item2.getTimestamp(), item1.getTimestamp());
                    }
                });

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ProductListActivity", String.valueOf(databaseError.toException()));
            }
        });

        adapter = new ProductAdapter(arrayList, this);
        recyclerView.setAdapter(adapter);



        btnManageMain = (Button) findViewById(R.id.btnManageMain_shop);
        btnManageMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProductListActivity.this, ManageMainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
