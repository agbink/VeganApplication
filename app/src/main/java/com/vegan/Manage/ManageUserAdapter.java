package com.vegan.Manage;

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
import com.vegan.User;

import java.util.ArrayList;

public class ManageUserAdapter extends RecyclerView.Adapter<ManageUserAdapter.ManageUserViewHolder> {
    private ArrayList<User> arrayList;
    private Context context;


    public ManageUserAdapter(ArrayList<User> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public ManageUserAdapter.ManageUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.manage_user_item, parent, false);
        ManageUserAdapter.ManageUserViewHolder holder = new ManageUserAdapter.ManageUserViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ManageUserAdapter.ManageUserViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.MGuserName_user.setText("이름 : " + arrayList.get(position).getUsername());
        holder.MGUserID_user.setText("이메일 : " + arrayList.get(position).getEmailId());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ManageUserDetailActivity.class);
                intent.putExtra("ManageUserDetail", arrayList.get(position));
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (arrayList != null) {
            return arrayList.size();
        }
        return 0;
    }

    public class ManageUserViewHolder extends RecyclerView.ViewHolder {
        TextView MGuserName_user, MGUserID_user;

        public ManageUserViewHolder(@NonNull View itemView) {
            super(itemView);
            this.MGuserName_user = itemView.findViewById(R.id.MGUserName_user);
            this.MGUserID_user = itemView.findViewById(R.id.MGUserID_user);
        }
    }
}