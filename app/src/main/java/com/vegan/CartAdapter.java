package com.vegan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private Context context;
    private List<Cart> cartList;

    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    private DecimalFormat decimalFormat = new DecimalFormat("###,###");
    private TextView overTotalAmount;

    public CartAdapter(Context context, List<Cart> cartList, TextView overTotalAmount) {
        this.context = context;
        this.cartList = cartList;
        this.overTotalAmount = overTotalAmount;

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_item, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        Cart cart = cartList.get(position);

        Glide.with(context)
                .load(cart.getProductImg())
                .into(holder.productImg);
        holder.name.setText(cart.getProductName());
        holder.brand.setText(cart.getProductBrand());
        holder.price.setText(decimalFormat.format(Integer.parseInt(cart.getProductPrice())) + "원");
        holder.quantity.setText(String.valueOf(cart.getSelectedQuantity()));
        holder.totalPrice.setText(String.valueOf(decimalFormat.format(cart.getTotalPrice())) + "원");

        holder.deleteItem.setOnClickListener(v -> {
            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            if (firebaseUser != null) {
                databaseReference = FirebaseDatabase.getInstance().getReference("CurrentUser")
                        .child(firebaseUser.getUid())
                        .child("AddToCart")
                        .child(cart.getDataId());
                databaseReference.removeValue()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                cartList.remove(position);
                                notifyDataSetChanged();
                                calculateTotalPrice();
                            } else {
                                Toast.makeText(context, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        holder.addBtn.setOnClickListener(v -> {
            int quantity = cart.getSelectedQuantity();
            int stock = cart.getProductStock();
            if (quantity < stock) {
                quantity++;
                cart.setSelectedQuantity(quantity);
                holder.quantity.setText(String.valueOf(quantity));
                cart.setTotalPrice(quantity * Integer.parseInt(cart.getProductPrice()));
                holder.totalPrice.setText(decimalFormat.format(cart.getTotalPrice()) + "원");

                // Firebase 업데이트
                updateCartInFirebase(cart);

                calculateTotalPrice();
            } else {
                Toast.makeText(context, "최대수량입니다.", Toast.LENGTH_SHORT).show();
            }
        });

        holder.minusBtn.setOnClickListener(v -> {
            int quantity = cart.getSelectedQuantity();
            if (quantity > 1) {
                quantity--;
                cart.setSelectedQuantity(quantity);
                holder.quantity.setText(String.valueOf(quantity));
                cart.setTotalPrice(quantity * Integer.parseInt(cart.getProductPrice()));
                holder.totalPrice.setText(decimalFormat.format(cart.getTotalPrice()) + "원");

                // Firebase 업데이트
                updateCartInFirebase(cart);

                calculateTotalPrice();
            } else {
                Toast.makeText(context, "수량은 1개 이상 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void updateCartInFirebase(Cart cart) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            DatabaseReference cartRef = firebaseDatabase.getReference("CurrentUser")
                    .child(firebaseUser.getUid())
                    .child("AddToCart")
                    .child(cart.getDataId());
            cartRef.setValue(cart).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(context, "Firebase 업데이트 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    @Override
    public int getItemCount() {
        return cartList.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView productImg, deleteItem;
        TextView name, brand, price, quantity, totalPrice;
        TextView minusBtn, addBtn;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            productImg = itemView.findViewById(R.id.cart_pimg);
            deleteItem = itemView.findViewById(R.id.delete_item);
            name = itemView.findViewById(R.id.product_name);
            brand = itemView.findViewById(R.id.product_bname);
            price = itemView.findViewById(R.id.product_price);
            quantity = itemView.findViewById(R.id.total_quantity);
            totalPrice = itemView.findViewById(R.id.total_price);
            minusBtn = itemView.findViewById(R.id.minusBtn);
            addBtn = itemView.findViewById(R.id.addBtn);
        }
    }

    private void calculateTotalPrice() {
        int totalPrice = 0;
        for (Cart cart : cartList) {
            totalPrice += cart.getSelectedQuantity() * Integer.parseInt(cart.getProductPrice());
        }
        overTotalAmount.setText(decimalFormat.format(totalPrice) + "원");
    }
}
