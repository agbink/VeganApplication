package com.vegan.manage;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vegan.R;
import com.vegan.api.ApiReview;
import com.vegan.api.RetrofitClient;
import com.vegan.api.TokenManager;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageReviewAdapter extends RecyclerView.Adapter<ManageReviewAdapter.ManageReviewViewHolder> {
    private ArrayList<ApiReview> arrayList;
    private Context context;

    public ManageReviewAdapter(ArrayList<ApiReview> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public ManageReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.manage_review_item, parent, false);
        return new ManageReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageReviewViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ApiReview review = arrayList.get(position);

        if (review.getReviewImageUrl() != null && !review.getReviewImageUrl().isEmpty()) {
            holder.MGReviewInputimg_review.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView).load(review.getReviewImageUrl()).into(holder.MGReviewInputimg_review);
        } else {
            holder.MGReviewInputimg_review.setVisibility(View.GONE);
        }

        holder.MGOrderID_order.setText(String.valueOf(review.getId()));
        holder.MGReviewDate_review.setText(review.getCreatedAt() != null ? review.getCreatedAt().replace("T", " ") : "");
        holder.MGReviewUsername_review.setText(review.getUsername());
        holder.MGReviewReviewdes_review.setText(review.getContent());
        holder.MGReviewProductName_review.setText(review.getProductName());
        holder.MGReviewUserrating_review.setRating(review.getRating());

        holder.MGRemoveReview_review.setOnClickListener(v -> {
            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_confirm2);
            dialog.show();

            ((TextView) dialog.findViewById(R.id.confirmTextView))
                    .setText("후기를 삭제하시겠습니까?\n삭제 후에는 작업을 되돌릴 수 없습니다.");

            Button btnleft = dialog.findViewById(R.id.btn_left);
            btnleft.setText("취소");
            btnleft.setOnClickListener(x -> dialog.dismiss());

            Button btnright = dialog.findViewById(R.id.btn_right);
            btnright.setText("확인");
            btnright.setOnClickListener(x -> {
                dialog.dismiss();
                String token = TokenManager.getInstance().getToken();
                RetrofitClient.getAdminApi().deleteReview(token, review.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            int pos = arrayList.indexOf(review);
                            if (pos >= 0) {
                                arrayList.remove(pos);
                                notifyItemRemoved(pos);
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("ManageReviewAdapter", "리뷰 삭제 실패", t);
                    }
                });
            });
        });
    }

    @Override
    public int getItemCount() { return arrayList != null ? arrayList.size() : 0; }

    public class ManageReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView MGReviewInputimg_review;
        TextView MGOrderID_order, MGReviewDate_review, MGReviewUsername_review,
                MGReviewReviewdes_review, MGReviewProductName_review;
        Button MGRemoveReview_review;
        RatingBar MGReviewUserrating_review;

        public ManageReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            MGReviewInputimg_review   = itemView.findViewById(R.id.MGReviewInputimg_review);
            MGOrderID_order           = itemView.findViewById(R.id.MGOrderID_order);
            MGReviewDate_review       = itemView.findViewById(R.id.MGReviewDate_review);
            MGReviewUsername_review   = itemView.findViewById(R.id.MGReviewUsername_review);
            MGReviewReviewdes_review  = itemView.findViewById(R.id.MGReviewReviewdes_review);
            MGReviewProductName_review = itemView.findViewById(R.id.MGReviewProductName_review);
            MGRemoveReview_review     = itemView.findViewById(R.id.MGRemoveReview_review);
            MGReviewUserrating_review = itemView.findViewById(R.id.MGReviewUserrating_review);
        }
    }
}
