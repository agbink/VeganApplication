package com.vegan.Manage;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vegan.R;
import com.vegan.review.Review;

import java.util.ArrayList;

public class ManageReviewAdapter extends RecyclerView.Adapter<ManageReviewAdapter.ManageReviewViewHolder> {
    private ArrayList<Review> arrayList;
    private Context context;
    FirebaseDatabase firebaseDatabase;
    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;
    Dialog dialog;

    public ManageReviewAdapter(ArrayList<Review> arrayList, Context context){
        this.arrayList = arrayList;
        this.context = context;
    }
    @NonNull
    @Override
    public ManageReviewAdapter.ManageReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.manage_review_item, parent, false);
        ManageReviewAdapter.ManageReviewViewHolder holder = new ManageReviewAdapter.ManageReviewViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ManageReviewAdapter.ManageReviewViewHolder holder, @SuppressLint("RecyclerView") int position) {

        if (arrayList.get(position).getRimage() != null && !arrayList.get(position).getRimage().isEmpty()) {
            // 이미지가 있는 경우 표시
            holder.MGReviewInputimg_review.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView)
                    .load(arrayList.get(position).getRimage())
                    .into(holder.MGReviewInputimg_review);
        } else {
            // 이미지가 없는 경우 숨김
            holder.MGReviewInputimg_review.setVisibility(View.GONE);
        }

        holder.MGOrderID_order.setText(arrayList.get(position).getReviewid());
        holder.MGReviewDate_review.setText(arrayList.get(position).getRdatetime());
        holder.MGReviewUsername_review.setText(arrayList.get(position).getUsername());
        holder.MGReviewReviewdes_review.setText(arrayList.get(position).getRcontent());
        holder.MGReviewProductName_review.setText(arrayList.get(position).getPname());

        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm2);

        databaseReference = FirebaseDatabase.getInstance().getReference("Review");

        holder.MGRemoveReview_review.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.show();

                TextView confirmTextView = dialog.findViewById(R.id.confirmTextView);
                confirmTextView.setText("후기를 삭제하시겠습니까?\n삭제 후에는 작업을 되돌릴 수 없습니다.");

                Button btnleft1 = dialog.findViewById(R.id.btn_left);
                btnleft1.setText("취소");
                btnleft1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                Button btnright1 = dialog.findViewById(R.id.btn_right);
                btnright1.setText("확인");
                btnright1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        databaseReference.child(arrayList.get(position).getReviewid())
                                .removeValue()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            arrayList.remove(arrayList.get(position));
                                            notifyDataSetChanged();
                                            Intent intent = ((ManageReviewActivity)context).getIntent();
                                            ((ManageReviewActivity)context).finish(); //현재 액티비티 종료 실시
                                            ((ManageReviewActivity)context).overridePendingTransition(0, 0); //효과 없애기
                                            ((ManageReviewActivity)context).startActivity(intent); //현재 액티비티 재실행 실시
                                            ((ManageReviewActivity)context).overridePendingTransition(0, 0); //효과 없애기
                                        } else {
                                        }
                                    }
                                });
                    }
                });
            }
        });

        holder.MGReviewUserrating_review.setRating(arrayList.get(position).getRscore());
    }

    @Override
    public int getItemCount() {
        if (arrayList != null) {
            return arrayList.size();
        }
        return 0;
    }

    public class ManageReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView MGReviewInputimg_review;
        TextView MGOrderID_order, MGReviewDate_review, MGReviewUsername_review, MGReviewReviewdes_review, MGReviewProductName_review;
        Button MGRemoveReview_review;
        RatingBar MGReviewUserrating_review;
        public ManageReviewViewHolder(@NonNull View itemView) {
            super(itemView);

            this.MGReviewInputimg_review = itemView.findViewById(R.id.MGReviewInputimg_review);
            this.MGOrderID_order = itemView.findViewById(R.id.MGOrderID_order);
            this.MGReviewDate_review = itemView.findViewById(R.id.MGReviewDate_review);
            this.MGReviewUsername_review = itemView.findViewById(R.id.MGReviewUsername_review);
            this.MGReviewReviewdes_review = itemView.findViewById(R.id.MGReviewReviewdes_review);
            this.MGReviewProductName_review = itemView.findViewById(R.id.MGReviewProductName_review);

            this.MGRemoveReview_review = itemView.findViewById(R.id.MGRemoveReview_review);
            this.MGReviewUserrating_review = itemView.findViewById(R.id.MGReviewUserrating_review);

        }
    }
}