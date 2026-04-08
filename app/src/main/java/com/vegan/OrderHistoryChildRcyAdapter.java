package com.vegan;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.vegan.review.ReviewWriteActivity;

import java.util.ArrayList;

public class OrderHistoryChildRcyAdapter extends RecyclerView.Adapter<OrderHistoryChildRcyAdapter.ChildViewHolder> {

    private ArrayList<MyOrder> childModelArrayList;
    private Context cxt;

    public OrderHistoryChildRcyAdapter(ArrayList<MyOrder> childModelArrayList, Context mContext) {
        this.cxt = mContext;
        this.childModelArrayList = childModelArrayList;
    }

    @Override
    public ChildViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_order_history_child, parent, false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, @SuppressLint("RecyclerView") int position) {
        MyOrder order = childModelArrayList.get(position);

        Glide.with(holder.itemView)
                .load(order.getOrderImg())
                .into(holder.orderhistory_img);
        holder.pro_name.setText(order.getProductName());
        holder.pro_price.setText(order.getProductPrice() + "원");
        holder.ordervalue.setText(order.getTotalQuantity() + "개");

        String state = order.getOrderstate();
        if ("paid".equals(state)) {
            holder.OrderState_orderhistory.setText("결제 완료");
            holder.ordhreviewBtn.setVisibility(View.INVISIBLE);
        } else if ("배송완료".equals(state)) {
            holder.OrderState_orderhistory.setText("배송 완료");
            holder.ordhreviewBtn.setVisibility(View.VISIBLE);
        }

        String isReviewCompleted = childModelArrayList.get(position).getDoReview();
        if ("No".equals(isReviewCompleted)) {

        } else if ("Yes".equals(isReviewCompleted)) {
            holder.ordhreviewBtn.setText("후기 작성완료");
            holder.ordhreviewBtn.setBackgroundTintList(ColorStateList.valueOf(cxt.getResources().getColor(R.color.btn_click))); //버튼색변경
            holder.ordhreviewBtn.setTextColor(cxt.getResources().getColor(R.color.white)); // 글자색 변경
            holder.ordhreviewBtn.setEnabled(false);
        }

        holder.ordhreviewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("No".equals(isReviewCompleted)) {
                    Intent intent = new Intent(cxt, ReviewWriteActivity.class);
                    intent.putExtra("item", childModelArrayList.get(position));
//                    Log.d("myOrderId", String.valueOf(childModelArrayList.get(position)+"가져왔음"));
                    cxt.startActivity(intent);
                    ((Activity)cxt).finish();

                } else if ("Yes".equals(isReviewCompleted)) {

                }
            }
        });

    }


    @Override
    public int getItemCount() {
        return (childModelArrayList != null) ? childModelArrayList.size() : 0;
    }

    public class ChildViewHolder extends RecyclerView.ViewHolder {
        public ImageView orderhistory_img;
        public TextView pro_name, pro_price, ordervalue;
        public TextView OrderState_orderhistory;
        public AppCompatButton ordhreviewBtn;

        public ChildViewHolder(View itemView) {
            super(itemView);
            orderhistory_img = itemView.findViewById(R.id.orderhistory_img);
            pro_name = itemView.findViewById(R.id.pro_name);
            pro_price = itemView.findViewById(R.id.pro_price);
            ordervalue = itemView.findViewById(R.id.ordervalue);
            ordhreviewBtn = itemView.findViewById(R.id.ordhreviewBtn);
            OrderState_orderhistory = itemView.findViewById(R.id.OrderState_orderhistory);
        }
    }
}
