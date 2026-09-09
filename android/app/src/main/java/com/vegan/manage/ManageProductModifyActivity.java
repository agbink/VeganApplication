package com.vegan.manage;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.vegan.R;
import com.vegan.api.ApiProduct;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;
import com.vegan.product.ProductListActivity;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageProductModifyActivity extends AppCompatActivity {

    private EditText ModifyPid, ModifyCategory, ModifyPimg, ModifyPDetailimg,
            ModifyPbname, ModifyPname, ModifyPprice, ModifyStock, ModifySalesCount;
    private Button MGRemoveProduct, MGModifiyProduct;
    private ApiProduct item = null;
    Dialog dialog, dialog2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_product_modify);

        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(v -> onBackPressed());

        ModifyPid        = (EditText) findViewById(R.id.ModifyPid);
        ModifyCategory   = (EditText) findViewById(R.id.ModifyCategory);
        ModifyPimg       = (EditText) findViewById(R.id.ModifyPimg);
        ModifyPDetailimg = (EditText) findViewById(R.id.ModifyPDetailimg);
        ModifyPbname     = (EditText) findViewById(R.id.ModifyPbname);
        ModifyPname      = (EditText) findViewById(R.id.ModifyPname);
        ModifyPprice     = (EditText) findViewById(R.id.ModifyPprice);
        ModifyStock      = (EditText) findViewById(R.id.ModifyStock);
        ModifySalesCount = (EditText) findViewById(R.id.ModifySalesCount);
        MGRemoveProduct  = (Button) findViewById(R.id.MGRemoveProduct);
        MGModifiyProduct = (Button) findViewById(R.id.MGProductModify);

        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm2);

        dialog2 = new Dialog(this);
        dialog2.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog2.setContentView(R.layout.dialog_confirm2);

        final Object object = getIntent().getSerializableExtra("ManageProductModify");
        if (object instanceof ApiProduct) {
            item = (ApiProduct) object;
        }
        if (item == null) { finish(); return; }

        ModifyPid.setText(String.valueOf(item.getId()));
        ModifyPid.setEnabled(false); // ID는 수정 불가
        ModifyCategory.setText(String.valueOf(item.getCategory()));
        ModifyPimg.setText(item.getImageUrl());
        ModifyPDetailimg.setText(item.getDetailImageUrl());
        ModifyPbname.setText(item.getBrandName());
        ModifyPname.setText(item.getName());
        ModifyPprice.setText(String.valueOf(item.getPrice()));
        ModifyStock.setText(String.valueOf(item.getStock()));
        ModifySalesCount.setText(String.valueOf(item.getSalesCount()));

        // 상품 삭제
        MGRemoveProduct.setOnClickListener(v -> {
            dialog.show();
            ((TextView) dialog.findViewById(R.id.confirmTextView))
                    .setText("상품을 삭제하시겠습니까?\n삭제 후에는 작업을 되돌릴 수 없습니다.");
            Button btnleft = dialog.findViewById(R.id.btn_left);
            btnleft.setText("취소");
            btnleft.setOnClickListener(x -> dialog.dismiss());
            Button btnright = dialog.findViewById(R.id.btn_right);
            btnright.setText("확인");
            btnright.setOnClickListener(x -> {
                dialog.dismiss();
                deleteProduct();
            });
        });

        // 상품 수정
        MGModifiyProduct.setOnClickListener(v -> {
            dialog2.show();
            ((TextView) dialog2.findViewById(R.id.confirmTextView))
                    .setText("상품 정보를 수정하시겠습니까?\n수정 후에는 작업을 되돌릴 수 없습니다.");
            Button btnleft = dialog2.findViewById(R.id.btn_left);
            btnleft.setText("취소");
            btnleft.setOnClickListener(x -> dialog2.dismiss());
            Button btnright = dialog2.findViewById(R.id.btn_right);
            btnright.setText("확인");
            btnright.setOnClickListener(x -> {
                dialog2.dismiss();
                updateProduct();
            });
        });
    }

    private void deleteProduct() {
        String token = TokenManager.getInstance().getToken();
        RetrofitClient.getAdminApi().deleteProduct(token, item.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("ManageProductModify", "상품 삭제 완료");
                startActivity(new Intent(ManageProductModifyActivity.this, ProductListActivity.class));
                finish();
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("ManageProductModify", "상품 삭제 실패", t);
                Toast.makeText(ManageProductModifyActivity.this, "삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProduct() {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("name", ModifyPname.getText().toString().trim());
            body.put("brandName", ModifyPbname.getText().toString().trim());
            body.put("price", Integer.parseInt(ModifyPprice.getText().toString().trim()));
            body.put("imageUrl", ModifyPimg.getText().toString().trim());
            body.put("detailImageUrl", ModifyPDetailimg.getText().toString().trim());
            body.put("stock", Integer.parseInt(ModifyStock.getText().toString().trim()));
            body.put("category", Integer.parseInt(ModifyCategory.getText().toString().trim()));
            body.put("salesCount", Integer.parseInt(ModifySalesCount.getText().toString().trim()));
            body.put("description", "");

            String token = TokenManager.getInstance().getToken();
            RetrofitClient.getAdminApi().updateProduct(token, item.getId(), body).enqueue(new Callback<ApiProduct>() {
                @Override
                public void onResponse(Call<ApiProduct> call, Response<ApiProduct> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(ManageProductModifyActivity.this, "상품 수정이 되었습니다.", Toast.LENGTH_SHORT).show();
                        // 수정된 상품 정보로 화면 재진입
                        Intent intent = new Intent(ManageProductModifyActivity.this, ManageProductModifyActivity.class);
                        intent.putExtra("ManageProductModify", response.body());
                        startActivity(intent);
                        finish();
                    }
                }
                @Override
                public void onFailure(Call<ApiProduct> call, Throwable t) {
                    Log.e("ManageProductModify", "상품 수정 실패", t);
                    Toast.makeText(ManageProductModifyActivity.this, "수정에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "입력한 값이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
