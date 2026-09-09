package com.vegan.review;

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

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.CustomViewHolder> {

    private ArrayList<ApiReview> dataList;

    public ReviewAdapter(ArrayList<ApiReview> dataList) {
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_review, parent, false);
        return new CustomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        ApiReview review = dataList.get(position);

        String imgUrl = review.getReviewImageUrl();
        if (imgUrl != null && !imgUrl.isEmpty()) {
            holder.inputimg.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView).load(imgUrl).into(holder.inputimg);
        } else {
            holder.inputimg.setVisibility(View.GONE);
        }

        holder.reviewdes.setText(review.getContent());
        holder.userrating.setRating(review.getRating());
        holder.reviewdate.setText(review.getCreatedAt() != null
                ? review.getCreatedAt().replace("T", " ") : "");
        holder.username.setText(review.getUsername());
        holder.reviewproductname.setText(review.getProductName());
    }

    @Override
    public int getItemCount() {
        return dataList != null ? dataList.size() : 0;
    }

    public static class CustomViewHolder extends RecyclerView.ViewHolder {
        ImageView inputimg;
        RatingBar userrating;
        TextView username, reviewdes, reviewdate, reviewproductname;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            inputimg = itemView.findViewById(R.id.inputimg);
            username = itemView.findViewById(R.id.username);
            reviewdes = itemView.findViewById(R.id.reviewdes);
            userrating = itemView.findViewById(R.id.userrating);
            reviewdate = itemView.findViewById(R.id.reviewdate);
            reviewproductname = itemView.findViewById(R.id.reviewproductname);
        }
    }
}
