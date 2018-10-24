package com.example.chaitanya.heatwaverisknotifier;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;



public class MainActivity extends AppCompatActivity {

    private Fragment mCurrentFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.createNotificationChannel(this);

        final FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        HomeFragment homeFragment = new HomeFragment();
        mCurrentFragment = homeFragment;
        fragmentTransaction.add(R.id.fragment_frame, homeFragment);
        fragmentTransaction.commit();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        Log.i("TORRID_NAV", "Navigating...");
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.remove(mCurrentFragment);
                        switch (menuItem.getItemId()){
                            case R.id.home:
                                mCurrentFragment = new HomeFragment();
                                break;
                            case R.id.profile:
                                mCurrentFragment = null;
                                break;
                            case R.id.services:
                                mCurrentFragment = new ServicesFragment();
                                break;
                        }
                        fragmentTransaction.add(R.id.fragment_frame, mCurrentFragment);
                        fragmentTransaction.commit();
                        return true;
                    }
                }
        );

    }

}
