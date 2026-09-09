package com.vegan.product;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vegan.manage.ManageProductModifyActivity;
import com.vegan.R;
import com.vegan.api.ApiProduct;

import java.util.ArrayList;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private ArrayList<ApiProduct> arrayList;
    private Context context;

    public ProductAdapter(ArrayList<ApiProduct> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_item, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ApiProduct item = arrayList.get(position);
        Glide.with(holder.itemView).load(item.getImageUrl()).into(holder.imageView);
        holder.textName.setText("상품명 : " + item.getName());
        holder.textPrice.setText("가격 : " + item.getPrice());
        holder.textStock.setText("재고수량 : " + item.getStock());
        holder.textSalesCount.setText("총 판매량 : " + item.getSalesCount());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ManageProductModifyActivity.class);
            intent.putExtra("ManageProductModify", item);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return arrayList != null ? arrayList.size() : 0; }

    public class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textName, textPrice, textStock, textSalesCount;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView     = itemView.findViewById(R.id.imageView);
            textName      = itemView.findViewById(R.id.textName);
            textPrice     = itemView.findViewById(R.id.textPrice);
            textStock     = itemView.findViewById(R.id.textStock);
            textSalesCount = itemView.findViewById(R.id.textSalescount);
        }
    }
}
