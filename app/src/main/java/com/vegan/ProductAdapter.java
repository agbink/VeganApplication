package com.vegan;

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
import com.vegan.Manage.ManageProductModifyActivity;

import java.util.ArrayList;
public class ProductAdapter  extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder>{
    private ArrayList<ItemsDomain> arrayList;
    private Context context;

    public ProductAdapter(ArrayList<ItemsDomain> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public ProductAdapter.ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_item, parent, false);
        ProductViewHolder holder = new ProductViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ProductAdapter.ProductViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Glide.with(holder.itemView)
                .load(arrayList.get(position).getPimg())
                .into(holder.imageView);
        holder.textName.setText("상품명 :" + arrayList.get(position).getPname());
        holder.textPrice.setText("가격 : " + String.valueOf(arrayList.get(position).getPprice()));
        holder.textStock.setText("재고수량 : " + String.valueOf(arrayList.get(position).getStock()));
        holder.textSalesCount.setText("총 판매량 : " + String.valueOf(arrayList.get(position).getSalescount()));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ManageProductModifyActivity.class);
                intent.putExtra("ManageProductModify", arrayList.get(position));
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (arrayList != null) {
            return arrayList.size();
        }
        return 0;
    }

    public class ProductViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        TextView textName;
        TextView textPrice;
        TextView textStock;
        TextView textSalesCount;
        ImageView detailImg;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            this.textName = itemView.findViewById(R.id.textName);
            this.textPrice = itemView.findViewById(R.id.textPrice);
            this.imageView = itemView.findViewById(R.id.imageView);
            this.textStock = itemView.findViewById(R.id.textStock);
            this.textSalesCount = itemView.findViewById(R.id.textSalescount);
            this.detailImg = imageView.findViewById(R.id.detail_longimg);
        }
    }

}