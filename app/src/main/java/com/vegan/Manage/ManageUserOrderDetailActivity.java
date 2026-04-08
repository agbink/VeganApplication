package com.vegan.Manage;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vegan.MyOrder;
import com.vegan.R;
import com.vegan.User;

public class ManageUserOrderDetailActivity extends AppCompatActivity {

    TextView MGOrderName_detail, MGUserIDToken_detail, MGOrderID_detail, MGEachOrderID_detail;
    TextView MGOrderDate_detail, MGOrderProductID_detail, MGOrderProductName_detail, MGProductPrice_detail;
    TextView MGOrderStock_detail, MGOrderTotalPrice_detail, MGOverTotalPrice_detail, MGOrderPhone_detail;
    TextView MGOrderPostcode_detail, MGOrderAddress_detail, MGOrderState_detail, MGOrderDoReview_detail;

    private MyOrder myOrder = null;

    Button MGRemoveOrder, MGOrderStateModify;

    Dialog dialog;

    Dialog dialog2;

    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private DatabaseReference databaseReferenceAdmin;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_user_order_detail);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 뒤로가기 버튼, 디폴트로 true만 해도 백버튼이 생김


        MGOrderName_detail = (TextView) findViewById(R.id.MGOrderName_detail);
        MGUserIDToken_detail = (TextView) findViewById(R.id.MGUserIDToken_detail);
        MGOrderID_detail = (TextView) findViewById(R.id.MGOrderID_detail);
        MGEachOrderID_detail = (TextView) findViewById(R.id.MGEachOrderID_detail);
        MGOrderDate_detail = (TextView) findViewById(R.id.MGOrderDate_detail);
        MGOrderProductID_detail = (TextView) findViewById(R.id.MGOrderProductID_detail);
        MGOrderProductName_detail = (TextView) findViewById(R.id.MGOrderProductName_detail);
        MGProductPrice_detail = (TextView) findViewById(R.id.MGProductPrice_detail);
        MGOverTotalPrice_detail = (TextView) findViewById(R.id.MGOverTotalPrice_detail);
        MGOrderStock_detail = (TextView) findViewById(R.id.MGOrderStock_detail);
        MGOrderTotalPrice_detail = (TextView) findViewById(R.id.MGOrderTotalPrice_detail);
        MGOrderPhone_detail = (TextView) findViewById(R.id.MGOrderPhone_detail);
        MGOrderAddress_detail = (TextView) findViewById(R.id.MGOrderAddress_detail);
        MGOrderState_detail = (TextView) findViewById(R.id.MGOrderState_detail);
        MGOrderDoReview_detail = (TextView) findViewById(R.id.MGOrderDoReview_detail);
        MGRemoveOrder = (Button) findViewById(R.id.MGRemoveOrder);
        MGOrderStateModify = (Button) findViewById(R.id.MGOrderStateModify);

        database = FirebaseDatabase.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("CurrentUser");

        databaseReferenceAdmin = FirebaseDatabase.getInstance().getReference("Admin");

        firebaseAuth = FirebaseAuth.getInstance();

        dialog = new Dialog(ManageUserOrderDetailActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm2);

        dialog2 = new Dialog(ManageUserOrderDetailActivity.this);
        dialog2.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog2.setContentView(R.layout.dialog_confirm2);

        final Object object = getIntent().getSerializableExtra("ManageUserOrderDetail");
        if(object instanceof MyOrder){
            myOrder = (MyOrder) object;
        }

        MGOrderName_detail.setText(myOrder.getUserName());
        MGUserIDToken_detail.setText(myOrder.getUseridtoken());
        MGOrderID_detail.setText(myOrder.getOrderId());
        MGEachOrderID_detail.setText(myOrder.getEachOrderedId());
        MGOrderDate_detail.setText(myOrder.getOrderDate());
        MGOrderProductID_detail.setText(String.valueOf(myOrder.getProductId()));
        MGOrderProductName_detail.setText(myOrder.getProductName());
        MGProductPrice_detail.setText(String.valueOf(myOrder.getProductPrice()));
        MGOrderStock_detail.setText(String.valueOf(myOrder.getTotalQuantity()));
        MGOrderTotalPrice_detail.setText(String.valueOf(myOrder.getTotalPrice()));
        MGOverTotalPrice_detail.setText(String.valueOf(myOrder.getTotalPrice()));
        MGOrderPhone_detail.setText(myOrder.getPhone());
        MGOrderAddress_detail.setText(myOrder.getAddress());
        MGOrderState_detail.setText(myOrder.getOrderstate());
        MGOrderDoReview_detail.setText(myOrder.getDoReview());

        MGRemoveOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.show();

                TextView confirmTextView = dialog.findViewById(R.id.confirmTextView);
                confirmTextView.setText("주문을 삭제하시겠습니까?\n삭제 후에는 작업을 되돌릴 수 없습니다.");

                Button btnleft1 = dialog.findViewById(R.id.btn_left);
                btnleft1.setText("취소");
                btnleft1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                Button btnright1 = dialog.findViewById(R.id.btn_right);
                btnright1.setText("확인");
                btnright1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        databaseReference.child(String.valueOf(myOrder.getUseridtoken())).child("MyOrder").child(myOrder.getOrderId()).child(myOrder.getEachOrderedId()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialog.dismiss();
                                Log.d("ManageUserOrderDetailActivity", "주문 삭제 완료");
                                Intent intent = new Intent(ManageUserOrderDetailActivity.this, ManageUserOrderActivity.class);
                                startActivity(intent);

                            }
                        });

                        databaseReferenceAdmin.child("UserOrder").child(myOrder.getEachOrderedId()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialog.dismiss();
                                Log.d("ManageUserOrderDetailActivity", "주문 삭제 완료");
                                Intent intent = new Intent(ManageUserOrderDetailActivity.this, ManageUserOrderActivity.class);
                                startActivity(intent);

                            }
                        });
                    }
                });

            }
        });

        MGOrderStateModify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog2.show();

                TextView confirmTextView = dialog2.findViewById(R.id.confirmTextView);
                confirmTextView.setText("배송 완료로 처리하시겠습니까?\n처리 후에는 작업을 되돌릴 수 없습니다.");

                Button btnleft1 = dialog2.findViewById(R.id.btn_left);
                btnleft1.setText("취소");
                btnleft1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog2.dismiss();
                    }
                });

                Button btnright1 = dialog2.findViewById(R.id.btn_right);
                btnright1.setText("확인");
                btnright1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        databaseReference.child(String.valueOf(myOrder.getUseridtoken())).child("MyOrder").child(myOrder.getOrderId()).child(myOrder.getEachOrderedId()).child("orderstate").setValue("배송완료").addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialog2.dismiss();
                                Log.d("ManageUserOrderDetailActivity", "배송 완료 처리");
                                finish();

                            }
                        });

                        databaseReferenceAdmin.child("UserOrder").child(myOrder.getEachOrderedId()).child("orderstate").setValue("배송완료").addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialog2.dismiss();
                                Log.d("ManageUserOrderDetailActivity", "배송 완료 처리");
                                finish();

                            }
                        });
                    }
                });


            }
        });



    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) { //뒤로가기
            onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}