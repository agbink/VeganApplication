package com.vegan.main;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainAdapter2#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainAdapter2 extends FragmentStateAdapter {

    public int mCount;
    public MainAdapter2(FragmentActivity fa, int count) {
        super(fa);
        mCount = count;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        int index = getRealPosition(position);

        if(index==0) return new mainslide2_Fg1();
        else if(index==1) return new mainslide2_Fg2();
        else if(index==2) return new mainslide2_Fg3();
        else return null;
    }

    @Override
    public int getItemCount() {
        return 2000;
    }

    public int getRealPosition(int position) { return position % mCount; }
}