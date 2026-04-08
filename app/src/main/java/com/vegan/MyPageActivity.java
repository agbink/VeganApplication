package com.vegan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vegan.review.ReviewHistoryActivity;
import com.vegan.review.ReviewWriteActivity;

public class MyPageActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView Tv_my_name;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseRef;
    Dialog dialog;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);

        dialog = new Dialog(MyPageActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.customdialog);

        mAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("User");

        Tv_my_name = findViewById(R.id.my_name);

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavigation);

        bottomNavigationView.setSelectedItemId(R.id.tab_mypage);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.tab_home) {
                    startActivity(new Intent(MyPageActivity.this, MainActivity.class));
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.tab_category) {
                    startActivity(new Intent(MyPageActivity.this, CategoryActivity.class));
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.tab_cart) {
                    startActivity(new Intent(MyPageActivity.this, CartActivity.class));
                    finish();
                    return true;
                } else if (item.getItemId() == R.id.tab_mypage) {
                    startActivity(new Intent(MyPageActivity.this, MyPageActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            DatabaseReference userRef = mDatabaseRef.child(uid);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String name = dataSnapshot.child("username").getValue(String.class) + "님";
                        Tv_my_name.setText(name);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
//                    Toast.makeText(MyPageActivity.this, "회원정보를 불러오는데에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        ImageButton orderBtn = findViewById(R.id.orderhistory_move);
        orderBtn.setOnClickListener(this);

        ImageButton ReviewBtn = findViewById(R.id.rv_move);
        ReviewBtn.setOnClickListener(this);

        ImageButton modifyBtn = findViewById(R.id.mod_move);
        modifyBtn.setOnClickListener(this);

        ImageButton withdrawBtn = findViewById(R.id.wdl_move);
        withdrawBtn.setOnClickListener(this);

        ImageButton logoutBtn = findViewById(R.id.logout_move);
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showLogoutConfirmationDialog();
            }
        });

        Button ordermBtn = findViewById(R.id.order_move);
        ordermBtn.setOnClickListener(this);

        Button reviewBtn = findViewById(R.id.review_move);
        reviewBtn.setOnClickListener(this);

        Button changeBtn = findViewById(R.id.modify_move);
        changeBtn.setOnClickListener(this);

        Button wdlBtn = findViewById(R.id.wd_move);
        wdlBtn.setOnClickListener(this);

        Button logoutmBtn = findViewById(R.id.out_move);
        logoutmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showLogoutConfirmationDialog();
            }
        });

    }

    @Override
    public void onClick(View v) {
        Intent intent;

        int id = v.getId();

        if (id == R.id.orderhistory_move) {
            intent = new Intent(MyPageActivity.this, OrderHistoryActivity.class);
            startActivity(intent);
        } else if (id == R.id.order_move) {
            intent = new Intent(MyPageActivity.this, OrderHistoryActivity.class);
            startActivity(intent);
        }

        if (id == R.id.rv_move) {
            intent = new Intent(MyPageActivity.this, ReviewHistoryActivity.class);
            startActivity(intent);
        } else if (id == R.id.review_move) {
            intent = new Intent(MyPageActivity.this, ReviewHistoryActivity.class);
            startActivity(intent);
        }

        if (id == R.id.mod_move) {
            intent = new Intent(MyPageActivity.this, ChangeActivity.class);
            startActivity(intent);
        } else if (id == R.id.modify_move) {
            intent = new Intent(MyPageActivity.this, ChangeActivity.class);
            startActivity(intent);
        }

        if (id == R.id.wd_move) {
            intent = new Intent(MyPageActivity.this, WithdrawActivity.class);
            startActivity(intent);
        } else if (id == R.id.wdl_move) {
            intent = new Intent(MyPageActivity.this, WithdrawActivity.class);
            startActivity(intent);
        }


        }
    public void showLogoutConfirmationDialog() {

        dialog.show();

        TextView confirmTextView = dialog.findViewById(R.id.say);
        confirmTextView.setText("로그아웃하시겠습니까?");

        Button btnno = dialog.findViewById(R.id.btnNo);
        Button btnok = dialog.findViewById(R.id.btnOk);
        btnno.setText("아니요");
        btnok.setText("예");

        btnok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
                dialog.dismiss();
            }
        });
        btnno.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }
    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(MyPageActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    }
