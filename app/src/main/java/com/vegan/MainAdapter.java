package com.vegan;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainAdapter extends FragmentStateAdapter {
    public int mCount;
    public MainAdapter(FragmentActivity fa, int count) {
        super(fa);
        mCount = count;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MainAdapter.
     */
    // TODO: Rename and change types and number of parameters
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        int index = getRealPosition(position);

        if(index==0) return new mainslide1_Fg1();
        else if(index==1) return new mainslide1_Fg2();
        else if(index==2) return new mainslide1_Fg3();
        else return null;
    }

    @Override
    public int getItemCount() {
        return 2000;
    }

    public int getRealPosition(int position) { return position % mCount; }
}
