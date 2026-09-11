package com.vegan.manage;

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
import com.vegan.order.MyOrder;
import com.vegan.R;

import java.util.ArrayList;

public class ManageUserOrderAdapter extends RecyclerView.Adapter<ManageUserOrderAdapter.ManageUserOrderViewHolder> {
    private ArrayList<MyOrder> arrayList;
    private Context context;

    public ManageUserOrderAdapter(ArrayList<MyOrder> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public ManageUserOrderAdapter.ManageUserOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.manage_order_item, parent, false);
        ManageUserOrderAdapter.ManageUserOrderViewHolder holder = new ManageUserOrderAdapter.ManageUserOrderViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ManageUserOrderAdapter.ManageUserOrderViewHolder holder, @SuppressLint("RecyclerView") int position) {
        MyOrder currentOrder = arrayList.get(position);
        MyOrder previousOrder = position > 0 ? arrayList.get(position - 1) : null;


        Glide.with(holder.itemView)
                .load(arrayList.get(position).getOrderImg())
                .into(holder.MGProductImg_order);

        holder.MGOrderID_order.setText(arrayList.get(position).getOrderId());
        holder.MGOrderDate_order.setText(arrayList.get(position).getOrderDate());
        holder.MGProductName_order.setText(arrayList.get(position).getProductName());
        holder.MGOrderPrice_order.setText(arrayList.get(position).getTotalPrice() + "원");
        holder.MGOrderStock_order.setText(arrayList.get(position).getTotalQuantity() + "개");
        holder.MGOrderState_order.setText(arrayList.get(position).getOrderstate());
        holder.MGEachOrderId_order.setText(arrayList.get(position).getEachOrderedId());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ManageUserOrderDetailActivity.class);
                intent.putExtra("ManageUserOrderDetail", arrayList.get(position));
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

    public class ManageUserOrderViewHolder extends RecyclerView.ViewHolder {
        ImageView MGProductImg_order;
        TextView MGOrderID_order, MGOrderDate_order, MGProductName_order, MGOrderPrice_order, MGOrderStock_order, MGOrderState_order, MGEachOrderId_order;

        public ManageUserOrderViewHolder(@NonNull View itemView) {
            super(itemView);

            this.MGProductImg_order = itemView.findViewById(R.id.MGProductImg_order);
            this.MGOrderID_order = itemView.findViewById(R.id.MGOrderID_order);
            this.MGOrderDate_order = itemView.findViewById(R.id.MGOrderDate_order);
            this.MGProductName_order = itemView.findViewById(R.id.MGProductName_order);
            this.MGOrderPrice_order = itemView.findViewById(R.id.MGOrderPrice_order);
            this.MGOrderStock_order = itemView.findViewById(R.id.MGOrderStock_order);
            this.MGOrderState_order = itemView.findViewById(R.id.MGOrderState_order);
            this.MGEachOrderId_order = itemView.findViewById(R.id.MGEachOrderId_order);
        ;}
    }
}
