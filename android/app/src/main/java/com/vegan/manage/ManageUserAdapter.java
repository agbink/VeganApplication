package com.vegan.manage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vegan.R;
import com.vegan.api.ApiUser;

import java.util.ArrayList;

public class ManageUserAdapter extends RecyclerView.Adapter<ManageUserAdapter.ManageUserViewHolder> {
    private ArrayList<ApiUser> arrayList;
    private Context context;

    public ManageUserAdapter(ArrayList<ApiUser> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public ManageUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.manage_user_item, parent, false);
        return new ManageUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageUserViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ApiUser user = arrayList.get(position);
        holder.MGuserName_user.setText("이름 : " + user.getUsername());
        holder.MGUserID_user.setText("이메일 : " + user.getEmail());
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ManageUserDetailActivity.class);
            intent.putExtra("userId", user.getId());
            intent.putExtra("userEmail", user.getEmail());
            intent.putExtra("userName", user.getUsername());
            intent.putExtra("userPhone", user.getPhone());
            intent.putExtra("userAddress", user.getAddress());
            intent.putExtra("userProvider", user.getProvider());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return arrayList != null ? arrayList.size() : 0; }

    public class ManageUserViewHolder extends RecyclerView.ViewHolder {
        TextView MGuserName_user, MGUserID_user;
        public ManageUserViewHolder(@NonNull View itemView) {
            super(itemView);
            this.MGuserName_user = itemView.findViewById(R.id.MGUserName_user);
            this.MGUserID_user = itemView.findViewById(R.id.MGUserID_user);
        }
    }
}
