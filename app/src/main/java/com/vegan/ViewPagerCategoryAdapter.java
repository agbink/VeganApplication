package com.vegan;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class ViewPagerCategoryAdapter extends FragmentPagerAdapter {

    public ViewPagerCategoryAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        if (position==0){
            return new Category1Fragment();
        } else if (position==1){
            return new Category2Fragment();
        } else if (position==2){
            return new Category3Fragment();
        } else { //3
            return new Category4Fragment();
        }
    }

    @Override
    public int getCount() {
        return 4; //no. of tabs
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position==0) {
            return "전체";
        } else if (position==1) {
            return "푸드";
        } else if (position==2) {
            return "패션";
        } else { //3
            return "뷰티";
        }
    }
}
