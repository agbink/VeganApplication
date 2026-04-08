package com.vegan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.DecimalFormat;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    Context context;
    List<Cart> cartList;

    DecimalFormat decimalFormat = new DecimalFormat("###,###");

    public OrderAdapter(Context context, List<Cart> cartList) {
        this.context = context;
        this.cartList = cartList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OrderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.order_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Glide.with(holder.itemView)
                .load(cartList.get(position).getProductImg())
                .into(holder.pimg_orderitem);
        holder.pName_orderitem.setText(cartList.get(position).getProductName());
        holder.pPrice_orderitem.setText(String.valueOf(decimalFormat.format(cartList.get(position).getTotalPrice())) + "원");
        holder.pQauntity_orderitem.setText(decimalFormat.format(cartList.get(position).getSelectedQuantity()) + "개");
    }

    @Override
    public int getItemCount() {
        if (cartList != null) {
            return cartList.size();
        }
        return 0;
    }

    public class OrderViewHolder extends RecyclerView.ViewHolder {

        ImageView pimg_orderitem;
        TextView pName_orderitem, pPrice_orderitem, pQauntity_orderitem;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);

            pimg_orderitem = itemView.findViewById(R.id.pimg_orderitem);
            pName_orderitem = itemView.findViewById(R.id.pName_orderitem);
            pPrice_orderitem = itemView.findViewById(R.id.pPrice_orderitem);
            pQauntity_orderitem = itemView.findViewById(R.id.pQauntity_orderitem);
        }
    }
}