package com.example.chaitanya.heatwaverisknotifier;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;


public class MainActivity extends AppCompatActivity {

    private static final String USER_ID = "userid";
    private Fragment mCurrentFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.createNotificationChannel(this);
        Intent pushNotificationIntent = new Intent(this, PushNotificationService.class);
        startService(pushNotificationIntent);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        HomeFragment homeFragment = new HomeFragment();
        mCurrentFragment = homeFragment;
        fragmentTransaction.add(R.id.fragment_frame, homeFragment);
        fragmentTransaction.commit();

        /*if(!preferences.getBoolean(USER_ID, false)){

        }*/

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        Log.i("TORRID_NAV", "Navigating...");
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        switch (menuItem.getItemId()){
                            case R.id.home:
                                mCurrentFragment = new HomeFragment();
                                break;
                            case R.id.profile:
                                mCurrentFragment = new ProfileFragment();
                                break;
                            case R.id.services:
                                mCurrentFragment = new ServicesFragment();
                                break;
                        }
                        fragmentTransaction.replace(R.id.fragment_frame, mCurrentFragment);
                        fragmentTransaction.commit();
                        return true;
                    }
                }
        );

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
    }

}
