package com.vegan.review;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vegan.R;
import com.vegan.api.ApiReview;
import com.vegan.api.ImageUploadUtil;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;
import com.vegan.cart.CartActivity;
import com.vegan.category.CategoryActivity;
import com.vegan.main.MainActivity;
import com.vegan.main.MyPageActivity;
import com.vegan.order.MyOrder;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewWriteActivity extends AppCompatActivity {

    private static final int GALLERY_CODE = 1;
    private ImageView uploadImage, Pimg;
    private Button uploadBtn;
    private RatingBar RatingBarEt;
    private EditText reviewEt;
    private TextView Pname;
    private MyOrder item = null;
    private Uri imageUri = null;
    private Dialog dialog, dialog2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_write);

        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(v -> onBackPressed());

        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm2);

        dialog2 = new Dialog(this);
        dialog2.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog2.setContentView(R.layout.dialog_confirm);

        uploadBtn   = findViewById(R.id.writeUploadBtn);
        uploadImage = findViewById(R.id.writeUploadImage);
        reviewEt    = findViewById(R.id.writeReviewEt);
        RatingBarEt = findViewById(R.id.writeRatingBar);
        Pname       = findViewById(R.id.writePname);
        Pimg        = findViewById(R.id.writePImg);

        final Object object = getIntent().getSerializableExtra("item");
        if (object instanceof MyOrder) {
            item = (MyOrder) object;
        }
        if (item != null) {
            Pname.setText(item.getProductName());
            Glide.with(getApplicationContext()).load(item.getOrderImg()).into(Pimg);
        }

        // 갤러리에서 이미지 선택
        uploadImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, GALLERY_CODE);
        });

        uploadBtn.setOnClickListener(v -> {
            String content = reviewEt.getText().toString().trim();
            if (content.isEmpty()) { showAlertDialog(); return; }

            if (imageUri != null) {
                // 이미지가 있으면 먼저 서버에 업로드 후 리뷰 등록
                String token = TokenManager.getInstance().getToken();
                ImageUploadUtil.upload(this, imageUri, token,
                        imageUrl -> submitReview(content, imageUrl),
                        () -> {
                            Log.e("ReviewWriteActivity", "이미지 업로드 실패");
                            submitReview(content, ""); // 이미지 없이라도 등록
                        });
            } else {
                submitReview(content, "");
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.tab_mypage);
        bottomNavigationView.setOnNavigationItemSelectedListener(navItem -> {
            if (navItem.getItemId() == R.id.tab_home) {
                startActivity(new Intent(this, MainActivity.class)); finish(); return true;
            } else if (navItem.getItemId() == R.id.tab_category) {
                startActivity(new Intent(this, CategoryActivity.class)); finish(); return true;
            } else if (navItem.getItemId() == R.id.tab_cart) {
                startActivity(new Intent(this, CartActivity.class)); finish(); return true;
            } else if (navItem.getItemId() == R.id.tab_mypage) {
                startActivity(new Intent(this, MyPageActivity.class)); finish(); return true;
            }
            return false;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_CODE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            uploadImage.setImageURI(imageUri);
        }
    }

    private void submitReview(String content, String imageUrl) {
        if (item == null) return;
        String token = TokenManager.getInstance().getToken();
        float rating = RatingBarEt.getRating();

        Map<String, Object> body = new HashMap<>();
        body.put("productId",      (long) item.getProductId());
        body.put("productName",    item.getProductName());
        body.put("productImg",     item.getOrderImg());
        body.put("content",        content);
        body.put("rating",         rating);
        body.put("reviewImageUrl", imageUrl);

        RetrofitClient.getReviewApi().createReview(token, body).enqueue(new Callback<ApiReview>() {
            @Override
            public void onResponse(Call<ApiReview> call, Response<ApiReview> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showSuccessDialog();
                } else {
                    Toast.makeText(ReviewWriteActivity.this, "리뷰 등록에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ApiReview> call, Throwable t) {
                Log.e("ReviewWriteActivity", "리뷰 등록 실패", t);
                showAlertDialog();
            }
        });
    }

    private void showSuccessDialog() {
        dialog.show();
        ((TextView) dialog.findViewById(R.id.confirmTextView)).setText("후기 작성을 완료했습니다.\n감사합니다.");
        Button left = dialog.findViewById(R.id.btn_left);
        left.setText("후기 확인");
        left.setOnClickListener(v -> { startActivity(new Intent(this, ReviewHistoryActivity.class)); finish(); });
        Button right = dialog.findViewById(R.id.btn_right);
        right.setText("홈 이동");
        right.setOnClickListener(v -> { startActivity(new Intent(this, MainActivity.class)); finish(); });
    }

    private void showAlertDialog() {
        dialog2.show();
        ((TextView) dialog2.findViewById(R.id.confirmTextView)).setText("후기를 작성해주세요.");
        Button ok = dialog2.findViewById(R.id.btn_ok);
        ok.setText("확인");
        ok.setOnClickListener(v -> dialog2.dismiss());
    }
}
