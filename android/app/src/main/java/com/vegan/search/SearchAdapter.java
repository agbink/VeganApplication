package com.vegan.search;

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
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.MyViewHolder> {

    private List<ApiProduct> searchResults;
    private DecimalFormat decimalFormat = new DecimalFormat("###,###");

    public SearchAdapter(List<ApiProduct> searchResults) {
        this.searchResults = searchResults;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ApiProduct product = searchResults.get(position);
        Glide.with(holder.itemView).load(product.getImageUrl()).into(holder.pimg);
        holder.pbname.setText(product.getBrandName());
        holder.pname.setText(product.getName());
        holder.pprice.setText(decimalFormat.format(product.getPrice()) + "원");

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProductDetailActivity.class);
            intent.putExtra("detail", searchResults.get(position));
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView pimg;
        TextView pname, pbname, pprice;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            pimg = itemView.findViewById(R.id.searchpimg);
            pbname = itemView.findViewById(R.id.searchpbname);
            pname = itemView.findViewById(R.id.searchpname);
            pprice = itemView.findViewById(R.id.searchpprice);
        }
    }
}
