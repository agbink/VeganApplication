package com.vegan.order;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vegan.R;

import java.util.ArrayList;

public class OrderHistoryParentRcyAdapter extends RecyclerView.Adapter<OrderHistoryParentRcyAdapter.MyViewHolder> {
    private ArrayList<MyOrder> parentModelArrayList;
    public Context cxt;

    public OrderHistoryParentRcyAdapter(ArrayList<MyOrder> parentModelArrayList ,Context context) {
        this.parentModelArrayList = parentModelArrayList;
        this.cxt = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_order_history_parent, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public int getItemCount() {
        if(parentModelArrayList != null){
            return parentModelArrayList.size();
        }
        return 0;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.orderDate.setText(parentModelArrayList.get(position).getOrderDate());
        holder.ordh_OrderId.setText(parentModelArrayList.get(position).getOrderId());

        ArrayList<MyOrder> childModelArrayList = parentModelArrayList.get(position).getChildModelArrayList();

        OrderHistoryChildRcyAdapter childRecyclerViewAdapter = new OrderHistoryChildRcyAdapter(childModelArrayList, holder.childRecyclerView.getContext());
        holder.childRecyclerView.setAdapter(childRecyclerViewAdapter);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(cxt, LinearLayoutManager.VERTICAL, false);
        holder.childRecyclerView.setLayoutManager(layoutManager);
        holder.childRecyclerView.setHasFixedSize(true);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(holder.childRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        holder.childRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView orderDate;
        public RecyclerView childRecyclerView;
        public TextView ordh_OrderId;

        public MyViewHolder(View itemView) {
            super(itemView);

            ordh_OrderId = itemView.findViewById(R.id.ordh_OrderId);
            orderDate = itemView.findViewById(R.id.orderDate);
            childRecyclerView = itemView.findViewById(R.id.Child_RV);


        }
    }
    private void onOhBtnClicked() {
        Intent intent = new Intent(cxt, OrderCompleteActivity.class);
        cxt.startActivity(intent);
    }
}
