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

import java.util.ArrayList;

public class ReviewHistoryAdapter extends RecyclerView.Adapter<ReviewHistoryAdapter.ReviewHistoryViewHolder> {
    private ArrayList<Review> reviewhistoryList;
    private Context context;


    public ReviewHistoryAdapter(ArrayList<Review> reviewhistoryList, Context context) {
        this.reviewhistoryList = reviewhistoryList;
        this.context = context;
    }

    @NonNull
    @Override
    public ReviewHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.review_history_list, parent, false);
        ReviewHistoryViewHolder holder = new ReviewHistoryViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewHistoryViewHolder holder, @SuppressLint("RecyclerView") int position) {

        if (reviewhistoryList.get(position).getRimage() != null && !reviewhistoryList.get(position).getRimage().isEmpty()) {
            // 이미지가 있는 경우 표시
            holder.recyclerImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView)
                    .load(reviewhistoryList.get(position).getRimage())
                    .into(holder.recyclerImage);
        } else {
            // 이미지가 없는 경우 숨김
            holder.recyclerImage.setVisibility(View.GONE);
        }

        holder.recyclerEt.setText(String.valueOf(reviewhistoryList.get(position).getRcontent()));
        holder.reviewdate.setText(String.valueOf(reviewhistoryList.get(position).getRdatetime()));
        holder.ProductPrice.setText(String.valueOf(reviewhistoryList.get(position).getPprice()) + "원");
        holder.ProductName.setText(String.valueOf(reviewhistoryList.get(position).getPname()));
        holder.TotalQ.setText(String.valueOf(reviewhistoryList.get(position).getTotalquantity())+ "개");
        Glide.with(holder.itemView).load(reviewhistoryList.get(position).getPimg()).into(holder.ProductImg);
        holder.RHUserrating_review.setRating(reviewhistoryList.get(position).getRscore());

    }

    @Override
    public int getItemCount() {
        if (reviewhistoryList != null) {
            return reviewhistoryList.size();
        }
        return 0;
    }

    public class ReviewHistoryViewHolder extends RecyclerView.ViewHolder {
        private ImageView recyclerImage;
        private TextView recyclerEt;
        //private RatingBar recyclerRating;
        private TextView reviewdate;

        private TextView ProductName;
        private ImageView ProductImg;

        private TextView ProductPrice; //추가
        private TextView TotalQ; //추가
        private RatingBar RHUserrating_review;



        public ReviewHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            this.recyclerImage = itemView.findViewById(R.id.reviewhistoryPhoto);
            this.recyclerEt = itemView.findViewById(R.id.reviewhistoryText);
            //this.recyclerRating = itemView.findViewById(R.id.reviewhistoryRate);
            this.reviewdate = itemView.findViewById(R.id.reviewhistoryDate);
            this.ProductPrice = itemView.findViewById(R.id.reviewhistoryprice);
            this.ProductName = itemView.findViewById(R.id.reviewhistoryPn);
            this.ProductImg = itemView.findViewById(R.id.reviewhistoryPImg);
            this.TotalQ = itemView.findViewById(R.id.reviewhistoryquantity);
            this.RHUserrating_review = itemView.findViewById(R.id.RHUserrating_review);

        }
    }
}