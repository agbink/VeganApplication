package com.vegan.product;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vegan.R;
import com.vegan.api.ApiProduct;
import com.vegan.api.ApiReview;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;
import com.vegan.cart.BuyNowActivity;
import com.vegan.cart.CartActivity;
import com.vegan.review.ReviewActivity;
import com.vegan.review.ReviewAdapter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {

    private ApiProduct item = null;
    private TextView quantity, price, name, bname, reviewCount;
    private int totalQuantity = 1;
    private Dialog dialog, dialog2;
    private ImageView detailedImg, detailedLongImg, addItem, removeItem;
    private Button addToCart, buyNow, moreReviewsButton;
    private RecyclerView recyclerView;
    private ReviewAdapter adapter;
    private ArrayList<ApiReview> reviewList;
    private RatingBar ratingBar;
    private DecimalFormat decimalFormat = new DecimalFormat("###,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        quantity = findViewById(R.id.quantity);
        detailedImg = findViewById(R.id.detailed_img);
        addItem = findViewById(R.id.add_item);
        removeItem = findViewById(R.id.remove_item);
        price = findViewById(R.id.detail_price);
        name = findViewById(R.id.detailed_name);
        bname = findViewById(R.id.detailed_bname);
        detailedLongImg = findViewById(R.id.detail_longimg);
        addToCart = findViewById(R.id.addcart);
        buyNow = findViewById(R.id.buyNow);
        moreReviewsButton = findViewById(R.id.moreReviewsButton);
        recyclerView = findViewById(R.id.recyclerView);
        ratingBar = findViewById(R.id.ratingBar);
        reviewCount = findViewById(R.id.reviewCount);

        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(v -> onBackPressed());
        ImageView cart = findViewById(R.id.cart);
        cart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));

        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm2);
        dialog2 = new Dialog(this);
        dialog2.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog2.setContentView(R.layout.dialog_confirm);

        // 상품 정보 인텐트로 전달받기
        final Object object = getIntent().getSerializableExtra("detail");
        if (object instanceof ApiProduct) {
            item = (ApiProduct) object;
            updateProductUI();
            setupReviews();
        }

        // 수량 버튼
        addItem.setOnClickListener(v -> {
            if (item != null && totalQuantity < item.getStock()) {
                totalQuantity++;
                quantity.setText(String.valueOf(totalQuantity));
            }
        });
        removeItem.setOnClickListener(v -> {
            if (totalQuantity > 1) {
                totalQuantity--;
                quantity.setText(String.valueOf(totalQuantity));
            }
        });

        // 장바구니 담기
        addToCart.setOnClickListener(v -> {
            if (item == null) return;
            if (item.getStock() <= 0) { showStockDialog(); return; }
            if (totalQuantity > item.getStock()) { showImpossibleDialog(); return; }
            addToCartApi();
        });

        // 바로 구매
        buyNow.setOnClickListener(v -> {
            if (item == null || item.getStock() <= 0 || totalQuantity > item.getStock()) {
                showImpossibleDialog();
                return;
            }
            Intent intent = new Intent(this, BuyNowActivity.class);
            intent.putExtra("productName", item.getName());
            intent.putExtra("productPrice", String.valueOf(item.getPrice()));
            intent.putExtra("selectedQuantity", totalQuantity);
            intent.putExtra("totalPrice", item.getPrice() * totalQuantity);
            intent.putExtra("productId", item.getId());
            intent.putExtra("productImg", item.getImageUrl());
            intent.putExtra("productStock", item.getStock());
            startActivity(intent);
        });

        // 리뷰 더보기 버튼
        moreReviewsButton.setOnClickListener(v -> {
            if (item != null) {
                Intent intent = new Intent(this, ReviewActivity.class);
                intent.putExtra("pid", item.getId());
                startActivity(intent);
            }
        });
        reviewCount.setOnClickListener(v -> moreReviewsButton.performClick());
    }

    private void updateProductUI() {
        Glide.with(getApplicationContext()).load(item.getImageUrl()).into(detailedImg);
        price.setText(decimalFormat.format(item.getPrice()) + "원");
        name.setText(item.getName());
        bname.setText(item.getBrandName());
        Glide.with(getApplicationContext()).load(item.getDetailImageUrl()).into(detailedLongImg);
    }

    private void setupReviews() {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewList = new ArrayList<>();
        adapter = new ReviewAdapter(reviewList);
        recyclerView.setAdapter(adapter);

        RetrofitClient.getReviewApi().getReviews(item.getId()).enqueue(new Callback<List<ApiReview>>() {
            @Override
            public void onResponse(Call<List<ApiReview>> call, Response<List<ApiReview>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    reviewList.clear();
                    int count = 0;
                    float totalRating = 0;
                    for (ApiReview r : response.body()) {
                        if (count < 3) { reviewList.add(r); count++; }
                        totalRating += r.getRating();
                    }
                    int total = response.body().size();
                    adapter.notifyDataSetChanged();
                    if (total > 0) {
                        ratingBar.setRating(totalRating / total);
                        reviewCount.setText("리뷰 " + total + "개 보기");
                    } else {
                        ratingBar.setRating(0);
                        reviewCount.setText("리뷰 0개 보기");
                    }
                }
            }
            @Override
            public void onFailure(Call<List<ApiReview>> call, Throwable t) {
                Log.e("ProductDetailActivity", "리뷰 로드 실패", t);
            }
        });
    }

    private void addToCartApi() {
        String token = TokenManager.getInstance().getToken();
        Map<String, Object> body = new HashMap<>();
        body.put("productId", item.getId());
        body.put("productName", item.getName());
        body.put("productBrand", item.getBrandName());
        body.put("productPrice", item.getPrice());
        body.put("productImg", item.getImageUrl());
        body.put("productStock", item.getStock());
        body.put("selectedQuantity", totalQuantity);

        RetrofitClient.getCartApi().addItem(token, body).enqueue(new Callback<com.vegan.api.ApiCartItem>() {
            @Override
            public void onResponse(Call<com.vegan.api.ApiCartItem> call, Response<com.vegan.api.ApiCartItem> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showAddCartDialog();
                } else {
                    Toast.makeText(ProductDetailActivity.this, "장바구니 추가에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<com.vegan.api.ApiCartItem> call, Throwable t) {
                Log.e("ProductDetailActivity", "장바구니 추가 실패", t);
                Toast.makeText(ProductDetailActivity.this, "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddCartDialog() {
        dialog.show();
        TextView msg = dialog.findViewById(R.id.confirmTextView);
        msg.setText("상품을 장바구니에 담았습니다.\n장바구니로 이동하시겠습니까?");
        Button left = dialog.findViewById(R.id.btn_left);
        left.setText("쇼핑 계속하기");
        left.setOnClickListener(v -> dialog.dismiss());
        Button right = dialog.findViewById(R.id.btn_right);
        right.setText("장바구니 이동");
        right.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
    }

    private void showStockDialog() {
        dialog2.show();
        ((TextView) dialog2.findViewById(R.id.confirmTextView)).setText("해당 상품은 현재 일시 품절입니다.");
        Button ok = dialog2.findViewById(R.id.btn_ok);
        ok.setText("확인");
        ok.setOnClickListener(v -> dialog2.dismiss());
    }

    private void showImpossibleDialog() {
        dialog2.show();
        ((TextView) dialog2.findViewById(R.id.confirmTextView))
                .setText("현재 구매 가능 개수는\n" + (item != null ? item.getStock() : 0) + "개입니다.");
        Button ok = dialog2.findViewById(R.id.btn_ok);
        ok.setText("확인");
        ok.setOnClickListener(v -> dialog2.dismiss());
    }
}
