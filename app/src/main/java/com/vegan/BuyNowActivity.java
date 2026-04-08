package com.vegan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class BuyNowActivity extends AppCompatActivity {


    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference, databaseReference2, databaseReferenceProduct, databaseReferenceAdmin;

    private TextView buynow_pname, buynow_pprice, buynow_totalquantity, overTotalAmount;
    private ImageView buynow_pimg;
    private TextView orderName, orderPhone, orderAddress;
    private Button btnPayment;
    private String productName, productPrice, productImg;
    private int totalPrice, pId, productStock, selectedQuantity;
    int total = 0;
    private String strOrderName, strOrderPhone, strOrderAddress;

    private long mNow;
    private Date mDate;
    private final SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_now);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();


        buynow_pimg = findViewById(R.id.buynow_pimg);
        buynow_pname = findViewById(R.id.buynow_pname);
        buynow_pprice = findViewById(R.id.buynow_pprice);
        buynow_totalquantity = findViewById(R.id.buynow_totalquantity);
        overTotalAmount = findViewById(R.id.buynow_overtotalPrice);
        orderName = findViewById(R.id.buynow_name);
        orderPhone = findViewById(R.id.buynow_phone);
        orderAddress = findViewById(R.id.buynow_address);
        btnPayment = findViewById(R.id.buynow_btnPayment);
        databaseReference2 = FirebaseDatabase.getInstance().getReference("User");
        databaseReference = FirebaseDatabase.getInstance().getReference("CurrentUser");
        databaseReferenceProduct = firebaseDatabase.getReference("Product");
        databaseReferenceAdmin = firebaseDatabase.getReference("Admin");

        // MyOrder에 있는 결제 정보 데이터베이스 가져와서 화면에 뿌리기
        databaseReference2.child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // 파이어베이스 데이터베이스의 데이터를 받아오는 곳

                User user = dataSnapshot.getValue(User.class); //  만들어 뒀던 Product 객체에 데이터를 담는다.
                orderName.setText(user.getUsername());
                orderPhone.setText(user.getPhone());
                orderAddress.setText(user.getAddress());
                strOrderName = user.getUsername();
                strOrderPhone = user.getPhone();
                strOrderAddress = user.getAddress();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // 디비를 가져오던 중 에러 발생 시
                Log.e("OrderActivity", String.valueOf(databaseError.toException())); // 에러문 출력
            }
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            productName = bundle.getString("productName");
            productPrice = bundle.getString("productPrice");
            selectedQuantity = bundle.getInt("selectedQuantity");
            productImg = bundle.getString("productImg");
            totalPrice = bundle.getInt("totalPrice");
            pId = bundle.getInt("pId");
            productStock = bundle.getInt("productStock");

            Log.d("BuyNow", productName + productPrice + selectedQuantity + productImg + totalPrice + pId + productStock);
        }

        Glide.with(getApplicationContext()).load(productImg).into(buynow_pimg);

        DecimalFormat decimalFormat = new DecimalFormat("###,###");

        buynow_pname.setText(productName);
        buynow_pprice.setText(String.valueOf(decimalFormat.format(Integer.parseInt(productPrice))) + "원");
        buynow_totalquantity.setText(String.valueOf(decimalFormat.format(selectedQuantity)) + "개");


        overTotalAmount.setText(String.valueOf(decimalFormat.format(totalPrice)) + "원");


        final String orderId = databaseReference.push().getKey();


        btnPayment = findViewById(R.id.buynow_btnPayment);

        btnPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String myOrderId = databaseReference.child("MyOrder").push().getKey();
                final HashMap<String, Object> cartMap = new HashMap<>();
                cartMap.put("productName", productName);
                cartMap.put("productPrice", productPrice);
                cartMap.put("totalQuantity", selectedQuantity);
                cartMap.put("totalPrice", totalPrice);
                cartMap.put("productId", pId);
                cartMap.put("overTotalPrice", totalPrice);
                cartMap.put("userName", strOrderName);
                cartMap.put("phone", strOrderPhone);
                cartMap.put("address", strOrderAddress);
                cartMap.put("orderId", myOrderId);
                cartMap.put("orderDate", getTime());
                cartMap.put("orderImg", productImg);
                cartMap.put("eachOrderedId", orderId);
                cartMap.put("doReview", "No");
                cartMap.put("orderstate", "paid");
                cartMap.put("useridtoken", firebaseUser.getUid());
                Log.d("OrderActivity1", total + "");

                int totalStock = productStock - Integer.valueOf(selectedQuantity);

                // Update user order
                databaseReference.child(firebaseUser.getUid()).child("MyOrder").child(myOrderId).child(orderId).setValue(cartMap)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Log.d("BuyNowActivity", "User order update completed");
                            }
                        });

                // Update product stock
                databaseReferenceProduct.child(String.valueOf(pId)).child("stock").setValue(totalStock).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d("BuyNowActivity", "Product stock updated");
                    }
                });

                // Update salesCount using a Firebase transaction
                databaseReferenceProduct.child(String.valueOf(pId)).child("salescount")
                        .runTransaction(new Transaction.Handler() {
                            @NonNull
                            @Override
                            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                                Integer currentSalesCount = mutableData.getValue(Integer.class);
                                if (currentSalesCount == null) {
                                    currentSalesCount = 0;
                                }
                                mutableData.setValue(currentSalesCount + selectedQuantity);
                                return Transaction.success(mutableData);
                            }

                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                                if (committed) {
                                    Log.d("BuyNowActivity", "Sales count updated successfully");
                                } else {
                                    Log.e("BuyNowActivity", "Failed to update sales count", databaseError.toException());
                                }
                            }
                        });

                // Navigate to OrderCompleteActivity
                Intent intent = new Intent(BuyNowActivity.this, OrderCompleteActivity.class);
                intent.putExtra("orderId", orderId);
                intent.putExtra("myOrderId", myOrderId);

                startActivity(intent);
                finish();
            }
        });
    }
    private String getTime() {
        mNow = System.currentTimeMillis();
        mDate = new Date(mNow);
        return mFormat.format(mDate);
    }
}

