package com.vegan.main;

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
import com.vegan.R;
import com.vegan.api.ApiProduct;
import com.vegan.product.ProductDetailActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class NewArrivalAdapter extends RecyclerView.Adapter<NewArrivalAdapter.CustomViewHolder> {

    private ArrayList<ApiProduct> arrayList;
    private Context context;
    private DecimalFormat decimalFormat = new DecimalFormat("###,###");

    public NewArrivalAdapter(ArrayList<ApiProduct> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mainlist_item, parent, false);
        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ApiProduct product = arrayList.get(position);
        holder.pbname.setText(product.getBrandName());
        holder.pname.setText(product.getName());
        holder.tv_pprice.setText(decimalFormat.format(product.getPrice()) + "원");
        Glide.with(holder.itemView).load(product.getImageUrl()).into(holder.iv_pimg);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("detail", arrayList.get(position));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(arrayList.size(), 6);
    }

    public static class CustomViewHolder extends RecyclerView.ViewHolder {
        ImageView iv_pimg;
        TextView pbname, pname, tv_pprice;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            iv_pimg = itemView.findViewById(R.id.iv_pimg);
            pbname = itemView.findViewById(R.id.pbname);
            pname = itemView.findViewById(R.id.pname);
            tv_pprice = itemView.findViewById(R.id.tv_pprice);
        }
    }
}
