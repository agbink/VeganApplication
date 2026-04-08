package com.vegan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.vegan.review.Review;
import com.vegan.review.ReviewActivity;
import com.vegan.review.ReviewAdapter;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class ProductDetailActivity extends AppCompatActivity {

    private ItemsDomain item = null;
    private TextView quantity, price, name, bname, reviewCount;
    private int totalQuantity = 1;
    private int totalPrice = 0;
    private Dialog dialog, dialog2;
    private int pid, getstock;
    private ImageView detailedImg, detailedLongImg, addItem, removeItem;
    private Button addToCart, buyNow, moreReviewsButton;
    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private ReviewAdapter adapter;
    private ArrayList<Review> arrayList;
    private RatingBar ratingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        initializeViews();
        setupBackButton();
        setupCartButton();
        setupDialogs();
        setupFirebase();
        loadProductDetails();
        setupReviewSection();
        setupQuantityButtons();
        setupAddToCartButton();
        setupBuyNowButton();
    }

    private void initializeViews() {
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
    }

    private void setupBackButton() {
        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(v -> onBackPressed());
    }

    private void setupCartButton() {
        ImageView cart = findViewById(R.id.cart);
        cart.setOnClickListener(v -> startActivity(new Intent(ProductDetailActivity.this, CartActivity.class)));
    }

    private void setupDialogs() {
        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm2);

        dialog2 = new Dialog(this);
        dialog2.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog2.setContentView(R.layout.dialog_confirm);
    }

    private void setupFirebase() {
        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("CurrentUser");
        auth = FirebaseAuth.getInstance();
    }

    private void loadProductDetails() {
        final Object object = getIntent().getSerializableExtra("detail");
        if (object instanceof ItemsDomain) {
            item = (ItemsDomain) object;
            updateProductUI();
        }
    }

    private void updateProductUI() {
        if (item != null) {
            DecimalFormat decimalFormat = new DecimalFormat("###,###");
            Glide.with(getApplicationContext()).load(item.getPimg()).into(detailedImg);
            price.setText(decimalFormat.format(item.getPprice()) + "원");
            name.setText(item.getPname());
            bname.setText(item.getPbname());
            Glide.with(getApplicationContext()).load(item.getPdetailimg()).into(detailedLongImg);
            totalPrice = item.getPprice() * totalQuantity;
            pid = item.getPid();
            getstock = item.getStock();
        }
    }

    private void setupReviewSection() {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        arrayList = new ArrayList<>();
        adapter = new ReviewAdapter(arrayList, FirebaseAuth.getInstance(), database.getReference("User"));
        recyclerView.setAdapter(adapter);

        loadReviews();
        setupReviewCountButton();
        setupMoreReviewsButton();
    }

    private void loadReviews() {
        Query reviewQuery = database.getReference("Review").orderByChild("pid").equalTo(pid);
        reviewQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                arrayList.clear();
                int count = 0;
                float totalRating = 0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (count >= 3) break;

                    Review review = snapshot.getValue(Review.class);
                    if (review != null) {
                        totalRating += review.getRscore();
                        arrayList.add(review);
                        count++;
                    }
                }

                adapter.notifyDataSetChanged();
                updateReviewUI(count, totalRating);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ProductDetailActivity", "Error loading reviews: " + databaseError.getMessage());
            }
        });
    }

    private void updateReviewUI(int count, float totalRating) {
        if (count > 0) {
            float averageRating = totalRating / count;
            ratingBar.setRating(averageRating);
            reviewCount.setText("리뷰 " + count + "개 보기");
        } else {
            ratingBar.setRating(0);
            reviewCount.setText("리뷰 0개 보기");
        }
    }

    private void setupReviewCountButton() {
        reviewCount.setOnClickListener(v -> {
            Intent intent = new Intent(ProductDetailActivity.this, ReviewActivity.class);
            intent.putExtra("pid", pid);
            startActivity(intent);
        });
    }

    private void setupMoreReviewsButton() {
        moreReviewsButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProductDetailActivity.this, ReviewActivity.class);
            intent.putExtra("pid", pid);
            startActivity(intent);
        });
    }

    private void setupQuantityButtons() {
        addItem.setOnClickListener(v -> {
            if (totalQuantity < 10) {
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
    }

    private void setupAddToCartButton() {
        addToCart.setOnClickListener(v -> {
            if (getstock > 0) {
                if (totalQuantity <= getstock) {
                    addToCart();
                } else {
                    showImpossibleDialog();
                }
            } else {
                showStockDialog();
            }
        });
    }

    private void addToCart() {
        final HashMap<String, Object> cartMap = new HashMap<>();
        FirebaseUser firebaseUser = auth.getCurrentUser();
        String cartID = databaseReference.push().getKey();

        cartMap.put("productName", item.getPname());
        cartMap.put("productBrand", item.getPbname());
        cartMap.put("productPrice", String.valueOf(item.getPprice()));
        cartMap.put("selectedQuantity", totalQuantity);
        cartMap.put("totalPrice", totalPrice * totalQuantity);
        cartMap.put("pId", item.getPid());
        cartMap.put("productImg", item.getPimg());
        cartMap.put("productStock", item.getStock());

        databaseReference.child(firebaseUser.getUid()).child("AddToCart").child(cartID).setValue(cartMap)
                .addOnCompleteListener(task -> showDialog());
    }

    private void setupBuyNowButton() {
        buyNow.setOnClickListener(v -> {
            if (getstock > 0 && totalQuantity <= getstock) {
                Intent intent = new Intent(ProductDetailActivity.this, BuyNowActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("productName", item.getPname());
                bundle.putString("productPrice", String.valueOf(item.getPprice()));
                bundle.putInt("selectedQuantity", totalQuantity);
                bundle.putInt("totalPrice", totalPrice * totalQuantity);
                bundle.putInt("pId", item.getPid());
                bundle.putString("productImg", item.getPimg());
                bundle.putInt("productStock", item.getStock());
                intent.putExtras(bundle);
                startActivity(intent);
            } else {
                showImpossibleDialog();
            }
        });
    }

    public void showDialog() {
        dialog.show();
        TextView confirmTextView = dialog.findViewById(R.id.confirmTextView);
        confirmTextView.setText("상품을 장바구니에 담았습니다.\n장바구니로 이동하시겠습니까?");

        Button btnleft = dialog.findViewById(R.id.btn_left);
        btnleft.setText("쇼핑 계속하기");
        btnleft.setOnClickListener(v -> dialog.dismiss());

        Button btnright = dialog.findViewById(R.id.btn_right);
        btnright.setText("장바구니 이동");
        btnright.setOnClickListener(v -> {
            Intent intent = new Intent(ProductDetailActivity.this, CartActivity.class);
            startActivity(intent);
        });
    }

    public void showStockDialog() {
        dialog2.show();
        TextView confirmTextView = dialog2.findViewById(R.id.confirmTextView);
        confirmTextView.setText("해당 상품은 현재 일시 품절입니다.");

        Button btnOk = dialog2.findViewById(R.id.btn_ok);
        btnOk.setText("확인");
        btnOk.setOnClickListener(v -> dialog2.dismiss());
    }

    public void showImpossibleDialog() {
        dialog2.show();
        TextView confirmTextView = dialog2.findViewById(R.id.confirmTextView);
        confirmTextView.setText("현재 구매 가능 개수는\n" + getstock + "개입니다.");

        Button btnOk = dialog2.findViewById(R.id.btn_ok);
        btnOk.setText("확인");
        btnOk.setOnClickListener(v -> dialog2.dismiss());
    }
}