package com.vegan.order;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vegan.R;

import java.text.DecimalFormat;
import java.util.List;

public class OrderCompleteAdapter extends RecyclerView.Adapter<OrderCompleteAdapter.OrderCompleteViewHolder> {

    private Context context;
    private List<MyOrder> myOrderList;
    private DecimalFormat decimalFormat = new DecimalFormat("###,###");

    public OrderCompleteAdapter(Context context, List<MyOrder> myOrderList) {
        this.context = context;
        this.myOrderList = myOrderList;
    }

    @NonNull
    @Override
    public OrderCompleteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OrderCompleteViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.order_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull OrderCompleteViewHolder holder, int position) {
        MyOrder order = myOrderList.get(position);
        Glide.with(holder.itemView).load(order.getOrderImg()).into(holder.pimg_orderitem);
        holder.pName_orderitem.setText(order.getProductName());
        holder.pPrice_orderitem.setText(decimalFormat.format(order.getTotalPrice()) + "원");
        holder.pQauntity_orderitem.setText(decimalFormat.format(order.getTotalQuantity()) + "개");
    }

    @Override
    public int getItemCount() {
        return myOrderList != null ? myOrderList.size() : 0;
    }

    public static class OrderCompleteViewHolder extends RecyclerView.ViewHolder {
        ImageView pimg_orderitem;
        TextView pName_orderitem, pPrice_orderitem, pQauntity_orderitem;

        public OrderCompleteViewHolder(@NonNull View itemView) {
            super(itemView);
            pimg_orderitem = itemView.findViewById(R.id.pimg_orderitem);
            pName_orderitem = itemView.findViewById(R.id.pName_orderitem);
            pPrice_orderitem = itemView.findViewById(R.id.pPrice_orderitem);
            pQauntity_orderitem = itemView.findViewById(R.id.pQauntity_orderitem);
        }
    }
}
