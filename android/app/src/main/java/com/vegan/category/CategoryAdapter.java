package com.vegan.category;

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

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private ArrayList<ApiProduct> arrayList;
    private Context context;
    private DecimalFormat decimalFormat = new DecimalFormat("###,###");

    public CategoryAdapter(ArrayList<ApiProduct> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_item, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ApiProduct product = arrayList.get(position);
        Glide.with(holder.itemView).load(product.getImageUrl()).into(holder.pimg);
        holder.pname.setText(product.getName());
        holder.pbname.setText(product.getBrandName());
        holder.pprice.setText(decimalFormat.format(product.getPrice()) + "원");

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("detail", arrayList.get(position));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return arrayList != null ? arrayList.size() : 0;
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView pimg;
        TextView pbname, pname, pprice;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            pimg = itemView.findViewById(R.id.searchpimg);
            pbname = itemView.findViewById(R.id.searchpbname);
            pname = itemView.findViewById(R.id.searchpname);
            pprice = itemView.findViewById(R.id.searchpprice);
        }
    }
}
