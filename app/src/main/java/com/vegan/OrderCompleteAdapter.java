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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.util.List;

public class OrderCompleteAdapter extends RecyclerView.Adapter<OrderCompleteAdapter.OrderCompleteViewHolder>{

    Context context;
    List<MyOrder> myOrderList;

    FirebaseDatabase firebaseDatabase;
    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;

    int totalPrice = 0;

    DecimalFormat decimalFormat = new DecimalFormat("###,###");


    public OrderCompleteAdapter(Context context, List<MyOrder> myOrderList){
        this.context = context;
        this.myOrderList = myOrderList;
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public OrderCompleteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OrderCompleteViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.order_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull OrderCompleteViewHolder holder, int position) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("CurrentUser");

        Glide.with(holder.itemView)
                .load(myOrderList.get(position).getOrderImg())
                .into(holder.pimg_orderitem);
        holder.pName_orderitem.setText(myOrderList.get(position).getProductName());
        holder.pPrice_orderitem.setText(String.valueOf(decimalFormat.format(myOrderList.get(position).getTotalPrice())) + "원");
        holder.pQauntity_orderitem.setText(decimalFormat.format(myOrderList.get(position).getTotalQuantity()) + "개");
    }

    @Override
    public int getItemCount() {
        if(myOrderList != null){
            return myOrderList.size();
        }
        return 0;
    }

    public class OrderCompleteViewHolder extends RecyclerView.ViewHolder {
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