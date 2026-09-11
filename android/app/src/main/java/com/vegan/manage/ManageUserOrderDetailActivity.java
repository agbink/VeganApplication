package com.vegan.manage;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vegan.R;
import com.vegan.api.ApiOrder;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;
import com.vegan.order.MyOrder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageUserOrderDetailActivity extends AppCompatActivity {

    TextView MGOrderName_detail, MGUserIDToken_detail, MGOrderID_detail, MGEachOrderID_detail;
    TextView MGOrderDate_detail, MGOrderProductID_detail, MGOrderProductName_detail, MGProductPrice_detail;
    TextView MGOrderStock_detail, MGOrderTotalPrice_detail, MGOverTotalPrice_detail, MGOrderPhone_detail;
    TextView MGOrderAddress_detail, MGOrderState_detail, MGOrderDoReview_detail;

    Button MGRemoveOrder, MGOrderStateModify;
    Dialog dialog, dialog2;
    MyOrder myOrder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_user_order_detail);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        MGOrderName_detail      = (TextView) findViewById(R.id.MGOrderName_detail);
        MGUserIDToken_detail    = (TextView) findViewById(R.id.MGUserIDToken_detail);
        MGOrderID_detail        = (TextView) findViewById(R.id.MGOrderID_detail);
        MGEachOrderID_detail    = (TextView) findViewById(R.id.MGEachOrderID_detail);
        MGOrderDate_detail      = (TextView) findViewById(R.id.MGOrderDate_detail);
        MGOrderProductID_detail = (TextView) findViewById(R.id.MGOrderProductID_detail);
        MGOrderProductName_detail = (TextView) findViewById(R.id.MGOrderProductName_detail);
        MGProductPrice_detail   = (TextView) findViewById(R.id.MGProductPrice_detail);
        MGOverTotalPrice_detail = (TextView) findViewById(R.id.MGOverTotalPrice_detail);
        MGOrderStock_detail     = (TextView) findViewById(R.id.MGOrderStock_detail);
        MGOrderTotalPrice_detail = (TextView) findViewById(R.id.MGOrderTotalPrice_detail);
        MGOrderPhone_detail     = (TextView) findViewById(R.id.MGOrderPhone_detail);
        MGOrderAddress_detail   = (TextView) findViewById(R.id.MGOrderAddress_detail);
        MGOrderState_detail     = (TextView) findViewById(R.id.MGOrderState_detail);
        MGOrderDoReview_detail  = (TextView) findViewById(R.id.MGOrderDoReview_detail);
        MGRemoveOrder           = (Button) findViewById(R.id.MGRemoveOrder);
        MGOrderStateModify      = (Button) findViewById(R.id.MGOrderStateModify);

        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm2);

        dialog2 = new Dialog(this);
        dialog2.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog2.setContentView(R.layout.dialog_confirm2);

        final Object object = getIntent().getSerializableExtra("ManageUserOrderDetail");
        if (object instanceof MyOrder) {
            myOrder = (MyOrder) object;
        }
        if (myOrder == null) { finish(); return; }

        MGOrderName_detail.setText(myOrder.getUserName());
        MGUserIDToken_detail.setText(myOrder.getUseridtoken());
        MGOrderID_detail.setText(myOrder.getOrderId());
        MGEachOrderID_detail.setText(myOrder.getEachOrderedId());
        MGOrderDate_detail.setText(myOrder.getOrderDate());
        MGOrderProductID_detail.setText(String.valueOf(myOrder.getProductId()));
        MGOrderProductName_detail.setText(myOrder.getProductName());
        MGProductPrice_detail.setText(myOrder.getProductPrice());
        MGOrderStock_detail.setText(String.valueOf(myOrder.getTotalQuantity()));
        MGOrderTotalPrice_detail.setText(String.valueOf(myOrder.getTotalPrice()));
        MGOverTotalPrice_detail.setText(String.valueOf(myOrder.getOverTotalPrice()));
        MGOrderPhone_detail.setText(myOrder.getPhone());
        MGOrderAddress_detail.setText(myOrder.getAddress());
        MGOrderState_detail.setText(myOrder.getOrderstate());
        MGOrderDoReview_detail.setText(myOrder.getDoReview());

        // 주문 삭제
        MGRemoveOrder.setOnClickListener(v -> {
            dialog.show();
            ((TextView) dialog.findViewById(R.id.confirmTextView))
                    .setText("주문을 삭제하시겠습니까?\n삭제 후에는 작업을 되돌릴 수 없습니다.");
            Button btnleft = dialog.findViewById(R.id.btn_left);
            btnleft.setText("취소");
            btnleft.setOnClickListener(x -> dialog.dismiss());
            Button btnright = dialog.findViewById(R.id.btn_right);
            btnright.setText("확인");
            btnright.setOnClickListener(x -> {
                dialog.dismiss();
                deleteOrder();
            });
        });

        // 배송완료 처리
        MGOrderStateModify.setOnClickListener(v -> {
            dialog2.show();
            ((TextView) dialog2.findViewById(R.id.confirmTextView))
                    .setText("배송 완료로 처리하시겠습니까?\n처리 후에는 작업을 되돌릴 수 없습니다.");
            Button btnleft = dialog2.findViewById(R.id.btn_left);
            btnleft.setText("취소");
            btnleft.setOnClickListener(x -> dialog2.dismiss());
            Button btnright = dialog2.findViewById(R.id.btn_right);
            btnright.setText("확인");
            btnright.setOnClickListener(x -> {
                dialog2.dismiss();
                updateOrderState();
            });
        });
    }

    private void deleteOrder() {
        String token = TokenManager.getInstance().getToken();
        long orderId = Long.parseLong(myOrder.getOrderId());
        RetrofitClient.getAdminApi().deleteOrder(token, orderId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("ManageUserOrderDetail", "주문 삭제 완료");
                startActivity(new Intent(ManageUserOrderDetailActivity.this, ManageUserOrderActivity.class));
                finish();
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("ManageUserOrderDetail", "주문 삭제 실패", t);
            }
        });
    }

    private void updateOrderState() {
        String token = TokenManager.getInstance().getToken();
        long orderId = Long.parseLong(myOrder.getOrderId());
        RetrofitClient.getAdminApi().updateOrderState(token, orderId).enqueue(new Callback<ApiOrder>() {
            @Override
            public void onResponse(Call<ApiOrder> call, Response<ApiOrder> response) {
                Log.d("ManageUserOrderDetail", "배송완료 처리");
                MGOrderState_detail.setText("배송완료");
                finish();
            }
            @Override
            public void onFailure(Call<ApiOrder> call, Throwable t) {
                Log.e("ManageUserOrderDetail", "상태 변경 실패", t);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
