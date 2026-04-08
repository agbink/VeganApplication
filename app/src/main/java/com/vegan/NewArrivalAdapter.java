package com.vegan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class NewArrivalAdapter extends RecyclerView.Adapter<NewArrivalAdapter.CustomViewHolder> {
    private ArrayList<ItemsDomain> arrayList;
    private Context context;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    private DatabaseReference databaseReferenceReview;

    DecimalFormat decimalFormat = new DecimalFormat("###,###");

    public NewArrivalAdapter(ArrayList<ItemsDomain> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public NewArrivalAdapter.CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mainlist_item, parent, false);
        CustomViewHolder holder = new CustomViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull NewArrivalAdapter.CustomViewHolder holder, @SuppressLint("RecyclerView") int position) {
        firebaseDatabase = FirebaseDatabase.getInstance();
        ItemsDomain item = arrayList.get(position);

        holder.pbname.setText(item.getPbname());
        holder.pname.setText(item.getPname());
        holder.tv_pprice.setText(decimalFormat.format(item.getPprice()) + "원");

        Glide.with(holder.itemView)
                .load(item.getPimg())
                .into(holder.iv_pimg);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ProductDetailActivity.class);
                intent.putExtra("detail", arrayList.get(position));
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(arrayList.size(), 6);
    }

    public static class CustomViewHolder extends RecyclerView.ViewHolder {
        ImageView iv_pimg;
        TextView pbname;
        TextView pname;
        TextView tv_pprice;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            this.iv_pimg = itemView.findViewById(R.id.iv_pimg);
            this.pbname = itemView.findViewById(R.id.pbname);
            this.pname = itemView.findViewById(R.id.pname);
            this.tv_pprice = itemView.findViewById(R.id.tv_pprice);
        }
    }
}
