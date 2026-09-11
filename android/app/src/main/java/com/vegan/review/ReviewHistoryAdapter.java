package com.vegan.review;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vegan.R;
import com.vegan.api.ApiReview;

import java.util.ArrayList;

public class ReviewHistoryAdapter extends RecyclerView.Adapter<ReviewHistoryAdapter.ReviewHistoryViewHolder> {

    private ArrayList<ApiReview> reviewList;
    private Context context;

    public ReviewHistoryAdapter(ArrayList<ApiReview> reviewList, Context context) {
        this.reviewList = reviewList;
        this.context = context;
    }

    @NonNull
    @Override
    public ReviewHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.review_history_list, parent, false);
        return new ReviewHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewHistoryViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ApiReview review = reviewList.get(position);

        String imgUrl = review.getReviewImageUrl();
        if (imgUrl != null && !imgUrl.isEmpty()) {
            holder.recyclerImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView).load(imgUrl).into(holder.recyclerImage);
        } else {
            holder.recyclerImage.setVisibility(View.GONE);
        }

        holder.recyclerEt.setText(review.getContent());
        holder.reviewdate.setText(review.getCreatedAt() != null
                ? review.getCreatedAt().replace("T", " ") : "");
        holder.ProductName.setText(review.getProductName());
        Glide.with(holder.itemView).load(review.getProductImg()).into(holder.ProductImg);
        holder.RHUserrating_review.setRating(review.getRating());
        // 가격/수량 필드는 ReviewHistoryAdapter에서 표시할 데이터 없음 (API 응답에 없음)
        holder.ProductPrice.setText("");
        holder.TotalQ.setText("");
    }

    @Override
    public int getItemCount() {
        return reviewList != null ? reviewList.size() : 0;
    }

    public class ReviewHistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView recyclerImage, ProductImg;
        TextView recyclerEt, reviewdate, ProductName, ProductPrice, TotalQ;
        RatingBar RHUserrating_review;

        public ReviewHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerImage = itemView.findViewById(R.id.reviewhistoryPhoto);
            recyclerEt = itemView.findViewById(R.id.reviewhistoryText);
            reviewdate = itemView.findViewById(R.id.reviewhistoryDate);
            ProductPrice = itemView.findViewById(R.id.reviewhistoryprice);
            ProductName = itemView.findViewById(R.id.reviewhistoryPn);
            ProductImg = itemView.findViewById(R.id.reviewhistoryPImg);
            TotalQ = itemView.findViewById(R.id.reviewhistoryquantity);
            RHUserrating_review = itemView.findViewById(R.id.RHUserrating_review);
        }
    }
}
