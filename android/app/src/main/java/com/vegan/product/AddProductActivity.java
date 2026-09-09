package com.vegan.product;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.vegan.manage.ManageMainActivity;
import com.vegan.R;
import com.vegan.api.ApiProduct;
import com.vegan.api.ImageUploadUtil;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddProductActivity extends AppCompatActivity {

    private static final int GALLERY_MAIN   = 1;
    private static final int GALLERY_DETAIL = 2;

    private EditText AddPBrand, AddPname, AddPPrice, AddStock, AddSalesCount;
    private ImageButton AddPimg, AddPDetailimg;
    private Spinner spinner;
    private Button btnMGProductAdd;
    private Dialog dialog;

    private Uri imageUri1 = null;   // 메인 이미지
    private Uri imageUri2 = null;   // 상세 이미지
    private String uploadedUrl1 = "";
    private String uploadedUrl2 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        ((ImageView) findViewById(R.id.back)).setOnClickListener(v -> onBackPressed());

        AddPimg       = (ImageButton) findViewById(R.id.AddPImg);
        AddPDetailimg = (ImageButton) findViewById(R.id.AddPDetailimg);
        AddPBrand     = (EditText) findViewById(R.id.AddPBrand);
        AddPname      = (EditText) findViewById(R.id.AddPname);
        AddPPrice     = (EditText) findViewById(R.id.AddPPrice);
        AddStock      = (EditText) findViewById(R.id.AddStock);
        AddSalesCount = (EditText) findViewById(R.id.AddSalesCount);
        btnMGProductAdd = (Button) findViewById(R.id.btnMGProductAdd);
        spinner       = (Spinner) findViewById(R.id.category_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.category_select_item, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm2);

        // 갤러리에서 메인 이미지 선택
        AddPimg.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(i, GALLERY_MAIN);
        });

        // 갤러리에서 상세 이미지 선택
        AddPDetailimg.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(i, GALLERY_DETAIL);
        });

        btnMGProductAdd.setOnClickListener(v -> {
            dialog.show();
            ((TextView) dialog.findViewById(R.id.confirmTextView)).setText("상품을 추가하시겠습니까?");
            Button btnleft  = dialog.findViewById(R.id.btn_left);
            btnleft.setText("취소");
            btnleft.setOnClickListener(x -> dialog.dismiss());
            Button btnright = dialog.findViewById(R.id.btn_right);
            btnright.setText("확인");
            btnright.setOnClickListener(x -> { dialog.dismiss(); uploadAndAdd(); });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        if (requestCode == GALLERY_MAIN) {
            imageUri1 = data.getData();
            AddPimg.setImageURI(imageUri1);
        } else if (requestCode == GALLERY_DETAIL) {
            imageUri2 = data.getData();
            AddPDetailimg.setImageURI(imageUri2);
        }
    }

    /**
     * 이미지 업로드 순서:
     * 1) 메인 이미지 업로드
     * 2) 상세 이미지 업로드
     * 3) 두 URL이 모두 준비되면 상품 추가 API 호출
     */
    private void uploadAndAdd() {
        String token = TokenManager.getInstance().getToken();

        if (imageUri1 != null) {
            ImageUploadUtil.upload(this, imageUri1, token,
                    url -> {
                        uploadedUrl1 = url;
                        uploadSecondImage(token);
                    },
                    () -> {
                        uploadedUrl1 = "";
                        uploadSecondImage(token);
                    });
        } else {
            uploadSecondImage(token);
        }
    }

    private void uploadSecondImage(String token) {
        if (imageUri2 != null) {
            ImageUploadUtil.upload(this, imageUri2, token,
                    url -> { uploadedUrl2 = url; addProduct(); },
                    () -> { uploadedUrl2 = ""; addProduct(); });
        } else {
            addProduct();
        }
    }

    private void addProduct() {
        try {
            String selectedCategory = spinner.getSelectedItem().toString();
            int categoryId;
            if (selectedCategory.equals("101-푸드"))      categoryId = 101;
            else if (selectedCategory.equals("102-패션")) categoryId = 102;
            else if (selectedCategory.equals("103-뷰티")) categoryId = 103;
            else                                           categoryId = 101;

            Map<String, Object> body = new HashMap<>();
            body.put("name",          AddPname.getText().toString().trim());
            body.put("brandName",     AddPBrand.getText().toString().trim());
            body.put("price",         Integer.parseInt(AddPPrice.getText().toString().trim()));
            body.put("imageUrl",      uploadedUrl1);
            body.put("detailImageUrl", uploadedUrl2);
            body.put("stock",         Integer.parseInt(AddStock.getText().toString().trim()));
            body.put("salesCount",    Integer.parseInt(AddSalesCount.getText().toString().trim()));
            body.put("category",      categoryId);
            body.put("description",   "");

            String token = TokenManager.getInstance().getToken();
            RetrofitClient.getAdminApi().addProduct(token, body).enqueue(new Callback<ApiProduct>() {
                @Override
                public void onResponse(Call<ApiProduct> call, Response<ApiProduct> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddProductActivity.this, "상품이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(AddProductActivity.this, ManageMainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(AddProductActivity.this, "추가 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<ApiProduct> call, Throwable t) {
                    Log.e("AddProductActivity", "상품 추가 실패", t);
                    Toast.makeText(AddProductActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "입력한 값이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
