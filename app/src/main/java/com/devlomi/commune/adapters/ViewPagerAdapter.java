package com.devlomi.commune.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.devlomi.commune.activities.main.calls.CallsFragment;
import com.devlomi.commune.activities.main.chats.FragmentChats;
import com.devlomi.commune.activities.main.status.StatusFragment;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    private int count = 3;

    public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                return new FragmentChats();
            case 1:
                return new StatusFragment();
            case 2:
                return new CallsFragment();
            default:
                throw new IllegalStateException("Not implemented Fragment exception");
        }

    }

    @Override
    public int getCount() {
        return count;

    }



}
