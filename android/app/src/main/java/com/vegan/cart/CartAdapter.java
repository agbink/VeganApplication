package com.vegan.cart;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vegan.api.ApiCartItem;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;
import com.vegan.R;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private Context context;
    private List<ApiCartItem> cartList;
    private TextView overTotalAmount;
    private DecimalFormat decimalFormat = new DecimalFormat("###,###");

    public CartAdapter(Context context, List<ApiCartItem> cartList, TextView overTotalAmount) {
        this.context = context;
        this.cartList = cartList;
        this.overTotalAmount = overTotalAmount;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_item, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        ApiCartItem item = cartList.get(position);

        Glide.with(context).load(item.getProductImg()).into(holder.productImg);
        holder.name.setText(item.getProductName());
        holder.brand.setText(item.getProductBrand());
        holder.price.setText(decimalFormat.format(item.getProductPrice()) + "원");
        holder.quantity.setText(String.valueOf(item.getSelectedQuantity()));
        holder.totalPrice.setText(decimalFormat.format(item.getProductPrice() * item.getSelectedQuantity()) + "원");

        holder.deleteItem.setOnClickListener(v -> {
            String token = TokenManager.getInstance().getToken();
            RetrofitClient.getCartApi().deleteItem(token, item.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        int pos = cartList.indexOf(item);
                        if (pos >= 0) {
                            cartList.remove(pos);
                            notifyItemRemoved(pos);
                            calculateTotal();
                        }
                    } else {
                        Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(context, "서버 연결 실패", Toast.LENGTH_SHORT).show();
                }
            });
        });

        holder.addBtn.setOnClickListener(v -> {
            int qty = item.getSelectedQuantity();
            if (qty < item.getProductStock()) {
                updateQuantity(item, holder, qty + 1);
            } else {
                Toast.makeText(context, "최대수량입니다.", Toast.LENGTH_SHORT).show();
            }
        });

        holder.minusBtn.setOnClickListener(v -> {
            int qty = item.getSelectedQuantity();
            if (qty > 1) {
                updateQuantity(item, holder, qty - 1);
            } else {
                Toast.makeText(context, "수량은 1개 이상 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateQuantity(ApiCartItem item, CartViewHolder holder, int newQty) {
        String token = TokenManager.getInstance().getToken();
        Map<String, Integer> body = new HashMap<>();
        body.put("quantity", newQty);
        RetrofitClient.getCartApi().updateQuantity(token, item.getId(), body).enqueue(new Callback<ApiCartItem>() {
            @Override
            public void onResponse(Call<ApiCartItem> call, Response<ApiCartItem> response) {
                if (response.isSuccessful()) {
                    item.setSelectedQuantity(newQty);
                    holder.quantity.setText(String.valueOf(newQty));
                    holder.totalPrice.setText(decimalFormat.format(item.getProductPrice() * newQty) + "원");
                    calculateTotal();
                }
            }
            @Override
            public void onFailure(Call<ApiCartItem> call, Throwable t) {
                Log.e("CartAdapter", "수량 변경 실패", t);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    private void calculateTotal() {
        int total = 0;
        for (ApiCartItem item : cartList) {
            total += item.getProductPrice() * item.getSelectedQuantity();
        }
        overTotalAmount.setText(decimalFormat.format(total) + "원");
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
}
